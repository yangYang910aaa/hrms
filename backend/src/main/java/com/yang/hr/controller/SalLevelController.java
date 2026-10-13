package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.SalLevelDTO;
import com.yang.hr.entity.SalLevel;
import com.yang.hr.service.SalLevelService;
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

/**
 * 薪酬级别接口（薪酬管理 → 薪酬级别）
 *
 * 全部要求 sal:level:* 权限。要"级别下拉"请用 /api/common/salary-level-options（仅需登录）。
 */
@RestController
@RequestMapping("/api/sal/level")
public class SalLevelController {

    private final SalLevelService levelService;

    public SalLevelController(SalLevelService levelService) {
        this.levelService = levelService;
    }

    @GetMapping("/page")
    @SaCheckPermission("sal:level:list")
    public R<Page<SalLevel>> page(@RequestParam(defaultValue = "1") long current,
                                  @RequestParam(defaultValue = "10") long size,
                                  @RequestParam(required = false) String levelName) {
        return R.ok(levelService.page(current, size, levelName));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("sal:level:list")
    public R<SalLevel> detail(@PathVariable Long id) {
        return R.ok(levelService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("sal:level:add")
    public R<Long> create(@Valid @RequestBody SalLevelDTO dto) {
        return R.ok("新增成功", levelService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("sal:level:edit")
    public R<Void> update(@Valid @RequestBody SalLevelDTO dto) {
        levelService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("sal:level:remove")
    public R<Void> delete(@PathVariable Long id) {
        levelService.delete(id);
        return R.ok("删除成功", null);
    }
}
