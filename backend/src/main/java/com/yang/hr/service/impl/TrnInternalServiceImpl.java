package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.TrnInternalDTO;
import com.yang.hr.entity.TrnInternal;
import com.yang.hr.mapper.TrnInternalMapper;
import com.yang.hr.service.DeptService;
import com.yang.hr.service.TrnCourseService;
import com.yang.hr.service.TrnInternalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * 内部培训期次服务实现
 */
@Service
public class TrnInternalServiceImpl implements TrnInternalService {

    private static final Logger log = LoggerFactory.getLogger(TrnInternalServiceImpl.class);

    private final TrnInternalMapper internalMapper;
    private final TrnCourseService courseService;
    private final DeptService deptService;

    public TrnInternalServiceImpl(TrnInternalMapper internalMapper,
                                  TrnCourseService courseService,
                                  DeptService deptService) {
        this.internalMapper = internalMapper;
        this.courseService = courseService;
        this.deptService = deptService;
    }

    @Override
    public Page<TrnInternal> page(long current, long size, Long courseId, Long deptId, Integer status) {
        LambdaQueryWrapper<TrnInternal> wrapper = new LambdaQueryWrapper<TrnInternal>()
                .eq(courseId != null, TrnInternal::getCourseId, courseId)
                .eq(deptId != null, TrnInternal::getDeptId, deptId)
                .eq(status != null, TrnInternal::getStatus, status)
                .orderByDesc(TrnInternal::getStartDate)
                .orderByDesc(TrnInternal::getId);
        return internalMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public TrnInternal getById(Long id) {
        TrnInternal entity = internalMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "内部培训记录不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TrnInternalDTO dto) {
        courseService.assertExists(dto.getCourseId());
        assertDeptExists(dto.getDeptId());
        assertDateOrder(dto.getStartDate(), dto.getEndDate());

        TrnInternal entity = new TrnInternal();
        entity.setCourseId(dto.getCourseId());
        entity.setBatchNo(trimToNull(dto.getBatchNo()));
        entity.setDeptId(dto.getDeptId());
        entity.setTrainer(trimToNull(dto.getTrainer()));
        entity.setPlace(trimToNull(dto.getPlace()));
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setPlanPersons(nvl(dto.getPlanPersons()));
        entity.setActualPersons(nvl(dto.getActualPersons()));
        entity.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        internalMapper.insert(entity);
        log.info("新增内部培训：courseId={} batchNo={}（id={}）", entity.getCourseId(), entity.getBatchNo(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TrnInternalDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少内部培训记录ID");
        }
        getById(dto.getId());
        courseService.assertExists(dto.getCourseId());
        assertDeptExists(dto.getDeptId());
        assertDateOrder(dto.getStartDate(), dto.getEndDate());

        // 可空列（batch_no/dept_id/日期/文本）无条件写，支持清空；
        // NOT NULL 的 plan_persons/actual_persons/status 用条件 set（不传保持原值）
        internalMapper.update(null, new LambdaUpdateWrapper<TrnInternal>()
                .eq(TrnInternal::getId, dto.getId())
                .set(TrnInternal::getCourseId, dto.getCourseId())
                .set(TrnInternal::getBatchNo, trimToNull(dto.getBatchNo()))
                .set(TrnInternal::getDeptId, dto.getDeptId())
                .set(TrnInternal::getTrainer, trimToNull(dto.getTrainer()))
                .set(TrnInternal::getPlace, trimToNull(dto.getPlace()))
                .set(TrnInternal::getStartDate, dto.getStartDate())
                .set(TrnInternal::getEndDate, dto.getEndDate())
                .set(dto.getPlanPersons() != null, TrnInternal::getPlanPersons, dto.getPlanPersons())
                .set(dto.getActualPersons() != null, TrnInternal::getActualPersons, dto.getActualPersons())
                .set(dto.getStatus() != null, TrnInternal::getStatus, dto.getStatus())
                .set(TrnInternal::getRemark, dto.getRemark()));
        log.info("修改内部培训：id={} courseId={}", dto.getId(), dto.getCourseId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        // 无唯一业务键，直接逻辑删除
        internalMapper.deleteById(id);
        log.info("删除内部培训：id={}", id);
    }

    // ------------------------------------------------------------------ 内部方法

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    /** 部门可空：传了才校验，不存在会抛「部门不存在或已删除」（复用 DeptService 的单一提示） */
    private void assertDeptExists(Long deptId) {
        if (deptId != null) {
            deptService.getById(deptId);
        }
    }

    private void assertDateOrder(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BizException(ResultCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
    }
}
