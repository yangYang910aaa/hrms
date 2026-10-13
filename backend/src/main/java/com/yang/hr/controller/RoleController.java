package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.RoleDTO;
import com.yang.hr.entity.SysRole;
import com.yang.hr.service.RoleService;
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
 * 角色管理接口
 */
@RestController
@RequestMapping("/api/system/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/page")
    @SaCheckPermission("system:role:list")
    public R<Page<SysRole>> page(@RequestParam(defaultValue = "1") long current,
                                 @RequestParam(defaultValue = "10") long size,
                                 @RequestParam(required = false) String roleName,
                                 @RequestParam(required = false) Integer status) {
        return R.ok(roleService.page(current, size, roleName, status));
    }

    /** 全部启用角色（用户表单的角色下拉） */
    @GetMapping("/list")
    @SaCheckPermission("system:role:list")
    public R<List<SysRole>> list() {
        return R.ok(roleService.listAll());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:role:list")
    public R<SysRole> detail(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("system:role:add")
    public R<Long> create(@Valid @RequestBody RoleDTO dto) {
        return R.ok("新增成功", roleService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("system:role:edit")
    public R<Void> update(@Valid @RequestBody RoleDTO dto) {
        roleService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:role:remove")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok("删除成功", null);
    }

    /** 该角色已勾选的菜单ID（前端菜单树回显） */
    @GetMapping("/{id}/menus")
    @SaCheckPermission("system:role:assign")
    public R<List<Long>> menus(@PathVariable Long id) {
        return R.ok(roleService.menuIds(id));
    }

    /** 给角色分配菜单（覆盖式） */
    @PutMapping("/{id}/menus")
    @SaCheckPermission("system:role:assign")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok("授权成功", null);
    }
}
