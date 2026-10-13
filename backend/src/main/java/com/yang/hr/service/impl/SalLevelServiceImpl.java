package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.DeleteMask;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.SalLevelDTO;
import com.yang.hr.entity.SalLevel;
import com.yang.hr.mapper.SalLevelMapper;
import com.yang.hr.service.SalLevelService;
import com.yang.hr.vo.SalaryLevelOptionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 薪酬级别服务实现
 */
@Service
public class SalLevelServiceImpl implements SalLevelService {

    private static final Logger log = LoggerFactory.getLogger(SalLevelServiceImpl.class);

    /** 逻辑删除时给唯一业务键加的后缀标记，避免二次录入撞唯一索引（规划书 §9 第 16 条） */
    private final SalLevelMapper levelMapper;

    public SalLevelServiceImpl(SalLevelMapper levelMapper) {
        this.levelMapper = levelMapper;
    }

    @Override
    public Page<SalLevel> page(long current, long size, String levelName) {
        LambdaQueryWrapper<SalLevel> wrapper = new LambdaQueryWrapper<SalLevel>()
                .like(StringUtils.hasText(levelName), SalLevel::getLevelName, trim(levelName))
                .orderByAsc(SalLevel::getId);
        return levelMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public SalLevel getById(Long id) {
        SalLevel entity = levelMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "薪酬级别不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SalLevelDTO dto) {
        assertLevelNoUnique(dto.getLevelNo(), null);

        SalLevel entity = new SalLevel();
        entity.setLevelName(dto.getLevelName());
        entity.setLevelNo(trimToNull(dto.getLevelNo()));
        entity.setBaseSalary(dto.getBaseSalary() == null ? BigDecimal.ZERO : dto.getBaseSalary());
        entity.setRemark(dto.getRemark());
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        levelMapper.insert(entity);
        log.info("新增薪酬级别：{} {}（id={}）", entity.getLevelNo(), entity.getLevelName(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SalLevelDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少薪酬级别ID");
        }
        getById(dto.getId());
        assertLevelNoUnique(dto.getLevelNo(), dto.getId());

        // 可空文本无条件写（支持清空）；NOT NULL 的 base_salary 用条件 set（不传保持原值）
        levelMapper.update(null, new LambdaUpdateWrapper<SalLevel>()
                .eq(SalLevel::getId, dto.getId())
                .set(SalLevel::getLevelName, dto.getLevelName())
                .set(SalLevel::getLevelNo, trimToNull(dto.getLevelNo()))
                .set(dto.getBaseSalary() != null, SalLevel::getBaseSalary, dto.getBaseSalary())
                .set(SalLevel::getRemark, dto.getRemark()));
        log.info("修改薪酬级别：id={} {}", dto.getId(), dto.getLevelName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalLevel level = getById(id);
        assertNoChildren(id);
        rewriteLevelNo(level);
        levelMapper.deleteById(id);
        log.info("删除薪酬级别：{} {}（id={}）", level.getLevelNo(), level.getLevelName(), id);
    }

    @Override
    public void assertExists(Long id) {
        if (id == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "薪酬级别ID不能为空");
        }
        if (levelMapper.selectById(id) == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "薪酬级别不存在或已删除");
        }
    }

    @Override
    public List<SalaryLevelOptionVO> options() {
        List<SalLevel> list = levelMapper.selectList(new LambdaQueryWrapper<SalLevel>()
                .select(SalLevel::getId, SalLevel::getLevelNo, SalLevel::getLevelName, SalLevel::getBaseSalary)
                .orderByAsc(SalLevel::getId));
        return list.stream().map(item -> {
            SalaryLevelOptionVO vo = new SalaryLevelOptionVO();
            vo.setId(item.getId());
            vo.setLevelNo(item.getLevelNo());
            vo.setLevelName(item.getLevelName());
            vo.setBaseSalary(item.getBaseSalary());
            return vo;
        }).toList();
    }

    // ------------------------------------------------------------------ 内部方法

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void assertLevelNoUnique(String levelNo, Long excludeId) {
        if (!StringUtils.hasText(levelNo)) {
            return;
        }
        LambdaQueryWrapper<SalLevel> wrapper = new LambdaQueryWrapper<SalLevel>()
                .eq(SalLevel::getLevelNo, levelNo.trim());
        if (excludeId != null) {
            wrapper.ne(SalLevel::getId, excludeId);
        }
        if (levelMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "级别编号已存在：" + levelNo);
        }
    }

    /** 级别被加班标准或在册职工引用时拒绝删除，否则那些引用会变成孤儿 */
    private void assertNoChildren(Long levelId) {
        String detail = levelMapper.countChildren(levelId).stream()
                .filter(item -> item.getCnt() != null && item.getCnt() > 0)
                .map(item -> item.getLabel() + " " + item.getCnt() + " 条")
                .collect(Collectors.joining("、"));
        if (!detail.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "该级别仍被引用（" + detail + "），请先处理后再删除");
        }
    }

    /** 逻辑删除前先给唯一键让路，否则以后再录入同级别编号会撞唯一索引 */
    private void rewriteLevelNo(SalLevel level) {
        String levelNo = level.getLevelNo();
        if (!StringUtils.hasText(levelNo) || levelNo.contains(DeleteMask.MARK)) {
            return;
        }
        levelMapper.update(null, new LambdaUpdateWrapper<SalLevel>()
                .eq(SalLevel::getId, level.getId())
                // 列宽 30：截断后再拼，超长会 Data too long 让删除接口 500
                .set(SalLevel::getLevelNo, DeleteMask.masked(levelNo, level.getId(), 30)));
    }
}
