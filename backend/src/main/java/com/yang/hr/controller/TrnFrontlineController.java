package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.TrnFrontlineDTO;
import com.yang.hr.entity.TrnFrontline;
import com.yang.hr.service.TrnFrontlineService;
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
 * 一线工培训接口（培训管理 → 一线工培训）
 *
 * 全部要求 trn:frontline:* 权限。表单要"课程下拉/部门下拉"请用
 * /api/common/course-options 与 /api/common/dept-tree（仅需登录）。
 */
@RestController
@RequestMapping("/api/trn/frontline")
public class TrnFrontlineController {

    private final TrnFrontlineService frontlineService;

    public TrnFrontlineController(TrnFrontlineService frontlineService) {
        this.frontlineService = frontlineService;
    }

    /** 条件分页：课程 / 部门 / 状态 */
    @GetMapping("/page")
    @SaCheckPermission("trn:frontline:list")
    public R<Page<TrnFrontline>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) Long courseId,
                                      @RequestParam(required = false) Long deptId,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(frontlineService.page(current, size, courseId, deptId, status));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("trn:frontline:list")
    public R<TrnFrontline> detail(@PathVariable Long id) {
        return R.ok(frontlineService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("trn:frontline:add")
    public R<Long> create(@Valid @RequestBody TrnFrontlineDTO dto) {
        return R.ok("新增成功", frontlineService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("trn:frontline:edit")
    public R<Void> update(@Valid @RequestBody TrnFrontlineDTO dto) {
        frontlineService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("trn:frontline:remove")
    public R<Void> delete(@PathVariable Long id) {
        frontlineService.delete(id);
        return R.ok("删除成功", null);
    }
}
