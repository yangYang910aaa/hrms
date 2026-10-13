package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.SalArchiveBatchCalcDTO;
import com.yang.hr.dto.SalArchiveCalcDTO;
import com.yang.hr.dto.SalArchiveUpdateDTO;
import com.yang.hr.entity.SalArchive;
import com.yang.hr.mapper.SalArchiveMapper;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.SalArchiveService;
import com.yang.hr.vo.SalArchiveCalcItemVO;
import com.yang.hr.vo.SalArchiveCalcResultVO;
import com.yang.hr.vo.SalArchiveVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 工资档案服务实现 —— 工资自动核算的核心
 *
 * 计算全部走 {@link SalArchiveMapper#selectCalcRows} 一条 SQL 汇总（不重算批次2 的金额），
 * 落库后档案里的每一项都是**可追溯的快照**：之后改了加班标准 / 社保比例 / 薪酬级别，
 * 已生成的档案都不变，要变就显式重算。
 */
@Service
public class SalArchiveServiceImpl implements SalArchiveService {

    private static final Logger log = LoggerFactory.getLogger(SalArchiveServiceImpl.class);

    /** 状态：草稿 */
    private static final int STATUS_DRAFT = 0;

    /** 状态：已确认 */
    private static final int STATUS_CONFIRMED = 1;

    /** 状态：已发放 */
    private static final int STATUS_PAID = 2;

    private static final int SCALE = 2;

    /**
     * 金额列 DECIMAL(12,2) 的上限。
     * ⚠️ 实发是 7 项相加，单项各可到 1e10，求和可达 7e10 —— 必然超出列上限，
     * 不拦就会撞 MySQL out-of-range 落 500（本该 400），故算完比一次。
     */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private static final BigDecimal MIN_AMOUNT = MAX_AMOUNT.negate();

    private final SalArchiveMapper archiveMapper;
    private final EmployeeService employeeService;

    public SalArchiveServiceImpl(SalArchiveMapper archiveMapper, EmployeeService employeeService) {
        this.archiveMapper = archiveMapper;
        this.employeeService = employeeService;
    }

    @Override
    public Page<SalArchiveVO> page(long current, long size, Long employeeId, String month, Integer status) {
        Page<SalArchiveVO> page = new Page<>(current, size);
        archiveMapper.selectVoPage(page, employeeId, StringUtils.hasText(month) ? month.trim() : null, status);
        return page;
    }

    @Override
    public SalArchiveVO getById(Long id) {
        SalArchiveVO vo = archiveMapper.selectVoById(id);
        if (vo == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "工资档案不存在或已删除");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SalArchiveCalcDTO dto) {
        employeeService.assertExists(dto.getEmployeeId());
        String month = trim(dto.getMonth());
        SalArchive exist = findByEmployeeMonth(dto.getEmployeeId(), month);
        if (exist != null) {
            assertDraft(exist, "重算");
        }
        SalArchiveCalcItemVO row = calcRow(dto.getEmployeeId(), month);
        fill(row, exist);
        persist(row, exist, month);
        log.info("计算工资档案：employeeId={} month={} 实发={}（{}，id={}）",
                dto.getEmployeeId(), month, row.getNetSalary(),
                exist == null ? "新建" : "覆盖重算", row.getArchiveId());
        return row.getArchiveId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalArchiveCalcResultVO calcBatch(SalArchiveBatchCalcDTO dto) {
        String month = trim(dto.getMonth());
        List<Long> wanted = normalize(dto.getEmployeeIds());
        List<SalArchiveCalcItemVO> rows = archiveMapper.selectCalcRows(month, null);

        SalArchiveCalcResultVO result = new SalArchiveCalcResultVO();
        result.setMonth(month);
        result.setItems(new ArrayList<>());

        Set<Long> found = new HashSet<>();
        List<SalArchiveCalcItemVO> picked = new ArrayList<>();
        for (SalArchiveCalcItemVO row : rows) {
            found.add(row.getEmployeeId());
            if (!wanted.isEmpty() && !wanted.contains(row.getEmployeeId())) {
                continue;
            }
            picked.add(row);
        }
        if (!wanted.isEmpty()) {
            for (Long id : wanted) {
                if (!found.contains(id)) {
                    result.getNotices().add("职工ID " + id + " 不存在或已删除，已跳过");
                }
            }
        }

        for (SalArchiveCalcItemVO row : picked) {
            result.setTotal(result.getTotal() + 1);
            SalArchive exist = findByEmployeeMonth(row.getEmployeeId(), month);
            if (exist != null && exist.getStatus() != STATUS_DRAFT) {
                // 已确认 / 已发放：重算只覆盖草稿，避免改了已经发出去的钱
                row.setAction("SKIPPED");
                row.setArchiveId(exist.getId());
                row.setArchiveStatus(exist.getStatus());
                row.addNotice("该月档案已" + statusLabel(exist.getStatus()) + "，重算只覆盖草稿，已跳过");
                result.setSkipped(result.getSkipped() + 1);
                result.getItems().add(row);
                continue;
            }
            if (row.getLevelId() == null) {
                row.setAction("SKIPPED");
                row.addNotice("未设置薪酬级别，算不出基本工资，已跳过（请先在职工管理里设置）");
                result.setSkipped(result.getSkipped() + 1);
                result.getItems().add(row);
                continue;
            }
            try {
                // fill 与 persist 都要隔离：实发超限是在 fill 里算出来的，不纳入隔离照样会整批失败
                fill(row, exist);
                persist(row, exist, month);
            } catch (BizException e) {
                // 单个人算不了（如实发超限）不该让整批回滚：记为跳过，把原因带回前端。
                // 已写入的部分保留（本方法仍是同一个事务，只是不再向上抛）。
                row.setAction("SKIPPED");
                row.addNotice(e.getMessage());
                result.setSkipped(result.getSkipped() + 1);
                result.getItems().add(row);
                log.warn("批量计算跳过职工 {}：{}", row.getEmployeeId(), e.getMessage());
                continue;
            }
            if (exist == null) {
                row.setAction("CREATED");
                result.setCreated(result.getCreated() + 1);
            } else {
                row.setAction("UPDATED");
                result.setUpdated(result.getUpdated() + 1);
            }
            result.getItems().add(row);
        }
        log.info("批量计算工资档案：month={} 共{}人 新建{} 重算{} 跳过{}",
                month, result.getTotal(), result.getCreated(), result.getUpdated(), result.getSkipped());
        // 结果级汇总：前端想一次性提示"跳过 N 人"不必自行聚合各人明细
        if (result.getSkipped() > 0) {
            result.getNotices().add("共跳过 " + result.getSkipped() + " 人，原因见各人明细");
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SalArchiveUpdateDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少工资档案ID");
        }
        SalArchive entity = archiveMapper.selectById(dto.getId());
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "工资档案不存在或已删除");
        }
        assertDraft(entity, "修改");

        // 只有「其他扣款 / 个税」可手改；不传保持原值，要清零必须显式传 0
        BigDecimal deduct = dto.getDeductAmount() != null ? dto.getDeductAmount() : entity.getDeductAmount();
        BigDecimal tax = dto.getTax() != null ? dto.getTax() : entity.getTax();
        // 实发按最终值重算，口径与计算时完全一致（只算一次：它可能抛 400，不该被算两遍）
        BigDecimal net = netOf(entity, deduct, tax);

        archiveMapper.update(null, new LambdaUpdateWrapper<SalArchive>()
                .eq(SalArchive::getId, dto.getId())
                .set(dto.getDeductAmount() != null, SalArchive::getDeductAmount, dto.getDeductAmount())
                .set(dto.getTax() != null, SalArchive::getTax, dto.getTax())
                .set(SalArchive::getNetSalary, net));
        log.info("修改工资档案草稿：id={} 其他扣款={} 个税={} 实发={} 原因={}",
                dto.getId(), deduct, tax, net, dto.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        SalArchive entity = archiveMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "工资档案不存在或已删除");
        }
        if (entity.getStatus() == null || entity.getStatus() != STATUS_DRAFT) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "只有草稿状态可以确认（当前：" + statusLabel(entity.getStatus()) + "）");
        }
        archiveMapper.update(null, new LambdaUpdateWrapper<SalArchive>()
                .eq(SalArchive::getId, id)
                .set(SalArchive::getStatus, STATUS_CONFIRMED));
        log.info("确认工资档案：id={} employeeId={} month={}", id, entity.getEmployeeId(), entity.getMonth());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalArchive entity = archiveMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "工资档案不存在或已删除");
        }
        assertDraft(entity, "删除");
        releaseUniqueKey(entity);
        archiveMapper.deleteById(id);
        log.info("删除工资档案：id={} employeeId={} month={}", id, entity.getEmployeeId(), entity.getMonth());
    }

    // ------------------------------------------------------------------ 内部方法

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private List<Long> normalize(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private SalArchive findByEmployeeMonth(Long employeeId, String month) {
        return archiveMapper.selectOne(new LambdaQueryWrapper<SalArchive>()
                .eq(SalArchive::getEmployeeId, employeeId)
                .eq(SalArchive::getMonth, month));
    }

    private void assertDraft(SalArchive entity, String action) {
        if (entity.getStatus() == null || entity.getStatus() != STATUS_DRAFT) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "该月档案已" + statusLabel(entity.getStatus()) + "，不能" + action
                            + "（已确认/已发放的工资不允许改动）");
        }
    }

    /**
     * 派生单项（加班/计件/计时/奖罚/社保/水电）落库前逐一比上限。
     * ⚠️ 实发有 netOf 把守，但单项各自也是 DECIMAL(12,2)：同月几千条子表明理时
     * SUM 仍可超限，不拦就会撞 MySQL out-of-range 落 500（本该 400）。
     */
    private void checkAmount(BigDecimal value, String label) {
        if (value != null && (value.compareTo(MAX_AMOUNT) > 0 || value.compareTo(MIN_AMOUNT) < 0)) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    label + " " + value + " 超出可存储范围（±" + MAX_AMOUNT + "），请核对当月登记明细");
        }
    }

    /** 取单个职工的当月汇总行；职工没设薪酬级别时明确报错，而不是静默按 0 算 */
    private SalArchiveCalcItemVO calcRow(Long employeeId, String month) {
        List<SalArchiveCalcItemVO> rows = archiveMapper.selectCalcRows(month, employeeId);
        if (rows.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "职工不存在或已删除");
        }
        SalArchiveCalcItemVO row = rows.get(0);
        if (row.getLevelId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "职工【" + row.getEmployeeName() + "】未设置薪酬级别，算不出基本工资，请先在职工管理里设置");
        }
        return row;
    }

    /**
     * 把汇总行补成"可落库"的完整明细：手改项沿用原草稿值（重算不冲掉用户手改的税/其他扣款）、
     * 算实发、附提示。`exist` 为 null 表示新建（手改项默认 0）。
     */
    private void fill(SalArchiveCalcItemVO row, SalArchive exist) {
        BigDecimal deduct = exist != null && exist.getDeductAmount() != null
                ? exist.getDeductAmount() : BigDecimal.ZERO;
        BigDecimal tax = exist != null && exist.getTax() != null ? exist.getTax() : BigDecimal.ZERO;
        row.setDeductAmount(deduct);
        row.setTax(tax);
        row.setNetSalary(netOf(row.getBaseSalary(), row.getOvertimeAmount(), row.getPieceAmount(),
                row.getTimeAmount(), row.getRewardAmount(), row.getSocialSecurity(),
                row.getUtilityFee(), deduct, tax));

        if (row.getSocialSecurityRegistered() == null || row.getSocialSecurityRegistered() == 0) {
            row.addNotice("当月未登记社保，个人扣缴按 0 计");
        }
        if (row.getEmployType() != null && row.getEmployType() == 3) {
            row.addNotice("该职工已离职，仍按当月登记结清工资");
        }
        if (isZero(row.getOvertimeAmount()) && isZero(row.getPieceAmount()) && isZero(row.getTimeAmount())
                && isZero(row.getRewardAmount()) && isZero(row.getUtilityFee())) {
            row.addNotice("当月无加班/计件/计时/奖罚/水电登记，仅发基本工资");
        }
    }

    /** 落库（新建或覆盖草稿） */
    private void persist(SalArchiveCalcItemVO row, SalArchive exist, String month) {
        // 派生单项逐一防溢出（netOf 只把守实发合计，单项超限照样会撞库）
        checkAmount(row.getOvertimeAmount(), "当月加班合计");
        checkAmount(row.getPieceAmount(), "当月计件合计");
        checkAmount(row.getTimeAmount(), "当月计时合计");
        checkAmount(row.getRewardAmount(), "当月奖罚净额");
        checkAmount(row.getSocialSecurity(), "当月社保个人扣缴");
        checkAmount(row.getUtilityFee(), "当月水电合计");

        SalArchive entity = exist != null ? exist : new SalArchive();
        entity.setEmployeeId(row.getEmployeeId());
        entity.setMonth(month);
        entity.setLevelId(row.getLevelId());
        // baseSalary 是唯一可能为 null 的金额（职工设了级别、但该级别被删时 LEFT JOIN 带不出来）——
        // 列是 NOT NULL，不兜会把 null 写进去撞库落 500，故这里 nvl 兜 0。
        // socialSecurity 同款兜底：SQL 已在外层 COALESCE 保证非 null，这里双保险，
        // 杜绝 null 被 updateById 跳过导致「重算后旧扣缴残留、实发却没扣」的自相矛盾。
        entity.setBaseSalary(nvl(row.getBaseSalary()));
        entity.setOvertimeAmount(nvl(row.getOvertimeAmount()));
        entity.setPieceAmount(nvl(row.getPieceAmount()));
        entity.setTimeAmount(nvl(row.getTimeAmount()));
        entity.setRewardAmount(nvl(row.getRewardAmount()));
        entity.setSocialSecurity(nvl(row.getSocialSecurity()));
        entity.setUtilityFee(nvl(row.getUtilityFee()));
        entity.setDeductAmount(row.getDeductAmount());
        entity.setTax(row.getTax());
        entity.setNetSalary(row.getNetSalary());
        entity.setStatus(STATUS_DRAFT);
        entity.setCalcTime(LocalDateTime.now());
        if (exist == null) {
            entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
            archiveMapper.insert(entity);
        } else {
            archiveMapper.updateById(entity);
        }
        row.setArchiveId(entity.getId());
    }

    /** 改草稿时重算实发：沿用档案里已存的其他项，只换成新手改的扣款/个税 */
    private BigDecimal netOf(SalArchive entity, BigDecimal deduct, BigDecimal tax) {
        return netOf(entity.getBaseSalary(), entity.getOvertimeAmount(), entity.getPieceAmount(),
                entity.getTimeAmount(), entity.getRewardAmount(), entity.getSocialSecurity(),
                entity.getUtilityFee(), deduct, tax);
    }

    /**
     * 实发 = 基本 + 加班 + 计件 + 计时 + 奖罚 − 社保 − 水电 − 其他扣款 − 个税
     *
     * 实发允许为负（扣缴超过应发时如实反映，不额外设下限，公式即口径）；
     * 但绝对值超出 DECIMAL(12,2) 时必须拦成 400，否则撞 MySQL out-of-range 落 500。
     */
    private BigDecimal netOf(BigDecimal base, BigDecimal overtime, BigDecimal piece, BigDecimal time,
                             BigDecimal reward, BigDecimal social, BigDecimal utility,
                             BigDecimal deduct, BigDecimal tax) {
        BigDecimal net = nvl(base).add(nvl(overtime)).add(nvl(piece)).add(nvl(time))
                .add(nvl(reward))
                .subtract(nvl(social)).subtract(nvl(utility))
                .subtract(nvl(deduct)).subtract(nvl(tax))
                .setScale(SCALE, RoundingMode.HALF_UP);
        if (net.compareTo(MAX_AMOUNT) > 0 || net.compareTo(MIN_AMOUNT) < 0) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "实发工资 " + net + " 超出可存储范围（±" + MAX_AMOUNT + "），请核对各项金额");
        }
        return net;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isZero(BigDecimal value) {
        return value == null || value.signum() == 0;
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case STATUS_DRAFT -> "草稿";
            case STATUS_CONFIRMED -> "确认";
            case STATUS_PAID -> "发放";
            default -> "未知状态";
        };
    }

    /**
     * 逻辑删除前让出复合唯一键 (employee_id, month)：把 `employee_id` 置为 **`-id`**。
     * 与批次1/批次2 的"让路"约定一致（负值唯一，已删行按 deleted=0 过滤查不到）。
     */
    private void releaseUniqueKey(SalArchive entity) {
        Long employeeId = entity.getEmployeeId();
        if (employeeId == null || employeeId < 0) {
            return;
        }
        archiveMapper.update(null, new LambdaUpdateWrapper<SalArchive>()
                .eq(SalArchive::getId, entity.getId())
                .set(SalArchive::getEmployeeId, -entity.getId()));
    }
}
