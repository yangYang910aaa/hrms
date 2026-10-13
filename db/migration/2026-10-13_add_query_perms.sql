-- =============================================================================
--  迁移脚本 · 2026-10-13
--  用途：M6 批次5 —— 补齐第 6 模块「报表统计 + 公共查询」的按钮级权限点
--  背景：菜单实测
--    · 报表统计(4) → 统计图表(400, report:chart:list)     —— 只读，:list 够用
--    · 公共查询(5) → 职工查询(500, query:employee:list)   —— 只读，:list 够用
--    · 公共查询(5) → 通知管理(501, query:notice:list)     —— 要做增删改/发布，缺 4 个权限点
--        原先「通知管理」只有 :list 一个权限点，@SaCheckPermission 落地不了，故本脚本补齐。
--  幂等：可重复执行（先按 id 段清理再插入）
--  执行：mysql -D hr_system -u root -p < 本文件
--  说明：id 段 1400–1449 已避开 system(1008–1052)、hr(1100–1152)、trn(1200–1222)、sal(1300–1373)。
-- =============================================================================

SET NAMES utf8mb4;
USE `hr_system`;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1400 AND 1449;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 1400 AND 1449;

INSERT INTO `sys_menu` (`id`,`parent_id`,`menu_name`,`menu_type`,`perms`,`sort`,`visible`,`status`) VALUES
  -- 通知管理（501）：通知打印只读，不单开 print 权限点（能看即能打），故只补这 4 个
  (1400, 501, '通知新增', 'F', 'query:notice:add',     1, 1, 1),
  (1401, 501, '通知修改', 'F', 'query:notice:edit',    2, 1, 1),
  (1402, 501, '通知删除', 'F', 'query:notice:remove',  3, 1, 1),
  (1403, 501, '通知发布', 'F', 'query:notice:publish', 4, 1, 1);

-- 授权：1=超管（虽走 "*" 通道，补上是为了角色-菜单树里能看到勾选状态）
--       2=人事专员（公共查询 → 通知管理，它已有 501 菜单）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `deleted` = 0 AND `id` BETWEEN 1400 AND 1449;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, `id` FROM `sys_menu` WHERE `deleted` = 0 AND `id` BETWEEN 1400 AND 1449;

SELECT COUNT(*) AS 菜单总数 FROM `sys_menu` WHERE `deleted` = 0;
