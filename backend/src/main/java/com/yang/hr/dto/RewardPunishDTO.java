package com.yang.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 奖罚登记入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class RewardPunishDTO {

    private Long id;

    @NotNull(message = "不能为空")
    private Long employeeId;

    /** 类型 1奖励 2处罚（字典 reward_type） */
    @NotNull(message = "不能为空")
    @Min(value = 1, message = "取值非法（1奖励 2处罚）")
    @Max(value = 2, message = "取值非法（1奖励 2处罚）")
    private Integer type;

    @NotBlank(message = "不能为空")
    @Size(max = 255, message = "长度不能超过 255")
    private String reason;

    @DecimalMin(value = "0", message = "不能为负数")
    @Digits(integer = 10, fraction = 2, message = "最多 10 位整数、2 位小数")
    private BigDecimal amount;

    private LocalDate happenDate;

    @Size(max = 30, message = "长度不能超过 30")
    private String operator;
}
