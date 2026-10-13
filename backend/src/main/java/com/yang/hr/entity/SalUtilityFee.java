package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 薪酬-水电费登记（sal_utility_fee）
 *
 * 有**复合唯一键** `uk_employee_month(employee_id, month)` —— 一名职工每月至多一条。
 * 逻辑删除无法用文本后缀改写该唯一键，由 Service 把 `employee_id` 置为 **`-id`** 让路
 * （见 SalUtilityFeeServiceImpl.releaseUniqueKey）。
 *
 * `total_fee` 是派生列（= 水费 + 电费 + 住宿费），由服务端计算，**不接受前端传入**。
 */
@Getter
@Setter
@TableName("sal_utility_fee")
public class SalUtilityFee extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 职工ID（hr_employee.id） */
    private Long employeeId;

    /** 所属月份 YYYY-MM */
    private String month;

    /** 水费 */
    private BigDecimal waterFee;

    /** 电费 */
    private BigDecimal electricFee;

    /** 住宿费 */
    private BigDecimal dormFee;

    /** 合计（派生：水 + 电 + 住宿） */
    private BigDecimal totalFee;
}
