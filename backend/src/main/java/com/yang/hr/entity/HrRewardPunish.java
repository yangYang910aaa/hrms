package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 人事-奖罚登记（hr_reward_punish）
 *
 * 无唯一业务键：删除直接逻辑删除。
 */
@Getter
@Setter
@TableName("hr_reward_punish")
public class HrRewardPunish extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    /** 类型 1奖励 2处罚（字典 reward_type） */
    private Integer type;

    /** 事由 */
    private String reason;

    /** 金额 */
    private BigDecimal amount;

    /** 发生日期 */
    private LocalDate happenDate;

    /** 经办人 */
    private String operator;
}
