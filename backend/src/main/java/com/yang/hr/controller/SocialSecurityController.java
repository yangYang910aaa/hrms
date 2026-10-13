package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.SocialSecurityDTO;
import com.yang.hr.entity.HrSocialSecurity;
import com.yang.hr.service.SocialSecurityService;
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
 * 职工社保接口（人事管理 → 社保管理）
 */
@RestController
@RequestMapping("/api/hr/social")
public class SocialSecurityController {

    private final SocialSecurityService socialSecurityService;

    public SocialSecurityController(SocialSecurityService socialSecurityService) {
        this.socialSecurityService = socialSecurityService;
    }

    /** 条件分页：按职工 / 月份筛选 */
    @GetMapping("/page")
    @SaCheckPermission("hr:social:list")
    public R<Page<HrSocialSecurity>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) Long employeeId,
                                          @RequestParam(required = false) String month) {
        return R.ok(socialSecurityService.page(current, size, employeeId, month));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("hr:social:list")
    public R<HrSocialSecurity> detail(@PathVariable Long id) {
        return R.ok(socialSecurityService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("hr:social:add")
    public R<Long> create(@Valid @RequestBody SocialSecurityDTO dto) {
        return R.ok("新增成功", socialSecurityService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("hr:social:edit")
    public R<Void> update(@Valid @RequestBody SocialSecurityDTO dto) {
        socialSecurityService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("hr:social:remove")
    public R<Void> delete(@PathVariable Long id) {
        socialSecurityService.delete(id);
        return R.ok("删除成功", null);
    }
}
