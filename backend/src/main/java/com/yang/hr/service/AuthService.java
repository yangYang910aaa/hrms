package com.yang.hr.service;

import com.yang.hr.dto.LoginDTO;
import com.yang.hr.vo.LoginVO;
import com.yang.hr.vo.MenuTreeVO;
import com.yang.hr.vo.UserInfoVO;

import java.util.List;

/**
 * 鉴权服务
 */
public interface AuthService {

    /** 登录：校验口令 → 签发 token → 返回用户信息 */
    LoginVO login(LoginDTO dto);

    /** 退出登录（注销当前 token） */
    void logout();

    /** 当前登录用户信息（含角色与权限码） */
    UserInfoVO currentUser();

    /** 当前登录用户可见的菜单树 */
    List<MenuTreeVO> currentUserMenus();
}
