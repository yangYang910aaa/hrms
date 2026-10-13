package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 报表单项（一个分类名 + 一个计数值），供 ECharts 的 series.data 直接消费。
 *
 * 统一的「name/value」形状让 8 张图共用同一套前端渲染代码（饼图/柱状图都吃 name+value）。
 */
@Getter
@Setter
public class ReportItemVO {

    /** 分类名（如「男」「本科」「25-29岁」「技术部」「3月」），展示用 */
    private String name;

    /** 计数值（人数） */
    private long value;

    public ReportItemVO() {
    }

    public ReportItemVO(String name, long value) {
        this.name = name;
        this.value = value;
    }
}
