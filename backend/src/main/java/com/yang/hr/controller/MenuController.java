package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.MenuDTO;
import com.yang.hr.entity.SysMenu;
import com.yang.hr.service.MenuService;
import com.yang.hr.vo.MenuTreeVO;
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
 * 菜单/权限点接口
 */
@RestController
@RequestMapping("/api/system/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/page")
    @SaCheckPermission("system:menu:list")
    public R<Page<SysMenu>> page(@RequestParam(defaultValue = "1") long current,
                                 @RequestParam(defaultValue = "10") long size,
                                 @RequestParam(required = false) String menuName,
                                 @RequestParam(required = false) String menuType,
                                 @RequestParam(required = false) Integer status) {
        return R.ok(menuService.page(current, size, menuName, menuType, status));
    }

    /** 完整菜单树（含按钮与隐藏项，供菜单管理页使用；与 /api/auth/menus 不同，后者只给当前用户可见菜单） */
    @GetMapping("/tree")
    @SaCheckPermission("system:menu:list")
    public R<List<MenuTreeVO>> tree() {
        return R.ok(menuService.tree());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:menu:list")
    public R<SysMenu> detail(@PathVariable Long id) {
        return R.ok(menuService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("system:menu:add")
    public R<Long> create(@Valid @RequestBody MenuDTO dto) {
        return R.ok("新增成功", menuService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("system:menu:edit")
    public R<Void> update(@Valid @RequestBody MenuDTO dto) {
        menuService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:menu:remove")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok("删除成功", null);
    }
}
