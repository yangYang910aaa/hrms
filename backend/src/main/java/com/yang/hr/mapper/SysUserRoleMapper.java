package com.yang.hr.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-角色关联表 Mapper。
 *
 * 关联表一律**物理删除**：它没有业务含义，逻辑删除只会让"查某用户的角色"白白多扫行、
 * 还容易在重新授权时撞唯一键（uk_user_role）。
 */
public interface SysUserRoleMapper {

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId} AND deleted = 0")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /** 统计有多少用户挂着这个角色（删除角色前的保护） */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId} AND deleted = 0")
    long countByRoleId(@Param("roleId") Long roleId);

    /** 该角色下的所有用户ID（角色授权变更后需要把这些人的会话踢掉，让其重新拉菜单） */
    @Select("SELECT user_id FROM sys_user_role WHERE role_id = #{roleId} AND deleted = 0")
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    @Insert("<script>"
            + "INSERT INTO sys_user_role (user_id, role_id) VALUES "
            + "<foreach collection='roleIds' item='roleId' separator=','>(#{userId}, #{roleId})</foreach>"
            + "</script>")
    int batchInsert(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 统计"启用中的超级管理员"人数，用于阻止把最后一个超管删掉/停用导致系统自锁。
     */
    @Select("SELECT COUNT(DISTINCT u.id) FROM sys_user u "
            + "JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0 "
            + "JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 "
            + "WHERE r.role_code = 'SUPER_ADMIN' AND u.deleted = 0 AND u.status = 1")
    long countEnabledSuperAdmins();
}
