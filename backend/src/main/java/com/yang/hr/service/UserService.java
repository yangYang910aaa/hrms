package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.UserDTO;
import com.yang.hr.entity.SysUser;

import java.util.List;

/**
 * 用户服务
 */
public interface UserService {

    Page<SysUser> page(long current, long size, String username, String realName, Long deptId, Integer status);

    SysUser getById(Long id);

    Long create(UserDTO dto);

    void update(UserDTO dto);

    void delete(Long id);

    /** 重置口令（会强制该用户下线） */
    void resetPassword(Long id, String newPassword);

    /** 启用/停用（停用会强制该用户下线） */
    void changeStatus(Long id, Integer status);

    /** 该用户已拥有的角色ID */
    List<Long> roleIds(Long userId);

    /** 给用户分配角色（覆盖式，会强制该用户下线以便新权限立即生效） */
    void assignRoles(Long userId, List<Long> roleIds);
}
