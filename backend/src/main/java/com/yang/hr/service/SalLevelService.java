package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.SalLevelDTO;
import com.yang.hr.entity.SalLevel;
import com.yang.hr.vo.SalaryLevelOptionVO;

import java.util.List;

/**
 * 薪酬级别服务
 */
public interface SalLevelService {

    /** 条件分页：级别名称模糊 */
    Page<SalLevel> page(long current, long size, String levelName);

    SalLevel getById(Long id);

    Long create(SalLevelDTO dto);

    void update(SalLevelDTO dto);

    void delete(Long id);

    /** 校验薪酬级别存在，不存在则抛业务异常（职工主档 / 加班标准共用的 levelId 校验） */
    void assertExists(Long id);

    /** 薪酬级别下拉项（供 /api/common/salary-level-options 使用） */
    List<SalaryLevelOptionVO> options();
}
