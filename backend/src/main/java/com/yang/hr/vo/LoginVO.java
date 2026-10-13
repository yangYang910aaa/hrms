package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 登录成功返回：token 名称 + token 值 + 用户信息
 *
 * 前端拿到后统一按 header[tokenName] = tokenValue 携带。
 */
@Getter
@Setter
public class LoginVO {

    /** token 的请求头名称，如 satoken */
    private String tokenName;

    /** token 值 */
    private String tokenValue;

    /** 有效期（秒） */
    private Long expiresIn;

    private UserInfoVO userInfo;
}
