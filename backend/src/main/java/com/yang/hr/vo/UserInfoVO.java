package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 当前登录用户信息（不含口令）
 */
@Getter
@Setter
public class UserInfoVO {

    private Long id;

    private String username;

    private String realName;

    private Long deptId;

    /** 头像地址（前端可直接用的 URL 路径，如 /avatars/admin.png）；未设置时为 null，前端退化为默认图标 */
    private String avatar;

    /** 角色编码列表，如 ["SUPER_ADMIN"] */
    private List<String> roles;

    /** 权限标识列表，如 ["hr:employee:add"]；前端按钮级控制用 */
    private List<String> perms;
}
