package com.yang.hr.service;

import com.yang.hr.vo.ReportOverviewVO;

/**
 * 报表统计服务（报表统计 → 统计图表）。
 */
public interface ReportService {

    /**
     * 统计图表总览：一页 8 图所需的全部聚合数据。
     *
     * @param year 趋势类（入职/辞职/离职率）统计年份；传 null 用当前年
     */
    ReportOverviewVO overview(Integer year);
}
