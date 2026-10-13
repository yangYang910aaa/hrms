-- =============================================================================
--  迁移脚本 · 2026-10-13
--  用途：M4.5 培训管理 —— 补齐「培训管理」3 张表的按钮级权限点，供 @SaCheckPermission 使用
--  背景：菜单 600/601/602（课程管理/内部培训/一线工培训）原先**只有 :list**，
--        没有增删改权限点，接口级校验没法落地。
--  幂等：可重复执行（先按 id 段清理再插入）
--  执行：mysql -D hr_system -u root -p < 本文件
--  说明：id 段 1200–1222 已避开 M3（1000–1052）与 M4（1100–1152）用掉的区间。
-- =============================================================================

SET NAMES utf8mb4;
USE `hr_system`;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1200 AND 1222;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 1200 AND 1222;

INSERT INTO `sys_menu` (`id`,`parent_id`,`menu_name`,`menu_type`,`perms`,`sort`,`visible`,`status`) VALUES
  -- 课程管理（600）
  (1200, 600, '课程新增',   'F', 'trn:course:add',    1, 1, 1),
  (1201, 600, '课程修改',   'F', 'trn:course:edit',   2, 1, 1),
  (1202, 600, '课程删除',   'F', 'trn:course:remove', 3, 1, 1),
  -- 内部培训（601）
  (1210, 601, '内部培训新增', 'F', 'trn:internal:add',    1, 1, 1),
  (1211, 601, '内部培训修改', 'F', 'trn:internal:edit',   2, 1, 1),
  (1212, 601, '内部培训删除', 'F', 'trn:internal:remove', 3, 1, 1),
  -- 一线工培训（602）
  (1220, 602, '一线工培训新增', 'F', 'trn:frontline:add',    1, 1, 1),
  (1221, 602, '一线工培训修改', 'F', 'trn:frontline:edit',   2, 1, 1),
  (1222, 602, '一线工培训删除', 'F', 'trn:frontline:remove', 3, 1, 1);

-- 授权：1=超管（虽走 "*" 通道，补上是为了角色-菜单树里能看到勾选状态，便于将来改成按授权判定）
--       2=人事专员（人事管理 + 培训管理 + 公共查询）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `deleted` = 0 AND `id` BETWEEN 1200 AND 1222;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, `id` FROM `sys_menu` WHERE `deleted` = 0 AND `id` BETWEEN 1200 AND 1222;

SELECT COUNT(*) AS 菜单总数 FROM `sys_menu` WHERE `deleted` = 0;
