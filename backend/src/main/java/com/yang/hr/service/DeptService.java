package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.DeptDTO;
import com.yang.hr.entity.SysDept;
import com.yang.hr.vo.DeptTreeVO;

import java.util.List;

/**
 * 组织结构（部门）服务
 */
public interface DeptService {

    Page<SysDept> page(long current, long size, String deptName, Integer status);

    /** 完整部门树（含停用部门，供管理页展示） */
    List<DeptTreeVO> tree();

    /** 只含启用中的部门树（供表单下拉用：停用部门不出现，父节点被过滤时子节点上提为根） */
    List<DeptTreeVO> enabledTree();

    SysDept getById(Long id);

    Long create(DeptDTO dto);

    void update(DeptDTO dto);

    void delete(Long id);
}
