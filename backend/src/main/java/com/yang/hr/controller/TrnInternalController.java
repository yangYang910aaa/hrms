package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.TrnInternalDTO;
import com.yang.hr.entity.TrnInternal;
import com.yang.hr.service.TrnInternalService;
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
 * 内部培训接口（培训管理 → 内部培训）
 *
 * 全部要求 trn:internal:* 权限。表单要"课程下拉/部门下拉"请用
 * /api/common/course-options 与 /api/common/dept-tree（仅需登录）。
 */
@RestController
@RequestMapping("/api/trn/internal")
public class TrnInternalController {

    private final TrnInternalService internalService;

    public TrnInternalController(TrnInternalService internalService) {
        this.internalService = internalService;
    }

    /** 条件分页：课程 / 部门 / 状态 */
    @GetMapping("/page")
    @SaCheckPermission("trn:internal:list")
    public R<Page<TrnInternal>> page(@RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) Long courseId,
                                     @RequestParam(required = false) Long deptId,
                                     @RequestParam(required = false) Integer status) {
        return R.ok(internalService.page(current, size, courseId, deptId, status));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("trn:internal:list")
    public R<TrnInternal> detail(@PathVariable Long id) {
        return R.ok(internalService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("trn:internal:add")
    public R<Long> create(@Valid @RequestBody TrnInternalDTO dto) {
        return R.ok("新增成功", internalService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("trn:internal:edit")
    public R<Void> update(@Valid @RequestBody TrnInternalDTO dto) {
        internalService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("trn:internal:remove")
    public R<Void> delete(@PathVariable Long id) {
        internalService.delete(id);
        return R.ok("删除成功", null);
    }
}
