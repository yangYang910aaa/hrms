-- =============================================================================
--  人力资源管理系统 · 数据库建表脚本（DDL）
--  库     : hr_system
--  引擎   : InnoDB      字符集 : utf8mb4 / utf8mb4_0900_ai_ci
--  表数   : 28 张  →  sys_* 10 张 | hr_* 7 张 | trn_* 3 张 | sal_* 8 张
--  执行   : 用 root 建表；应用运行用 hr_app（仅 hr_system.*）
--  ⚠️ 警告 : 每张表都是 DROP TABLE IF EXISTS 再 CREATE —— **重复执行会清空全库数据**！
--           这是"一次性建库"脚本；日常加数据请改走 db/migration/ 下的幂等迁移脚本。
--  说明   : 不建物理外键（MyBatis-Plus 项目惯例，关系由代码与索引维护），
--           全部表含公共字段 create_time / update_time / create_by / deleted。
--  配套   : 初始数据见同目录 data.sql
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `hr_system`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `hr_system`;

-- =============================================================================
--  一、系统设置（10 张）
-- =============================================================================

DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`   BIGINT      NOT NULL DEFAULT 0        COMMENT '父部门ID，0 表示根',
  `dept_name`   VARCHAR(50) NOT NULL                  COMMENT '部门名称',
  `dept_code`   VARCHAR(50) DEFAULT NULL              COMMENT '部门编码',
  `leader`      VARCHAR(30) DEFAULT NULL              COMMENT '负责人',
  `phone`       VARCHAR(20) DEFAULT NULL              COMMENT '联系电话',
  `sort`        INT         NOT NULL DEFAULT 0        COMMENT '显示顺序',
  `status`      TINYINT     NOT NULL DEFAULT 1        COMMENT '状态 1正常 0停用',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT      DEFAULT NULL              COMMENT '创建人ID',
  `deleted`     TINYINT     NOT NULL DEFAULT 0        COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统-组织结构/部门（树形）';

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        VARCHAR(50)  NOT NULL                 COMMENT '登录账号',
  `password`        VARCHAR(100) NOT NULL                 COMMENT '口令（BCrypt $2a$）',
  `real_name`       VARCHAR(30)  NOT NULL                 COMMENT '姓名',
  `dept_id`         BIGINT       DEFAULT NULL             COMMENT '所属部门',
  `phone`           VARCHAR(20)  DEFAULT NULL             COMMENT '手机号',
  `email`           VARCHAR(50)  DEFAULT NULL             COMMENT '邮箱',
  `avatar`          VARCHAR(255) DEFAULT NULL             COMMENT '头像地址',
  `status`          TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1正常 0停用',
  `last_login_time` DATETIME     DEFAULT NULL             COMMENT '最后登录时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`       BIGINT       DEFAULT NULL             COMMENT '创建人ID',
  `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统-用户';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name`   VARCHAR(30) NOT NULL                  COMMENT '角色名称',
  `role_code`   VARCHAR(50) NOT NULL                  COMMENT '角色标识',
  `sort`        INT         NOT NULL DEFAULT 0        COMMENT '显示顺序',
  `status`      TINYINT     NOT NULL DEFAULT 1        COMMENT '状态 1正常 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL             COMMENT '备注',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT      DEFAULT NULL,
  `deleted`     TINYINT     NOT NULL DEFAULT 0        COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统-角色';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL                 COMMENT '用户ID',
  `role_id`     BIGINT   NOT NULL                 COMMENT '角色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT   DEFAULT NULL,
  `deleted`     TINYINT  NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统-用户角色关联';

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0        COMMENT '父菜单ID，0 表示根',
  `menu_name`   VARCHAR(50)  NOT NULL                  COMMENT '菜单名称',
  `menu_type`   CHAR(1)      NOT NULL DEFAULT 'C'      COMMENT 'M目录 C菜单 F按钮',
  `path`        VARCHAR(200) DEFAULT NULL              COMMENT '路由地址',
  `component`   VARCHAR(255) DEFAULT NULL              COMMENT '组件路径',
  `perms`       VARCHAR(100) DEFAULT NULL              COMMENT '权限标识',
  `icon`        VARCHAR(100) DEFAULT NULL              COMMENT '图标',
  `sort`        INT          NOT NULL DEFAULT 0        COMMENT '显示顺序',
  `visible`     TINYINT      NOT NULL DEFAULT 1        COMMENT '是否显示 1显示 0隐藏',
  `status`      TINYINT      NOT NULL DEFAULT 1        COMMENT '状态 1正常 0停用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT       DEFAULT NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0        COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  UNIQUE KEY `uk_perms` (`perms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统-菜单/权限点';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id`     BIGINT   NOT NULL                 COMMENT '角色ID',
  `menu_id`     BIGINT   NOT NULL                 COMMENT '菜单ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT   DEFAULT NULL,
  `deleted`     TINYINT  NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统-角色菜单关联';

DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_name`   VARCHAR(50)  NOT NULL                 COMMENT '字典名称',
  `dict_type`   VARCHAR(50)  NOT NULL                 COMMENT '字典类型标识',
  `status`      TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1正常 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT       DEFAULT NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统-字典类型';

DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_type`   VARCHAR(50)  NOT NULL                 COMMENT '字典类型标识',
  `dict_label`  VARCHAR(50)  NOT NULL                 COMMENT '字典标签',
  `dict_value`  VARCHAR(50)  NOT NULL                 COMMENT '字典键值',
  `sort`        INT          NOT NULL DEFAULT 0       COMMENT '显示顺序',
  `is_default`  TINYINT      NOT NULL DEFAULT 0       COMMENT '是否默认 1是 0否',
  `status`      TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1正常 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT       DEFAULT NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统-字典数据';

DROP TABLE IF EXISTS `sys_social_rate`;
CREATE TABLE `sys_social_rate` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rate_name`       VARCHAR(30)   NOT NULL                 COMMENT '险种名称（养老保险/住房公积金…）',
  `rate_code`       VARCHAR(30)   NOT NULL                 COMMENT '险种编码',
  `company_rate`    DECIMAL(6,4)  NOT NULL DEFAULT 0.0000  COMMENT '单位比例',
  `personal_rate`   DECIMAL(6,4)  NOT NULL DEFAULT 0.0000  COMMENT '个人比例',
  `base_min`        DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '缴费基数下限',
  `base_max`        DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '缴费基数上限',
  `effective_month` CHAR(7)       DEFAULT NULL             COMMENT '生效月份 YYYY-MM',
  `sort`            INT           NOT NULL DEFAULT 0       COMMENT '显示顺序',
  `status`          TINYINT       NOT NULL DEFAULT 1       COMMENT '状态 1启用 0停用',
  `remark`          VARCHAR(255)  DEFAULT NULL,
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`       BIGINT        DEFAULT NULL,
  `deleted`         TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rate_code` (`rate_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统-社保比例配置';

DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`        VARCHAR(100) NOT NULL                 COMMENT '标题',
  `notice_type`  TINYINT      NOT NULL DEFAULT 1       COMMENT '类型 1通知 2公告',
  `content`      TEXT                                  COMMENT '正文',
  `publisher`    VARCHAR(30)  DEFAULT NULL             COMMENT '发布人',
  `publish_time` DATETIME     DEFAULT NULL             COMMENT '发布时间',
  `status`       TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 0草稿 1已发布 2已下线',
  `print_count`  INT          NOT NULL DEFAULT 0       COMMENT '打印次数（通知打印）',
  `remark`       VARCHAR(255) DEFAULT NULL,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`    BIGINT       DEFAULT NULL,
  `deleted`      TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`,`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统-通知公告';

-- =============================================================================
--  二、人事管理（7 张）
-- =============================================================================

DROP TABLE IF EXISTS `hr_employee`;
CREATE TABLE `hr_employee` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `emp_no`          VARCHAR(30)  NOT NULL                 COMMENT '工号（唯一）',
  `name`            VARCHAR(30)  NOT NULL                 COMMENT '姓名',
  `gender`          TINYINT      DEFAULT NULL             COMMENT '性别 1男 2女（字典 sex）',
  `birth_date`      DATE         DEFAULT NULL             COMMENT '出生日期',
  `id_card`         VARCHAR(18)  DEFAULT NULL             COMMENT '身份证号',
  `phone`           VARCHAR(20)  DEFAULT NULL             COMMENT '联系电话',
  `email`           VARCHAR(50)  DEFAULT NULL             COMMENT '邮箱',
  `address`         VARCHAR(200) DEFAULT NULL             COMMENT '联系地址',
  `edu_level`       VARCHAR(20)  DEFAULT NULL             COMMENT '学历（字典 edu_level）',
  `dept_id`         BIGINT       DEFAULT NULL             COMMENT '所属部门',
  `position`        VARCHAR(50)  DEFAULT NULL             COMMENT '岗位',
  `hire_date`       DATE         DEFAULT NULL             COMMENT '入职日期',
  `leave_date`      DATE         DEFAULT NULL             COMMENT '离职日期（离职率/辞职统计用）',
  `employ_type`     TINYINT      NOT NULL DEFAULT 1       COMMENT '在职状态 1在职 2实习 3离职（字典 employ_type）',
  `salary_level_id` BIGINT       DEFAULT NULL             COMMENT '薪酬级别（关联 sal_level.id，用于工资计算）',
  `status`          TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1正常 0停用',
  `photo`           VARCHAR(255) DEFAULT NULL             COMMENT '照片地址',
  `remark`          VARCHAR(255) DEFAULT NULL,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`       BIGINT       DEFAULT NULL,
  `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_no` (`emp_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_name` (`name`),
  KEY `idx_employ_type` (`employ_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人事-职工主档';

DROP TABLE IF EXISTS `hr_social_security`;
CREATE TABLE `hr_social_security` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id`     BIGINT        NOT NULL                 COMMENT '职工ID',
  `ss_no`           VARCHAR(30)   DEFAULT NULL             COMMENT '社保编号',
  `base_amount`     DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '缴费基数',
  `company_rate`    DECIMAL(6,4)  NOT NULL DEFAULT 0.0000  COMMENT '单位比例',
  `personal_rate`   DECIMAL(6,4)  NOT NULL DEFAULT 0.0000  COMMENT '个人比例',
  `company_amount`  DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '单位缴纳额',
  `personal_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '个人缴纳额',
  `month`           CHAR(7)       NOT NULL                 COMMENT '所属月份 YYYY-MM',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`       BIGINT        DEFAULT NULL,
  `deleted`         TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_month` (`employee_id`,`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人事-职工社保';

DROP TABLE IF EXISTS `hr_reward_punish`;
CREATE TABLE `hr_reward_punish` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` BIGINT        NOT NULL                 COMMENT '职工ID',
  `type`        TINYINT       NOT NULL                 COMMENT '类型 1奖励 2处罚（字典 reward_type）',
  `reason`      VARCHAR(255)  NOT NULL                 COMMENT '事由',
  `amount`      DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '金额',
  `happen_date` DATE          DEFAULT NULL             COMMENT '发生日期',
  `operator`    VARCHAR(30)   DEFAULT NULL             COMMENT '经办人',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT        DEFAULT NULL,
  `deleted`     TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人事-奖罚登记';

DROP TABLE IF EXISTS `hr_transfer`;
CREATE TABLE `hr_transfer` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id`   BIGINT       NOT NULL                 COMMENT '职工ID',
  `from_dept_id`  BIGINT       DEFAULT NULL             COMMENT '原部门',
  `to_dept_id`    BIGINT       DEFAULT NULL             COMMENT '新部门',
  `from_position` VARCHAR(50)  DEFAULT NULL             COMMENT '原岗位',
  `to_position`   VARCHAR(50)  DEFAULT NULL             COMMENT '新岗位',
  `transfer_date` DATE         DEFAULT NULL             COMMENT '调动日期',
  `reason`        VARCHAR(255) DEFAULT NULL             COMMENT '调动原因',
  `approver`      VARCHAR(30)  DEFAULT NULL             COMMENT '审批人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`     BIGINT       DEFAULT NULL,
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人事-职工调动';

DROP TABLE IF EXISTS `hr_certificate`;
CREATE TABLE `hr_certificate` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` BIGINT       NOT NULL                 COMMENT '职工ID',
  `cert_name`   VARCHAR(100) NOT NULL                 COMMENT '证书名称',
  `cert_no`     VARCHAR(50)  DEFAULT NULL             COMMENT '证书编号',
  `issue_org`   VARCHAR(100) DEFAULT NULL             COMMENT '发证机构',
  `issue_date`  DATE         DEFAULT NULL             COMMENT '发证日期',
  `expire_date` DATE         DEFAULT NULL             COMMENT '有效期至',
  `file_url`    VARCHAR(255) DEFAULT NULL             COMMENT '扫描件地址',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT       DEFAULT NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人事-证书管理';

DROP TABLE IF EXISTS `hr_intern`;
CREATE TABLE `hr_intern` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(30)  NOT NULL                 COMMENT '姓名',
  `school`      VARCHAR(100) DEFAULT NULL             COMMENT '学校',
  `major`       VARCHAR(50)  DEFAULT NULL             COMMENT '专业',
  `dept_id`     BIGINT       DEFAULT NULL             COMMENT '实习部门',
  `mentor`      VARCHAR(30)  DEFAULT NULL             COMMENT '带教导师',
  `start_date`  DATE         DEFAULT NULL             COMMENT '开始日期',
  `end_date`    DATE         DEFAULT NULL             COMMENT '结束日期',
  `status`      TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1实习中 2已结束 3已转正',
  `employee_id` BIGINT       DEFAULT NULL             COMMENT '转正后关联职工ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT       DEFAULT NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人事-实习生管理';

DROP TABLE IF EXISTS `hr_leave`;
CREATE TABLE `hr_leave` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id`    BIGINT       NOT NULL                 COMMENT '职工ID',
  `leave_type`     TINYINT      NOT NULL                 COMMENT '请假类型（字典 leave_type）',
  `start_time`     DATETIME     NOT NULL                 COMMENT '开始时间',
  `end_time`       DATETIME     NOT NULL                 COMMENT '结束时间',
  `days`           DECIMAL(5,1) NOT NULL DEFAULT 0.0     COMMENT '请假天数',
  `reason`         VARCHAR(255) DEFAULT NULL             COMMENT '请假事由',
  `status`         TINYINT      NOT NULL DEFAULT 0       COMMENT '状态 0待审批 1已通过 2已驳回',
  `approver`       VARCHAR(30)  DEFAULT NULL             COMMENT '审批人',
  `approve_time`   DATETIME     DEFAULT NULL             COMMENT '审批时间',
  `approve_remark` VARCHAR(255) DEFAULT NULL             COMMENT '审批意见',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`      BIGINT       DEFAULT NULL,
  `deleted`        TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人事-请假管理';

-- =============================================================================
--  三、培训管理（3 张）
-- =============================================================================

DROP TABLE IF EXISTS `trn_course`;
CREATE TABLE `trn_course` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_no`   VARCHAR(30)  NOT NULL                 COMMENT '课程编号（唯一）',
  `course_name` VARCHAR(100) NOT NULL                 COMMENT '课程名称',
  `course_type` TINYINT      NOT NULL DEFAULT 1       COMMENT '类别 1内部培训 2一线工培训 3其他',
  `teacher`     VARCHAR(30)  DEFAULT NULL             COMMENT '讲师',
  `hours`       DECIMAL(6,1) NOT NULL DEFAULT 0.0     COMMENT '课时',
  `credit`      DECIMAL(4,1) NOT NULL DEFAULT 0.0     COMMENT '学分',
  `content`     TEXT                                  COMMENT '课程简介',
  `status`      TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT       DEFAULT NULL,
  `deleted`     TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_no` (`course_no`),
  KEY `idx_course_name` (`course_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训-课程管理';

DROP TABLE IF EXISTS `trn_internal`;
CREATE TABLE `trn_internal` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id`      BIGINT       NOT NULL                 COMMENT '培训课程ID',
  `batch_no`       VARCHAR(30)  DEFAULT NULL             COMMENT '期次编号',
  `dept_id`        BIGINT       DEFAULT NULL             COMMENT '主办/参训部门',
  `trainer`        VARCHAR(30)  DEFAULT NULL             COMMENT '讲师',
  `place`          VARCHAR(100) DEFAULT NULL             COMMENT '培训地点',
  `start_date`     DATE         DEFAULT NULL             COMMENT '开始日期',
  `end_date`       DATE         DEFAULT NULL             COMMENT '结束日期',
  `plan_persons`   INT          NOT NULL DEFAULT 0       COMMENT '计划人数',
  `actual_persons` INT          NOT NULL DEFAULT 0       COMMENT '实到人数',
  `status`         TINYINT      NOT NULL DEFAULT 0       COMMENT '状态 0计划 1进行中 2已完成',
  `remark`         VARCHAR(255) DEFAULT NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`      BIGINT       DEFAULT NULL,
  `deleted`        TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_start_date` (`start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训-内部培训';

DROP TABLE IF EXISTS `trn_frontline`;
CREATE TABLE `trn_frontline` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id`       BIGINT       NOT NULL                 COMMENT '培训课程ID',
  `batch_no`        VARCHAR(30)  DEFAULT NULL             COMMENT '期次编号',
  `dept_id`         BIGINT       NOT NULL                 COMMENT '参训（生产）部门',
  `trainer`         VARCHAR(30)  DEFAULT NULL             COMMENT '讲师',
  `start_date`      DATE         DEFAULT NULL             COMMENT '开始日期',
  `end_date`        DATE         DEFAULT NULL             COMMENT '结束日期',
  `worker_count`    INT          NOT NULL DEFAULT 0       COMMENT '参训人数',
  `qualified_count` INT          NOT NULL DEFAULT 0       COMMENT '合格人数',
  `status`          TINYINT      NOT NULL DEFAULT 0       COMMENT '状态 0计划 1进行中 2已完成',
  `remark`          VARCHAR(255) DEFAULT NULL,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`       BIGINT       DEFAULT NULL,
  `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训-一线工培训';

-- =============================================================================
--  四、薪酬管理（8 张）
-- =============================================================================

DROP TABLE IF EXISTS `sal_level`;
CREATE TABLE `sal_level` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `level_name`  VARCHAR(50)   NOT NULL                 COMMENT '级别名称',
  `level_no`    VARCHAR(30)   DEFAULT NULL             COMMENT '级别编号',
  `base_salary` DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '基本工资',
  `remark`      VARCHAR(255)  DEFAULT NULL,
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT        DEFAULT NULL,
  `deleted`     TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_no` (`level_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪酬-薪酬级别';

DROP TABLE IF EXISTS `sal_overtime_standard`;
CREATE TABLE `sal_overtime_standard` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `level_id`      BIGINT       NOT NULL                 COMMENT '薪酬级别ID',
  `overtime_type` TINYINT      NOT NULL                 COMMENT '加班类型 1工作日 2休息日 3节假日（字典 overtime_type）',
  `rate`          DECIMAL(5,2) NOT NULL DEFAULT 1.00    COMMENT '倍率',
  `remark`        VARCHAR(255) DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`     BIGINT       DEFAULT NULL,
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_type` (`level_id`,`overtime_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪酬-加班标准';

DROP TABLE IF EXISTS `sal_utility_fee`;
CREATE TABLE `sal_utility_fee` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id`  BIGINT        NOT NULL                 COMMENT '职工ID',
  `month`        CHAR(7)       NOT NULL                 COMMENT '所属月份 YYYY-MM',
  `water_fee`    DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '水费',
  `electric_fee` DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '电费',
  `dorm_fee`     DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '住宿费',
  `total_fee`    DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '合计',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`    BIGINT        DEFAULT NULL,
  `deleted`      TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_month` (`employee_id`,`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪酬-水电费登记';

DROP TABLE IF EXISTS `sal_overtime_record`;
CREATE TABLE `sal_overtime_record` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id`   BIGINT        NOT NULL                 COMMENT '职工ID',
  `month`         CHAR(7)       NOT NULL                 COMMENT '所属月份 YYYY-MM',
  `overtime_type` TINYINT       NOT NULL                 COMMENT '加班类型 1工作日 2休息日 3节假日',
  `hours`         DECIMAL(6,1)  NOT NULL DEFAULT 0.0     COMMENT '加班小时',
  `rate`          DECIMAL(5,2)  NOT NULL DEFAULT 1.00    COMMENT '倍率（录入时取自标准）',
  `amount`        DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '加班金额',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`     BIGINT        DEFAULT NULL,
  `deleted`       TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_employee_month` (`employee_id`,`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪酬-加班登记';

DROP TABLE IF EXISTS `sal_piece_wage`;
CREATE TABLE `sal_piece_wage` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id`  BIGINT        NOT NULL                 COMMENT '职工ID',
  `month`        CHAR(7)       NOT NULL                 COMMENT '所属月份 YYYY-MM',
  `product_name` VARCHAR(100)  DEFAULT NULL             COMMENT '产品/工序名称',
  `quantity`     DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT '数量',
  `unit_price`   DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT '单价',
  `amount`       DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '金额',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`    BIGINT        DEFAULT NULL,
  `deleted`      TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_employee_month` (`employee_id`,`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪酬-计件工资';

DROP TABLE IF EXISTS `sal_time_wage`;
CREATE TABLE `sal_time_wage` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` BIGINT        NOT NULL                 COMMENT '职工ID',
  `month`       CHAR(7)       NOT NULL                 COMMENT '所属月份 YYYY-MM',
  `hourly_rate` DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT '时薪',
  `hours`       DECIMAL(6,1)  NOT NULL DEFAULT 0.0     COMMENT '工时',
  `amount`      DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '金额',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT        DEFAULT NULL,
  `deleted`     TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_employee_month` (`employee_id`,`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪酬-计时工资';

DROP TABLE IF EXISTS `sal_archive`;
CREATE TABLE `sal_archive` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id`     BIGINT        NOT NULL                 COMMENT '职工ID',
  `month`           CHAR(7)       NOT NULL                 COMMENT '所属月份 YYYY-MM',
  `level_id`        BIGINT        DEFAULT NULL             COMMENT '当月薪酬级别',
  `base_salary`     DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '基本工资',
  `overtime_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '加班工资',
  `piece_amount`    DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '计件工资',
  `time_amount`     DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '计时工资',
  `reward_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '奖罚调整（奖+ 罚-）',
  `social_security` DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '社保个人扣缴',
  `utility_fee`     DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '水电费扣缴',
  `deduct_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '其他扣款',
  `tax`             DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '个税',
  `net_salary`      DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '实发工资',
  `status`          TINYINT       NOT NULL DEFAULT 0       COMMENT '状态 0草稿 1已确认 2已发放',
  `calc_time`       DATETIME      DEFAULT NULL             COMMENT '计算时间',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`       BIGINT        DEFAULT NULL,
  `deleted`         TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_month` (`employee_id`,`month`),
  KEY `idx_month` (`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪酬-工资档案（计算主表）';

DROP TABLE IF EXISTS `sal_payment`;
CREATE TABLE `sal_payment` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id`  BIGINT        NOT NULL                 COMMENT '职工ID',
  `month`        CHAR(7)       NOT NULL                 COMMENT '所属月份 YYYY-MM',
  `net_salary`   DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '实发工资',
  `pay_date`     DATE          DEFAULT NULL             COMMENT '发放日期',
  `pay_way`      VARCHAR(20)   DEFAULT NULL             COMMENT '发放方式 银行代发/现金',
  `bank_account` VARCHAR(30)   DEFAULT NULL             COMMENT '银行账号',
  `status`       TINYINT       NOT NULL DEFAULT 0       COMMENT '状态 0未发放 1已发放',
  `remark`       VARCHAR(255)  DEFAULT NULL,
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`    BIGINT        DEFAULT NULL,
  `deleted`      TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_month` (`employee_id`,`month`),
  KEY `idx_month` (`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪酬-工资发放';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
--  DDL 结束：共 28 张表（sys 10 + hr 7 + trn 3 + sal 8）
-- =============================================================================
