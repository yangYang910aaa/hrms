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
 * 水电费登记入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 *
 * 注意：**没有 `totalFee` 字段** —— 合计是派生值（水 + 电 + 住宿），一律服务端计算；
 * 前端就算多传了 totalFee 也不会被绑定，从契约上杜绝"前端算错合计"。
 */
@Getter
@Setter
public class SalUtilityFeeDTO {

    private Long id;

    @NotNull(message = "职工不能为空")
    private Long employeeId;

    /** 所属月份 YYYY-MM */
    @NotBlank(message = "所属月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "所属月份格式应为 YYYY-MM")
    private String month;

    /** 水费；不传默认 0，修改时不传保持原值 */
    @DecimalMin(value = "0", message = "水费不能为负数")
    @Digits(integer = 10, fraction = 2, message = "水费最多 10 位整数、2 位小数")
    private BigDecimal waterFee;

    /** 电费；不传默认 0，修改时不传保持原值 */
    @DecimalMin(value = "0", message = "电费不能为负数")
    @Digits(integer = 10, fraction = 2, message = "电费最多 10 位整数、2 位小数")
    private BigDecimal electricFee;

    /** 住宿费；不传默认 0，修改时不传保持原值 */
    @DecimalMin(value = "0", message = "住宿费不能为负数")
    @Digits(integer = 10, fraction = 2, message = "住宿费最多 10 位整数、2 位小数")
    private BigDecimal dormFee;
}
