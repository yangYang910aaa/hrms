package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.SalPieceWageDTO;
import com.yang.hr.entity.SalPieceWage;

/**
 * 计件工资服务
 */
public interface SalPieceWageService {

    /** 条件分页：职工 / 所属月份 */
    Page<SalPieceWage> page(long current, long size, Long employeeId, String month);

    SalPieceWage getById(Long id);

    Long create(SalPieceWageDTO dto);

    void update(SalPieceWageDTO dto);

    void delete(Long id);
}
