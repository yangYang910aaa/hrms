package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.LeaveApproveDTO;
import com.yang.hr.dto.LeaveDTO;
import com.yang.hr.entity.HrLeave;

/**
 * 请假管理服务
 */
public interface LeaveService {

    Page<HrLeave> page(long current, long size, Long employeeId, Integer status, Integer leaveType);

    HrLeave getById(Long id);

    /** 提交请假单，落库即为「待审批」 */
    Long create(LeaveDTO dto);

    /** 只允许改「待审批」的单据；不碰审批字段 */
    void update(LeaveDTO dto);

    void delete(Long id);

    /** 审批：通过 / 驳回。审批人与审批时间由服务端写入 */
    void approve(Long id, LeaveApproveDTO dto);
}
