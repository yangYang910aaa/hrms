package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.RoleDTO;
import com.yang.hr.entity.SysRole;

import java.util.List;

/**
 * 角色服务
 */
public interface RoleService {

    Page<SysRole> page(long current, long size, String roleName, Integer status);

    /** 全部启用角色（给用户表单的角色下拉用） */
    List<SysRole> listAll();

    SysRole getById(Long id);

    Long create(RoleDTO dto);

    void update(RoleDTO dto);

    void delete(Long id);

    /** 该角色已勾选的菜单ID */
    List<Long> menuIds(Long roleId);

    /** 给角色分配菜单（覆盖式） */
    void assignMenus(Long roleId, List<Long> menuIds);
}
