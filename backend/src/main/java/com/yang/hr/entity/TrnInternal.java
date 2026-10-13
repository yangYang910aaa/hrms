package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 培训-内部培训（trn_internal）—— 课程的一个培训期次
 *
 * - course_id 关联 trn_course.id（NOT NULL，由 Service 校验存在性）；
 * - dept_id 可空（主办/参训部门），传则校验存在；
 * - plan_persons / actual_persons / status 都是 NOT NULL，DTO 里可空 → 更新时"不传保持原值"；
 * - 无唯一业务键：删除直接逻辑删除，不需要改写键。
 */
@Getter
@Setter
@TableName("trn_internal")
public class TrnInternal extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 培训课程ID（trn_course.id） */
    private Long courseId;

    /** 期次编号 */
    private String batchNo;

    /** 主办/参训部门（可空） */
    private Long deptId;

    /** 讲师 */
    private String trainer;

    /** 培训地点 */
    private String place;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 计划人数 */
    private Integer planPersons;

    /** 实到人数 */
    private Integer actualPersons;

    /** 状态 0计划 1进行中 2已完成 */
    private Integer status;

    private String remark;
}
