package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.LeaveApproveDTO;
import com.yang.hr.dto.LeaveDTO;
import com.yang.hr.entity.HrLeave;
import com.yang.hr.entity.SysUser;
import com.yang.hr.mapper.HrLeaveMapper;
import com.yang.hr.mapper.SysUserMapper;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.LeaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假管理服务实现
 */
@Service
public class LeaveServiceImpl implements LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveServiceImpl.class);

    /** 待审批 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    private final HrLeaveMapper leaveMapper;
    private final SysUserMapper userMapper;
    private final EmployeeService employeeService;

    public LeaveServiceImpl(HrLeaveMapper leaveMapper,
                            SysUserMapper userMapper,
                            EmployeeService employeeService) {
        this.leaveMapper = leaveMapper;
        this.userMapper = userMapper;
        this.employeeService = employeeService;
    }

    @Override
    public Page<HrLeave> page(long current, long size, Long employeeId, Integer status, Integer leaveType) {
        LambdaQueryWrapper<HrLeave> wrapper = new LambdaQueryWrapper<HrLeave>()
                .eq(employeeId != null, HrLeave::getEmployeeId, employeeId)
                .eq(status != null, HrLeave::getStatus, status)
                .eq(leaveType != null, HrLeave::getLeaveType, leaveType)
                .orderByDesc(HrLeave::getStartTime)
                .orderByDesc(HrLeave::getId);
        return leaveMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public HrLeave getById(Long id) {
        HrLeave entity = leaveMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "请假单不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(LeaveDTO dto) {
        employeeService.assertExists(dto.getEmployeeId());
        assertTimeRange(dto);

        HrLeave entity = new HrLeave();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setLeaveType(dto.getLeaveType());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setDays(dto.getDays() == null ? BigDecimal.ZERO : dto.getDays());
        entity.setReason(dto.getReason());
        // 新建的单据一律「待审批」，审批字段留空 —— 不接受前端传入的状态
        entity.setStatus(STATUS_PENDING);
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        leaveMapper.insert(entity);
        log.info("提交请假单：employeeId={}（id={}）", entity.getEmployeeId(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(LeaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少请假单ID");
        }
        HrLeave exist = getById(dto.getId());
        // 已审批的单据不允许再改，否则审批结论可以被事后篡改
        if (exist.getStatus() != null && exist.getStatus() != STATUS_PENDING) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "该请假单已审批（" + statusText(exist.getStatus()) + "），不允许修改");
        }
        employeeService.assertExists(dto.getEmployeeId());
        assertTimeRange(dto);

        // 注意：这里刻意不写 status / approver / approve_time / approve_remark
        // days 是 NOT NULL 但 DTO 可空 —— 可空即"不传就保持原值"（要置 0 请显式传 0）
        leaveMapper.update(null, new LambdaUpdateWrapper<HrLeave>()
                .eq(HrLeave::getId, dto.getId())
                .set(HrLeave::getEmployeeId, dto.getEmployeeId())
                .set(HrLeave::getLeaveType, dto.getLeaveType())
                .set(HrLeave::getStartTime, dto.getStartTime())
                .set(HrLeave::getEndTime, dto.getEndTime())
                .set(dto.getDays() != null, HrLeave::getDays, dto.getDays())
                .set(HrLeave::getReason, dto.getReason()));
        log.info("修改请假单：id={} employeeId={}", dto.getId(), dto.getEmployeeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, LeaveApproveDTO dto) {
        HrLeave exist = getById(id);
        if (exist.getStatus() == null || exist.getStatus() != STATUS_PENDING) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "该请假单已是「" + statusText(exist.getStatus()) + "」，不能重复审批");
        }

        // 审批人取当前登录用户的真实姓名（不接受前端传入，避免伪造审批人）；审批时间取服务端时间
        Long userId = SecurityUtils.currentUserId();
        SysUser user = userMapper.selectById(userId);
        String approver = (user != null && StringUtils.hasText(user.getRealName()))
                ? user.getRealName() : ("用户" + userId);

        leaveMapper.update(null, new LambdaUpdateWrapper<HrLeave>()
                .eq(HrLeave::getId, id)
                .set(HrLeave::getStatus, dto.getStatus())
                .set(HrLeave::getApprover, approver)
                .set(HrLeave::getApproveTime, LocalDateTime.now())
                .set(HrLeave::getApproveRemark, trimToNull(dto.getApproveRemark())));
        log.info("请假单审批：id={} status={} approver={}", id, statusText(dto.getStatus()), approver);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HrLeave entity = getById(id);
        leaveMapper.deleteById(id);
        log.info("删除请假单：id={} employeeId={}", id, entity.getEmployeeId());
    }

    // ------------------------------------------------------------------ 内部方法

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void assertTimeRange(LeaveDTO dto) {
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BizException(ResultCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case STATUS_PENDING -> "待审批";
            case STATUS_APPROVED -> "已通过";
            case STATUS_REJECTED -> "已驳回";
            default -> "未知";
        };
    }
}
