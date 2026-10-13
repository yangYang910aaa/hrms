package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 人事-请假管理（hr_leave）
 *
 * 审批相关字段（status / approver / approve_time / approve_remark）**只能由审批专用接口改**，
 * 通用更新接口刻意不碰它们 —— 否则带 status 调 PUT 就能绕过审批（规划书 §9 第 23 条）。
 * 无唯一业务键：删除直接逻辑删除。
 */
@Getter
@Setter
@TableName("hr_leave")
public class HrLeave extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    /** 请假类型（字典 leave_type） */
    private Integer leaveType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 请假天数 */
    private BigDecimal days;

    private String reason;

    /** 状态 0待审批 1已通过 2已驳回 */
    private Integer status;

    /** 审批人（服务端按当前登录用户解析，不接受前端传入） */
    private String approver;

    private LocalDateTime approveTime;

    private String approveRemark;
}
