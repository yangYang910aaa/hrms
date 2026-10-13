package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.SalUtilityFeeDTO;
import com.yang.hr.entity.SalUtilityFee;
import com.yang.hr.mapper.SalUtilityFeeMapper;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.SalUtilityFeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 水电费登记服务实现
 *
 * 金额口径：`total_fee = 水费 + 电费 + 住宿费`，一律服务端算（DTO 里没有 totalFee，前端无从传入）。
 */
@Service
public class SalUtilityFeeServiceImpl implements SalUtilityFeeService {

    private static final Logger log = LoggerFactory.getLogger(SalUtilityFeeServiceImpl.class);

    /** 金额保留 2 位，四舍五入 */
    private static final int SCALE = 2;

    /**
     * 合计列 DECIMAL(12,2) 的上限。
     * ⚠️ 三个费用列都是 DECIMAL(12,2)（各可到约 1e10），**三项各自合规但相加可达 3e10**，
     * 会超出 DECIMAL(12,2) 的 10 位整数上限 → 撞 MySQL out-of-range 落 500 兜底（本该是 400）。
     */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private final SalUtilityFeeMapper utilityMapper;
    private final EmployeeService employeeService;

    public SalUtilityFeeServiceImpl(SalUtilityFeeMapper utilityMapper, EmployeeService employeeService) {
        this.utilityMapper = utilityMapper;
        this.employeeService = employeeService;
    }

    @Override
    public Page<SalUtilityFee> page(long current, long size, Long employeeId, String month) {
        LambdaQueryWrapper<SalUtilityFee> wrapper = new LambdaQueryWrapper<SalUtilityFee>()
                .eq(employeeId != null, SalUtilityFee::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(month), SalUtilityFee::getMonth, trim(month))
                .orderByDesc(SalUtilityFee::getMonth)
                .orderByDesc(SalUtilityFee::getId);
        return utilityMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public SalUtilityFee getById(Long id) {
        SalUtilityFee entity = utilityMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "水电费记录不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SalUtilityFeeDTO dto) {
        employeeService.assertExists(dto.getEmployeeId());
        String month = trim(dto.getMonth());
        assertMonthUnique(dto.getEmployeeId(), month, null);

        BigDecimal water = nvl(dto.getWaterFee());
        BigDecimal electric = nvl(dto.getElectricFee());
        BigDecimal dorm = nvl(dto.getDormFee());

        SalUtilityFee entity = new SalUtilityFee();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setMonth(month);
        entity.setWaterFee(water);
        entity.setElectricFee(electric);
        entity.setDormFee(dorm);
        entity.setTotalFee(total(water, electric, dorm));
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        utilityMapper.insert(entity);
        log.info("新增水电费登记：employeeId={} month={} total={}（id={}）",
                entity.getEmployeeId(), month, entity.getTotalFee(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SalUtilityFeeDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少水电费记录ID");
        }
        SalUtilityFee exist = getById(dto.getId());
        employeeService.assertExists(dto.getEmployeeId());
        String month = trim(dto.getMonth());
        assertMonthUnique(dto.getEmployeeId(), month, dto.getId());

        // 三个费用列都是 NOT NULL：不传沿用原值（要置 0 请显式传 0），合计按最终值重算
        BigDecimal water = dto.getWaterFee() != null ? dto.getWaterFee() : exist.getWaterFee();
        BigDecimal electric = dto.getElectricFee() != null ? dto.getElectricFee() : exist.getElectricFee();
        BigDecimal dorm = dto.getDormFee() != null ? dto.getDormFee() : exist.getDormFee();

        utilityMapper.update(null, new LambdaUpdateWrapper<SalUtilityFee>()
                .eq(SalUtilityFee::getId, dto.getId())
                .set(SalUtilityFee::getEmployeeId, dto.getEmployeeId())
                .set(SalUtilityFee::getMonth, month)
                .set(dto.getWaterFee() != null, SalUtilityFee::getWaterFee, dto.getWaterFee())
                .set(dto.getElectricFee() != null, SalUtilityFee::getElectricFee, dto.getElectricFee())
                .set(dto.getDormFee() != null, SalUtilityFee::getDormFee, dto.getDormFee())
                .set(SalUtilityFee::getTotalFee, total(water, electric, dorm)));
        log.info("修改水电费登记：id={} employeeId={} month={}", dto.getId(), dto.getEmployeeId(), month);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalUtilityFee entity = getById(id);
        releaseUniqueKey(entity);
        utilityMapper.deleteById(id);
        log.info("删除水电费登记：id={} employeeId={} month={}", id, entity.getEmployeeId(), entity.getMonth());
    }

    // ------------------------------------------------------------------ 内部方法

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 合计 = 水 + 电 + 住宿（三者都是 NOT NULL 列，`nvl` 只是防呆）。
     * 溢出保护见 {@link #MAX_AMOUNT} 的说明。
     */
    private BigDecimal total(BigDecimal water, BigDecimal electric, BigDecimal dorm) {
        BigDecimal total = nvl(water).add(nvl(electric)).add(nvl(dorm)).setScale(SCALE, RoundingMode.HALF_UP);
        if (total.compareTo(MAX_AMOUNT) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "水电费合计 " + total + " 超出可存储范围（上限 " + MAX_AMOUNT + "），请核对水费 / 电费 / 住宿费");
        }
        return total;
    }

    /** 同一职工同一月份只应有一条水电费记录（表上就是复合唯一键 uk_employee_month） */
    private void assertMonthUnique(Long employeeId, String month, Long excludeId) {
        LambdaQueryWrapper<SalUtilityFee> wrapper = new LambdaQueryWrapper<SalUtilityFee>()
                .eq(SalUtilityFee::getEmployeeId, employeeId)
                .eq(SalUtilityFee::getMonth, month);
        if (excludeId != null) {
            wrapper.ne(SalUtilityFee::getId, excludeId);
        }
        if (utilityMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "该职工在 " + month + " 已有水电费记录，请勿重复登记");
        }
    }

    /**
     * 逻辑删除前让出复合唯一键 (employee_id, month)：把 `employee_id` 置为 **`-id`**。
     * id 唯一 ⇒ 负值也唯一，且永不与真实的（正）职工ID 相同；已删行按 `deleted = 0` 过滤查不到。
     * 与 M5 批次1 加班标准的"让路"约定一致（见 SalOvertimeStandardServiceImpl.releaseUniqueKey）。
     */
    private void releaseUniqueKey(SalUtilityFee entity) {
        Long employeeId = entity.getEmployeeId();
        if (employeeId == null || employeeId < 0) {
            return;
        }
        utilityMapper.update(null, new LambdaUpdateWrapper<SalUtilityFee>()
                .eq(SalUtilityFee::getId, entity.getId())
                .set(SalUtilityFee::getEmployeeId, -entity.getId()));
    }
}
