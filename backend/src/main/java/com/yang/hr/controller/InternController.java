package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.InternDTO;
import com.yang.hr.entity.HrIntern;
import com.yang.hr.service.InternService;
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
 * 实习生管理接口（人事管理 → 实习生管理）
 */
@RestController
@RequestMapping("/api/hr/intern")
public class InternController {

    private final InternService internService;

    public InternController(InternService internService) {
        this.internService = internService;
    }

    /** 条件分页：姓名模糊 / 实习部门 / 状态 */
    @GetMapping("/page")
    @SaCheckPermission("hr:intern:list")
    public R<Page<HrIntern>> page(@RequestParam(defaultValue = "1") long current,
                                  @RequestParam(defaultValue = "10") long size,
                                  @RequestParam(required = false) String name,
                                  @RequestParam(required = false) Long deptId,
                                  @RequestParam(required = false) Integer status) {
        return R.ok(internService.page(current, size, name, deptId, status));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("hr:intern:list")
    public R<HrIntern> detail(@PathVariable Long id) {
        return R.ok(internService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("hr:intern:add")
    public R<Long> create(@Valid @RequestBody InternDTO dto) {
        return R.ok("新增成功", internService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("hr:intern:edit")
    public R<Void> update(@Valid @RequestBody InternDTO dto) {
        internService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("hr:intern:remove")
    public R<Void> delete(@PathVariable Long id) {
        internService.delete(id);
        return R.ok("删除成功", null);
    }
}
