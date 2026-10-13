package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.InternDTO;
import com.yang.hr.entity.HrIntern;

/**
 * 实习生管理服务
 */
public interface InternService {

    Page<HrIntern> page(long current, long size, String name, Long deptId, Integer status);

    HrIntern getById(Long id);

    Long create(InternDTO dto);

    void update(InternDTO dto);

    void delete(Long id);
}
