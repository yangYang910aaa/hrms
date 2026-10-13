package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.TransferDTO;
import com.yang.hr.entity.HrEmployee;
import com.yang.hr.entity.HrTransfer;
import com.yang.hr.mapper.HrEmployeeMapper;
import com.yang.hr.mapper.HrTransferMapper;
import com.yang.hr.mapper.SysDeptMapper;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 职工调动服务实现
 */
@Service
public class TransferServiceImpl implements TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferServiceImpl.class);

    private final HrTransferMapper transferMapper;
    private final HrEmployeeMapper employeeMapper;
    private final SysDeptMapper deptMapper;
    private final EmployeeService employeeService;

    public TransferServiceImpl(HrTransferMapper transferMapper,
                               HrEmployeeMapper employeeMapper,
                               SysDeptMapper deptMapper,
                               EmployeeService employeeService) {
        this.transferMapper = transferMapper;
        this.employeeMapper = employeeMapper;
        this.deptMapper = deptMapper;
        this.employeeService = employeeService;
    }

    @Override
    public Page<HrTransfer> page(long current, long size, Long employeeId) {
        LambdaQueryWrapper<HrTransfer> wrapper = new LambdaQueryWrapper<HrTransfer>()
                .eq(employeeId != null, HrTransfer::getEmployeeId, employeeId)
                .orderByDesc(HrTransfer::getTransferDate)
                .orderByDesc(HrTransfer::getId);
        return transferMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public HrTransfer getById(Long id) {
        HrTransfer entity = transferMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "调动记录不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TransferDTO dto) {
        employeeService.assertExists(dto.getEmployeeId());
        HrEmployee employee = employeeMapper.selectById(dto.getEmployeeId());

        // 原部门/原岗位不填就从主档快照带出，避免登记时还要人工抄一遍
        Long fromDeptId = dto.getFromDeptId() != null ? dto.getFromDeptId() : employee.getDeptId();
        String fromPosition = StringUtils.hasText(dto.getFromPosition())
                ? dto.getFromPosition().trim() : employee.getPosition();
        assertDeptExists(fromDeptId);
        assertDeptExists(dto.getToDeptId());
        assertChanged(fromDeptId, dto.getToDeptId(), fromPosition, dto.getToPosition());

        HrTransfer entity = new HrTransfer();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setFromDeptId(fromDeptId);
        entity.setToDeptId(dto.getToDeptId());
        entity.setFromPosition(fromPosition);
        entity.setToPosition(trimToNull(dto.getToPosition()));
        entity.setTransferDate(dto.getTransferDate());
        entity.setReason(dto.getReason());
        entity.setApprover(trimToNull(dto.getApprover()));
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        transferMapper.insert(entity);
        log.info("新增职工调动：employeeId={} {} → {}（id={}）",
                entity.getEmployeeId(), entity.getFromDeptId(), entity.getToDeptId(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TransferDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少调动记录ID");
        }
        getById(dto.getId());
        HrTransfer exist = requireEntity(dto.getId());
        employeeService.assertExists(dto.getEmployeeId());
        HrEmployee employee = employeeMapper.selectById(dto.getEmployeeId());

        // 原部门/原岗位的回退链：入参 → **本条记录已有值** → 职工主档当前值。
        // 中间这一级不能省：职工部门事后变了，再编辑这条历史记录的"其他字段"（比如只改原因）时，
        // 若直接取主档当前值，就会把历史改动掉。
        Long fromDeptId = dto.getFromDeptId() != null ? dto.getFromDeptId()
                : (exist.getFromDeptId() != null ? exist.getFromDeptId() : employee.getDeptId());
        String fromPosition = StringUtils.hasText(dto.getFromPosition())
                ? dto.getFromPosition().trim()
                : (StringUtils.hasText(exist.getFromPosition())
                        ? exist.getFromPosition() : employee.getPosition());
        assertDeptExists(fromDeptId);
        assertDeptExists(dto.getToDeptId());
        assertChanged(fromDeptId, dto.getToDeptId(), fromPosition, dto.getToPosition());

        transferMapper.update(null, new LambdaUpdateWrapper<HrTransfer>()
                .eq(HrTransfer::getId, dto.getId())
                .set(HrTransfer::getEmployeeId, dto.getEmployeeId())
                .set(HrTransfer::getFromDeptId, fromDeptId)
                .set(HrTransfer::getToDeptId, dto.getToDeptId())
                .set(HrTransfer::getFromPosition, fromPosition)
                .set(HrTransfer::getToPosition, trimToNull(dto.getToPosition()))
                .set(HrTransfer::getTransferDate, dto.getTransferDate())
                .set(HrTransfer::getReason, dto.getReason())
                .set(HrTransfer::getApprover, trimToNull(dto.getApprover())));
        log.info("修改职工调动：id={} employeeId={}", dto.getId(), dto.getEmployeeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HrTransfer entity = getById(id);
        transferMapper.deleteById(id);
        log.info("删除职工调动：id={} employeeId={}", id, entity.getEmployeeId());
    }

    // ------------------------------------------------------------------ 内部方法

    private HrTransfer requireEntity(Long id) {
        HrTransfer entity = transferMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "调动记录不存在或已删除");
        }
        return entity;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void assertDeptExists(Long deptId) {
        if (deptId == null) {
            return;
        }
        if (deptMapper.selectById(deptId) == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "部门不存在或已删除：" + deptId);
        }
    }

    /** 调动前后的部门与岗位不能完全一样，否则不是一次有效调动 */
    private void assertChanged(Long fromDeptId, Long toDeptId, String fromPosition, String toPosition) {
        if (Objects.equals(fromDeptId, toDeptId)
                && Objects.equals(trimToNull(fromPosition), trimToNull(toPosition))) {
            throw new BizException(ResultCode.PARAM_ERROR, "调动前后的部门与岗位完全相同，请检查");
        }
    }
}
