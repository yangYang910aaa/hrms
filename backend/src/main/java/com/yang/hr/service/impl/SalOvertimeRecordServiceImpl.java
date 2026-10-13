package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.SalOvertimeRecordDTO;
import com.yang.hr.entity.SalLevel;
import com.yang.hr.entity.SalOvertimeRecord;
import com.yang.hr.mapper.SalOvertimeRecordMapper;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.SalLevelService;
import com.yang.hr.service.SalOvertimeRecordService;
import com.yang.hr.service.SalOvertimeStandardService;
import com.yang.hr.vo.EmployeeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 加班登记服务实现
 *
 * **加班金额按 21.75 法定口径**（用户 2026-10-13 拍板）：
 * <pre>
 *   时薪   = 该职工薪酬级别的「基本工资」÷ 21.75 ÷ 8
 *   倍率   = sal_overtime_standard 中「该级别 × 该加班类型」的 rate
 *   金额   = 时薪 × 倍率 × 加班小时   （保留 2 位，HALF_UP）
 * </pre>
 * `rate` 与 `amount` 都由服务端算并落库（DTO 里没有这两个字段），保证口径不被前端绕过；
 * 落库的 rate 同时是"当时用的倍率"快照 —— 之后标准被改，也不影响已登记的金额。
 */
@Service
public class SalOvertimeRecordServiceImpl implements SalOvertimeRecordService {

    private static final Logger log = LoggerFactory.getLogger(SalOvertimeRecordServiceImpl.class);

    /** 制度月计薪天数（法定 21.75） */
    private static final BigDecimal MONTHLY_PAID_DAYS = new BigDecimal("21.75");

    /** 日标准工时 */
    private static final BigDecimal DAILY_HOURS = new BigDecimal("8");

    /** 时薪计算保留 6 位（中间精度），最终金额再收敛到 2 位，避免中间截断放大误差 */
    private static final int HOURLY_SCALE = 6;

    private static final int SCALE = 2;

    /** 金额列 DECIMAL(12,2) 的上限：派生金额超限会撞 MySQL out-of-range 落进 500 兜底，故提前拦成 400 */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private final SalOvertimeRecordMapper overtimeMapper;
    private final EmployeeService employeeService;
    private final SalLevelService levelService;
    private final SalOvertimeStandardService standardService;

    public SalOvertimeRecordServiceImpl(SalOvertimeRecordMapper overtimeMapper,
                                        EmployeeService employeeService,
                                        SalLevelService levelService,
                                        SalOvertimeStandardService standardService) {
        this.overtimeMapper = overtimeMapper;
        this.employeeService = employeeService;
        this.levelService = levelService;
        this.standardService = standardService;
    }

