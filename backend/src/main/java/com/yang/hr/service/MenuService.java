package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.MenuDTO;
import com.yang.hr.entity.SysMenu;
import com.yang.hr.vo.MenuTreeVO;

import java.util.List;

/**
 * 菜单/权限点服务
 */
public interface MenuService {

    Page<SysMenu> page(long current, long size, String menuName, String menuType, Integer status);

    /** 完整菜单树（含按钮与隐藏项，供菜单管理页使用） */
    List<MenuTreeVO> tree();

    SysMenu getById(Long id);

    Long create(MenuDTO dto);

    void update(MenuDTO dto);

    void delete(Long id);
}
