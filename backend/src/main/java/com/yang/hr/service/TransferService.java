package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.TransferDTO;
import com.yang.hr.entity.HrTransfer;

/**
 * 职工调动服务
 */
public interface TransferService {

    Page<HrTransfer> page(long current, long size, Long employeeId);

    HrTransfer getById(Long id);

    Long create(TransferDTO dto);

    void update(TransferDTO dto);

    void delete(Long id);
}
