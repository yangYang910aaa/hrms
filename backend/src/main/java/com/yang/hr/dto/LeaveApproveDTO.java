package com.yang.hr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 请假审批入参
 *
 * 审批人与审批时间由服务端解析/取当前时间，不接受前端传入。
 */
@Getter
@Setter
public class LeaveApproveDTO {

    /** 1通过 2驳回 */
    @NotNull(message = "不能为空")
    @Min(value = 1, message = "取值非法（1通过 2驳回）")
    @Max(value = 2, message = "取值非法（1通过 2驳回）")
    private Integer status;

    @Size(max = 255, message = "长度不能超过 255")
    private String approveRemark;
}
