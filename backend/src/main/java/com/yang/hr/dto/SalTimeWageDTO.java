package com.yang.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 计时工资入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 *
 * 注意：**没有 `amount` 字段** —— 金额是派生值（工时 × 时薪），一律服务端计算。
 */
@Getter
@Setter
public class SalTimeWageDTO {

    private Long id;

    @NotNull(message = "职工不能为空")
    private Long employeeId;

    /** 所属月份 YYYY-MM */
    @NotBlank(message = "所属月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "所属月份格式应为 YYYY-MM")
    private String month;

    /** 时薪（对应列 DECIMAL(10,2)，即 8 位整数 + 2 位小数）；不传默认 0，修改时不传保持原值 */
    @DecimalMin(value = "0", message = "时薪不能为负数")
    @Digits(integer = 8, fraction = 2, message = "时薪最多 8 位整数、2 位小数")
    private BigDecimal hourlyRate;

    /** 工时（对应列 DECIMAL(6,1)）；不传默认 0，修改时不传保持原值 */
    @DecimalMin(value = "0", message = "工时不能为负数")
    @Digits(integer = 5, fraction = 1, message = "工时最多 5 位整数、1 位小数")
    private BigDecimal hours;
}
