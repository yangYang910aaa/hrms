package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.DeleteMask;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.DictTypeDTO;
import com.yang.hr.entity.SysDictData;
import com.yang.hr.entity.SysDictType;
import com.yang.hr.mapper.SysDictDataMapper;
import com.yang.hr.mapper.SysDictTypeMapper;
import com.yang.hr.service.DictTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 字典类型服务实现
 */
@Service
public class DictTypeServiceImpl implements DictTypeService {

    private static final Logger log = LoggerFactory.getLogger(DictTypeServiceImpl.class);

    /** 逻辑删除时给唯一业务键加的后缀标记，避免二次录入撞唯一索引（规划书 §9 第 16 条） */
    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;

    public DictTypeServiceImpl(SysDictTypeMapper dictTypeMapper, SysDictDataMapper dictDataMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictDataMapper = dictDataMapper;
    }

    @Override
    public Page<SysDictType> page(long current, long size, String dictName, String dictType, Integer status) {
        // 本表没有 sort 列，按 id 升序即可
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
                .like(StringUtils.hasText(dictName), SysDictType::getDictName, dictName)
                .like(StringUtils.hasText(dictType), SysDictType::getDictType, dictType)
                .eq(status != null, SysDictType::getStatus, status)
                .orderByAsc(SysDictType::getId);
        return dictTypeMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public List<SysDictType> listEnabled() {
        return dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getStatus, 1)
                .orderByAsc(SysDictType::getId));
    }

    @Override
    public SysDictType getById(Long id) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "字典类型不存在或已删除");
        }
        return type;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DictTypeDTO dto) {
        assertDictTypeUnique(dto.getDictType(), null);

        SysDictType entity = new SysDictType();
        entity.setDictName(dto.getDictName());
        entity.setDictType(dto.getDictType().trim());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        dictTypeMapper.insert(entity);
        log.info("新增字典类型：{}（{}，id={}）", entity.getDictName(), entity.getDictType(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DictTypeDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少字典类型ID");
        }
        SysDictType exist = getById(dto.getId());

        // 标识是字典数据与前端代码共同的引用键，改掉会把已有数据项变孤儿、也会让前端 /dict/{type} 失效，故锁死
        if (!exist.getDictType().equals(dto.getDictType().trim())) {
            throw new BizException(ResultCode.PARAM_ERROR, "字典类型标识不允许修改");
        }

        dictTypeMapper.update(null, new LambdaUpdateWrapper<SysDictType>()
                .eq(SysDictType::getId, dto.getId())
                .set(SysDictType::getDictName, dto.getDictName())
                .set(SysDictType::getRemark, dto.getRemark())
                .set(dto.getStatus() != null, SysDictType::getStatus, dto.getStatus()));
        log.info("修改字典类型：id={} name={}", dto.getId(), dto.getDictName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDictType type = getById(id);

        long dataCount = dictDataMapper.selectCount(
                new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, type.getDictType()));
        if (dataCount > 0) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "该字典类型下还有 " + dataCount + " 条字典数据，请先删除字典数据");
        }

        rewriteDictType(type);
        dictTypeMapper.deleteById(id);
        log.info("删除字典类型：{}（{}，id={}）", type.getDictName(), type.getDictType(), id);
    }

    // ------------------------------------------------------------------ 内部方法

    private void assertDictTypeUnique(String dictType, Long excludeId) {
        if (!StringUtils.hasText(dictType)) {
            return;
        }
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dictType.trim());
        if (excludeId != null) {
            wrapper.ne(SysDictType::getId, excludeId);
        }
        if (dictTypeMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "字典类型标识已存在：" + dictType);
        }
    }

    /** 逻辑删除前先给唯一键让路，否则以后再建同标识字典类型会撞唯一索引 */
    private void rewriteDictType(SysDictType type) {
        String key = type.getDictType();
        if (!StringUtils.hasText(key) || key.contains(DeleteMask.MARK)) {
            return;
        }
        dictTypeMapper.update(null, new LambdaUpdateWrapper<SysDictType>()
                .eq(SysDictType::getId, type.getId())
                // 列宽 50：截断后再拼，超长会 Data too long 让删除接口 500
                .set(SysDictType::getDictType, DeleteMask.masked(key, type.getId(), 50)));
    }
}
