package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.EmployeeDTO;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.FileStorageService;
import com.yang.hr.vo.EmployeeVO;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 职工主档接口（人事管理 → 职工管理）
 *
 * 全部要求 hr:employee:* 权限。业务表单要"部门下拉"请用 /api/common/dept-tree（仅需登录）。
 */
@RestController
@RequestMapping("/api/hr/employee")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final FileStorageService fileStorageService;

    public EmployeeController(EmployeeService employeeService, FileStorageService fileStorageService) {
        this.employeeService = employeeService;
        this.fileStorageService = fileStorageService;
    }

    /** 条件分页：姓名模糊 / 部门 / 在职状态 / 状态 */
    @GetMapping("/page")
    @SaCheckPermission("hr:employee:list")
    public R<Page<EmployeeVO>> page(@RequestParam(defaultValue = "1") long current,
                                    @RequestParam(defaultValue = "10") long size,
                                    @RequestParam(required = false) String name,
                                    @RequestParam(required = false) Long deptId,
                                    @RequestParam(required = false) Integer employType,
                                    @RequestParam(required = false) Integer status) {
        return R.ok(employeeService.page(current, size, name, deptId, employType, status));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("hr:employee:list")
    public R<EmployeeVO> detail(@PathVariable Long id) {
        return R.ok(employeeService.getById(id));
    }

    /**
     * 某月生日的职工（生日提醒），缺省当月。
     * 路径 `/birthday` 是字面量，优先级高于 `/{id}` —— 由 m4d 端到端脚本验证过。
     */
    @GetMapping("/birthday")
    @SaCheckPermission("hr:employee:list")
    public R<List<EmployeeVO>> birthday(@RequestParam(required = false) Integer month) {
        return R.ok(employeeService.birthdays(month));
    }

    /**
     * 上传职工照片，返回可对外访问的 URL（前端再把它写进 photo 字段）。
     * 新增职工与编辑职工都会用到，所以权限取 add / edit 的「或」。
     */
    @PostMapping("/photo")
    @SaCheckPermission(value = {"hr:employee:add", "hr:employee:edit"}, mode = SaMode.OR)
    public R<String> uploadPhoto(@RequestParam("file") MultipartFile file) {
        return R.ok("上传成功", fileStorageService.storeImage(file));
    }

    @PostMapping
    @SaCheckPermission("hr:employee:add")
    public R<Long> create(@Valid @RequestBody EmployeeDTO dto) {
        return R.ok("新增成功", employeeService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("hr:employee:edit")
    public R<Void> update(@Valid @RequestBody EmployeeDTO dto) {
        employeeService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("hr:employee:remove")
    public R<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return R.ok("删除成功", null);
    }
}
