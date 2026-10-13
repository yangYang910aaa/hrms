package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 系统-社保比例配置（sys_social_rate）
 *
 * 比例以「小数」存储：0.1600 表示 16%（与 db/data.sql 种子数据一致）。
 * 该表驱动 M5 工资计算里的社保个人扣缴。
 */
@Getter
@Setter
@TableName("sys_social_rate")
public class SysSocialRate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 险种名称（养老保险 / 住房公积金…） */
    private String rateName;

    /** 险种编码（唯一，删除时会被改写以防唯一键冲突） */
    private String rateCode;

    /** 单位比例（小数，0.16 = 16%） */
    private BigDecimal companyRate;

    /** 个人比例（小数，可为 0，如工伤/生育仅单位缴纳） */
    private BigDecimal personalRate;

    /** 缴费基数下限；0 表示未配置（列 NOT NULL，0 即"未填"） */
    private BigDecimal baseMin;

    /** 缴费基数上限；0 表示不限制 */
    private BigDecimal baseMax;

    /** 生效月份 YYYY-MM */
    private String effectiveMonth;

    private Integer sort;

    /** 状态 1启用 0停用 */
    private Integer status;

    private String remark;
}
