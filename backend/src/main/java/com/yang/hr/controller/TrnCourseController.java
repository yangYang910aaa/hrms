package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.TrnCourseDTO;
import com.yang.hr.entity.TrnCourse;
import com.yang.hr.service.TrnCourseService;
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
 * 培训课程接口（培训管理 → 课程管理）
 *
 * 全部要求 trn:course:* 权限。期次表单要"课程下拉"请用 /api/common/course-options（仅需登录）。
 */
@RestController
@RequestMapping("/api/trn/course")
public class TrnCourseController {

    private final TrnCourseService courseService;

    public TrnCourseController(TrnCourseService courseService) {
        this.courseService = courseService;
    }

    /** 条件分页：课程名模糊 / 类别 / 状态 */
    @GetMapping("/page")
    @SaCheckPermission("trn:course:list")
    public R<Page<TrnCourse>> page(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size,
                                   @RequestParam(required = false) String courseName,
                                   @RequestParam(required = false) Integer courseType,
                                   @RequestParam(required = false) Integer status) {
        return R.ok(courseService.page(current, size, courseName, courseType, status));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("trn:course:list")
    public R<TrnCourse> detail(@PathVariable Long id) {
        return R.ok(courseService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("trn:course:add")
    public R<Long> create(@Valid @RequestBody TrnCourseDTO dto) {
        return R.ok("新增成功", courseService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("trn:course:edit")
    public R<Void> update(@Valid @RequestBody TrnCourseDTO dto) {
        courseService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("trn:course:remove")
    public R<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return R.ok("删除成功", null);
    }
}
