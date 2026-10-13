package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.SalUtilityFeeDTO;
import com.yang.hr.entity.SalUtilityFee;
import com.yang.hr.service.SalUtilityFeeService;
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
 * 水电费登记接口（薪酬管理 → 水电费登记）
 *
 * 全部要求 sal:utility:* 权限。合计（水 + 电 + 住宿）由服务端算，入参里没有 totalFee。
 */
@RestController
@RequestMapping("/api/sal/utility")
public class SalUtilityFeeController {

    private final SalUtilityFeeService utilityService;

    public SalUtilityFeeController(SalUtilityFeeService utilityService) {
        this.utilityService = utilityService;
    }

    @GetMapping("/page")
    @SaCheckPermission("sal:utility:list")
    public R<Page<SalUtilityFee>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) Long employeeId,
                                       @RequestParam(required = false) String month) {
        return R.ok(utilityService.page(current, size, employeeId, month));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("sal:utility:list")
    public R<SalUtilityFee> detail(@PathVariable Long id) {
        return R.ok(utilityService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("sal:utility:add")
    public R<Long> create(@Valid @RequestBody SalUtilityFeeDTO dto) {
        return R.ok("新增成功", utilityService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("sal:utility:edit")
    public R<Void> update(@Valid @RequestBody SalUtilityFeeDTO dto) {
        utilityService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("sal:utility:remove")
    public R<Void> delete(@PathVariable Long id) {
        utilityService.delete(id);
        return R.ok("删除成功", null);
    }
}
