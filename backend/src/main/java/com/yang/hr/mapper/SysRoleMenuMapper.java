package com.yang.hr.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-菜单关联表 Mapper（同样是物理删除，理由见 SysUserRoleMapper）
 */
public interface SysRoleMenuMapper {

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId} AND deleted = 0")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /** 该菜单被哪些角色授权着（删除菜单前取，用于把受影响用户踢下线） */
    @Select("SELECT DISTINCT role_id FROM sys_role_menu WHERE menu_id = #{menuId} AND deleted = 0")
    List<Long> selectRoleIdsByMenuId(@Param("menuId") Long menuId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /** 删除菜单时同步清理其授权关联 */
    @Delete("DELETE FROM sys_role_menu WHERE menu_id = #{menuId}")
    int deleteByMenuId(@Param("menuId") Long menuId);

    @Insert("<script>"
            + "INSERT INTO sys_role_menu (role_id, menu_id) VALUES "
            + "<foreach collection='menuIds' item='menuId' separator=','>(#{roleId}, #{menuId})</foreach>"
            + "</script>")
    int batchInsert(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}
