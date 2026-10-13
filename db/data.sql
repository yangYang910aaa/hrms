-- =============================================================================
--  人力资源管理系统 · 初始数据（DML）
--  依赖 : 先执行 schema.sql
--  ⚠️ 警告 : 本脚本含大量 DELETE FROM —— **只能执行一次，重复执行会清掉已录入的数据**！
--           日常补数据请改走 db/migration/ 下的幂等迁移脚本。
--  内容 : 部门 / 用户 / 角色 / 用户角色 / 菜单(41条) / 角色菜单 / 字典类型+数据
--         社保比例 6 项 + 通知公告示例
--         薪酬级别 + 加班标准（供 M5 工资计算开箱即用）
--         4 名演示职工（供图表与工资计算演示，可随时删除）
--  账号 : admin / ***REMOVED***   （BCrypt $2a$10$ 存储）
-- =============================================================================

SET NAMES utf8mb4;
USE `hr_system`;

-- ---------------------------------------------------------------------------
-- 1. 组织结构：总公司 → 人事部 / 财务部 / 技术部 / 生产部
-- ---------------------------------------------------------------------------
DELETE FROM `sys_dept`;
INSERT INTO `sys_dept` (`id`,`parent_id`,`dept_name`,`dept_code`,`leader`,`sort`,`status`) VALUES
  (1, 0, '总公司', 'HQ',   '总经理', 1, 1),
  (2, 1, '人事部', 'HR',   NULL,     1, 1),
  (3, 1, '财务部', 'FIN',  NULL,     2, 1),
  (4, 1, '技术部', 'TECH', NULL,     3, 1),
  (5, 1, '生产部', 'PROD', NULL,     4, 1);

-- ---------------------------------------------------------------------------
-- 2. 角色
-- ---------------------------------------------------------------------------
DELETE FROM `sys_role`;
INSERT INTO `sys_role` (`id`,`role_name`,`role_code`,`sort`,`status`,`remark`) VALUES
  (1, '超级管理员', 'SUPER_ADMIN',    1, 1, '拥有全部权限'),
  (2, '人事专员',   'HR_SPECIALIST',  2, 1, '人事管理全域 + 职工查询'),
  (3, '薪酬专员',   'SAL_SPECIALIST', 3, 1, '薪酬管理全域');

-- ---------------------------------------------------------------------------
-- 3. 用户（口令均为 ***REMOVED***，BCrypt $2a$ 已实测可校验）
-- ---------------------------------------------------------------------------
DELETE FROM `sys_user`;
INSERT INTO `sys_user` (`id`,`username`,`password`,`real_name`,`dept_id`,`phone`,`email`,`status`) VALUES
  (1, 'admin', '$2a$10$RHXfTed5A3W4Bet0rMc11u6mMAWDTjv0647QxUyR3qzsG0769J1/y', '系统管理员', 1, NULL, NULL, 1);

DELETE FROM `sys_user_role`;
INSERT INTO `sys_user_role` (`id`,`user_id`,`role_id`) VALUES
  (1, 1, 1);

