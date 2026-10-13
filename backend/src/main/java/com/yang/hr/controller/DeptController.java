package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.DeptDTO;
import com.yang.hr.entity.SysDept;
import com.yang.hr.service.DeptService;
import com.yang.hr.vo.DeptTreeVO;
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
 * 组织结构（部门）接口
 *
 * 全部要求 system:dept:* 权限；M4 人事模块的表单若需要"部门下拉"，会另开一个只要求登录的
 * 公共选项接口，而不是把这里的权限放宽。
 */
@RestController
@RequestMapping("/api/system/dept")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    /** 分页 + 条件查询 */
    @GetMapping("/page")
    @SaCheckPermission("system:dept:list")
    public R<Page<SysDept>> page(@RequestParam(defaultValue = "1") long current,
                                @RequestParam(defaultValue = "10") long size,
                                @RequestParam(required = false) String deptName,
                                @RequestParam(required = false) Integer status) {
        return R.ok(deptService.page(current, size, deptName, status));
    }

    /** 部门树（下拉选择、左侧导航用） */
    @GetMapping("/tree")
    @SaCheckPermission("system:dept:list")
    public R<List<DeptTreeVO>> tree() {
        return R.ok(deptService.tree());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:dept:list")
    public R<SysDept> detail(@PathVariable Long id) {
        return R.ok(deptService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("system:dept:add")
    public R<Long> create(@Valid @RequestBody DeptDTO dto) {
        return R.ok("新增成功", deptService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("system:dept:edit")
    public R<Void> update(@Valid @RequestBody DeptDTO dto) {
        deptService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:dept:remove")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok("删除成功", null);
    }
}
