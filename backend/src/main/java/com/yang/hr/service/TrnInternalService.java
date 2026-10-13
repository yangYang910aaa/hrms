package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.TrnInternalDTO;
import com.yang.hr.entity.TrnInternal;

/**
 * 内部培训期次服务
 */
public interface TrnInternalService {

    /** 条件分页：课程 / 部门 / 状态 */
    Page<TrnInternal> page(long current, long size, Long courseId, Long deptId, Integer status);

    TrnInternal getById(Long id);

    Long create(TrnInternalDTO dto);

    void update(TrnInternalDTO dto);

    void delete(Long id);
}
