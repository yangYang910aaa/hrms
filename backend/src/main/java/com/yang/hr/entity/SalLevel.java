package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 薪酬-薪酬级别（sal_level）—— 薪酬模块的主档
 *
 * - level_no 有唯一索引 uk_level_no，**删除时改写成 `原值 + '#del#' + id`**（规划书 §9 第 16 条）；
 * - base_salary 是工资计算的起点（基本工资），并被 M5 各处的「加班金额」按 21.75 口径折算时薪。
 */
@Getter
@Setter
@TableName("sal_level")
public class SalLevel extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 级别名称 */
    private String levelName;

    /** 级别编号（唯一，删除时改写以防唯一键冲突） */
    private String levelNo;

    /** 基本工资 */
    private BigDecimal baseSalary;

    private String remark;
}