    @Override
    public Page<SalOvertimeRecord> page(long current, long size, Long employeeId, String month, Integer overtimeType) {
        LambdaQueryWrapper<SalOvertimeRecord> wrapper = new LambdaQueryWrapper<SalOvertimeRecord>()
                .eq(employeeId != null, SalOvertimeRecord::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(month), SalOvertimeRecord::getMonth, trim(month))
                .eq(overtimeType != null, SalOvertimeRecord::getOvertimeType, overtimeType)
                .orderByDesc(SalOvertimeRecord::getMonth)
                .orderByDesc(SalOvertimeRecord::getId);
        return overtimeMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public SalOvertimeRecord getById(Long id) {
        SalOvertimeRecord entity = overtimeMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "加班记录不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SalOvertimeRecordDTO dto) {
        SalLevel level = resolveLevel(dto.getEmployeeId());
        BigDecimal rate = resolveRate(level.getId(), dto.getOvertimeType());
        BigDecimal hours = nvl(dto.getHours());

        SalOvertimeRecord entity = new SalOvertimeRecord();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setMonth(trim(dto.getMonth()));
        entity.setOvertimeType(dto.getOvertimeType());
        entity.setHours(hours);
        entity.setRate(rate);
        entity.setAmount(calcAmount(level.getBaseSalary(), rate, hours));
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        overtimeMapper.insert(entity);
        log.info("新增加班登记：employeeId={} month={} type={} hours={} rate={} amount={}（id={}）",
                entity.getEmployeeId(), entity.getMonth(), entity.getOvertimeType(),
                entity.getHours(), entity.getRate(), entity.getAmount(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SalOvertimeRecordDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少加班记录ID");
        }
        SalOvertimeRecord exist = getById(dto.getId());
        // 派生列全部按「最终值」重算：换职工/换类型/改小时都会连带刷新倍率与金额
        SalLevel level = resolveLevel(dto.getEmployeeId());
        BigDecimal rate = resolveRate(level.getId(), dto.getOvertimeType());
        BigDecimal hours = dto.getHours() != null ? dto.getHours() : exist.getHours();

        overtimeMapper.update(null, new LambdaUpdateWrapper<SalOvertimeRecord>()
                .eq(SalOvertimeRecord::getId, dto.getId())
                .set(SalOvertimeRecord::getEmployeeId, dto.getEmployeeId())
                .set(SalOvertimeRecord::getMonth, trim(dto.getMonth()))
                .set(SalOvertimeRecord::getOvertimeType, dto.getOvertimeType())
                .set(dto.getHours() != null, SalOvertimeRecord::getHours, dto.getHours())
                .set(SalOvertimeRecord::getRate, rate)
                .set(SalOvertimeRecord::getAmount, calcAmount(level.getBaseSalary(), rate, hours)));
        log.info("修改加班登记：id={} employeeId={} type={} hours={} rate={}",
                dto.getId(), dto.getEmployeeId(), dto.getOvertimeType(), hours, rate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalOvertimeRecord entity = getById(id);
        // 无唯一键（同职工同月可多条），直接逻辑删除
        overtimeMapper.deleteById(id);
        log.info("删除加班登记：id={} employeeId={} month={}", id, entity.getEmployeeId(), entity.getMonth());
    }

    // ------------------------------------------------------------------ 内部方法

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 取职工对应的薪酬级别（兼作职工存在性校验）。没设级别就算不出时薪，明确报错而不是静默算 0 */
    private SalLevel resolveLevel(Long employeeId) {
        EmployeeVO employee = employeeService.getById(employeeId);
        Long levelId = employee.getSalaryLevelId();
        if (levelId == null) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "职工【" + employee.getName() + "】未设置薪酬级别，无法计算加班金额，请先在职工管理里设置");
        }
        return levelService.getById(levelId);
    }

    /** 取「该级别 × 该加班类型」的倍率；没配标准就算不出倍率，明确报错提示去配置 */
    private BigDecimal resolveRate(Long levelId, Integer overtimeType) {
        BigDecimal rate = standardService.findRate(levelId, overtimeType);
        if (rate == null) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "该薪酬级别未配置「" + typeLabel(overtimeType) + "」加班标准，请先在加班标准里配置");
        }
        return rate;
    }

    /**
     * 时薪 = 基本工资 ÷ 21.75 ÷ 8；金额 = 时薪 × 倍率 × 小时。
     * 先按 21.75×8 合并成除数（等价，少一次舍入），时薪留 6 位中间精度，金额收敛到 2 位。
     */
    private BigDecimal calcAmount(BigDecimal baseSalary, BigDecimal rate, BigDecimal hours) {
        BigDecimal hourly = nvl(baseSalary)
                .divide(MONTHLY_PAID_DAYS.multiply(DAILY_HOURS), HOURLY_SCALE, RoundingMode.HALF_UP);
        BigDecimal amount = hourly.multiply(nvl(rate)).multiply(nvl(hours)).setScale(SCALE, RoundingMode.HALF_UP);
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "加班金额 " + amount + " 超出可存储范围（上限 " + MAX_AMOUNT + "），请核对基本工资 / 倍率 / 加班小时");
        }
        return amount;
    }

    private String typeLabel(Integer overtimeType) {
        if (overtimeType == null) {
            return "未知";
        }
        return switch (overtimeType) {
            case 1 -> "工作日";
            case 2 -> "休息日";
            case 3 -> "节假日";
            default -> "未知";
        };
    }
}
