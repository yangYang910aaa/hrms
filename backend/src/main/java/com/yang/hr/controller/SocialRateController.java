package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.SocialRateDTO;
import com.yang.hr.entity.SysSocialRate;
import com.yang.hr.service.SocialRateService;
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
 * 社保比例接口（系统设置 → 社保比例）
 *
 * 全部要求 system:social-rate:* 权限。
 */
@RestController
@RequestMapping("/api/system/social-rate")
public class SocialRateController {

    private final SocialRateService socialRateService;

    public SocialRateController(SocialRateService socialRateService) {
        this.socialRateService = socialRateService;
    }

    @GetMapping("/page")
    @SaCheckPermission("system:social-rate:list")
    public R<Page<SysSocialRate>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String rateName,
                                       @RequestParam(required = false) Integer status) {
        return R.ok(socialRateService.page(current, size, rateName, status));
    }

    /** 启用中的社保比例（下拉 / M5 工资计算取用） */
    @GetMapping("/list")
    @SaCheckPermission("system:social-rate:list")
    public R<List<SysSocialRate>> list() {
        return R.ok(socialRateService.listEnabled());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:social-rate:list")
    public R<SysSocialRate> detail(@PathVariable Long id) {
        return R.ok(socialRateService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("system:social-rate:add")
    public R<Long> create(@Valid @RequestBody SocialRateDTO dto) {
        return R.ok("新增成功", socialRateService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("system:social-rate:edit")
    public R<Void> update(@Valid @RequestBody SocialRateDTO dto) {
        socialRateService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:social-rate:remove")
    public R<Void> delete(@PathVariable Long id) {
        socialRateService.delete(id);
        return R.ok("删除成功", null);
    }
}
