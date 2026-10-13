package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.TrnFrontlineDTO;
import com.yang.hr.entity.TrnFrontline;
import com.yang.hr.mapper.TrnFrontlineMapper;
import com.yang.hr.service.DeptService;
import com.yang.hr.service.TrnCourseService;
import com.yang.hr.service.TrnFrontlineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * 一线工培训期次服务实现
 */
@Service
public class TrnFrontlineServiceImpl implements TrnFrontlineService {

    private static final Logger log = LoggerFactory.getLogger(TrnFrontlineServiceImpl.class);

    private final TrnFrontlineMapper frontlineMapper;
    private final TrnCourseService courseService;
    private final DeptService deptService;

    public TrnFrontlineServiceImpl(TrnFrontlineMapper frontlineMapper,
                                   TrnCourseService courseService,
                                   DeptService deptService) {
        this.frontlineMapper = frontlineMapper;
        this.courseService = courseService;
        this.deptService = deptService;
    }

    @Override
    public Page<TrnFrontline> page(long current, long size, Long courseId, Long deptId, Integer status) {
        LambdaQueryWrapper<TrnFrontline> wrapper = new LambdaQueryWrapper<TrnFrontline>()
                .eq(courseId != null, TrnFrontline::getCourseId, courseId)
                .eq(deptId != null, TrnFrontline::getDeptId, deptId)
                .eq(status != null, TrnFrontline::getStatus, status)
                .orderByDesc(TrnFrontline::getStartDate)
                .orderByDesc(TrnFrontline::getId);
        return frontlineMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public TrnFrontline getById(Long id) {
        TrnFrontline entity = frontlineMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "一线工培训记录不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TrnFrontlineDTO dto) {
        courseService.assertExists(dto.getCourseId());
        deptService.getById(dto.getDeptId());   // 必填部门：不存在直接抛「部门不存在或已删除」
        assertDateOrder(dto.getStartDate(), dto.getEndDate());
        int workerCount = nvl(dto.getWorkerCount());
        int qualifiedCount = nvl(dto.getQualifiedCount());
        assertQualifiedLeWorker(qualifiedCount, workerCount);

        TrnFrontline entity = new TrnFrontline();
        entity.setCourseId(dto.getCourseId());
        entity.setBatchNo(trimToNull(dto.getBatchNo()));
        entity.setDeptId(dto.getDeptId());
        entity.setTrainer(trimToNull(dto.getTrainer()));
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setWorkerCount(workerCount);
        entity.setQualifiedCount(qualifiedCount);
        entity.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        frontlineMapper.insert(entity);
        log.info("新增一线工培训：courseId={} deptId={}（id={}）", entity.getCourseId(), entity.getDeptId(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TrnFrontlineDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少一线工培训记录ID");
        }
        TrnFrontline exist = getById(dto.getId());
        courseService.assertExists(dto.getCourseId());
        deptService.getById(dto.getDeptId());
        assertDateOrder(dto.getStartDate(), dto.getEndDate());

        // worker/qualified 是 NOT NULL：不传沿用原值后再交叉校验，避免"只改一个"时漏检
        int workerCount = dto.getWorkerCount() != null ? dto.getWorkerCount() : exist.getWorkerCount();
        int qualifiedCount = dto.getQualifiedCount() != null ? dto.getQualifiedCount() : exist.getQualifiedCount();
        assertQualifiedLeWorker(qualifiedCount, workerCount);

        // 可空列（batch_no/日期/文本）无条件写；NOT NULL 的 worker/qualified/status 用条件 set
        frontlineMapper.update(null, new LambdaUpdateWrapper<TrnFrontline>()
                .eq(TrnFrontline::getId, dto.getId())
                .set(TrnFrontline::getCourseId, dto.getCourseId())
                .set(TrnFrontline::getBatchNo, trimToNull(dto.getBatchNo()))
                .set(TrnFrontline::getDeptId, dto.getDeptId())
                .set(TrnFrontline::getTrainer, trimToNull(dto.getTrainer()))
                .set(TrnFrontline::getStartDate, dto.getStartDate())
                .set(TrnFrontline::getEndDate, dto.getEndDate())
                .set(dto.getWorkerCount() != null, TrnFrontline::getWorkerCount, dto.getWorkerCount())
                .set(dto.getQualifiedCount() != null, TrnFrontline::getQualifiedCount, dto.getQualifiedCount())
                .set(dto.getStatus() != null, TrnFrontline::getStatus, dto.getStatus())
                .set(TrnFrontline::getRemark, dto.getRemark()));
        log.info("修改一线工培训：id={} courseId={}", dto.getId(), dto.getCourseId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        // 无唯一业务键，直接逻辑删除
        frontlineMapper.deleteById(id);
        log.info("删除一线工培训：id={}", id);
    }

    // ------------------------------------------------------------------ 内部方法

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private void assertDateOrder(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BizException(ResultCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
    }

    private void assertQualifiedLeWorker(int qualifiedCount, int workerCount) {
        if (qualifiedCount > workerCount) {
            throw new BizException(ResultCode.PARAM_ERROR, "合格人数不能大于参训人数");
        }
    }
}
