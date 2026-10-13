package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 薪酬-工资档案（sal_archive）—— 工资计算主表，M5 批次3 的核心
 *
 * 有**复合唯一键** `uk_employee_month(employee_id, month)`：一名职工每月一条档案。
 * 逻辑删除时把 `employee_id` 置为 **`-id`** 让出唯一键（见 SalArchiveServiceImpl）。
 *
 * **状态机**：0 草稿 → 1 已确认 → 2 已发放。
 * - 只有草稿可重算 / 可改 / 可删（已确认、已发放一律拒绝，防止改了已发出去的钱）；
 * - `deduct_amount`（其他扣款）与 `tax`（个税）是**唯一允许手改**的两项，其余列全部由服务端计算；
 * - `level_id` / `base_salary` 是**计算时的快照**，之后职工换薪酬级别不会改写历史档案。
 */
@Getter
@Setter
@TableName("sal_archive")
public class SalArchive extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 职工ID（hr_employee.id） */
    private Long employeeId;

    /** 所属月份 YYYY-MM */
    private String month;

    /** 当月薪酬级别快照（sal_level.id） */
    private Long levelId;

    /** 基本工资快照（取 sal_level.base_salary） */
    private BigDecimal baseSalary;

    /** 加班工资 = SUM(sal_overtime_record.amount) */
    private BigDecimal overtimeAmount;

    /** 计件工资 = SUM(sal_piece_wage.amount) */
    private BigDecimal pieceAmount;

    /** 计时工资 = SUM(sal_time_wage.amount) */
    private BigDecimal timeAmount;

    /** 奖罚调整 = 当月奖励合计 − 当月处罚合计（按 happen_date 归属） */
    private BigDecimal rewardAmount;

    /** 社保个人扣缴 = 该职工当月 hr_social_security.personal_amount（未登记按 0 并在结果里提示） */
    private BigDecimal socialSecurity;

    /** 水电费扣缴 = SUM(sal_utility_fee.total_fee) */
    private BigDecimal utilityFee;

    /** 其他扣款（默认 0，可手改） */
    private BigDecimal deductAmount;

    /** 个税（默认 0，可手改） */
    private BigDecimal tax;

    /** 实发工资 = 基本 + 加班 + 计件 + 计时 + 奖罚 − 社保 − 水电 − 其他扣款 − 个税 */
    private BigDecimal netSalary;

    /** 状态 0草稿 1已确认 2已发放 */
    private Integer status;

    /** 计算时间（每次重算刷新） */
    private LocalDateTime calcTime;
}
