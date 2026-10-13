package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 人事-职工社保（hr_social_security）
 *
 * 比例字段存小数（0.1600 = 16%），与 sys_social_rate 一致。
 * 单位/个人缴纳额在不填时由 Service 按「基数 × 比例」自动算（HALF_UP 保留 2 位）。
 * 无唯一业务键：删除直接逻辑删除，不需要改写键。
 */
@Getter
@Setter
@TableName("hr_social_security")
public class HrSocialSecurity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    /** 社保编号 */
    private String ssNo;

    /** 缴费基数 */
    private BigDecimal baseAmount;

    /** 单位比例（小数） */
    private BigDecimal companyRate;

    /** 个人比例（小数） */
    private BigDecimal personalRate;

    /** 单位缴纳额 */
    private BigDecimal companyAmount;

    /** 个人缴纳额 */
    private BigDecimal personalAmount;

    /** 所属月份 YYYY-MM */
    private String month;
}
