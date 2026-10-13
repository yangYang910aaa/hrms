package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 培训-课程管理（trn_course）—— 培训模块的课程主档
 *
 * - course_no 有唯一索引 uk_course_no，**删除时会被改写成 `原值 + '#del#' + id`**（规划书 §9 第 16 条）；
 * - course_type 存编码：1内部培训 2一线工培训 3其他（不进 sys_dict，枚举含义见注释与 DTO）；
 * - hours/credit 是 DECIMAL 小数列，课时 6,1、学分 4,1，入参须带 @Digits 防超列精度。
 */
@Getter
@Setter
@TableName("trn_course")
public class TrnCourse extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程编号（唯一，删除时改写以防唯一键冲突） */
    private String courseNo;

    private String courseName;

    /** 类别 1内部培训 2一线工培训 3其他 */
    private Integer courseType;

    /** 讲师 */
    private String teacher;

    /** 课时 */
    private BigDecimal hours;

    /** 学分 */
    private BigDecimal credit;

    /** 课程简介 */
    private String content;

    /** 状态 1启用 0停用 */
    private Integer status;

    private String remark;
}
