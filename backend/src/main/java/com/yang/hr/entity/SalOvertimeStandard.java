package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 薪酬-加班标准（sal_overtime_standard）—— 按「薪酬级别 × 加班类型」配置倍率
 *
 * 有**复合唯一键** `uk_level_type(level_id, overtime_type)`：删除时不便改写文本键，
 * 由 Service 把 `level_id` 置为 `-id` 让出唯一键（见 SalOvertimeStandardServiceImpl）。
 */
@Getter
@Setter
@TableName("sal_overtime_standard")
public class SalOvertimeStandard extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 薪酬级别ID（sal_level.id） */
    private Long levelId;

    /** 加班类型 1工作日 2休息日 3节假日（字典 overtime_type） */
    private Integer overtimeType;

    /** 倍率，如 1.50 / 2.00 / 3.00 */
    private BigDecimal rate;

    private String remark;
}
