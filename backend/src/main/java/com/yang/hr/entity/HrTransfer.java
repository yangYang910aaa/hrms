package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 人事-职工调动（hr_transfer）
 *
 * 这是一张「登记表」：记录调动前后信息，**不会自动改写 hr_employee 的部门/岗位**
 * （主档调整仍由人事在职工管理里做，避免一次登记产生看不见的副作用）。
 * 原部门/原岗位不填时，由 Service 从职工主档快照带出。
 * 无唯一业务键：删除直接逻辑删除。
 */
@Getter
@Setter
@TableName("hr_transfer")
public class HrTransfer extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    /** 原部门 */
    private Long fromDeptId;

    /** 新部门 */
    private Long toDeptId;

    /** 原岗位 */
    private String fromPosition;

    /** 新岗位 */
    private String toPosition;

    /** 调动日期 */
    private LocalDate transferDate;

    /** 调动原因 */
    private String reason;

    /** 审批人 */
    private String approver;
}
