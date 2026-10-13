package com.yang.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class RoleDTO {

    private Long id;

    @NotBlank(message = "不能为空")
    @Size(max = 30, message = "长度不能超过 30")
    private String roleName;

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过 50")
    @Pattern(regexp = "[A-Za-z0-9_]+", message = "只能包含字母、数字和下划线")
    private String roleCode;

    private Integer sort;

    private Integer status;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;
}
