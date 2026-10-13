package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.InternDTO;
import com.yang.hr.entity.HrIntern;
import com.yang.hr.mapper.HrInternMapper;
import com.yang.hr.mapper.SysDeptMapper;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.InternService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * 实习生管理服务实现
 */
@Service
public class InternServiceImpl implements InternService {

    private static final Logger log = LoggerFactory.getLogger(InternServiceImpl.class);

    private static final int STATUS_ONGOING = 1;
    private static final int STATUS_REGULARIZED = 3;

    private final HrInternMapper internMapper;
    private final SysDeptMapper deptMapper;
    private final EmployeeService employeeService;

    public InternServiceImpl(HrInternMapper internMapper,
                             SysDeptMapper deptMapper,
                             EmployeeService employeeService) {
        this.internMapper = internMapper;
        this.deptMapper = deptMapper;
        this.employeeService = employeeService;
    }

    @Override
    public Page<HrIntern> page(long current, long size, String name, Long deptId, Integer status) {
        LambdaQueryWrapper<HrIntern> wrapper = new LambdaQueryWrapper<HrIntern>()
                .like(StringUtils.hasText(name), HrIntern::getName, name)
                .eq(deptId != null, HrIntern::getDeptId, deptId)
                .eq(status != null, HrIntern::getStatus, status)
                .orderByDesc(HrIntern::getStartDate)
                .orderByDesc(HrIntern::getId);
        return internMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public HrIntern getById(Long id) {
        HrIntern entity = internMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "实习生记录不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(InternDTO dto) {
        validate(dto);
        int status = dto.getStatus() == null ? STATUS_ONGOING : dto.getStatus();
        Long employeeId = resolveEmployeeId(status, dto.getEmployeeId(), null);

        HrIntern entity = new HrIntern();
        copy(dto, entity);
        entity.setStatus(status);
        entity.setEmployeeId(employeeId);
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        internMapper.insert(entity);
        log.info("新增实习生：{}（id={}）", entity.getName(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(InternDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少实习生记录ID");
        }
        HrIntern exist = getById(dto.getId());
        validate(dto);
        // status 与 employeeId 必须成对推导，不能一个"不传就保持"、另一个"不传就清空" ——
        // 否则会出现「status 还是已转正，employeeId 却被清成 NULL」的自相矛盾数据。
        int status = dto.getStatus() != null ? dto.getStatus() : exist.getStatus();
        Long employeeId = resolveEmployeeId(status, dto.getEmployeeId(), exist.getEmployeeId());

        // 除状态/关联外全部无条件写入（这些列可空，这样清空才生效）
        internMapper.update(null, new LambdaUpdateWrapper<HrIntern>()
                .eq(HrIntern::getId, dto.getId())
                .set(HrIntern::getName, dto.getName())
                .set(HrIntern::getSchool, trimToNull(dto.getSchool()))
                .set(HrIntern::getMajor, trimToNull(dto.getMajor()))
                .set(HrIntern::getDeptId, dto.getDeptId())
                .set(HrIntern::getMentor, trimToNull(dto.getMentor()))
                .set(HrIntern::getStartDate, dto.getStartDate())
                .set(HrIntern::getEndDate, dto.getEndDate())
                .set(HrIntern::getStatus, status)
                .set(HrIntern::getEmployeeId, employeeId));
        log.info("修改实习生：id={} name={} status={}", dto.getId(), dto.getName(), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HrIntern entity = getById(id);
        internMapper.deleteById(id);
        log.info("删除实习生：id={} name={}", id, entity.getName());
    }

    // ------------------------------------------------------------------ 内部方法

    private void copy(InternDTO dto, HrIntern entity) {
        entity.setName(dto.getName());
        entity.setSchool(trimToNull(dto.getSchool()));
        entity.setMajor(trimToNull(dto.getMajor()));
        entity.setDeptId(dto.getDeptId());
        entity.setMentor(trimToNull(dto.getMentor()));
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validate(InternDTO dto) {
        if (dto.getDeptId() != null && deptMapper.selectById(dto.getDeptId()) == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "实习部门不存在或已删除");
        }
        LocalDate start = dto.getStartDate();
        LocalDate end = dto.getEndDate();
        if (start != null && end != null && end.isBefore(start)) {
            throw new BizException(ResultCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
    }

    /**
     * 「状态 = 已转正」与「关联了职工档案」必须等价，两个方向都要管住：
     * - 已转正 → 必须有 employeeId（入参优先，否则沿用本条记录已有的）
     * - 非转正 → 不允许带 employeeId（传了就报错；落库时也会被清掉，避免"实习中却挂着档案"）
     */
    private Long resolveEmployeeId(int status, Long inputEmployeeId, Long existEmployeeId) {
        if (status == STATUS_REGULARIZED) {
            Long effective = inputEmployeeId != null ? inputEmployeeId : existEmployeeId;
            if (effective == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "状态为「已转正」时必须关联对应的职工档案");
            }
            employeeService.assertExists(effective);
            return effective;
        }
        if (inputEmployeeId != null) {
            throw new BizException(ResultCode.PARAM_ERROR, "仅「已转正」的实习生才能关联职工档案");
        }
        return null;
    }
}
