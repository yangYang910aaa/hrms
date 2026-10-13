package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.RewardPunishDTO;
import com.yang.hr.entity.HrRewardPunish;
import com.yang.hr.mapper.HrRewardPunishMapper;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.RewardPunishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 奖罚登记服务实现
 */
@Service
public class RewardPunishServiceImpl implements RewardPunishService {

    private static final Logger log = LoggerFactory.getLogger(RewardPunishServiceImpl.class);

    private final HrRewardPunishMapper rewardMapper;
    private final EmployeeService employeeService;

    public RewardPunishServiceImpl(HrRewardPunishMapper rewardMapper, EmployeeService employeeService) {
        this.rewardMapper = rewardMapper;
        this.employeeService = employeeService;
    }

    @Override
    public Page<HrRewardPunish> page(long current, long size, Long employeeId, Integer type) {
        LambdaQueryWrapper<HrRewardPunish> wrapper = new LambdaQueryWrapper<HrRewardPunish>()
                .eq(employeeId != null, HrRewardPunish::getEmployeeId, employeeId)
                .eq(type != null, HrRewardPunish::getType, type)
                .orderByDesc(HrRewardPunish::getHappenDate)
                .orderByDesc(HrRewardPunish::getId);
        return rewardMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public HrRewardPunish getById(Long id) {
        HrRewardPunish entity = rewardMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "奖罚记录不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RewardPunishDTO dto) {
        employeeService.assertExists(dto.getEmployeeId());

        HrRewardPunish entity = new HrRewardPunish();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setType(dto.getType());
        entity.setReason(dto.getReason());
        entity.setAmount(dto.getAmount() == null ? BigDecimal.ZERO : dto.getAmount());
        entity.setHappenDate(dto.getHappenDate());
        entity.setOperator(trimToNull(dto.getOperator()));
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        rewardMapper.insert(entity);
        log.info("新增奖罚登记：employeeId={} type={}（id={}）",
                entity.getEmployeeId(), entity.getType(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RewardPunishDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少奖罚记录ID");
        }
        getById(dto.getId());
        employeeService.assertExists(dto.getEmployeeId());

        // type/reason 在 DTO 上是 @NotNull/@NotBlank，必然随请求送达，故无条件写入；
        // amount 是 NOT NULL 但 DTO 可空 —— 可空即"不传就保持原值"（要置 0 请显式传 0）。
        rewardMapper.update(null, new LambdaUpdateWrapper<HrRewardPunish>()
                .eq(HrRewardPunish::getId, dto.getId())
                .set(HrRewardPunish::getEmployeeId, dto.getEmployeeId())
                .set(HrRewardPunish::getType, dto.getType())
                .set(HrRewardPunish::getReason, dto.getReason())
                .set(dto.getAmount() != null, HrRewardPunish::getAmount, dto.getAmount())
                .set(HrRewardPunish::getHappenDate, dto.getHappenDate())
                .set(HrRewardPunish::getOperator, trimToNull(dto.getOperator())));
        log.info("修改奖罚登记：id={} employeeId={}", dto.getId(), dto.getEmployeeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HrRewardPunish entity = getById(id);
        rewardMapper.deleteById(id);
        log.info("删除奖罚登记：id={} employeeId={}", id, entity.getEmployeeId());
    }

    // ------------------------------------------------------------------ 内部方法

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
