-- =============================================================================
--  迁移脚本 · 2026-10-13
--  用途：M5 薪酬管理 —— 补齐「薪酬管理」8 个菜单的按钮级权限点，供 @SaCheckPermission 使用
--  背景：菜单 300–307（薪酬级别/加班标准/水电费/加班登记/计件/计时/工资档案/工资发放）
--        原先**只有 :list**，没有增删改权限点；工资档案/发放还各需 1–2 个动作点。
--  幂等：可重复执行（先按 id 段清理再插入）
--  执行：mysql --default-character-set=utf8mb4 -D hr_system -u root -p < 本文件
--  说明：id 段 1300–1399 已避开 M3（1000–1052）/ M4（1100–1152）/ M4.5（1200–1222）。
-- =============================================================================

SET NAMES utf8mb4;
USE `hr_system`;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1300 AND 1399;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 1300 AND 1399;

INSERT INTO `sys_menu` (`id`,`parent_id`,`menu_name`,`menu_type`,`perms`,`sort`,`visible`,`status`) VALUES
  -- 薪酬级别（300）
  (1300, 300, '薪酬级别新增', 'F', 'sal:level:add',    1, 1, 1),
  (1301, 300, '薪酬级别修改', 'F', 'sal:level:edit',   2, 1, 1),
  (1302, 300, '薪酬级别删除', 'F', 'sal:level:remove', 3, 1, 1),
  -- 加班标准（301）
  (1310, 301, '加班标准新增', 'F', 'sal:overtime-standard:add',    1, 1, 1),
  (1311, 301, '加班标准修改', 'F', 'sal:overtime-standard:edit',   2, 1, 1),
  (1312, 301, '加班标准删除', 'F', 'sal:overtime-standard:remove', 3, 1, 1),
  -- 水电费登记（302）
  (1320, 302, '水电费新增', 'F', 'sal:utility:add',    1, 1, 1),
  (1321, 302, '水电费修改', 'F', 'sal:utility:edit',   2, 1, 1),
  (1322, 302, '水电费删除', 'F', 'sal:utility:remove', 3, 1, 1),
  -- 加班登记（303）
  (1330, 303, '加班登记新增', 'F', 'sal:overtime:add',    1, 1, 1),
  (1331, 303, '加班登记修改', 'F', 'sal:overtime:edit',   2, 1, 1),
  (1332, 303, '加班登记删除', 'F', 'sal:overtime:remove', 3, 1, 1),
  -- 计件工资（304）
  (1340, 304, '计件工资新增', 'F', 'sal:piece:add',    1, 1, 1),
  (1341, 304, '计件工资修改', 'F', 'sal:piece:edit',   2, 1, 1),
  (1342, 304, '计件工资删除', 'F', 'sal:piece:remove', 3, 1, 1),
  -- 计时工资（305）
  (1350, 305, '计时工资新增', 'F', 'sal:time:add',    1, 1, 1),
  (1351, 305, '计时工资修改', 'F', 'sal:time:edit',   2, 1, 1),
  (1352, 305, '计时工资删除', 'F', 'sal:time:remove', 3, 1, 1),
  -- 工资档案（306）—— 另有「计算」「确认」两个动作点
  (1360, 306, '工资档案新增', 'F', 'sal:archive:add',     1, 1, 1),
  (1361, 306, '工资档案修改', 'F', 'sal:archive:edit',    2, 1, 1),
  (1362, 306, '工资档案删除', 'F', 'sal:archive:remove',  3, 1, 1),
  (1363, 306, '工资计算',     'F', 'sal:archive:calc',    4, 1, 1),
  (1364, 306, '工资确认',     'F', 'sal:archive:confirm', 5, 1, 1),
  -- 工资发放（307）—— 另有「发放」动作点
  (1370, 307, '发放单新增', 'F', 'sal:payment:add',    1, 1, 1),
  (1371, 307, '发放单修改', 'F', 'sal:payment:edit',   2, 1, 1),
  (1372, 307, '发放单删除', 'F', 'sal:payment:remove', 3, 1, 1),
  (1373, 307, '工资发放',   'F', 'sal:payment:pay',    4, 1, 1);

-- 授权：1=超管（虽走 "*" 通道，补上是为了角色-菜单树能看到勾选状态）；3=薪酬专员（薪酬管理全域）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `deleted` = 0 AND `id` BETWEEN 1300 AND 1399;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 3, `id` FROM `sys_menu` WHERE `deleted` = 0 AND `id` BETWEEN 1300 AND 1399;

SELECT COUNT(*) AS 菜单总数 FROM `sys_menu` WHERE `deleted` = 0;
