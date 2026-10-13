package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 培训-一线工培训（trn_frontline）—— 面向生产一线工人的培训期次
 *
 * 与 trn_internal 结构相近但**分表**（便于答辩逐条对照计划书原文）。两处关键差异：
 * - dept_id 这里是 **NOT NULL**（必须指明参训/生产部门）；trn_internal 的 dept_id 可空；
 * - 用「参训人数 / 合格人数」而非「计划/实到人数」，并额外约束「合格人数 ≤ 参训人数」。
 *
 * 无唯一业务键：删除直接逻辑删除，不需要改写键。
 */
@Getter
@Setter
@TableName("trn_frontline")
public class TrnFrontline extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 培训课程ID（trn_course.id） */
    private Long courseId;

    /** 期次编号 */
    private String batchNo;

    /** 参训（生产）部门（NOT NULL） */
    private Long deptId;

    /** 讲师 */
    private String trainer;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 参训人数 */
    private Integer workerCount;

    /** 合格人数 */
    private Integer qualifiedCount;

    /** 状态 0计划 1进行中 2已完成 */
    private Integer status;

    private String remark;
}
