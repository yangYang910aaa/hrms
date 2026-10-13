package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.yang.hr.common.R;
import com.yang.hr.service.ReportService;
import com.yang.hr.vo.ReportOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报表统计接口（报表统计 → 统计图表）。
 *
 * 该模块**只读**，全部走 `report:chart:list` 一个权限点即可，不需要额外的按钮权限点。
 * 一页 8 图一次性返回（{@link ReportOverviewVO}），避免 8 次往返。
 */
@RestController
@RequestMapping("/api/report/chart")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 统计总览：性别 / 学历 / 年龄 / 工龄 / 部门人员 / 入职趋势 / 辞职趋势 / 离职率。
     *
     * @param year 趋势类统计年份，省略则用当前年
     */
    @GetMapping("/overview")
    @SaCheckPermission("report:chart:list")
    public R<ReportOverviewVO> overview(@RequestParam(required = false) Integer year) {
        return R.ok(reportService.overview(year));
    }
}
