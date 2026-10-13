package com.yang.hr.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 社保比例入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class SocialRateDTO {

    private Long id;

    @NotBlank(message = "不能为空")
    @Size(max = 30, message = "长度不能超过 30")
    private String rateName;

    @NotBlank(message = "不能为空")
    @Size(max = 30, message = "长度不能超过 30")
    private String rateCode;

    /** 单位比例，小数表示：16% 填 0.16（列 DECIMAL(6,4)，超宽会撞 MySQL 落 500，靠 @Digits 拦成 400） */
    @DecimalMin(value = "0", message = "不能为负数")
    @DecimalMax(value = "1", message = "应为小数（16% 请填 0.16）")
    @Digits(integer = 2, fraction = 4, message = "最多 2 位整数、4 位小数")
    private BigDecimal companyRate;

    /** 个人比例，小数表示：8% 填 0.08；工伤/生育等仅单位缴纳的可填 0（列 DECIMAL(6,4)） */
    @DecimalMin(value = "0", message = "不能为负数")
    @DecimalMax(value = "1", message = "应为小数（8% 请填 0.08）")
    @Digits(integer = 2, fraction = 4, message = "最多 2 位整数、4 位小数")
    private BigDecimal personalRate;

    /** 缴费基数下限；0 表示未配置。两端都大于 0 时才校验"下限≤上限"（Service 做跨字段校验） */
    @DecimalMin(value = "0", message = "不能为负数")
    @Digits(integer = 10, fraction = 2, message = "最多 10 位整数、2 位小数")
    private BigDecimal baseMin;

    /** 缴费基数上限；0 表示不限制（该列 NOT NULL，用 0 当"未填"哨兵） */
    @DecimalMin(value = "0", message = "不能为负数")
    @Digits(integer = 10, fraction = 2, message = "最多 10 位整数、2 位小数")
    private BigDecimal baseMax;

    /** 生效月份，格式 YYYY-MM */
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "格式应为 YYYY-MM")
    private String effectiveMonth;

    private Integer sort;

    private Integer status;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;
}
