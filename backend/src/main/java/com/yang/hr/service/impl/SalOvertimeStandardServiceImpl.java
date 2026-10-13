package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.SalOvertimeStandardDTO;
import com.yang.hr.entity.SalOvertimeStandard;
import com.yang.hr.mapper.SalOvertimeStandardMapper;
import com.yang.hr.service.SalLevelService;
import com.yang.hr.service.SalOvertimeStandardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 加班标准服务实现
 */
@Service
public class SalOvertimeStandardServiceImpl implements SalOvertimeStandardService {

    private static final Logger log = LoggerFactory.getLogger(SalOvertimeStandardServiceImpl.class);

    /** 倍率默认值（DB 列 DEFAULT 1.00） */
    private static final BigDecimal DEFAULT_RATE = BigDecimal.ONE;

    private final SalOvertimeStandardMapper standardMapper;
    private final SalLevelService levelService;

    public SalOvertimeStandardServiceImpl(SalOvertimeStandardMapper standardMapper,
                                          SalLevelService levelService) {
        this.standardMapper = standardMapper;
        this.levelService = levelService;
    }

    @Override
    public Page<SalOvertimeStandard> page(long current, long size, Long levelId, Integer overtimeType) {
        LambdaQueryWrapper<SalOvertimeStandard> wrapper = new LambdaQueryWrapper<SalOvertimeStandard>()
                .eq(levelId != null, SalOvertimeStandard::getLevelId, levelId)
                .eq(overtimeType != null, SalOvertimeStandard::getOvertimeType, overtimeType)
                .orderByAsc(SalOvertimeStandard::getLevelId)
                .orderByAsc(SalOvertimeStandard::getOvertimeType);
        return standardMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public SalOvertimeStandard getById(Long id) {
        SalOvertimeStandard entity = standardMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "加班标准不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SalOvertimeStandardDTO dto) {
        levelService.assertExists(dto.getLevelId());
        assertLevelTypeUnique(dto.getLevelId(), dto.getOvertimeType(), null);

        SalOvertimeStandard entity = new SalOvertimeStandard();
        entity.setLevelId(dto.getLevelId());
        entity.setOvertimeType(dto.getOvertimeType());
        entity.setRate(dto.getRate() == null ? DEFAULT_RATE : dto.getRate());
        entity.setRemark(dto.getRemark());
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        standardMapper.insert(entity);
        log.info("新增加班标准：levelId={} type={} rate={}（id={}）",
                entity.getLevelId(), entity.getOvertimeType(), entity.getRate(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SalOvertimeStandardDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少加班标准ID");
        }
        getById(dto.getId());
        levelService.assertExists(dto.getLevelId());
        assertLevelTypeUnique(dto.getLevelId(), dto.getOvertimeType(), dto.getId());

        standardMapper.update(null, new LambdaUpdateWrapper<SalOvertimeStandard>()
                .eq(SalOvertimeStandard::getId, dto.getId())
                .set(SalOvertimeStandard::getLevelId, dto.getLevelId())
                .set(SalOvertimeStandard::getOvertimeType, dto.getOvertimeType())
                .set(dto.getRate() != null, SalOvertimeStandard::getRate, dto.getRate())
                .set(SalOvertimeStandard::getRemark, dto.getRemark()));
        log.info("修改加班标准：id={} levelId={} type={}", dto.getId(), dto.getLevelId(), dto.getOvertimeType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalOvertimeStandard entity = getById(id);
        releaseUniqueKey(entity);
        standardMapper.deleteById(id);
        log.info("删除加班标准：id={} levelId={} type={}", id, entity.getLevelId(), entity.getOvertimeType());
    }

    @Override
    public BigDecimal findRate(Long levelId, Integer overtimeType) {
        if (levelId == null || overtimeType == null) {
            return null;
        }
        SalOvertimeStandard entity = standardMapper.selectOne(
                new LambdaQueryWrapper<SalOvertimeStandard>()
                        .eq(SalOvertimeStandard::getLevelId, levelId)
                        .eq(SalOvertimeStandard::getOvertimeType, overtimeType));
        return entity == null ? null : entity.getRate();
    }

    // ------------------------------------------------------------------ 内部方法

    /** 同一级别 + 同一加班类型只能有一条标准（表上是复合唯一键 uk_level_type） */
    private void assertLevelTypeUnique(Long levelId, Integer overtimeType, Long excludeId) {
        LambdaQueryWrapper<SalOvertimeStandard> wrapper = new LambdaQueryWrapper<SalOvertimeStandard>()
                .eq(SalOvertimeStandard::getLevelId, levelId)
                .eq(SalOvertimeStandard::getOvertimeType, overtimeType);
        if (excludeId != null) {
            wrapper.ne(SalOvertimeStandard::getId, excludeId);
        }
        if (standardMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "该薪酬级别的该加班类型已配置标准，请勿重复");
        }
    }

    /**
     * 复合唯一键 (level_id, overtime_type) 无法用文本后缀改写（overtime_type 是 TINYINT），
     * 故把 level_id 置为 **-id** 让出唯一键：id 唯一 → 负值也唯一，且永不与真实（正）level_id 相同。
     * 已删行按 deleted=0 过滤，不会再被查到。
     */
    private void releaseUniqueKey(SalOvertimeStandard entity) {
        Long levelId = entity.getLevelId();
        if (levelId == null || levelId < 0) {
            return;
        }
        standardMapper.update(null, new LambdaUpdateWrapper<SalOvertimeStandard>()
                .eq(SalOvertimeStandard::getId, entity.getId())
                .set(SalOvertimeStandard::getLevelId, -entity.getId()));
    }
}
