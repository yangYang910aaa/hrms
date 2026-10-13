-- =============================================================================
-- 迁移脚本 · 2026-10-13（全面复查加固）
-- 用途：补 hr_intern.employee_id 索引 —— 该列是唯一没有索引的 employee_id
--       （其余 10 处 employee_id 均有索引或联合唯一键），实习生按职工反查会全表扫。
-- 幂等：可重复执行（先查 information_schema，索引已存在则跳过）
-- 执行：mysql --default-character-set=utf8mb4 -D hr_system -u root -p < 本文件
-- =============================================================================

SET NAMES utf8mb4;
USE `hr_system`;

SET @add_idx = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'hr_intern'
      AND index_name = 'idx_employee_id') = 0,
  'ALTER TABLE `hr_intern` ADD INDEX `idx_employee_id` (`employee_id`)',
  'SELECT 1');
PREPARE stmt FROM @add_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
