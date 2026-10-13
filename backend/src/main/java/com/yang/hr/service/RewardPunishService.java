package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.RewardPunishDTO;
import com.yang.hr.entity.HrRewardPunish;

/**
 * 奖罚登记服务
 */
public interface RewardPunishService {

    Page<HrRewardPunish> page(long current, long size, Long employeeId, Integer type);

    HrRewardPunish getById(Long id);

    Long create(RewardPunishDTO dto);

    void update(RewardPunishDTO dto);

    void delete(Long id);
}
