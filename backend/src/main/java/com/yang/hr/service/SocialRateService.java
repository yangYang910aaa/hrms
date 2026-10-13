package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.SocialRateDTO;
import com.yang.hr.entity.SysSocialRate;

import java.util.List;

/**
 * 社保比例服务
 */
public interface SocialRateService {

    Page<SysSocialRate> page(long current, long size, String rateName, Integer status);

    /** 启用中的社保比例（M5 工资计算与前端列表用） */
    List<SysSocialRate> listEnabled();

    SysSocialRate getById(Long id);

    Long create(SocialRateDTO dto);

    void update(SocialRateDTO dto);

    void delete(Long id);
}
