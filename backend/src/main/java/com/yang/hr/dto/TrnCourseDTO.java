package com.yang.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 培训课程入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class TrnCourseDTO {

    private Long id;

    /** 课程编号（唯一） */
    @NotBlank(message = "课程编号不能为空")
    @Size(max = 30, message = "课程编号长度不能超过 30")
    private String courseNo;

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 100, message = "课程名称长度不能超过 100")
    private String courseName;

    /** 类别 1内部培训 2一线工培训 3其他 */
    @NotNull(message = "课程类别不能为空")
    @Min(value = 1, message = "课程类别取值非法（1内部培训 2一线工培训 3其他）")
    @Max(value = 3, message = "课程类别取值非法（1内部培训 2一线工培训 3其他）")
    private Integer courseType;

    @Size(max = 30, message = "讲师长度不能超过 30")
    private String teacher;

    /** 课时（对应列 DECIMAL(6,1)） */
    @DecimalMin(value = "0", message = "课时不能为负数")
    @Digits(integer = 5, fraction = 1, message = "课时最多 5 位整数、1 位小数")
    private BigDecimal hours;

    /** 学分（对应列 DECIMAL(4,1)） */
    @DecimalMin(value = "0", message = "学分不能为负数")
    @Digits(integer = 3, fraction = 1, message = "学分最多 3 位整数、1 位小数")
    private BigDecimal credit;

    /** 课程简介（TEXT，不限长度） */
    private String content;

    /** 状态 1启用 0停用；不传默认 1，修改时不传保持原值 */
    @Min(value = 0, message = "状态取值非法（1启用 0停用）")
    @Max(value = 1, message = "状态取值非法（1启用 0停用）")
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