-- ---------------------------------------------------------------------------
-- 4. 菜单 / 权限点（41 条：M目录 6 + C菜单 27 + F按钮 8）
-- ---------------------------------------------------------------------------
DELETE FROM `sys_menu`;
INSERT INTO `sys_menu` (`id`,`parent_id`,`menu_name`,`menu_type`,`path`,`component`,`perms`,`icon`,`sort`,`visible`,`status`) VALUES
  -- 一级目录
  (1,   0, '系统设置', 'M', '/system', NULL, NULL, 'Setting',       1, 1, 1),
  (2,   0, '人事管理', 'M', '/hr',     NULL, NULL, 'User',          2, 1, 1),
  (6,   0, '培训管理', 'M', '/trn',    NULL, NULL, 'Reading',       3, 1, 1),
  (3,   0, '薪酬管理', 'M', '/sal',    NULL, NULL, 'Money',         4, 1, 1),
  (4,   0, '报表统计', 'M', '/report', NULL, NULL, 'DataAnalysis',  5, 1, 1),
  (5,   0, '公共查询', 'M', '/query',  NULL, NULL, 'Search',        6, 1, 1),
  -- 系统设置
  (100, 1, '组织结构', 'C', 'dept', 'system/dept/index',  'system:dept:list', 'OfficeBuilding', 1, 1, 1),
  (101, 1, '用户管理', 'C', 'user', 'system/user/index',  'system:user:list', 'UserFilled',     2, 1, 1),
  (102, 1, '角色管理', 'C', 'role', 'system/role/index',  'system:role:list', 'Avatar',         3, 1, 1),
  (103, 1, '菜单管理', 'C', 'menu', 'system/menu/index',  'system:menu:list', 'Menu',           4, 1, 1),
  (104, 1, '数据字典', 'C', 'dict', 'system/dict/index',  'system:dict:list', 'Collection',     5, 1, 1),
  (105, 1, '社保比例', 'C', 'social-rate', 'system/social-rate/index', 'system:social-rate:list', 'ScaleToOriginal', 6, 1, 1),
  -- 人事管理
  (200, 2, '职工管理',   'C', 'employee',    'hr/employee/index',    'hr:employee:list',    'Postcard', 1, 1, 1),
  (201, 2, '社保管理',   'C', 'social',      'hr/social/index',      'hr:social:list',      'Wallet',   2, 1, 1),
  (202, 2, '奖罚登记',   'C', 'reward',      'hr/reward/index',      'hr:reward:list',      'Trophy',   3, 1, 1),
  (203, 2, '职工调动',   'C', 'transfer',    'hr/transfer/index',    'hr:transfer:list',    'Switch',   4, 1, 1),
  (204, 2, '证书管理',   'C', 'certificate', 'hr/certificate/index', 'hr:certificate:list', 'Medal',    5, 1, 1),
  (205, 2, '实习生管理', 'C', 'intern',      'hr/intern/index',      'hr:intern:list',      'School',   6, 1, 1),
  (206, 2, '请假管理',   'C', 'leave',       'hr/leave/index',       'hr:leave:list',       'Calendar', 7, 1, 1),
  -- 培训管理
  (600, 6, '课程管理',   'C', 'course',    'trn/course/index',    'trn:course:list',    'Notebook', 1, 1, 1),
  (601, 6, '内部培训',   'C', 'internal',  'trn/internal/index',  'trn:internal:list',  'Reading',  2, 1, 1),
  (602, 6, '一线工培训', 'C', 'frontline', 'trn/frontline/index', 'trn:frontline:list', 'Tools',    3, 1, 1),
  -- 薪酬管理
  (300, 3, '薪酬级别',   'C', 'level',             'sal/level/index',             'sal:level:list',             'Coin',       1, 1, 1),
  (301, 3, '加班标准',   'C', 'overtime-standard', 'sal/overtime-standard/index', 'sal:overtime-standard:list', 'Timer',      2, 1, 1),
  (302, 3, '水电费登记', 'C', 'utility',           'sal/utility/index',           'sal:utility:list',           'Lightning',  3, 1, 1),
  (303, 3, '加班登记',   'C', 'overtime',          'sal/overtime/index',          'sal:overtime:list',          'AlarmClock', 4, 1, 1),
  (304, 3, '计件工资',   'C', 'piece',             'sal/piece/index',             'sal:piece:list',             'Box',        5, 1, 1),
  (305, 3, '计时工资',   'C', 'time',              'sal/time/index',              'sal:time:list',              'Clock',      6, 1, 1),
  (306, 3, '工资档案',   'C', 'archive',           'sal/archive/index',           'sal:archive:list',           'Document',   7, 1, 1),
  (307, 3, '工资发放',   'C', 'payment',           'sal/payment/index',           'sal:payment:list',           'CreditCard', 8, 1, 1),
  -- 报表 / 查询
  (400, 4, '统计图表', 'C', 'chart',    'report/chart/index',    'report:chart:list',    'PieChart', 1, 1, 1),
  (500, 5, '职工查询', 'C', 'employee', 'query/employee/index',  'query:employee:list',  'Search',   1, 1, 1),
  (501, 5, '通知管理', 'C', 'notice',   'query/notice/index',    'query:notice:list',    'Bell',     2, 1, 1),
  -- 按钮级权限（示范，可按需扩充）
  (1000, 200, '职工新增', 'F', NULL, NULL, 'hr:employee:add',      NULL, 1, 1, 1),
  (1001, 200, '职工修改', 'F', NULL, NULL, 'hr:employee:edit',     NULL, 2, 1, 1),
  (1002, 200, '职工删除', 'F', NULL, NULL, 'hr:employee:remove',   NULL, 3, 1, 1),
  (1003, 200, '职工导出', 'F', NULL, NULL, 'hr:employee:export',   NULL, 4, 1, 1),
  (1004, 101, '用户新增', 'F', NULL, NULL, 'system:user:add',      NULL, 1, 1, 1),
  (1005, 101, '用户修改', 'F', NULL, NULL, 'system:user:edit',     NULL, 2, 1, 1),
  (1006, 101, '用户删除', 'F', NULL, NULL, 'system:user:remove',   NULL, 3, 1, 1),
  (1007, 101, '重置口令', 'F', NULL, NULL, 'system:user:resetPwd', NULL, 4, 1, 1);

