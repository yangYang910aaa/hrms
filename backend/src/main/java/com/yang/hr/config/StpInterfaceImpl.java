package com.yang.hr.config;

import cn.dev33.satoken.stp.StpInterface;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.entity.SysMenu;
import com.yang.hr.mapper.SysMenuMapper;
import com.yang.hr.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限数据源：把 sys_role_menu / sys_role / sys_user_role 接到 @SaCheckPermission、@SaCheckRole 上。
 *
 * 超管返回 {"*"}（Sa-Token 的通配符约定，代表拥有所有权限），
 * 这样以后新增接口/权限点不必再回头给超管补授权。
 *
 * 说明：这里每次鉴权都会查一次库。课设规模（几十个菜单、几个用户）完全无压力；
 * 若要做优化，可把结果缓存进 SaSession —— 而"改角色即 kickout"已经保证了缓存不会变脏。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    /** Sa-Token 通配符：拥有所有权限 */
    private static final List<String> ALL_PERMISSIONS = List.of("*");

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;

    public StpInterfaceImpl(SysUserMapper userMapper, SysMenuMapper menuMapper) {
        this.userMapper = userMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = toUserId(loginId);
        if (userId == null) {
            return List.of();
        }
        if (SecurityUtils.isSuperAdmin(userMapper.selectRoleCodesByUserId(userId))) {
            return ALL_PERMISSIONS;
        }
        return menuMapper.selectEnabledByUserId(userId).stream()
                .map(SysMenu::getPerms)
                .filter(perms -> perms != null && !perms.isBlank())
                .distinct()
                .toList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = toUserId(loginId);
        return userId == null ? List.of() : userMapper.selectRoleCodesByUserId(userId);
    }

    private Long toUserId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return Long.valueOf(loginId.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
