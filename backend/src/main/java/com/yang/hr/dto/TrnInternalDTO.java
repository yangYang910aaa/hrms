package com.yang.hr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 内部培训期次入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class TrnInternalDTO {

    private Long id;

    /** 培训课程ID（必填，需存在） */
    @NotNull(message = "培训课程不能为空")
    private Long courseId;

    @Size(max = 30, message = "期次编号长度不能超过 30")
    private String batchNo;

    /** 主办/参训部门（可空；填了需存在） */
    private Long deptId;

    @Size(max = 30, message = "讲师长度不能超过 30")
    private String trainer;

    @Size(max = 100, message = "培训地点长度不能超过 100")
    private String place;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 计划人数；不传默认 0，修改时不传保持原值 */
    @Min(value = 0, message = "计划人数不能为负数")
    private Integer planPersons;

    /** 实到人数；不传默认 0，修改时不传保持原值 */
    @Min(value = 0, message = "实到人数不能为负数")
    private Integer actualPersons;

    /** 状态 0计划 1进行中 2已完成；不传默认 0，修改时不传保持原值 */
    @Min(value = 0, message = "状态取值非法（0计划 1进行中 2已完成）")
    @Max(value = 2, message = "状态取值非法（0计划 1进行中 2已完成）")
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
