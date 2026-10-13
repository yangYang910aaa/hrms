package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.DictDataDTO;
import com.yang.hr.entity.SysDictData;
import com.yang.hr.entity.SysDictType;
import com.yang.hr.mapper.SysDictDataMapper;
import com.yang.hr.mapper.SysDictTypeMapper;
import com.yang.hr.service.DictDataService;
import com.yang.hr.vo.DictOptionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 字典数据服务实现
 */
@Service
public class DictDataServiceImpl implements DictDataService {

    private static final Logger log = LoggerFactory.getLogger(DictDataServiceImpl.class);

    private final SysDictDataMapper dictDataMapper;
    private final SysDictTypeMapper dictTypeMapper;

    public DictDataServiceImpl(SysDictDataMapper dictDataMapper, SysDictTypeMapper dictTypeMapper) {
        this.dictDataMapper = dictDataMapper;
        this.dictTypeMapper = dictTypeMapper;
    }

    @Override
    public Page<SysDictData> page(long current, long size, String dictType, String dictLabel, Integer status) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<SysDictData>()
                .eq(StringUtils.hasText(dictType), SysDictData::getDictType, trim(dictType))
                .like(StringUtils.hasText(dictLabel), SysDictData::getDictLabel, dictLabel)
                .eq(status != null, SysDictData::getStatus, status)
                .orderByAsc(SysDictData::getSort)
                .orderByAsc(SysDictData::getId);
        return dictDataMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public List<DictOptionVO> options(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            return List.of();
        }
        String key = dictType.trim();

        // 类型不存在或已停用 → 返回空列表（下拉拿不到选项，但不抛错，避免业务表单一开就 400）
        SysDictType type = dictTypeMapper.selectOne(
                new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, key));
        if (type == null || !Integer.valueOf(1).equals(type.getStatus())) {
            return List.of();
        }

        List<SysDictData> list = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, key)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSort)
                .orderByAsc(SysDictData::getId));
        return list.stream().map(this::toOption).toList();
    }

    @Override
    public SysDictData getById(Long id) {
        SysDictData data = dictDataMapper.selectById(id);
        if (data == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "字典数据不存在或已删除");
        }
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DictDataDTO dto) {
        String dictType = trim(dto.getDictType());
        String dictValue = trim(dto.getDictValue());
        assertTypeExists(dictType);
        assertValueUnique(dictType, dictValue, null);

        Integer isDefault = normalizeFlag(dto.getIsDefault());
        if (isDefault == 1) {
            clearOtherDefaults(dictType, null);
        }

        SysDictData entity = new SysDictData();
        entity.setDictType(dictType);
        entity.setDictLabel(dto.getDictLabel());
        entity.setDictValue(dictValue);
        entity.setIsDefault(isDefault);
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        dictDataMapper.insert(entity);
        log.info("新增字典数据：{}={}（{}，id={}）", dictType, dictValue, entity.getDictLabel(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DictDataDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少字典数据ID");
        }
        getById(dto.getId());

        String dictType = trim(dto.getDictType());
        String dictValue = trim(dto.getDictValue());
        assertTypeExists(dictType);
        assertValueUnique(dictType, dictValue, dto.getId());

        // 没传 isDefault 就保持原值（与 sort/status 同策略），传了才处理，并把同类型其他默认项取消
        Integer isDefault = dto.getIsDefault() == null ? null : normalizeFlag(dto.getIsDefault());
        if (isDefault != null && isDefault == 1) {
            clearOtherDefaults(dictType, dto.getId());
        }

        dictDataMapper.update(null, new LambdaUpdateWrapper<SysDictData>()
                .eq(SysDictData::getId, dto.getId())
                .set(SysDictData::getDictType, dictType)
                .set(SysDictData::getDictLabel, dto.getDictLabel())
                .set(SysDictData::getDictValue, dictValue)
                .set(isDefault != null, SysDictData::getIsDefault, isDefault)
                .set(SysDictData::getRemark, dto.getRemark())
                .set(dto.getSort() != null, SysDictData::getSort, dto.getSort())
                .set(dto.getStatus() != null, SysDictData::getStatus, dto.getStatus()));
        log.info("修改字典数据：id={} {}={}", dto.getId(), dictType, dictValue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDictData data = getById(id);
        // 纯子表无唯一索引，直接逻辑删除即可
        dictDataMapper.deleteById(id);
        log.info("删除字典数据：{}={}（id={}）", data.getDictType(), data.getDictValue(), id);
    }

    // ------------------------------------------------------------------ 内部方法

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** 把任意入参规整成 0/1，null 视为 0 */
    private Integer normalizeFlag(Integer flag) {
        return flag != null && flag == 1 ? 1 : 0;
    }

    private void assertTypeExists(String dictType) {
        long exists = dictTypeMapper.selectCount(
                new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dictType));
        if (exists == 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "字典类型不存在：" + dictType);
        }
    }

    /** 同一 dict_type 下 dict_value 是字典的键，不允许重复 */
    private void assertValueUnique(String dictType, String dictValue, Long excludeId) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getDictValue, dictValue);
        if (excludeId != null) {
            wrapper.ne(SysDictData::getId, excludeId);
        }
        if (dictDataMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "字典类型 [" + dictType + "] 下已存在相同的字典键值：" + dictValue);
        }
    }

    /** 保证同一 dict_type 下至多一个默认项 */
    private void clearOtherDefaults(String dictType, Long excludeId) {
        LambdaUpdateWrapper<SysDictData> wrapper = new LambdaUpdateWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getIsDefault, 1)
                .set(SysDictData::getIsDefault, 0);
        if (excludeId != null) {
            wrapper.ne(SysDictData::getId, excludeId);
        }
        dictDataMapper.update(null, wrapper);
    }

    private DictOptionVO toOption(SysDictData data) {
        DictOptionVO vo = new DictOptionVO();
        vo.setLabel(data.getDictLabel());
        vo.setValue(data.getDictValue());
        vo.setIsDefault(data.getIsDefault());
        return vo;
    }
}
