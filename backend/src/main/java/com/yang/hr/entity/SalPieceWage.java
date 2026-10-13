package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 薪酬-计件工资（sal_piece_wage）
 *
 * 只有普通索引，**没有唯一键** —— 同一职工同月可以有多条（多个产品/工序）。
 * `amount` 是派生列（= 数量 × 单价），由服务端计算，**不接受前端传入**。
 */
@Getter
@Setter
@TableName("sal_piece_wage")
public class SalPieceWage extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 职工ID（hr_employee.id） */
    private Long employeeId;

    /** 所属月份 YYYY-MM */
    private String month;

    /** 产品/工序名称 */
    private String productName;

    /** 数量 */
    private BigDecimal quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 金额（派生：数量 × 单价） */
    private BigDecimal amount;
}
