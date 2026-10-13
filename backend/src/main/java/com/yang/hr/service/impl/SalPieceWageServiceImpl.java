package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.SalPieceWageDTO;
import com.yang.hr.entity.SalPieceWage;
import com.yang.hr.mapper.SalPieceWageMapper;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.SalPieceWageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 计件工资服务实现
 *
 * 金额口径：`amount = 数量 × 单价`，服务端算（DTO 里没有 amount）。
 */
@Service
public class SalPieceWageServiceImpl implements SalPieceWageService {

    private static final Logger log = LoggerFactory.getLogger(SalPieceWageServiceImpl.class);

    private static final int SCALE = 2;

    /**
     * 金额列 DECIMAL(12,2) 的上限。
     * ⚠️ 数量与单价各自可到 DECIMAL(10,2)（约 1e8），乘积可达 1e16 —— **必然**会超出 DECIMAL(12,2)，
     * 不拦就会撞 MySQL out-of-range 落进 500 兜底（本该是 400），故这里显式校验。
     */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private final SalPieceWageMapper pieceMapper;
    private final EmployeeService employeeService;

    public SalPieceWageServiceImpl(SalPieceWageMapper pieceMapper, EmployeeService employeeService) {
        this.pieceMapper = pieceMapper;
        this.employeeService = employeeService;
    }

    @Override
    public Page<SalPieceWage> page(long current, long size, Long employeeId, String month) {
        LambdaQueryWrapper<SalPieceWage> wrapper = new LambdaQueryWrapper<SalPieceWage>()
                .eq(employeeId != null, SalPieceWage::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(month), SalPieceWage::getMonth, trim(month))
                .orderByDesc(SalPieceWage::getMonth)
                .orderByDesc(SalPieceWage::getId);
        return pieceMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public SalPieceWage getById(Long id) {
        SalPieceWage entity = pieceMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "计件工资记录不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SalPieceWageDTO dto) {
        employeeService.assertExists(dto.getEmployeeId());
        BigDecimal quantity = nvl(dto.getQuantity());
        BigDecimal unitPrice = nvl(dto.getUnitPrice());

        SalPieceWage entity = new SalPieceWage();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setMonth(trim(dto.getMonth()));
        entity.setProductName(trimToNull(dto.getProductName()));
        entity.setQuantity(quantity);
        entity.setUnitPrice(unitPrice);
        entity.setAmount(calcAmount(quantity, unitPrice));
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        pieceMapper.insert(entity);
        log.info("新增计件工资：employeeId={} month={} quantity={} unitPrice={} amount={}（id={}）",
                entity.getEmployeeId(), entity.getMonth(), quantity, unitPrice, entity.getAmount(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SalPieceWageDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少计件工资记录ID");
        }
        SalPieceWage exist = getById(dto.getId());
        employeeService.assertExists(dto.getEmployeeId());

        // 数量/单价是 NOT NULL：不传沿用原值，金额按最终值重算
        BigDecimal quantity = dto.getQuantity() != null ? dto.getQuantity() : exist.getQuantity();
        BigDecimal unitPrice = dto.getUnitPrice() != null ? dto.getUnitPrice() : exist.getUnitPrice();
        BigDecimal amount = calcAmount(quantity, unitPrice);

        pieceMapper.update(null, new LambdaUpdateWrapper<SalPieceWage>()
                .eq(SalPieceWage::getId, dto.getId())
                .set(SalPieceWage::getEmployeeId, dto.getEmployeeId())
                .set(SalPieceWage::getMonth, trim(dto.getMonth()))
                .set(SalPieceWage::getProductName, trimToNull(dto.getProductName()))
                .set(dto.getQuantity() != null, SalPieceWage::getQuantity, dto.getQuantity())
                .set(dto.getUnitPrice() != null, SalPieceWage::getUnitPrice, dto.getUnitPrice())
                .set(SalPieceWage::getAmount, amount));
        log.info("修改计件工资：id={} employeeId={} month={} amount={}",
                dto.getId(), dto.getEmployeeId(), trim(dto.getMonth()), amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalPieceWage entity = getById(id);
        // 无唯一键（同职工同月可多条），直接逻辑删除
        pieceMapper.deleteById(id);
        log.info("删除计件工资：id={} employeeId={} month={}", id, entity.getEmployeeId(), entity.getMonth());
    }

    // ------------------------------------------------------------------ 内部方法

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal calcAmount(BigDecimal quantity, BigDecimal unitPrice) {
        BigDecimal amount = nvl(quantity).multiply(nvl(unitPrice)).setScale(SCALE, RoundingMode.HALF_UP);
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "计件金额 " + amount + " 超出可存储范围（上限 " + MAX_AMOUNT + "），请核对数量与单价");
        }
        return amount;
    }
}
