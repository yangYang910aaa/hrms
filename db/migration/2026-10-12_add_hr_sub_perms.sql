-- =============================================================================
--  迁移脚本 · 2026-10-12
--  用途：M4 批次2 —— 补齐「人事管理」6 张子表的按钮级权限点，供 @SaCheckPermission 使用
--  背景：菜单 201–206（社保/奖罚/调动/证书/实习生/请假）原先**只有 :list**，
--        没有增删改权限点，接口级校验没法落地。
--  幂等：可重复执行（先按 id 段清理再插入）
--  执行：mysql -D hr_system -u root -p < 本文件
--  说明：id 段 1100–1152 已避开 M3 批次2 用掉的 1000–1052。
--        本脚本一次性补全 6 张表的 18 个权限点；批次2a 先实现社保/奖罚/证书三套，
--        调动/实习生/请假（1120–1152）在批次2b 使用 —— 权限点先备好不影响既有功能。
-- =============================================================================

SET NAMES utf8mb4;
USE `hr_system`;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1100 AND 1152;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 1100 AND 1152;

INSERT INTO `sys_menu` (`id`,`parent_id`,`menu_name`,`menu_type`,`perms`,`sort`,`visible`,`status`) VALUES
  -- 社保管理（201）
  (1100, 201, '社保新增',   'F', 'hr:social:add',        1, 1, 1),
  (1101, 201, '社保修改',   'F', 'hr:social:edit',       2, 1, 1),
  (1102, 201, '社保删除',   'F', 'hr:social:remove',     3, 1, 1),
  -- 奖罚登记（202）
  (1110, 202, '奖罚新增',   'F', 'hr:reward:add',        1, 1, 1),
  (1111, 202, '奖罚修改',   'F', 'hr:reward:edit',       2, 1, 1),
  (1112, 202, '奖罚删除',   'F', 'hr:reward:remove',     3, 1, 1),
  -- 职工调动（203）
  (1120, 203, '调动新增',   'F', 'hr:transfer:add',      1, 1, 1),
  (1121, 203, '调动修改',   'F', 'hr:transfer:edit',     2, 1, 1),
  (1122, 203, '调动删除',   'F', 'hr:transfer:remove',   3, 1, 1),
  -- 证书管理（204）
  (1130, 204, '证书新增',   'F', 'hr:certificate:add',   1, 1, 1),
  (1131, 204, '证书修改',   'F', 'hr:certificate:edit',  2, 1, 1),
  (1132, 204, '证书删除',   'F', 'hr:certificate:remove',3, 1, 1),
  -- 实习生管理（205）
  (1140, 205, '实习生新增', 'F', 'hr:intern:add',        1, 1, 1),
  (1141, 205, '实习生修改', 'F', 'hr:intern:edit',       2, 1, 1),
  (1142, 205, '实习生删除', 'F', 'hr:intern:remove',     3, 1, 1),
  -- 请假管理（206）
  (1150, 206, '请假新增',   'F', 'hr:leave:add',         1, 1, 1),
  (1151, 206, '请假修改',   'F', 'hr:leave:edit',        2, 1, 1),
  (1152, 206, '请假删除',   'F', 'hr:leave:remove',      3, 1, 1);

-- 授权：1=超管（虽走 "*" 通道，补上是为了角色-菜单树里能看到勾选状态，便于将来改成按授权判定）
--       2=人事专员（人事管理全域）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `deleted` = 0 AND `id` BETWEEN 1100 AND 1152;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, `id` FROM `sys_menu` WHERE `deleted` = 0 AND `id` BETWEEN 1100 AND 1152;

SELECT COUNT(*) AS 菜单总数 FROM `sys_menu` WHERE `deleted` = 0;
