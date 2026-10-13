package com.yang.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 计件工资入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 *
 * 注意：**没有 `amount` 字段** —— 金额是派生值（数量 × 单价），一律服务端计算。
 */
@Getter
@Setter
public class SalPieceWageDTO {

    private Long id;

    @NotNull(message = "职工不能为空")
    private Long employeeId;

    /** 所属月份 YYYY-MM */
    @NotBlank(message = "所属月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "所属月份格式应为 YYYY-MM")
    private String month;

    /** 产品/工序名称（可空，支持清空） */
    @Size(max = 100, message = "产品/工序名称长度不能超过 100")
    private String productName;

    /** 数量（对应列 DECIMAL(10,2)，即 8 位整数 + 2 位小数）；不传默认 0，修改时不传保持原值 */
    @DecimalMin(value = "0", message = "数量不能为负数")
    @Digits(integer = 8, fraction = 2, message = "数量最多 8 位整数、2 位小数")
    private BigDecimal quantity;

    /** 单价（对应列 DECIMAL(10,2)，即 8 位整数 + 2 位小数）；不传默认 0，修改时不传保持原值 */
    @DecimalMin(value = "0", message = "单价不能为负数")
    @Digits(integer = 8, fraction = 2, message = "单价最多 8 位整数、2 位小数")
    private BigDecimal unitPrice;
}
