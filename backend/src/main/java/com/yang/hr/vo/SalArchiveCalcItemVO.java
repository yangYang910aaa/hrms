package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 工资档案「月度汇总」的单个职工明细（批量计算的返回项）。
 *
 * `action` 说明这次对该职工做了什么：`CREATED` 新建草稿 / `UPDATED` 覆盖原草稿 /
 * `SKIPPED` 跳过（已确认或已发放）。`notices` 是需要用户知道但不阻塞计算的提示
 * （如「未登记社保，按 0 扣缴」）。
 */
@Getter
@Setter
public class SalArchiveCalcItemVO {

    private Long employeeId;

    private String empNo;

    private String employeeName;

    private Integer employType;

    private Long levelId;

    private String levelName;

    private BigDecimal baseSalary;

    private BigDecimal overtimeAmount;

    private BigDecimal pieceAmount;

    private BigDecimal timeAmount;

    /** 奖罚调整（当月奖励 − 当月处罚） */
    private BigDecimal rewardAmount;

    /** 社保个人扣缴（未登记按 0） */
    private BigDecimal socialSecurity;

    /** 当月是否登记了社保（0 未登记 → socialSecurity 按 0 计） */
    private Integer socialSecurityRegistered;

    private BigDecimal utilityFee;

    private BigDecimal deductAmount;

    private BigDecimal tax;

    private BigDecimal netSalary;

    /** 本次动作：CREATED / UPDATED / SKIPPED */
    private String action;

    /** 已存在档案的 id（UPDATED / SKIPPED 时有值） */
    private Long archiveId;

    /** 档案当前状态（SKIPPED 时有值，用于说明为什么跳过） */
    private Integer archiveStatus;

    private List<String> notices = new ArrayList<>();

    public void addNotice(String notice) {
        this.notices.add(notice);
    }
}