-- ---------------------------------------------------------------------------
-- 5. 角色-菜单
--    角色1 全量 | 角色2 人事管理 + 培训管理 + 公共查询 | 角色3 薪酬管理 + 社保比例
-- ---------------------------------------------------------------------------
DELETE FROM `sys_role_menu`;
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `deleted` = 0;

INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES
  -- 人事专员
  (2, 2), (2, 5), (2, 500), (2, 501),
  (2, 200), (2, 201), (2, 202), (2, 203), (2, 204), (2, 205), (2, 206),
  (2, 1000), (2, 1001), (2, 1002), (2, 1003),
  (2, 6), (2, 600), (2, 601), (2, 602),
  -- 薪酬专员
  (3, 3),
  (3, 300), (3, 301), (3, 302), (3, 303), (3, 304), (3, 305), (3, 306), (3, 307),
  (3, 1), (3, 105);

-- ---------------------------------------------------------------------------
-- 6. 数据字典
-- ---------------------------------------------------------------------------
DELETE FROM `sys_dict_type`;
INSERT INTO `sys_dict_type` (`id`,`dict_name`,`dict_type`,`status`,`remark`) VALUES
  (1, '性别',     'sex',           1, '职工性别'),
  (2, '学历',     'edu_level',     1, '最高学历'),
  (3, '在职状态', 'employ_type',   1, '在职/实习/离职'),
  (4, '加班类型', 'overtime_type', 1, '工作日/休息日/节假日'),
  (5, '请假类型', 'leave_type',    1, NULL),
  (6, '奖罚类型', 'reward_type',   1, NULL);

DELETE FROM `sys_dict_data`;
INSERT INTO `sys_dict_data` (`dict_type`,`dict_label`,`dict_value`,`sort`,`is_default`,`status`) VALUES
  ('sex',           '男',         '1', 1, 1, 1),
  ('sex',           '女',         '2', 2, 0, 1),
  ('edu_level',     '初中及以下', '1', 1, 1, 1),
  ('edu_level',     '高中/中专',  '2', 2, 0, 1),
  ('edu_level',     '大专',       '3', 3, 0, 1),
  ('edu_level',     '本科',       '4', 4, 0, 1),
  ('edu_level',     '硕士',       '5', 5, 0, 1),
  ('edu_level',     '博士',       '6', 6, 0, 1),
  ('employ_type',   '在职',       '1', 1, 1, 1),
  ('employ_type',   '实习',       '2', 2, 0, 1),
  ('employ_type',   '离职',       '3', 3, 0, 1),
  ('overtime_type', '工作日加班', '1', 1, 0, 1),
  ('overtime_type', '休息日加班', '2', 2, 0, 1),
  ('overtime_type', '节假日加班', '3', 3, 0, 1),
  ('leave_type',    '事假',       '1', 1, 1, 1),
  ('leave_type',    '病假',       '2', 2, 0, 1),
  ('leave_type',    '年假',       '3', 3, 0, 1),
  ('leave_type',    '调休',       '4', 4, 0, 1),
  ('leave_type',    '婚假',       '5', 5, 0, 1),
  ('leave_type',    '产假',       '6', 6, 0, 1),
  ('reward_type',   '奖励',       '1', 1, 1, 1),
  ('reward_type',   '处罚',       '2', 2, 0, 1);

-- ---------------------------------------------------------------------------
-- 7. 薪酬级别 + 加班标准（倍率 1.5 / 2.0 / 3.0）
-- ---------------------------------------------------------------------------
DELETE FROM `sal_level`;
INSERT INTO `sal_level` (`id`,`level_name`,`level_no`,`base_salary`,`remark`) VALUES
  (1, '一级（初级）', 'L1', 4500.00, '应届/初级岗位'),
  (2, '二级（中级）', 'L2', 6500.00, '骨干员工'),
  (3, '三级（高级）', 'L3', 9000.00, '技术/管理岗');

