package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.TrnFrontlineDTO;
import com.yang.hr.entity.TrnFrontline;

/**
 * 一线工培训期次服务
 */
public interface TrnFrontlineService {

    /** 条件分页：课程 / 部门 / 状态 */
    Page<TrnFrontline> page(long current, long size, Long courseId, Long deptId, Integer status);

    TrnFrontline getById(Long id);

    Long create(TrnFrontlineDTO dto);

    void update(TrnFrontlineDTO dto);

    void delete(Long id);
}
