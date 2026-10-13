package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.DeleteMask;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.SocialRateDTO;
import com.yang.hr.entity.SysSocialRate;
import com.yang.hr.mapper.SysSocialRateMapper;
import com.yang.hr.service.SocialRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 社保比例服务实现
 */
@Service
public class SocialRateServiceImpl implements SocialRateService {

    private static final Logger log = LoggerFactory.getLogger(SocialRateServiceImpl.class);

    /** 逻辑删除时给唯一业务键加的后缀标记，避免二次录入撞唯一索引（规划书 §9 第 16 条） */
    private final SysSocialRateMapper socialRateMapper;

    public SocialRateServiceImpl(SysSocialRateMapper socialRateMapper) {
        this.socialRateMapper = socialRateMapper;
    }

    @Override
    public Page<SysSocialRate> page(long current, long size, String rateName, Integer status) {
        LambdaQueryWrapper<SysSocialRate> wrapper = new LambdaQueryWrapper<SysSocialRate>()
                .like(StringUtils.hasText(rateName), SysSocialRate::getRateName, rateName)
                .eq(status != null, SysSocialRate::getStatus, status)
                .orderByAsc(SysSocialRate::getSort)
                .orderByAsc(SysSocialRate::getId);
        return socialRateMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public List<SysSocialRate> listEnabled() {
        return socialRateMapper.selectList(new LambdaQueryWrapper<SysSocialRate>()
                .eq(SysSocialRate::getStatus, 1)
                .orderByAsc(SysSocialRate::getSort)
                .orderByAsc(SysSocialRate::getId));
    }

    @Override
    public SysSocialRate getById(Long id) {
        SysSocialRate rate = socialRateMapper.selectById(id);
        if (rate == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "社保比例不存在或已删除");
        }
        return rate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SocialRateDTO dto) {
        assertRateCodeUnique(dto.getRateCode(), null);
        assertBaseRange(dto.getBaseMin(), dto.getBaseMax());

        SysSocialRate entity = new SysSocialRate();
        entity.setRateName(dto.getRateName());
        entity.setRateCode(dto.getRateCode().trim());
        entity.setCompanyRate(nvl(dto.getCompanyRate()));
        entity.setPersonalRate(nvl(dto.getPersonalRate()));
        entity.setBaseMin(nvl(dto.getBaseMin()));
        entity.setBaseMax(nvl(dto.getBaseMax()));
        entity.setEffectiveMonth(trimToNull(dto.getEffectiveMonth()));
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        socialRateMapper.insert(entity);
        log.info("新增社保比例：{}（{}，id={}）", entity.getRateName(), entity.getRateCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SocialRateDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少社保比例ID");
        }
        SysSocialRate exist = getById(dto.getId());
        assertRateCodeUnique(dto.getRateCode(), dto.getId());

        // 前端可能只传了一个边界，这里用"入参优先、否则沿用原值"的合并值做校验，避免只改一边时漏判
        BigDecimal mergedMin = dto.getBaseMin() != null ? dto.getBaseMin() : exist.getBaseMin();
        BigDecimal mergedMax = dto.getBaseMax() != null ? dto.getBaseMax() : exist.getBaseMax();
        assertBaseRange(mergedMin, mergedMax);

        // 基数与比例列都是 NOT NULL：null 一律理解为"不改"，因此用条件 set，不能无脑写 null
        socialRateMapper.update(null, new LambdaUpdateWrapper<SysSocialRate>()
                .eq(SysSocialRate::getId, dto.getId())
                .set(SysSocialRate::getRateName, dto.getRateName())
                .set(SysSocialRate::getRateCode, dto.getRateCode().trim())
                .set(dto.getCompanyRate() != null, SysSocialRate::getCompanyRate, dto.getCompanyRate())
                .set(dto.getPersonalRate() != null, SysSocialRate::getPersonalRate, dto.getPersonalRate())
                .set(dto.getBaseMin() != null, SysSocialRate::getBaseMin, dto.getBaseMin())
                .set(dto.getBaseMax() != null, SysSocialRate::getBaseMax, dto.getBaseMax())
                .set(SysSocialRate::getEffectiveMonth, trimToNull(dto.getEffectiveMonth()))
                .set(SysSocialRate::getRemark, dto.getRemark())
                .set(dto.getSort() != null, SysSocialRate::getSort, dto.getSort())
                .set(dto.getStatus() != null, SysSocialRate::getStatus, dto.getStatus()));
        log.info("修改社保比例：id={} name={}", dto.getId(), dto.getRateName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysSocialRate rate = getById(id);
        rewriteRateCode(rate);
        socialRateMapper.deleteById(id);
        log.info("删除社保比例：{}（{}，id={}）", rate.getRateName(), rate.getRateCode(), id);
    }

    // ------------------------------------------------------------------ 内部方法

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void assertRateCodeUnique(String rateCode, Long excludeId) {
        if (!StringUtils.hasText(rateCode)) {
            return;
        }
        LambdaQueryWrapper<SysSocialRate> wrapper = new LambdaQueryWrapper<SysSocialRate>()
                .eq(SysSocialRate::getRateCode, rateCode.trim());
        if (excludeId != null) {
            wrapper.ne(SysSocialRate::getId, excludeId);
        }
        if (socialRateMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "险种编码已存在：" + rateCode);
        }
    }

    /**
     * 缴费基数上下限校验。
     *
     * 0 视为「未配置 / 不限制」（这两列是 NOT NULL，0 是唯一的"未填"哨兵），
     * 因此只有两端都 > 0 时才比较 —— 否则"只填下限、上限留空"会被落库成 (4000, 0)，
     * 即"下限大于上限"的非法配置，正好绕过校验。
     */
    private void assertBaseRange(BigDecimal baseMin, BigDecimal baseMax) {
        if (baseMin == null || baseMax == null) {
            return;
        }
        if (baseMin.signum() > 0 && baseMax.signum() > 0 && baseMin.compareTo(baseMax) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "缴费基数下限不能大于上限（上限填 0 表示不限制）");
        }
    }

    /** 逻辑删除前先给唯一键让路，否则以后再建同编码险种会撞唯一索引 */
    private void rewriteRateCode(SysSocialRate rate) {
        String code = rate.getRateCode();
        if (!StringUtils.hasText(code) || code.contains(DeleteMask.MARK)) {
            return;
        }
        socialRateMapper.update(null, new LambdaUpdateWrapper<SysSocialRate>()
                .eq(SysSocialRate::getId, rate.getId())
                // 列宽 30：截断后再拼，超长会 Data too long 让删除接口 500
                .set(SysSocialRate::getRateCode, DeleteMask.masked(code, rate.getId(), 30)));
    }
}
