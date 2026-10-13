-- =============================================================================
--  迁移脚本 · 2026-10-13
--  用途：页面文件改成语义化命名（**文件名 = 所在目录名**），同步 sys_menu.component
--  背景：前端 views/**/index.vue 全叫 index.vue，文件树里 8 个同名文件根本认不出谁是谁；
--        统一改成 <目录名>.vue —— system/dept/index → system/dept/Dept、
--        hr/employee/index → hr/employee/Employee、system/social-rate/index →
--        system/social-rate/SocialRate（连字符转帕斯卡）。
--        ⚠️ 前端 store/permission.ts 的映射规则是 '../views/${component}.vue'，
--        component 与文件名**必须同时改**，只改一边页面就会掉到占位页。
--        本次把**全部 27 个 C 菜单**都改掉（含尚未实现页面的 20 条），
--        避免"已实现用新约定、待实现用旧约定"的半截状态。
--  幂等：可重复执行 —— 条件里带旧值，第二次跑不再命中（0 rows affected）
--  执行：mysql -D hr_system -u root -p --default-character-set=utf8mb4 < 本文件
-- =============================================================================

SET NAMES utf8mb4;
USE `hr_system`;

-- 系统设置（目录 1）
UPDATE `sys_menu` SET `component` = 'system/dept/Dept'                WHERE `id` = 100 AND `component` = 'system/dept/index';
UPDATE `sys_menu` SET `component` = 'system/user/User'                WHERE `id` = 101 AND `component` = 'system/user/index';
UPDATE `sys_menu` SET `component` = 'system/role/Role'                WHERE `id` = 102 AND `component` = 'system/role/index';
UPDATE `sys_menu` SET `component` = 'system/menu/Menu'                WHERE `id` = 103 AND `component` = 'system/menu/index';
UPDATE `sys_menu` SET `component` = 'system/dict/Dict'                WHERE `id` = 104 AND `component` = 'system/dict/index';
UPDATE `sys_menu` SET `component` = 'system/social-rate/SocialRate'   WHERE `id` = 105 AND `component` = 'system/social-rate/index';

-- 人事管理（目录 2）
UPDATE `sys_menu` SET `component` = 'hr/employee/Employee'            WHERE `id` = 200 AND `component` = 'hr/employee/index';
UPDATE `sys_menu` SET `component` = 'hr/social/Social'                WHERE `id` = 201 AND `component` = 'hr/social/index';
UPDATE `sys_menu` SET `component` = 'hr/reward/Reward'                WHERE `id` = 202 AND `component` = 'hr/reward/index';
UPDATE `sys_menu` SET `component` = 'hr/transfer/Transfer'            WHERE `id` = 203 AND `component` = 'hr/transfer/index';
UPDATE `sys_menu` SET `component` = 'hr/certificate/Certificate'      WHERE `id` = 204 AND `component` = 'hr/certificate/index';
UPDATE `sys_menu` SET `component` = 'hr/intern/Intern'                WHERE `id` = 205 AND `component` = 'hr/intern/index';
UPDATE `sys_menu` SET `component` = 'hr/leave/Leave'                  WHERE `id` = 206 AND `component` = 'hr/leave/index';

-- 薪酬管理（目录 3）
UPDATE `sys_menu` SET `component` = 'sal/level/Level'                 WHERE `id` = 300 AND `component` = 'sal/level/index';
UPDATE `sys_menu` SET `component` = 'sal/overtime-standard/OvertimeStandard' WHERE `id` = 301 AND `component` = 'sal/overtime-standard/index';
UPDATE `sys_menu` SET `component` = 'sal/utility/Utility'             WHERE `id` = 302 AND `component` = 'sal/utility/index';
UPDATE `sys_menu` SET `component` = 'sal/overtime/Overtime'           WHERE `id` = 303 AND `component` = 'sal/overtime/index';
UPDATE `sys_menu` SET `component` = 'sal/piece/Piece'                 WHERE `id` = 304 AND `component` = 'sal/piece/index';
UPDATE `sys_menu` SET `component` = 'sal/time/Time'                   WHERE `id` = 305 AND `component` = 'sal/time/index';
UPDATE `sys_menu` SET `component` = 'sal/archive/Archive'             WHERE `id` = 306 AND `component` = 'sal/archive/index';
UPDATE `sys_menu` SET `component` = 'sal/payment/Payment'             WHERE `id` = 307 AND `component` = 'sal/payment/index';

-- 报表统计（目录 4）
UPDATE `sys_menu` SET `component` = 'report/chart/Chart'              WHERE `id` = 400 AND `component` = 'report/chart/index';

-- 公共查询（目录 5）
UPDATE `sys_menu` SET `component` = 'query/employee/Employee'         WHERE `id` = 500 AND `component` = 'query/employee/index';
UPDATE `sys_menu` SET `component` = 'query/notice/Notice'             WHERE `id` = 501 AND `component` = 'query/notice/index';

-- 培训管理（目录 6）
UPDATE `sys_menu` SET `component` = 'trn/course/Course'               WHERE `id` = 600 AND `component` = 'trn/course/index';
UPDATE `sys_menu` SET `component` = 'trn/internal/Internal'           WHERE `id` = 601 AND `component` = 'trn/internal/index';
UPDATE `sys_menu` SET `component` = 'trn/frontline/Frontline'         WHERE `id` = 602 AND `component` = 'trn/frontline/index';

-- 校验：应返回 0（没有残留的旧式 '<dir>/index' 写法）
SELECT COUNT(*) AS 残留旧写法 FROM `sys_menu`
 WHERE `deleted` = 0 AND `component` LIKE '%/index';

-- 校验：应返回 27（C 菜单都有 component）
SELECT COUNT(*) AS C菜单带component FROM `sys_menu`
 WHERE `deleted` = 0 AND `menu_type` = 'C' AND `component` IS NOT NULL;