DELETE FROM `sal_overtime_standard`;
INSERT INTO `sal_overtime_standard` (`id`,`level_id`,`overtime_type`,`rate`,`remark`) VALUES
  (1, 1, 1, 1.50, '工作日'), (2, 1, 2, 2.00, '休息日'), (3, 1, 3, 3.00, '节假日'),
  (4, 2, 1, 1.50, '工作日'), (5, 2, 2, 2.00, '休息日'), (6, 2, 3, 3.00, '节假日'),
  (7, 3, 1, 1.50, '工作日'), (8, 3, 2, 2.00, '休息日'), (9, 3, 3, 3.00, '节假日');

-- ---------------------------------------------------------------------------
-- 8. 社保比例配置（供 M5 工资计算自动扣缴；数值为演示口径，可按当地政策改）
-- ---------------------------------------------------------------------------
DELETE FROM `sys_social_rate`;
INSERT INTO `sys_social_rate`
  (`id`,`rate_name`,`rate_code`,`company_rate`,`personal_rate`,`base_min`,`base_max`,`effective_month`,`sort`,`status`,`remark`) VALUES
  (1, '养老保险',   'YL',  0.1600, 0.0800, 4000.00, 35000.00, '2026-01', 1, 1, '单位16% / 个人8%'),
  (2, '医疗保险',   'YB',  0.0950, 0.0200, 4000.00, 35000.00, '2026-01', 2, 1, '单位9.5% / 个人2%'),
  (3, '失业保险',   'SY',  0.0050, 0.0050, 4000.00, 35000.00, '2026-01', 3, 1, '单位0.5% / 个人0.5%'),
  (4, '工伤保险',   'GS',  0.0026, 0.0000, 4000.00, 35000.00, '2026-01', 4, 1, '仅单位缴纳'),
  (5, '生育保险',   'SC',  0.0100, 0.0000, 4000.00, 35000.00, '2026-01', 5, 1, '仅单位缴纳'),
  (6, '住房公积金', 'GJJ', 0.0700, 0.0700, 4000.00, 35000.00, '2026-01', 6, 1, '单位7% / 个人7%');

-- ---------------------------------------------------------------------------
-- 9. 通知公告示例（公共查询-通知管理）
-- ---------------------------------------------------------------------------
DELETE FROM `sys_notice`;
INSERT INTO `sys_notice` (`id`,`title`,`notice_type`,`content`,`publisher`,`publish_time`,`status`,`print_count`) VALUES
  (1, '关于2026年10月工资发放时间调整的通知', 1, '因国庆假期，10月份工资顺延至10月15日发放，请各位职工注意查收。', '系统管理员', '2026-10-08 09:00:00', 1, 0),
  (2, '关于开展第四季度安全生产培训的通知',   1, '定于10月下旬开展一线工安全生产培训，请生产部各班组按名单参加。', '系统管理员', '2026-10-10 14:30:00', 1, 0);

-- ---------------------------------------------------------------------------
-- 10. 演示职工（4 名，仅为图表/工资计算演示，可随时清空）
-- ---------------------------------------------------------------------------
DELETE FROM `hr_employee`;
INSERT INTO `hr_employee`
  (`id`,`emp_no`,`name`,`gender`,`birth_date`,`id_card`,`phone`,`edu_level`,`dept_id`,`position`,`hire_date`,`employ_type`,`salary_level_id`,`status`) VALUES
  (1, 'E1001', '张伟', 1, '1990-05-12', '310101199005121234', '13800000001', '4', 4, 'Java开发工程师', '2020-03-01', 1, 3, 1),
  (2, 'E1002', '李娜', 2, '1995-08-20', '310101199508202345', '13800000002', '4', 2, '人事专员',       '2021-07-15', 1, 2, 1),
  (3, 'E1003', '王强', 1, '1988-02-03', '310101198802033456', '13800000003', '2', 5, '车间班长',       '2015-09-01', 1, 2, 1),
  (4, 'E1004', '赵敏', 2, '2000-11-30', '310101200011304567', '13800000004', '3', 3, '会计',           '2023-06-01', 1, 1, 1);

-- =============================================================================
--  初始数据结束
-- =============================================================================
