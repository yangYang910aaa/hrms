package com.yang.hr.controller;

import com.yang.hr.common.R;
import com.yang.hr.dto.LoginDTO;
import com.yang.hr.service.AuthService;
import com.yang.hr.vo.LoginVO;
import com.yang.hr.vo.MenuTreeVO;
import com.yang.hr.vo.UserInfoVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 鉴权接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登录（白名单，无需 token） */
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok("登录成功", authService.login(dto));
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok("已退出登录", null);
    }

    /** 当前用户信息 + 角色 + 权限码 */
    @GetMapping("/me")
    public R<UserInfoVO> me() {
        return R.ok(authService.currentUser());
    }

    /** 当前用户菜单树（动态路由的数据源） */
    @GetMapping("/menus")
    public R<List<MenuTreeVO>> menus() {
        return R.ok(authService.currentUserMenus());
    }
}
