package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.PasswordDTO;
import com.yang.hr.dto.UserDTO;
import com.yang.hr.entity.SysUser;
import com.yang.hr.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/api/system/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/page")
    @SaCheckPermission("system:user:list")
    public R<Page<SysUser>> page(@RequestParam(defaultValue = "1") long current,
                                 @RequestParam(defaultValue = "10") long size,
                                 @RequestParam(required = false) String username,
                                 @RequestParam(required = false) String realName,
                                 @RequestParam(required = false) Long deptId,
                                 @RequestParam(required = false) Integer status) {
        return R.ok(userService.page(current, size, username, realName, deptId, status));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:user:list")
    public R<SysUser> detail(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("system:user:add")
    public R<Long> create(@Valid @RequestBody UserDTO dto) {
        return R.ok("新增成功", userService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("system:user:edit")
    public R<Void> update(@Valid @RequestBody UserDTO dto) {
        userService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:user:remove")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok("删除成功", null);
    }

    /** 重置口令（该用户会被强制下线） */
    @PutMapping("/{id}/reset-pwd")
    @SaCheckPermission("system:user:resetPwd")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordDTO dto) {
        userService.resetPassword(id, dto.getNewPassword());
        return R.ok("口令已重置，该用户需重新登录", null);
    }

    /** 启用/停用（停用后该用户会被强制下线） */
    @PutMapping("/{id}/status")
    @SaCheckPermission("system:user:edit")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.changeStatus(id, status);
        return R.ok(status == 1 ? "已启用" : "已停用，该用户已被强制下线", null);
    }

    /** 该用户已拥有的角色ID */
    @GetMapping("/{id}/roles")
    @SaCheckPermission("system:user:assign")
    public R<List<Long>> roles(@PathVariable Long id) {
        return R.ok(userService.roleIds(id));
    }

    /** 分配角色（覆盖式；该用户会被强制下线以便新权限立即生效） */
    @PutMapping("/{id}/roles")
    @SaCheckPermission("system:user:assign")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return R.ok("角色已更新，该用户需重新登录", null);
    }
}
