package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 薪酬级别下拉项（/api/common/salary-level-options 的返回元素）
 *
 * 职工表单（选薪酬级别）与加班标准表单（选级别）共用；带上基本工资便于前端直接显示。
 */
@Getter
@Setter
public class SalaryLevelOptionVO {

    private Long id;

    private String levelNo;

    private String levelName;

    /** 基本工资（前端展示/参考用） */
    private BigDecimal baseSalary;
}
