package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.SalArchiveBatchCalcDTO;
import com.yang.hr.dto.SalArchiveCalcDTO;
import com.yang.hr.dto.SalArchiveUpdateDTO;
import com.yang.hr.vo.SalArchiveCalcResultVO;
import com.yang.hr.vo.SalArchiveVO;

/**
 * 工资档案服务（M5 批次3 · 唯一亮点「工资自动核算」的入口）
 *
 * 实发口径（用户 2026-10-13 拍板，别再问）：
 * <pre>
 *   实发 = 基本 + 加班 + 计件 + 计时 + 奖罚 − 社保 − 水电 − 其他扣款 − 个税
 *   基本 = 职工薪酬级别的 base_salary（算的时候打成快照）
 *   加班/计件/计时/水电 = 批次2 已算好落库的金额，直接 SUM，不重算
 *   奖罚 = 当月奖励合计 − 当月处罚合计（按 happen_date 归属）
 *   社保 = 该职工当月 hr_social_security.personal_amount（未登记按 0 并在结果里提示）
 *   其他扣款 / 个税 = 计算时默认 0，之后可手改
 * </pre>
 * 状态机：0 草稿 → 1 已确认 → 2 已发放；**已确认/已发放不可改、不可重算**，重算只覆盖草稿。
 */
public interface SalArchiveService {

    /** 条件分页：职工 / 月份 / 状态 */
    Page<SalArchiveVO> page(long current, long size, Long employeeId, String month, Integer status);

    SalArchiveVO getById(Long id);

    /** 单条计算入库：为某职工某月算一条档案（已有草稿则覆盖重算；已确认/已发放则拒绝） */
    Long create(SalArchiveCalcDTO dto);

    /** 批量计算入库：选月份（可指定职工），一次算完写档案，返回逐人明细与提示 */
    SalArchiveCalcResultVO calcBatch(SalArchiveBatchCalcDTO dto);

    /** 修改草稿：只开放「其他扣款 / 个税」两项手改，实发随之重算 */
    void update(SalArchiveUpdateDTO dto);

    /** 确认档案：0 草稿 → 1 已确认 */
    void confirm(Long id);

    /** 删除档案：仅草稿可删（删除时给 (employee_id, month) 唯一键让路） */
    void delete(Long id);
}
