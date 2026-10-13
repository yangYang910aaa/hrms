package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 薪酬-工资发放（sal_payment）
 *
 * 有**复合唯一键** `uk_employee_month(employee_id, month)`：一名职工每月一条发放单，
 * 逻辑删除时同样把 `employee_id` 置 **`-id`** 让路（见 SalPaymentServiceImpl）。
 *
 * **状态机**：0 未发放 → 1 已发放。发放时服务端写 `pay_date`（当天），
 * 并把对应工资档案置为 2 已发放 —— 之后该档案不可改、不可重算。
 */
@Getter
@Setter
@TableName("sal_payment")
public class SalPayment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 职工ID（hr_employee.id） */
    private Long employeeId;

    /** 所属月份 YYYY-MM */
    private String month;

    /** 实发工资（生成时从工资档案的 net_salary 带入） */
    private BigDecimal netSalary;

    /** 发放日期（发放时由服务端写当天，不接受前端传入） */
    private LocalDate payDate;

    /** 发放方式 银行代发 / 现金 */
    private String payWay;

    /** 银行账号 */
    private String bankAccount;

    /** 状态 0未发放 1已发放 */
    private Integer status;

    private String remark;
}
