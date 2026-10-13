package com.yang.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 加班标准入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class SalOvertimeStandardDTO {

    private Long id;

    /** 薪酬级别ID（必填，需存在） */
    @NotNull(message = "薪酬级别不能为空")
    private Long levelId;

    /** 加班类型 1工作日 2休息日 3节假日 */
    @NotNull(message = "加班类型不能为空")
    @Min(value = 1, message = "加班类型取值非法（1工作日 2休息日 3节假日）")
    @Max(value = 3, message = "加班类型取值非法（1工作日 2休息日 3节假日）")
    private Integer overtimeType;

    /** 倍率（对应列 DECIMAL(5,2)）；不传默认 1.00，修改时不传保持原值 */
    @DecimalMin(value = "0", message = "倍率不能为负数")
    @Digits(integer = 3, fraction = 2, message = "倍率最多 3 位整数、2 位小数")
    private BigDecimal rate;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
