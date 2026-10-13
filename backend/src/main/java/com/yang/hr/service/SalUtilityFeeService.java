package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.SalUtilityFeeDTO;
import com.yang.hr.entity.SalUtilityFee;

/**
 * 水电费登记服务
 */
public interface SalUtilityFeeService {

    /** 条件分页：职工 / 所属月份 */
    Page<SalUtilityFee> page(long current, long size, Long employeeId, String month);

    SalUtilityFee getById(Long id);

    Long create(SalUtilityFeeDTO dto);

    void update(SalUtilityFeeDTO dto);

    void delete(Long id);
}
