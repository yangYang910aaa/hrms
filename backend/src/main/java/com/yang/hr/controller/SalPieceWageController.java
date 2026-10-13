package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.SalPieceWageDTO;
import com.yang.hr.entity.SalPieceWage;
import com.yang.hr.service.SalPieceWageService;
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
 * 计件工资接口（薪酬管理 → 计件工资）
 *
 * 全部要求 sal:piece:* 权限。金额（数量 × 单价）由服务端算，入参里没有 amount。
 */
@RestController
@RequestMapping("/api/sal/piece")
public class SalPieceWageController {

    private final SalPieceWageService pieceService;

    public SalPieceWageController(SalPieceWageService pieceService) {
        this.pieceService = pieceService;
    }

    @GetMapping("/page")
    @SaCheckPermission("sal:piece:list")
    public R<Page<SalPieceWage>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) Long employeeId,
                                      @RequestParam(required = false) String month) {
        return R.ok(pieceService.page(current, size, employeeId, month));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("sal:piece:list")
    public R<SalPieceWage> detail(@PathVariable Long id) {
        return R.ok(pieceService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("sal:piece:add")
    public R<Long> create(@Valid @RequestBody SalPieceWageDTO dto) {
        return R.ok("新增成功", pieceService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("sal:piece:edit")
    public R<Void> update(@Valid @RequestBody SalPieceWageDTO dto) {
        pieceService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("sal:piece:remove")
    public R<Void> delete(@PathVariable Long id) {
        pieceService.delete(id);
        return R.ok("删除成功", null);
    }
}
