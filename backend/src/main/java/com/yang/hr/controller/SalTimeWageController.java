package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.SalTimeWageDTO;
import com.yang.hr.entity.SalTimeWage;
import com.yang.hr.service.SalTimeWageService;
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
 * 计时工资接口（薪酬管理 → 计时工资）
 *
 * 全部要求 sal:time:* 权限。金额（工时 × 时薪）由服务端算，入参里没有 amount。
 */
@RestController
@RequestMapping("/api/sal/time")
public class SalTimeWageController {

    private final SalTimeWageService timeService;

    public SalTimeWageController(SalTimeWageService timeService) {
        this.timeService = timeService;
    }

    @GetMapping("/page")
    @SaCheckPermission("sal:time:list")
    public R<Page<SalTimeWage>> page(@RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) Long employeeId,
                                     @RequestParam(required = false) String month) {
        return R.ok(timeService.page(current, size, employeeId, month));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("sal:time:list")
    public R<SalTimeWage> detail(@PathVariable Long id) {
        return R.ok(timeService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("sal:time:add")
    public R<Long> create(@Valid @RequestBody SalTimeWageDTO dto) {
        return R.ok("新增成功", timeService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("sal:time:edit")
    public R<Void> update(@Valid @RequestBody SalTimeWageDTO dto) {
        timeService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("sal:time:remove")
    public R<Void> delete(@PathVariable Long id) {
        timeService.delete(id);
        return R.ok("删除成功", null);
    }
}
