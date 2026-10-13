package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 报表统计总览（公共查询/报表统计 → 统计图表一页 8 图的**一次性载荷**）。
 *
 * 口径说明（很重要，前端图例/注释沿用）：
 * - **人员结构类**（性别 / 学历 / 年龄 / 工龄 / 部门人员）只统计**在职人员**（employ_type ≠ 3，含实习），
 *   是"在职人员结构"的常规口径；每张图各项之和 = {@link #total}（无值走「未填」/「未分配」桶）。
 * - **流动类**（入职趋势 / 辞职趋势 / 离职率）用**全量**（含离职）：
 *   · 入职趋势 = 指定年份内 `hire_date` 落在该年的人，按月分组（1–12 月）；
 *   · 辞职趋势 = 指定年份内 `leave_date` 落在该年的人，按月分组；
 *   · 离职率 = 离职人数 ÷ (在职人数 + 离职人数) × 100%，保留 2 位小数。
 * - 统计范围是**未逻辑删除**的职工（MyBatis-Plus 自动过滤 deleted=1），不区分 status（停用也计入）。
 */
@Getter
@Setter
public class ReportOverviewVO {

    /** 在职人数（employ_type ≠ 3） */
    private long total;

    /** 离职人数（employ_type = 3） */
    private long resigned;

    /** 离职率（百分比，保留 2 位小数） */
    private BigDecimal turnoverRate;

    /** 趋势统计年份（缺省为当前年） */
    private int year;

    /** 性别分布（只统计在职） */
    private List<ReportItemVO> gender;

    /** 学历分布（只统计在职） */
    private List<ReportItemVO> education;

    /** 年龄分布（只统计在职，按出生日期算周岁分桶） */
    private List<ReportItemVO> age;

    /** 工龄分布（只统计在职，按入职日期算工龄分桶） */
    private List<ReportItemVO> workYears;

    /** 部门人员分布（只统计在职，未分配部门单列一桶） */
    private List<ReportItemVO> dept;

    /** 入职趋势（指定年份，1–12 月） */
    private List<ReportItemVO> hireTrend;

    /** 辞职趋势（指定年份，1–12 月） */
    private List<ReportItemVO> resignTrend;
}
