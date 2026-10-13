package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 薪酬-计时工资（sal_time_wage）
 *
 * 只有普通索引，**没有唯一键** —— 同一职工同月可以有多条。
 * `amount` 是派生列（= 工时 × 时薪），由服务端计算，**不接受前端传入**。
 */
@Getter
@Setter
@TableName("sal_time_wage")
public class SalTimeWage extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 职工ID（hr_employee.id） */
    private Long employeeId;

    /** 所属月份 YYYY-MM */
    private String month;

    /** 时薪 */
    private BigDecimal hourlyRate;

    /** 工时 */
    private BigDecimal hours;

    /** 金额（派生：工时 × 时薪） */
    private BigDecimal amount;
}
