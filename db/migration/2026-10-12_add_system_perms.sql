-- =============================================================================
--  迁移脚本 · 2026-10-12
--  用途：M3 批次2 —— 补齐「系统设置」模块的按钮级权限点，供 @SaCheckPermission 使用
--  幂等：可重复执行（先按 id 清理再插入）
--  说明：原 data.sql 只种了 system:user:* 与 hr:employee:* 两组按钮权限，
--        部门/角色/菜单/字典/社保比例 只有 :list 没有增删改，接口级校验没法落地。
--  执行：mysql -D hr_system -u root -p < 本文件
-- =============================================================================

SET NAMES utf8mb4;
USE `hr_system`;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1008 AND 1052;
DELETE FROM `sys_menu` WHERE `id` = 1008 OR `id` BETWEEN 1010 AND 1052;

INSERT INTO `sys_menu` (`id`,`parent_id`,`menu_name`,`menu_type`,`perms`,`sort`,`visible`,`status`) VALUES
  -- 用户管理（101）补充：分配角色
  (1008, 101, '分配角色',     'F', 'system:user:assign',       5, 1, 1),
  -- 组织结构（100）
  (1010, 100, '部门新增',     'F', 'system:dept:add',          1, 1, 1),
  (1011, 100, '部门修改',     'F', 'system:dept:edit',         2, 1, 1),
  (1012, 100, '部门删除',     'F', 'system:dept:remove',       3, 1, 1),
  -- 角色管理（102）
  (1020, 102, '角色新增',     'F', 'system:role:add',          1, 1, 1),
  (1021, 102, '角色修改',     'F', 'system:role:edit',         2, 1, 1),
  (1022, 102, '角色删除',     'F', 'system:role:remove',       3, 1, 1),
  (1023, 102, '分配菜单',     'F', 'system:role:assign',       4, 1, 1),
  -- 菜单管理（103）
  (1030, 103, '菜单新增',     'F', 'system:menu:add',          1, 1, 1),
  (1031, 103, '菜单修改',     'F', 'system:menu:edit',         2, 1, 1),
  (1032, 103, '菜单删除',     'F', 'system:menu:remove',       3, 1, 1),
  -- 数据字典（104）
  (1040, 104, '字典新增',     'F', 'system:dict:add',          1, 1, 1),
  (1041, 104, '字典修改',     'F', 'system:dict:edit',         2, 1, 1),
  (1042, 104, '字典删除',     'F', 'system:dict:remove',       3, 1, 1),
  -- 社保比例（105）
  (1050, 105, '比例新增',     'F', 'system:social-rate:add',   1, 1, 1),
  (1051, 105, '比例修改',     'F', 'system:social-rate:edit',  2, 1, 1),
  (1052, 105, '比例删除',     'F', 'system:social-rate:remove',3, 1, 1);

-- 超管角色补授权：超管在 StpInterface 里走 "*" 全权通道，这里补上是为了数据一致
-- （角色-菜单树里能看到勾选状态，也便于将来改成"按授权判定"而不必补数据）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `deleted` = 0 AND (`id` = 1008 OR `id` BETWEEN 1010 AND 1052);

SELECT COUNT(*) AS 菜单总数 FROM `sys_menu` WHERE `deleted` = 0;
