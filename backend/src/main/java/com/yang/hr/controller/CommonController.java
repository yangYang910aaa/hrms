package com.yang.hr.controller;

import com.yang.hr.common.R;
import com.yang.hr.service.DeptService;
import com.yang.hr.service.DictDataService;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.SalLevelService;
import com.yang.hr.service.TrnCourseService;
import com.yang.hr.vo.CourseOptionVO;
import com.yang.hr.vo.DeptTreeVO;
import com.yang.hr.vo.DictOptionVO;
import com.yang.hr.vo.EmployeeOptionVO;
import com.yang.hr.vo.SalaryLevelOptionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公共只读选项接口：只要求登录，不要业务权限。
 *
 * 设计意图（与交接文档 §9「M4 部门下拉」同一原则）：业务表单（职工编辑页的性别/学历下拉、部门下拉等）
 * 需要读字典项与部门树，但填表的人未必有 system:dict:list / system:dept:list 权限。
 * 与其把管理接口的权限放宽，不如单开一个"仅需登录"的只读入口。
 * SaTokenConfig 拦截 /api/**，本路径未在白名单，因此天然「仅需登录」。
 */
@RestController
@RequestMapping("/api/common")
public class CommonController {

    private final DictDataService dictDataService;
    private final DeptService deptService;
    private final EmployeeService employeeService;
    private final TrnCourseService trnCourseService;
    private final SalLevelService salLevelService;

    public CommonController(DictDataService dictDataService,
                            DeptService deptService,
                            EmployeeService employeeService,
                            TrnCourseService trnCourseService,
                            SalLevelService salLevelService) {
        this.dictDataService = dictDataService;
        this.deptService = deptService;
        this.employeeService = employeeService;
        this.trnCourseService = trnCourseService;
        this.salLevelService = salLevelService;
    }

    /** 按字典类型取启用中的字典项（类型不存在或已停用 → 空数组，不报错） */
    @GetMapping("/dict/{dictType}")
    public R<List<DictOptionVO>> dictItems(@PathVariable String dictType) {
        return R.ok(dictDataService.options(dictType));
    }

    /** 部门树（职工表单的"部门下拉"用，只含启用中的部门）—— 不要为此放宽 /api/system/dept/tree 的权限 */
    @GetMapping("/dept-tree")
    public R<List<DeptTreeVO>> deptTree() {
        return R.ok(deptService.enabledTree());
    }

    /** 职工下拉项（id / 工号 / 姓名）—— 人事各子表列表渲染职工姓名用 */
    @GetMapping("/employee-options")
    public R<List<EmployeeOptionVO>> employeeOptions() {
        return R.ok(employeeService.options());
    }

    /** 课程下拉项（id / 课程编号 / 课程名称）—— 培训期次表列表渲染课程名用 */
    @GetMapping("/course-options")
    public R<List<CourseOptionVO>> courseOptions() {
        return R.ok(trnCourseService.options());
    }

    /** 薪酬级别下拉项（id / 编号 / 名称 / 基本工资）—— 职工表单与加班标准表单选级别用 */
    @GetMapping("/salary-level-options")
    public R<List<SalaryLevelOptionVO>> salaryLevelOptions() {
        return R.ok(salLevelService.options());
    }
}
