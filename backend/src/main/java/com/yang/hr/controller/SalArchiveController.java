package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.SalArchiveBatchCalcDTO;
import com.yang.hr.dto.SalArchiveCalcDTO;
import com.yang.hr.dto.SalArchiveUpdateDTO;
import com.yang.hr.service.SalArchiveService;
import com.yang.hr.vo.SalArchiveCalcResultVO;
import com.yang.hr.vo.SalArchiveVO;
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
 * 工资档案接口（薪酬管理 → 工资档案）—— M5 批次3 的「工资自动核算」
 *
 * | 方法 | 路径 | 权限 | 说明 |
 * |---|---|---|---|
 * | GET | `/page` | `sal:archive:list` | 条件分页 |
 * | GET | `/{id}` | `sal:archive:list` | 详情 |
 * | POST | `/` | `sal:archive:add` | **单条计算入库**（职工 + 月份） |
 * | POST | `/calc` | `sal:archive:calc` | **批量计算入库**（月份，可指定职工） |
 * | PUT | `/` | `sal:archive:edit` | 改草稿（只开放其他扣款 / 个税） |
 * | PUT | `/{id}/confirm` | `sal:archive:confirm` | 确认（草稿 → 已确认） |
 * | DELETE | `/{id}` | `sal:archive:remove` | 删除（仅草稿） |
 *
 * 实发 = 基本 + 加班 + 计件 + 计时 + 奖罚 − 社保 − 水电 − 其他扣款 − 个税，全部服务端算。
 */
@RestController
@RequestMapping("/api/sal/archive")
public class SalArchiveController {

    private final SalArchiveService archiveService;

    public SalArchiveController(SalArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @GetMapping("/page")
    @SaCheckPermission("sal:archive:list")
    public R<Page<SalArchiveVO>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) Long employeeId,
                                      @RequestParam(required = false) String month,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(archiveService.page(current, size, employeeId, month, status));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("sal:archive:list")
    public R<SalArchiveVO> detail(@PathVariable Long id) {
        return R.ok(archiveService.getById(id));
    }

    /** 单条计算入库：为某职工某月算一条档案（已有草稿则覆盖重算） */
    @PostMapping
    @SaCheckPermission("sal:archive:add")
    public R<Long> create(@Valid @RequestBody SalArchiveCalcDTO dto) {
        return R.ok("计算完成", archiveService.create(dto));
    }

    /** 批量计算入库：选月份（可指定职工），返回逐人明细与提示 */
    @PostMapping("/calc")
    @SaCheckPermission("sal:archive:calc")
    public R<SalArchiveCalcResultVO> calc(@Valid @RequestBody SalArchiveBatchCalcDTO dto) {
        return R.ok("计算完成", archiveService.calcBatch(dto));
    }

    /** 改草稿：只开放「其他扣款 / 个税」两项手改，实发随之重算 */
    @PutMapping
    @SaCheckPermission("sal:archive:edit")
    public R<Void> update(@Valid @RequestBody SalArchiveUpdateDTO dto) {
        archiveService.update(dto);
        return R.ok("修改成功", null);
    }

    /** 确认档案：0 草稿 → 1 已确认（确认后不可改、不可重算） */
    @PutMapping("/{id}/confirm")
    @SaCheckPermission("sal:archive:confirm")
    public R<Void> confirm(@PathVariable Long id) {
        archiveService.confirm(id);
        return R.ok("已确认", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("sal:archive:remove")
    public R<Void> delete(@PathVariable Long id) {
        archiveService.delete(id);
        return R.ok("删除成功", null);
    }
}
