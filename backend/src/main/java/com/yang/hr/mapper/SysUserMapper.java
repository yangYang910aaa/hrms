package com.yang.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yang.hr.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查用户拥有的角色编码（只取启用中的角色）
     */
    @Select("""
            SELECT r.role_code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id AND ur.deleted = 0
            WHERE ur.user_id = #{userId}
              AND r.deleted = 0
              AND r.status = 1
            ORDER BY r.sort
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
