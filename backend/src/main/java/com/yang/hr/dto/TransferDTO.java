package com.yang.hr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 职工调动入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 *
 * 原部门/原岗位可以不填，Service 会从职工主档快照带出。
 */
@Getter
@Setter
public class TransferDTO {

    private Long id;

    @NotNull(message = "不能为空")
    private Long employeeId;

    /** 原部门（不填则取职工主档当前部门） */
    private Long fromDeptId;

    /** 新部门 */
    private Long toDeptId;

    /** 原岗位（不填则取职工主档当前岗位） */
    @Size(max = 50, message = "长度不能超过 50")
    private String fromPosition;

    /** 新岗位 */
    @Size(max = 50, message = "长度不能超过 50")
    private String toPosition;

    private LocalDate transferDate;

    @Size(max = 255, message = "长度不能超过 255")
    private String reason;

    @Size(max = 30, message = "长度不能超过 30")
    private String approver;
}
