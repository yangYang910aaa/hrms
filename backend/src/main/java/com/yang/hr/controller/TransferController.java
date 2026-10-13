package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.TransferDTO;
import com.yang.hr.entity.HrTransfer;
import com.yang.hr.service.TransferService;
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
 * 职工调动接口（人事管理 → 职工调动）
 */
@RestController
@RequestMapping("/api/hr/transfer")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /** 条件分页：按职工筛选 */
    @GetMapping("/page")
    @SaCheckPermission("hr:transfer:list")
    public R<Page<HrTransfer>> page(@RequestParam(defaultValue = "1") long current,
                                    @RequestParam(defaultValue = "10") long size,
                                    @RequestParam(required = false) Long employeeId) {
        return R.ok(transferService.page(current, size, employeeId));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("hr:transfer:list")
    public R<HrTransfer> detail(@PathVariable Long id) {
        return R.ok(transferService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("hr:transfer:add")
    public R<Long> create(@Valid @RequestBody TransferDTO dto) {
        return R.ok("新增成功", transferService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("hr:transfer:edit")
    public R<Void> update(@Valid @RequestBody TransferDTO dto) {
        transferService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("hr:transfer:remove")
    public R<Void> delete(@PathVariable Long id) {
        transferService.delete(id);
        return R.ok("删除成功", null);
    }
}
