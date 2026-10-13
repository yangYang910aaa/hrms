package com.yang.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 加班登记入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 *
 * 注意：**没有 `rate` / `amount` 字段** —— 倍率按「薪酬级别 + 加班类型」取自加班标准，
 * 金额按 21.75 口径由服务端算（时薪 = 级别基本工资 ÷ 21.75 ÷ 8；金额 = 时薪 × 倍率 × 小时）。
 * 前端传入的倍率/金额一律不接受，避免绕过服务端口径。
 */
@Getter
@Setter
public class SalOvertimeRecordDTO {

    private Long id;

    @NotNull(message = "职工不能为空")
    private Long employeeId;

    /** 所属月份 YYYY-MM */
    @NotBlank(message = "所属月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "所属月份格式应为 YYYY-MM")
    private String month;

    /** 加班类型 1工作日 2休息日 3节假日 */
    @NotNull(message = "加班类型不能为空")
    @Min(value = 1, message = "加班类型取值非法（1工作日 2休息日 3节假日）")
    @Max(value = 3, message = "加班类型取值非法（1工作日 2休息日 3节假日）")
    private Integer overtimeType;

    /** 加班小时（对应列 DECIMAL(6,1)）；不传默认 0，修改时不传保持原值 */
    @DecimalMin(value = "0", message = "加班小时不能为负数")
    @Digits(integer = 5, fraction = 1, message = "加班小时最多 5 位整数、1 位小数")
    private BigDecimal hours;
}
