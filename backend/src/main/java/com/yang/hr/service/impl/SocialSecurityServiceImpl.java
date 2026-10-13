package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.SocialSecurityDTO;
import com.yang.hr.entity.HrSocialSecurity;
import com.yang.hr.mapper.HrSocialSecurityMapper;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.SocialSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 职工社保服务实现
 */
@Service
public class SocialSecurityServiceImpl implements SocialSecurityService {

    private static final Logger log = LoggerFactory.getLogger(SocialSecurityServiceImpl.class);

    /** 金额保留 2 位，四舍五入 */
    private static final int SCALE = 2;

    private final HrSocialSecurityMapper socialMapper;
    private final EmployeeService employeeService;

    public SocialSecurityServiceImpl(HrSocialSecurityMapper socialMapper, EmployeeService employeeService) {
        this.socialMapper = socialMapper;
        this.employeeService = employeeService;
    }

    @Override
    public Page<HrSocialSecurity> page(long current, long size, Long employeeId, String month) {
        LambdaQueryWrapper<HrSocialSecurity> wrapper = new LambdaQueryWrapper<HrSocialSecurity>()
                .eq(employeeId != null, HrSocialSecurity::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(month), HrSocialSecurity::getMonth, trim(month))
                .orderByDesc(HrSocialSecurity::getMonth)
                .orderByDesc(HrSocialSecurity::getId);
        return socialMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public HrSocialSecurity getById(Long id) {
        HrSocialSecurity entity = socialMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "社保记录不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SocialSecurityDTO dto) {
        employeeService.assertExists(dto.getEmployeeId());
        String month = trim(dto.getMonth());
        assertMonthUnique(dto.getEmployeeId(), month, null);

        BigDecimal base = nvl(dto.getBaseAmount());
        BigDecimal companyRate = nvl(dto.getCompanyRate());
        BigDecimal personalRate = nvl(dto.getPersonalRate());

        HrSocialSecurity entity = new HrSocialSecurity();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setSsNo(trimToNull(dto.getSsNo()));
        entity.setBaseAmount(base);
        entity.setCompanyRate(companyRate);
        entity.setPersonalRate(personalRate);
        // 不填缴纳额就按「基数 × 比例」算，填了就按填的存（实际缴纳额可能与理论值不同）
        entity.setCompanyAmount(dto.getCompanyAmount() != null
                ? dto.getCompanyAmount() : calc(base, companyRate));
        entity.setPersonalAmount(dto.getPersonalAmount() != null
                ? dto.getPersonalAmount() : calc(base, personalRate));
        entity.setMonth(month);
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        socialMapper.insert(entity);
        log.info("新增职工社保：employeeId={} month={}（id={}）", entity.getEmployeeId(), month, entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SocialSecurityDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少社保记录ID");
        }
        HrSocialSecurity exist = getById(dto.getId());
        employeeService.assertExists(dto.getEmployeeId());
        String month = trim(dto.getMonth());
        assertMonthUnique(dto.getEmployeeId(), month, dto.getId());

        // 基数/比例都是 NOT NULL：不传沿用原值
        BigDecimal base = dto.getBaseAmount() != null ? dto.getBaseAmount() : exist.getBaseAmount();
        BigDecimal companyRate = dto.getCompanyRate() != null ? dto.getCompanyRate() : exist.getCompanyRate();
        BigDecimal personalRate = dto.getPersonalRate() != null ? dto.getPersonalRate() : exist.getPersonalRate();
        // 缴纳额语义（与 create 的"填了就按填的存"一致）：
        // ① 显式传了缴纳额 → 按传的存（实际缴纳额可能与理论值不同）；
        // ② 没传但本次动了基数/比例 → 按新值重算（否则基数改了缴纳额还是旧的，记录自相矛盾）；
        // ③ 没传也没动基数/比例（如只改社保号）→ 保持原值，不冲掉用户手工调整过的缴纳额。
        boolean companyInputChanged = dto.getBaseAmount() != null || dto.getCompanyRate() != null;
        boolean personalInputChanged = dto.getBaseAmount() != null || dto.getPersonalRate() != null;
        BigDecimal companyAmount = dto.getCompanyAmount() != null
                ? dto.getCompanyAmount()
                : (companyInputChanged ? calc(base, companyRate) : exist.getCompanyAmount());
        BigDecimal personalAmount = dto.getPersonalAmount() != null
                ? dto.getPersonalAmount()
                : (personalInputChanged ? calc(base, personalRate) : exist.getPersonalAmount());

        socialMapper.update(null, new LambdaUpdateWrapper<HrSocialSecurity>()
                .eq(HrSocialSecurity::getId, dto.getId())
                .set(HrSocialSecurity::getEmployeeId, dto.getEmployeeId())
                .set(HrSocialSecurity::getSsNo, trimToNull(dto.getSsNo()))
                .set(dto.getBaseAmount() != null, HrSocialSecurity::getBaseAmount, dto.getBaseAmount())
                .set(dto.getCompanyRate() != null, HrSocialSecurity::getCompanyRate, dto.getCompanyRate())
                .set(dto.getPersonalRate() != null, HrSocialSecurity::getPersonalRate, dto.getPersonalRate())
                .set(HrSocialSecurity::getCompanyAmount, companyAmount)
                .set(HrSocialSecurity::getPersonalAmount, personalAmount)
                .set(HrSocialSecurity::getMonth, month));
        log.info("修改职工社保：id={} employeeId={} month={}", dto.getId(), dto.getEmployeeId(), month);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HrSocialSecurity entity = getById(id);
        releaseUniqueKey(entity);
        // 无唯一业务键，直接逻辑删除
        socialMapper.deleteById(id);
        log.info("删除职工社保：id={} employeeId={} month={}", id, entity.getEmployeeId(), entity.getMonth());
    }

    // ------------------------------------------------------------------ 内部方法

    /**
     * 逻辑删除前让出 (employee_id, month) 唯一键（2026-10-13 复查加）：把 employee_id 置 **-id**。
     * 与 sal_* 各表的"让路"约定一致 —— 软删行仍占唯一索引，不让路则删除后同月不能重新登记。
     */
    private void releaseUniqueKey(HrSocialSecurity entity) {
        Long employeeId = entity.getEmployeeId();
        if (employeeId == null || employeeId < 0) {
            return;
        }
        socialMapper.update(null, new LambdaUpdateWrapper<HrSocialSecurity>()
                .eq(HrSocialSecurity::getId, entity.getId())
                .set(HrSocialSecurity::getEmployeeId, -entity.getId()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal calc(BigDecimal base, BigDecimal rate) {
        return base.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 同一职工同一月份只应有一条社保记录（重复会让 M5 工资计算重复扣缴）。
     * ⚠️ 表上没有 (employee_id, month) 唯一索引，这里是 Service 级校验；并发场景仍可能穿透。
     */
    private void assertMonthUnique(Long employeeId, String month, Long excludeId) {
        LambdaQueryWrapper<HrSocialSecurity> wrapper = new LambdaQueryWrapper<HrSocialSecurity>()
                .eq(HrSocialSecurity::getEmployeeId, employeeId)
                .eq(HrSocialSecurity::getMonth, month);
        if (excludeId != null) {
            wrapper.ne(HrSocialSecurity::getId, excludeId);
        }
        if (socialMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "该职工在 " + month + " 已有社保记录，请勿重复登记");
        }
    }
}
