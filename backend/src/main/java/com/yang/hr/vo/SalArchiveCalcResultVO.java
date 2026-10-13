package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量计算工资档案的汇总结果
 */
@Getter
@Setter
public class SalArchiveCalcResultVO {

    /** 计算的月份 YYYY-MM */
    private String month;

    /** 参与计算的职工数 */
    private int total;

    /** 新建草稿数 */
    private int created;

    /** 覆盖重算的草稿数 */
    private int updated;

    /** 跳过数（已确认 / 已发放，不可重算） */
    private int skipped;

    /** 全局提示（如「传入的职工ID中有 2 个不存在」） */
    private List<String> notices = new ArrayList<>();

    /** 按职工的明细 */
    private List<SalArchiveCalcItemVO> items;
}
