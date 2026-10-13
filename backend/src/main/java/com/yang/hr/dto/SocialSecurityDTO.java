package com.yang.hr.dto;

import jakarta.validation.constraints.DecimalMax;
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
 * 职工社保入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class SocialSecurityDTO {

    private Long id;

    @NotNull(message = "不能为空")
    private Long employeeId;

    @Size(max = 30, message = "长度不能超过 30")
    private String ssNo;

    /** 缴费基数 */
    @DecimalMin(value = "0", message = "不能为负数")
    @Digits(integer = 10, fraction = 2, message = "最多 10 位整数、2 位小数")
    private BigDecimal baseAmount;

    /** 单位比例，小数表示：16% 填 0.16（列 DECIMAL(6,4)，超宽会撞 MySQL 落 500，靠 @Digits 拦成 400） */
    @DecimalMin(value = "0", message = "不能为负数")
    @DecimalMax(value = "1", message = "应为小数（16% 请填 0.16）")
    @Digits(integer = 2, fraction = 4, message = "最多 2 位整数、4 位小数")
    private BigDecimal companyRate;

    /** 个人比例，小数表示：8% 填 0.08（列 DECIMAL(6,4)） */
    @DecimalMin(value = "0", message = "不能为负数")
    @DecimalMax(value = "1", message = "应为小数（8% 请填 0.08）")
    @Digits(integer = 2, fraction = 4, message = "最多 2 位整数、4 位小数")
    private BigDecimal personalRate;

    /** 单位缴纳额：不填则按「基数 × 单位比例」自动计算 */
    @DecimalMin(value = "0", message = "不能为负数")
    @Digits(integer = 10, fraction = 2, message = "最多 10 位整数、2 位小数")
    private BigDecimal companyAmount;

    /** 个人缴纳额：不填则按「基数 × 个人比例」自动计算 */
    @DecimalMin(value = "0", message = "不能为负数")
    @Digits(integer = 10, fraction = 2, message = "最多 10 位整数、2 位小数")
    private BigDecimal personalAmount;

    /** 所属月份 YYYY-MM */
    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "格式应为 YYYY-MM")
    private String month;
}
