package com.yang.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yang.hr.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单 Mapper
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 按用户查其角色被授权的所有启用菜单（去重）。
     * 目录、菜单、按钮都会查出来，调用方自行按 menu_type 分流。
     */
    @Select("""
            SELECT DISTINCT m.*
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id AND rm.deleted = 0
            JOIN sys_user_role ur ON ur.role_id = rm.role_id AND ur.deleted = 0
            WHERE ur.user_id = #{userId}
              AND m.deleted = 0
              AND m.status = 1
            ORDER BY m.parent_id, m.sort
            """)
    List<SysMenu> selectEnabledByUserId(@Param("userId") Long userId);

    /**
     * 全部启用菜单（超级管理员快捷通道用）。
     * 这样以后新增菜单不必逐个给超管补授权。
     */
    @Select("""
            SELECT m.*
            FROM sys_menu m
            WHERE m.deleted = 0
              AND m.status = 1
            ORDER BY m.parent_id, m.sort
            """)
    List<SysMenu> selectAllEnabled();
}
