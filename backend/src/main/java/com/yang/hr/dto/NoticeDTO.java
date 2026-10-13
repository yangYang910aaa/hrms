package com.yang.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 通知公告入参（sys_notice）。
 *
 * ⚠️ **刻意不含** `publisher` / `publishTime` / `status` / `printCount`：
 * - 发布人与发布时间由「发布」接口按当前登录用户与服务端时间写入，不接受前端伪造；
 * - 状态只能通过 `/{id}/publish`、`/{id}/offline` 两个专用接口流转（与请假审批同一思路）；
 * - 打印次数只能通过 `/{id}/print` 自增。
 * 这样通用新增/修改接口就不会成为绕过状态机的后门。
 */
@Getter
@Setter
public class NoticeDTO {

    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过100个字符")
    private String title;

    /** 类型 1通知 2公告 */
    @NotNull(message = "请选择通知类型")
    private Integer noticeType;

    private String content;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
