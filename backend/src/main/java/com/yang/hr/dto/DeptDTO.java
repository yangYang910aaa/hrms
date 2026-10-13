package com.yang.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 部门入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class DeptDTO {

    private Long id;

    /** 父部门ID，0 表示根 */
    @NotNull(message = "不能为空")
    private Long parentId;

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过 50")
    private String deptName;

    @Size(max = 50, message = "长度不能超过 50")
    private String deptCode;

    @Size(max = 30, message = "长度不能超过 30")
    private String leader;

    @Size(max = 20, message = "长度不能超过 20")
    private String phone;

    private Integer sort;

    private Integer status;
}
