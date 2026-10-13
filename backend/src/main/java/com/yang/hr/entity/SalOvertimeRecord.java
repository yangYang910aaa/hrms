package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 薪酬-加班登记（sal_overtime_record）
 *
 * 只有普通索引 `idx_employee_month`，**没有唯一键** —— 同一职工同月可以有多条（不同加班类型 / 不同批次）。
 *
 * `rate` 与 `amount` 都是派生列，由服务端计算，**不接受前端传入**：
 * 时薪 = 职工薪酬级别的 base_salary ÷ 21.75 ÷ 8，倍率取 `sal_overtime_standard`，
 * 加班金额 = 时薪 × 倍率 × 加班小时（见 SalOvertimeRecordServiceImpl）。
 */
@Getter
@Setter
@TableName("sal_overtime_record")
public class SalOvertimeRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 职工ID（hr_employee.id） */
    private Long employeeId;

    /** 所属月份 YYYY-MM */
    private String month;

    /** 加班类型 1工作日 2休息日 3节假日（字典 overtime_type） */
    private Integer overtimeType;

    /** 加班小时 */
    private BigDecimal hours;

    /** 倍率（派生：录入时按薪酬级别 + 加班类型取自 sal_overtime_standard） */
    private BigDecimal rate;

    /** 加班金额（派生：时薪 × 倍率 × 小时） */
    private BigDecimal amount;
}
