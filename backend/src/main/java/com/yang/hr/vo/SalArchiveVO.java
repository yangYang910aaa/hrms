package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工资档案视图：档案字段 + 联表带出的职工工号/姓名/在职状态与薪酬级别名。
 *
 * 列表与详情共用；所有金额列都是**服务端算好落库的值**（前端只读展示）。
 */
@Getter
@Setter
public class SalArchiveVO {

    private Long id;

    private Long employeeId;

    /** 联表带出：职工工号 */
    private String empNo;

    /** 联表带出：职工姓名 */
    private String employeeName;

    /** 联表带出：在职状态（1 在职 / 2 实习 / 3 离职） */
    private Integer employType;

    private String month;

    /** 计算时的薪酬级别快照 */
    private Long levelId;

    /** 联表带出：薪酬级别名称 */
    private String levelName;

    private BigDecimal baseSalary;

    private BigDecimal overtimeAmount;

    private BigDecimal pieceAmount;

    private BigDecimal timeAmount;

    private BigDecimal rewardAmount;

    private BigDecimal socialSecurity;

    private BigDecimal utilityFee;

    /** 其他扣款（默认 0，可手改） */
    private BigDecimal deductAmount;

    /** 个税（默认 0，可手改） */
    private BigDecimal tax;

    private BigDecimal netSalary;

    /** 状态 0草稿 1已确认 2已发放 */
    private Integer status;

    private LocalDateTime calcTime;

    private LocalDateTime createTime;

    private Long createBy;
}
