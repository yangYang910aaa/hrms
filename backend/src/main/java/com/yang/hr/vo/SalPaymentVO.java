package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工资发放视图：发放单字段 + 联表带出的职工工号/姓名
 */
@Getter
@Setter
public class SalPaymentVO {

    private Long id;

    private Long employeeId;

    /** 联表带出：职工工号 */
    private String empNo;

    /** 联表带出：职工姓名 */
    private String employeeName;

    private String month;

    /** 实发工资（生成时从工资档案带入） */
    private BigDecimal netSalary;

    /** 发放日期（发放时由服务端写当天） */
    private LocalDate payDate;

    /** 发放方式 银行代发 / 现金 */
    private String payWay;

    /** 银行账号 */
    private String bankAccount;

    /** 状态 0未发放 1已发放 */
    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
