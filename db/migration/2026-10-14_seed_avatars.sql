-- ============================================================================
-- 2026-10-14 给演示数据补头像（前端 UI 优化：审计结论第 7 条"每行同一个灰色占位头像"）
--
-- 头像是 AI 生成的扁平插画头像（512x512 PNG，共 21 张：admin + 20 名职工），
-- 存放在 frontend/public/avatars/，由 Vite 静态服务，访问路径 /avatars/<文件>.png。
--
-- ⚠️ 为什么放 frontend/public/ 而不是 backend/uploads/：
--   后者被 .gitignore，别人 clone 仓库后数据库里存的路径会全部指向不存在的文件（裂图）；
--   放 public/ 会随仓库一起提交，演示环境开箱即用。
--   （用户自己上传的照片仍走 /uploads/**，由 FileStorageService 写盘，两者互不影响。）
--
-- hr_employee.photo / sys_user.avatar 存的是前端可直接用的 URL，与上传接口返回值的格式一致。
--
-- 幂等性：只填 NULL / 空串，**不覆盖用户已上传的照片**，重复执行结果不变。
-- 执行方式：root 账号跑，务必带 --default-character-set=utf8mb4。
-- ============================================================================

-- ---------- 系统用户（管理员） ----------
UPDATE `sys_user` SET `avatar` = '/avatars/admin.png'
 WHERE `username` = 'admin' AND `deleted` = 0 AND (`avatar` IS NULL OR `avatar` = '');

-- ---------- 职工（按 emp_no 这个业务键定位，不写死 id） ----------
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1001.png'
 WHERE `emp_no` = 'E1001' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1002.png'
 WHERE `emp_no` = 'E1002' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1003.png'
 WHERE `emp_no` = 'E1003' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1004.png'
 WHERE `emp_no` = 'E1004' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1005.png'
 WHERE `emp_no` = 'E1005' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1006.png'
 WHERE `emp_no` = 'E1006' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1007.png'
 WHERE `emp_no` = 'E1007' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1008.png'
 WHERE `emp_no` = 'E1008' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1009.png'
 WHERE `emp_no` = 'E1009' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1010.png'
 WHERE `emp_no` = 'E1010' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1011.png'
 WHERE `emp_no` = 'E1011' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1012.png'
 WHERE `emp_no` = 'E1012' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1013.png'
 WHERE `emp_no` = 'E1013' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1014.png'
 WHERE `emp_no` = 'E1014' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1015.png'
 WHERE `emp_no` = 'E1015' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1016.png'
 WHERE `emp_no` = 'E1016' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1017.png'
 WHERE `emp_no` = 'E1017' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1018.png'
 WHERE `emp_no` = 'E1018' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1019.png'
 WHERE `emp_no` = 'E1019' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');
UPDATE `hr_employee` SET `photo` = '/avatars/emp-1020.png'
 WHERE `emp_no` = 'E1020' AND `deleted` = 0 AND (`photo` IS NULL OR `photo` = '');

-- ---------- 验收查询 ----------
SELECT `username`, `avatar` FROM `sys_user` WHERE `username` = 'admin';
SELECT COUNT(*) AS filled_employees FROM `hr_employee`
 WHERE `deleted` = 0 AND `photo` LIKE '/avatars/%';
