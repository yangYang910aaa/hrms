package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.SalOvertimeStandardDTO;
import com.yang.hr.entity.SalOvertimeStandard;

/**
 * 加班标准服务
 */
public interface SalOvertimeStandardService {

    /** 条件分页：薪酬级别 / 加班类型 */
    Page<SalOvertimeStandard> page(long current, long size, Long levelId, Integer overtimeType);

    SalOvertimeStandard getById(Long id);

    Long create(SalOvertimeStandardDTO dto);

    void update(SalOvertimeStandardDTO dto);

    void delete(Long id);

    /**
     * 取某级别 + 某加班类型的倍率（加班登记自动算金额时用）。
     * 没有配置则返回 null，由调用方决定兜底口径。
     */
    java.math.BigDecimal findRate(Long levelId, Integer overtimeType);
}
