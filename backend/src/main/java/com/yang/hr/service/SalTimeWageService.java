package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.SalTimeWageDTO;
import com.yang.hr.entity.SalTimeWage;

/**
 * 计时工资服务
 */
public interface SalTimeWageService {

    /** 条件分页：职工 / 所属月份 */
    Page<SalTimeWage> page(long current, long size, Long employeeId, String month);

    SalTimeWage getById(Long id);

    Long create(SalTimeWageDTO dto);

    void update(SalTimeWageDTO dto);

    void delete(Long id);
}
