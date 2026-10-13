package com.yang.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 菜单入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class MenuDTO {

    private Long id;

    /** 父菜单ID，0 表示根 */
    @NotNull(message = "不能为空")
    private Long parentId;

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过 50")
    private String menuName;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "[MCF]", message = "只能是 M(目录)/C(菜单)/F(按钮)")
    private String menuType;

    @Size(max = 200, message = "长度不能超过 200")
    private String path;

    @Size(max = 255, message = "长度不能超过 255")
    private String component;

    @Size(max = 100, message = "长度不能超过 100")
    private String perms;

    @Size(max = 100, message = "长度不能超过 100")
    private String icon;

    private Integer sort;

    private Integer visible;

    private Integer status;
}
