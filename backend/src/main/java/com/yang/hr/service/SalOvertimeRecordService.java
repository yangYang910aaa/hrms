package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.SalOvertimeRecordDTO;
import com.yang.hr.entity.SalOvertimeRecord;

/**
 * 加班登记服务
 */
public interface SalOvertimeRecordService {

    /** 条件分页：职工 / 所属月份 / 加班类型 */
    Page<SalOvertimeRecord> page(long current, long size, Long employeeId, String month, Integer overtimeType);

    SalOvertimeRecord getById(Long id);

    Long create(SalOvertimeRecordDTO dto);

    void update(SalOvertimeRecordDTO dto);

    void delete(Long id);
}
