package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.vo.EmployeeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 职工查询接口（公共查询 → 职工查询）。
 *
 * 这是「职工主档」的**只读**入口：查询条件与分页结果都与 `/api/hr/employee/page` 同构，
 * 直接复用 {@link EmployeeService#page}，不重复实现一套查询口径；差别只在**权限点**——
 * 本接口要 `query:employee:list`，而不是管理面的 `hr:employee:list`。
 * 这样「只给公共查询权限、不给人事管理权限」的角色也能查人，不用把管理接口的权限放宽。
 */
@RestController
@RequestMapping("/api/query/employee")
public class QueryEmployeeController {

    private final EmployeeService employeeService;

    public QueryEmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /** 条件分页：姓名模糊 / 部门 / 在职状态 / 状态 */
    @GetMapping("/page")
    @SaCheckPermission("query:employee:list")
    public R<Page<EmployeeVO>> page(@RequestParam(defaultValue = "1") long current,
                                    @RequestParam(defaultValue = "10") long size,
                                    @RequestParam(required = false) String name,
                                    @RequestParam(required = false) Long deptId,
                                    @RequestParam(required = false) Integer employType,
                                    @RequestParam(required = false) Integer status) {
        return R.ok(employeeService.page(current, size, name, deptId, employType, status));
    }

    /** 职工详情（联表带部门名/级别名），供查询页"查看"弹窗 */
    @GetMapping("/{id}")
    @SaCheckPermission("query:employee:list")
    public R<EmployeeVO> detail(@PathVariable Long id) {
        return R.ok(employeeService.getById(id));
    }
}
