package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 人事-实习生管理（hr_intern）
 *
 * **独立表**：实习生未必是职工，所以用姓名而非 employee_id 标识；
 * employee_id 仅在「已转正(3)」时回填，指向转正后建立的职工档案。
 * 无唯一业务键：删除直接逻辑删除。
 */
@Getter
@Setter
@TableName("hr_intern")
public class HrIntern extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String school;

    private String major;

    /** 实习部门 */
    private Long deptId;

    /** 带教导师 */
    private String mentor;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 状态 1实习中 2已结束 3已转正 */
    private Integer status;

    /** 转正后关联的职工ID（仅状态=3 时有值） */
    private Long employeeId;
}
