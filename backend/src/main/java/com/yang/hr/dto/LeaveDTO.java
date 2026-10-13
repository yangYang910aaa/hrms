package com.yang.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假单入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 *
 * ⚠️ **刻意不含** status / approver / approveTime / approveRemark ——
 * 这四个审批字段只能由 `PUT /api/hr/leave/{id}/approve` 改，
 * 否则带 status 调通用更新就能绕过审批（规划书 §9 第 23 条）。
 */
@Getter
@Setter
public class LeaveDTO {

    private Long id;

    @NotNull(message = "不能为空")
    private Long employeeId;

    /** 请假类型（字典 leave_type） */
    @NotNull(message = "不能为空")
    @Min(value = 1, message = "取值非法")
    private Integer leaveType;

    @NotNull(message = "不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "不能为空")
    private LocalDateTime endTime;

    @DecimalMin(value = "0", message = "不能为负数")
    @Digits(integer = 4, fraction = 1, message = "最多 4 位整数、1 位小数")
    private BigDecimal days;

    @Size(max = 255, message = "长度不能超过 255")
    private String reason;
}
