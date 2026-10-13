package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.DictDataDTO;
import com.yang.hr.entity.SysDictData;
import com.yang.hr.vo.DictOptionVO;

import java.util.List;

/**
 * 字典数据服务
 */
public interface DictDataService {

    Page<SysDictData> page(long current, long size, String dictType, String dictLabel, Integer status);

    /** 按类型取启用中的字典项，供前端下拉（类型不存在或已停用时返回空列表） */
    List<DictOptionVO> options(String dictType);

    SysDictData getById(Long id);

    Long create(DictDataDTO dto);

    void update(DictDataDTO dto);

    void delete(Long id);
}
