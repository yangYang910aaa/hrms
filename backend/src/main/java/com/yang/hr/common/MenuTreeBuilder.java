package com.yang.hr.common;

import com.yang.hr.entity.SysMenu;
import com.yang.hr.vo.MenuTreeVO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单树构建器（纯函数，无 Spring 依赖，便于单测）
 */
public final class MenuTreeBuilder {

    private MenuTreeBuilder() {
    }

    /**
     * 给「当前用户」用的菜单树：按钮不进树、隐藏菜单不进树。
     *
     * 规则：
     * 1. 按钮（F）不进树，它只用于 perms 权限校验；
     * 2. 隐藏菜单（visible=0）不进树；
     * 3. 排序按 sort 升序，sort 相同按 id 升序；
     * 4. 父节点不在列表里（未授权 / 已停用）时，该节点上提为根 —— 避免整棵子树凭空消失。
     */
    public static List<MenuTreeVO> build(List<SysMenu> menus) {
        return assemble(menus, false);
    }

    /**
     * 给「菜单管理页」用的完整树：按钮与隐藏菜单都要显示出来，否则管理员改不了它们。
     */
    public static List<MenuTreeVO> buildFull(List<SysMenu> menus) {
        return assemble(menus, true);
    }

    private static List<MenuTreeVO> assemble(List<SysMenu> menus, boolean includeAll) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }

        List<MenuTreeVO> nodes = menus.stream()
                .filter(m -> includeAll || !m.isButton())
                .filter(m -> includeAll || m.getVisible() == null || m.getVisible() == 1)
                .map(MenuTreeBuilder::toVo)
                .sorted(Comparator
                        .comparing(MenuTreeVO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MenuTreeVO::getId))
                .toList();

        Map<Long, MenuTreeVO> byId = new LinkedHashMap<>();
        for (MenuTreeVO node : nodes) {
            byId.put(node.getId(), node);
        }

        List<MenuTreeVO> roots = new ArrayList<>();
        for (MenuTreeVO node : nodes) {
            MenuTreeVO parent = node.getParentId() == null ? null : byId.get(node.getParentId());
            if (parent == null || parent == node) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private static MenuTreeVO toVo(SysMenu m) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(m.getId());
        vo.setParentId(m.getParentId());
        vo.setName(m.getMenuName());
        vo.setPath(m.getPath());
        vo.setComponent(m.getComponent());
        vo.setPerms(m.getPerms());
        vo.setIcon(m.getIcon());
        vo.setType(m.getMenuType());
        vo.setSort(m.getSort());
        vo.setVisible(m.getVisible());
        vo.setStatus(m.getStatus());
        return vo;
    }
}
