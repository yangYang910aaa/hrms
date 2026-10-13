package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点（只含目录 M 与菜单 C，按钮 F 不进入树）
 */
@Getter
@Setter
public class MenuTreeVO {

    private Long id;

    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 前端路由 */
    private String path;

    /** 前端组件 */
    private String component;

    private String perms;

    private String icon;

    /** M目录 / C菜单 / F按钮 */
    private String type;

    private Integer sort;

    /** 是否显示 1显示 0隐藏（菜单管理页需要） */
    private Integer visible;

    /** 状态 1正常 0停用（菜单管理页需要） */
    private Integer status;

    private List<MenuTreeVO> children = new ArrayList<>();
}
