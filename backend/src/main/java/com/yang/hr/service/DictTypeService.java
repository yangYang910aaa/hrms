package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.DictTypeDTO;
import com.yang.hr.entity.SysDictType;

import java.util.List;

/**
 * 字典类型服务
 */
public interface DictTypeService {

    Page<SysDictType> page(long current, long size, String dictName, String dictType, Integer status);

    /** 启用中的字典类型（业务管理页左侧类型列表 / 类型下拉用） */
    List<SysDictType> listEnabled();

    SysDictType getById(Long id);

    Long create(DictTypeDTO dto);

    void update(DictTypeDTO dto);

    void delete(Long id);
}
