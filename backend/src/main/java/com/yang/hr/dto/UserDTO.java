package com.yang.hr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 用户入参（新增与修改共用）
 *
 * - 新增：password 必填（Service 校验），可同时带 roleIds；
 * - 修改：**username 不可改**（登录标识，改了等于换账号），password 与 status 都会被忽略；
 *   改口令走 reset-pwd 接口，改角色走 /{id}/roles 接口，改状态走 /{id}/status 接口
 *   —— 那三个接口才带"踢下线"和防自锁保护，通用更新接口不碰它们，避免被绕过。
 */
@Getter
@Setter
public class UserDTO {

    private Long id;

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过 50")
    private String username;

    @Size(max = 100, message = "长度不能超过 100")
    private String password;

    @NotBlank(message = "不能为空")
    @Size(max = 30, message = "长度不能超过 30")
    private String realName;

    private Long deptId;

    @Size(max = 20, message = "长度不能超过 20")
    private String phone;

    @Email(message = "格式不正确")
    @Size(max = 50, message = "长度不能超过 50")
    private String email;

    @Size(max = 255, message = "长度不能超过 255")
    private String avatar;

    private Integer status;

    /** 新增时可直接分配角色 */
    private List<Long> roleIds;
}
