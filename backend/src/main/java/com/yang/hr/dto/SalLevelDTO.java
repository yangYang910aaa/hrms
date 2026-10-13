package com.yang.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 薪酬级别入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class SalLevelDTO {

    private Long id;

    @NotBlank(message = "级别名称不能为空")
    @Size(max = 50, message = "级别名称长度不能超过 50")
    private String levelName;

    /** 级别编号（唯一；不传则保持原值/为空） */
    @Size(max = 30, message = "级别编号长度不能超过 30")
    private String levelNo;

    /** 基本工资；不传默认 0，修改时不传保持原值 */
    @DecimalMin(value = "0", message = "基本工资不能为负数")
    @Digits(integer = 10, fraction = 2, message = "基本工资最多 10 位整数、2 位小数")
    private BigDecimal baseSalary;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
