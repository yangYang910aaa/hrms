package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.SalTimeWageDTO;
import com.yang.hr.entity.SalTimeWage;
import com.yang.hr.mapper.SalTimeWageMapper;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.SalTimeWageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 计时工资服务实现
 *
 * 金额口径：`amount = 工时 × 时薪`，服务端算（DTO 里没有 amount）。
 */
@Service
public class SalTimeWageServiceImpl implements SalTimeWageService {

    private static final Logger log = LoggerFactory.getLogger(SalTimeWageServiceImpl.class);

    private static final int SCALE = 2;

    /**
     * 金额列 DECIMAL(12,2) 的上限。
     * ⚠️ 时薪可到 DECIMAL(10,2)（约 1e8），工时到 DECIMAL(6,1)（约 1e5），乘积可达 1e13，
     * 会超出 DECIMAL(12,2)，故显式拦成 400（否则落进 500 兜底）。
     */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private final SalTimeWageMapper timeMapper;
    private final EmployeeService employeeService;

    public SalTimeWageServiceImpl(SalTimeWageMapper timeMapper, EmployeeService employeeService) {
        this.timeMapper = timeMapper;
        this.employeeService = employeeService;
    }

    @Override
    public Page<SalTimeWage> page(long current, long size, Long employeeId, String month) {
        LambdaQueryWrapper<SalTimeWage> wrapper = new LambdaQueryWrapper<SalTimeWage>()
                .eq(employeeId != null, SalTimeWage::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(month), SalTimeWage::getMonth, trim(month))
                .orderByDesc(SalTimeWage::getMonth)
                .orderByDesc(SalTimeWage::getId);
        return timeMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public SalTimeWage getById(Long id) {
        SalTimeWage entity = timeMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "计时工资记录不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SalTimeWageDTO dto) {
        employeeService.assertExists(dto.getEmployeeId());
        BigDecimal hourlyRate = nvl(dto.getHourlyRate());
        BigDecimal hours = nvl(dto.getHours());

        SalTimeWage entity = new SalTimeWage();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setMonth(trim(dto.getMonth()));
        entity.setHourlyRate(hourlyRate);
        entity.setHours(hours);
        entity.setAmount(calcAmount(hours, hourlyRate));
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        timeMapper.insert(entity);
        log.info("新增计时工资：employeeId={} month={} hourlyRate={} hours={} amount={}（id={}）",
                entity.getEmployeeId(), entity.getMonth(), hourlyRate, hours, entity.getAmount(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SalTimeWageDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少计时工资记录ID");
        }
        SalTimeWage exist = getById(dto.getId());
        employeeService.assertExists(dto.getEmployeeId());

        // 时薪/工时是 NOT NULL：不传沿用原值，金额按最终值重算
        BigDecimal hourlyRate = dto.getHourlyRate() != null ? dto.getHourlyRate() : exist.getHourlyRate();
        BigDecimal hours = dto.getHours() != null ? dto.getHours() : exist.getHours();
        BigDecimal amount = calcAmount(hours, hourlyRate);

        timeMapper.update(null, new LambdaUpdateWrapper<SalTimeWage>()
                .eq(SalTimeWage::getId, dto.getId())
                .set(SalTimeWage::getEmployeeId, dto.getEmployeeId())
                .set(SalTimeWage::getMonth, trim(dto.getMonth()))
                .set(dto.getHourlyRate() != null, SalTimeWage::getHourlyRate, dto.getHourlyRate())
                .set(dto.getHours() != null, SalTimeWage::getHours, dto.getHours())
                .set(SalTimeWage::getAmount, amount));
        log.info("修改计时工资：id={} employeeId={} month={} amount={}",
                dto.getId(), dto.getEmployeeId(), trim(dto.getMonth()), amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalTimeWage entity = getById(id);
        // 无唯一键（同职工同月可多条），直接逻辑删除
        timeMapper.deleteById(id);
        log.info("删除计时工资：id={} employeeId={} month={}", id, entity.getEmployeeId(), entity.getMonth());
    }

    // ------------------------------------------------------------------ 内部方法

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal calcAmount(BigDecimal hours, BigDecimal hourlyRate) {
        BigDecimal amount = nvl(hours).multiply(nvl(hourlyRate)).setScale(SCALE, RoundingMode.HALF_UP);
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "计时金额 " + amount + " 超出可存储范围（上限 " + MAX_AMOUNT + "），请核对工时与时薪");
        }
        return amount;
    }
}
