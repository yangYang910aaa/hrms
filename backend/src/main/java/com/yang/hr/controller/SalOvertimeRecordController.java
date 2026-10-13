package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.SalOvertimeRecordDTO;
import com.yang.hr.entity.SalOvertimeRecord;
import com.yang.hr.service.SalOvertimeRecordService;
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
 * 加班登记接口（薪酬管理 → 加班登记）
 *
 * 全部要求 sal:overtime:* 权限。倍率与金额由服务端按 21.75 口径算，入参里没有 rate/amount。
 */
@RestController
@RequestMapping("/api/sal/overtime")
public class SalOvertimeRecordController {

    private final SalOvertimeRecordService overtimeService;

    public SalOvertimeRecordController(SalOvertimeRecordService overtimeService) {
        this.overtimeService = overtimeService;
    }

    @GetMapping("/page")
    @SaCheckPermission("sal:overtime:list")
    public R<Page<SalOvertimeRecord>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long employeeId,
                                           @RequestParam(required = false) String month,
                                           @RequestParam(required = false) Integer overtimeType) {
        return R.ok(overtimeService.page(current, size, employeeId, month, overtimeType));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("sal:overtime:list")
    public R<SalOvertimeRecord> detail(@PathVariable Long id) {
        return R.ok(overtimeService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("sal:overtime:add")
    public R<Long> create(@Valid @RequestBody SalOvertimeRecordDTO dto) {
        return R.ok("新增成功", overtimeService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("sal:overtime:edit")
    public R<Void> update(@Valid @RequestBody SalOvertimeRecordDTO dto) {
        overtimeService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("sal:overtime:remove")
    public R<Void> delete(@PathVariable Long id) {
        overtimeService.delete(id);
        return R.ok("删除成功", null);
    }
}
