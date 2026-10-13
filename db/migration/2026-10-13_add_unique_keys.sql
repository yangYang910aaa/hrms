-- =============================================================================
-- 迁移脚本 · 2026-10-13（全面复查加固 · 第二批：唯一键收口）
-- 用途：
--   1. hr_social_security 的 (employee_id, month) 加唯一键 —— 此前仅 Service 级校验，
--      并发登记可穿透产生重复记录，直接污染 M5 工资计算（社保重复扣缴 / 取数不稳定）。
--   2. sys_menu.perms 加唯一键 —— perms 是整套鉴权的键，重复即授权歧义；此前仅应用层校验。
-- 配套代码改动（同批提交，缺一不可）：
--   - SocialSecurityServiceImpl.delete：逻辑删除前把 employee_id 置 -id 让路
--     （软删行仍占唯一索引，不让路则删后同月不能重新登记）；
--   - MenuServiceImpl.delete：逻辑删除前把 perms 改写为「截断原值 + #del# + id」让路。
-- 幂等：可重复执行（信息字典判断 + 一次性数据修正本身可重跑）
-- 执行：mysql --default-character-set=utf8mb4 -D hr_system -u root -p < 本文件
-- =============================================================================

SET NAMES utf8mb4;
USE `hr_system`;

-- ---------- 1. hr_social_security：(employee_id, month) 唯一键 ----------
-- 1a. 一次性数据修正：历史软删行仍带正 employee_id，会占住唯一键 → 让路（-id 负值永不撞真实值）
UPDATE hr_social_security SET employee_id = -id WHERE deleted = 1 AND employee_id > 0;

-- 1b. 非唯一索引换唯一键（唯一键自带索引，drop 旧的避免冗余）
SET @drop_idx = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'hr_social_security'
      AND index_name = 'idx_employee_month') > 0,
  'ALTER TABLE hr_social_security DROP INDEX idx_employee_month',
  'SELECT 1');
PREPARE s1 FROM @drop_idx; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @add_uk = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'hr_social_security'
      AND index_name = 'uk_employee_month') = 0,
  'ALTER TABLE hr_social_security ADD UNIQUE KEY uk_employee_month (employee_id, month)',
  'SELECT 1');
PREPARE s2 FROM @add_uk; EXECUTE s2; DEALLOCATE PREPARE s2;

-- ---------- 2. sys_menu.perms 唯一键 ----------
-- 2a. 一次性数据修正：历史软删行的 perms 仍占唯一键 → 改写让路（列宽 100，LEFT 截断防超长）
UPDATE sys_menu
SET perms = CONCAT(LEFT(perms, 100 - CHAR_LENGTH(CONCAT('#del#', id))), '#del#', id)
WHERE deleted = 1 AND perms IS NOT NULL AND perms <> '' AND perms NOT LIKE '%#del#%';

-- 2b. 非唯一索引换唯一键
SET @drop_idx2 = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sys_menu'
      AND index_name = 'idx_perms') > 0,
  'ALTER TABLE sys_menu DROP INDEX idx_perms',
  'SELECT 1');
PREPARE s3 FROM @drop_idx2; EXECUTE s3; DEALLOCATE PREPARE s3;

SET @add_uk2 = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sys_menu'
      AND index_name = 'uk_perms') = 0,
  'ALTER TABLE sys_menu ADD UNIQUE KEY uk_perms (perms)',
  'SELECT 1');
PREPARE s4 FROM @add_uk2; EXECUTE s4; DEALLOCATE PREPARE s4;
