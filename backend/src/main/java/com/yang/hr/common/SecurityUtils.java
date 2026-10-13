package com.yang.hr.common;

import cn.dev33.satoken.stp.StpUtil;

import java.util.List;

/**
 * 当前登录用户工具
 */
public final class SecurityUtils {

    /** 超级管理员角色编码：走"全菜单/全权限"通道 */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    private SecurityUtils() {
    }

    /** 当前登录用户ID；未登录时抛 NotLoginException，由全局异常处理器转 401 */
    public static Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /** 当前登录用户ID；未登录返回 null（用于可能被内部调用的场景） */
    public static Long currentUserIdOrNull() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return loginId == null ? null : Long.valueOf(loginId.toString());
    }

    /** 是否超级管理员（按角色编码判断） */
    public static boolean isSuperAdmin(List<String> roleCodes) {
        return roleCodes != null && roleCodes.contains(ROLE_SUPER_ADMIN);
    }
}
