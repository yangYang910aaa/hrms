package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.SalOvertimeStandardDTO;
import com.yang.hr.entity.SalOvertimeStandard;
import com.yang.hr.service.SalOvertimeStandardService;
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
 * 加班标准接口（薪酬管理 → 加班标准）
 *
 * 全部要求 sal:overtime-standard:* 权限。要"级别下拉"请用 /api/common/salary-level-options。
 */
@RestController
@RequestMapping("/api/sal/overtime-standard")
public class SalOvertimeStandardController {

    private final SalOvertimeStandardService standardService;

    public SalOvertimeStandardController(SalOvertimeStandardService standardService) {
        this.standardService = standardService;
    }

    @GetMapping("/page")
    @SaCheckPermission("sal:overtime-standard:list")
    public R<Page<SalOvertimeStandard>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) Long levelId,
                                             @RequestParam(required = false) Integer overtimeType) {
        return R.ok(standardService.page(current, size, levelId, overtimeType));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("sal:overtime-standard:list")
    public R<SalOvertimeStandard> detail(@PathVariable Long id) {
        return R.ok(standardService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("sal:overtime-standard:add")
    public R<Long> create(@Valid @RequestBody SalOvertimeStandardDTO dto) {
        return R.ok("新增成功", standardService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("sal:overtime-standard:edit")
    public R<Void> update(@Valid @RequestBody SalOvertimeStandardDTO dto) {
        standardService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("sal:overtime-standard:remove")
    public R<Void> delete(@PathVariable Long id) {
        standardService.delete(id);
        return R.ok("删除成功", null);
    }
}
