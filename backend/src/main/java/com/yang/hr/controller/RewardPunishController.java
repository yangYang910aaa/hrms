package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.RewardPunishDTO;
import com.yang.hr.entity.HrRewardPunish;
import com.yang.hr.service.RewardPunishService;
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
 * 奖罚登记接口（人事管理 → 奖罚登记）
 */
@RestController
@RequestMapping("/api/hr/reward")
public class RewardPunishController {

    private final RewardPunishService rewardPunishService;

    public RewardPunishController(RewardPunishService rewardPunishService) {
        this.rewardPunishService = rewardPunishService;
    }

    /** 条件分页：按职工 / 类型筛选 */
    @GetMapping("/page")
    @SaCheckPermission("hr:reward:list")
    public R<Page<HrRewardPunish>> page(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) Long employeeId,
                                        @RequestParam(required = false) Integer type) {
        return R.ok(rewardPunishService.page(current, size, employeeId, type));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("hr:reward:list")
    public R<HrRewardPunish> detail(@PathVariable Long id) {
        return R.ok(rewardPunishService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("hr:reward:add")
    public R<Long> create(@Valid @RequestBody RewardPunishDTO dto) {
        return R.ok("新增成功", rewardPunishService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("hr:reward:edit")
    public R<Void> update(@Valid @RequestBody RewardPunishDTO dto) {
        rewardPunishService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("hr:reward:remove")
    public R<Void> delete(@PathVariable Long id) {
        rewardPunishService.delete(id);
        return R.ok("删除成功", null);
    }
}
