package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成工资发放单的汇总结果
 */
@Getter
@Setter
public class SalPaymentGenResultVO {

    /** 生成的月份 YYYY-MM */
    private String month;

    /** 参与生成的「已确认档案」数 */
    private int total;

    /** 新建发放单数 */
    private int created;

    /** 跳过：该职工该月已有发放单 */
    private int skippedExists;

    /** 该月仍是草稿的档案数（需先确认才能生成发放单） */
    private int draftCount;

    /** 该月已发放的档案数 */
    private int paidCount;

    /** 提示（如「职工ID x 不存在」「有 2 条档案还是草稿，请先确认」） */
    private List<String> notices = new ArrayList<>();
}
