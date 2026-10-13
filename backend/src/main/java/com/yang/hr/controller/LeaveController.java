package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.LeaveApproveDTO;
import com.yang.hr.dto.LeaveDTO;
import com.yang.hr.entity.HrLeave;
import com.yang.hr.service.LeaveService;
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
 * 请假管理接口（人事管理 → 请假管理）
 *
 * 审批走专用接口 /{id}/approve：通用更新接口与提交接口都**刻意不碰**审批字段，
 * 避免"带 status 调 PUT 就能自己批自己"的后门（规划书 §9 第 23 条）。
 */
@RestController
@RequestMapping("/api/hr/leave")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /** 条件分页：按职工 / 状态 / 请假类型筛选 */
    @GetMapping("/page")
    @SaCheckPermission("hr:leave:list")
    public R<Page<HrLeave>> page(@RequestParam(defaultValue = "1") long current,
                                 @RequestParam(defaultValue = "10") long size,
                                 @RequestParam(required = false) Long employeeId,
                                 @RequestParam(required = false) Integer status,
                                 @RequestParam(required = false) Integer leaveType) {
        return R.ok(leaveService.page(current, size, employeeId, status, leaveType));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("hr:leave:list")
    public R<HrLeave> detail(@PathVariable Long id) {
        return R.ok(leaveService.getById(id));
    }

    /** 提交请假单（落库即「待审批」） */
    @PostMapping
    @SaCheckPermission("hr:leave:add")
    public R<Long> create(@Valid @RequestBody LeaveDTO dto) {
        return R.ok("提交成功", leaveService.create(dto));
    }

    /** 修改请假单（仅限「待审批」） */
    @PutMapping
    @SaCheckPermission("hr:leave:edit")
    public R<Void> update(@Valid @RequestBody LeaveDTO dto) {
        leaveService.update(dto);
        return R.ok("修改成功", null);
    }

    /** 审批请假单：status=1 通过 / 2 驳回。审批人与审批时间由服务端写入 */
    @PutMapping("/{id}/approve")
    @SaCheckPermission("hr:leave:edit")
    public R<Void> approve(@PathVariable Long id, @Valid @RequestBody LeaveApproveDTO dto) {
        leaveService.approve(id, dto);
        return R.ok("审批完成", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("hr:leave:remove")
    public R<Void> delete(@PathVariable Long id) {
        leaveService.delete(id);
        return R.ok("删除成功", null);
    }
}
