package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.CertificateDTO;
import com.yang.hr.entity.HrCertificate;
import com.yang.hr.service.CertificateService;
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
 * 证书管理接口（人事管理 → 证书管理）
 */
@RestController
@RequestMapping("/api/hr/certificate")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /** 条件分页：按职工 / 证书名称模糊筛选 */
    @GetMapping("/page")
    @SaCheckPermission("hr:certificate:list")
    public R<Page<HrCertificate>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) Long employeeId,
                                       @RequestParam(required = false) String certName) {
        return R.ok(certificateService.page(current, size, employeeId, certName));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("hr:certificate:list")
    public R<HrCertificate> detail(@PathVariable Long id) {
        return R.ok(certificateService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("hr:certificate:add")
    public R<Long> create(@Valid @RequestBody CertificateDTO dto) {
        return R.ok("新增成功", certificateService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("hr:certificate:edit")
    public R<Void> update(@Valid @RequestBody CertificateDTO dto) {
        certificateService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("hr:certificate:remove")
    public R<Void> delete(@PathVariable Long id) {
        certificateService.delete(id);
        return R.ok("删除成功", null);
    }
}
