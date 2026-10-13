package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统-菜单/权限点（sys_menu）
 */
@Getter
@Setter
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单ID，0 表示根 */
    private Long parentId;

    private String menuName;

    /** M目录 C菜单 F按钮 */
    private String menuType;

    /** 前端路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 权限标识，如 hr:employee:list */
    private String perms;

    private String icon;

    private Integer sort;

    /** 是否显示 1显示 0隐藏 */
    private Integer visible;

    /** 状态 1正常 0停用 */
    private Integer status;

    /** 是否按钮权限点（不参与菜单树，只贡献 perms） */
    public boolean isButton() {
        return "F".equals(menuType);
    }
}
