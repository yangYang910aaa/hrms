package com.yang.hr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 实习生入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class InternDTO {

    private Long id;

    @NotBlank(message = "不能为空")
    @Size(max = 30, message = "长度不能超过 30")
    private String name;

    @Size(max = 100, message = "长度不能超过 100")
    private String school;

    @Size(max = 50, message = "长度不能超过 50")
    private String major;

    /** 实习部门 */
    private Long deptId;

    @Size(max = 30, message = "长度不能超过 30")
    private String mentor;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 状态 1实习中 2已结束 3已转正 */
    @Min(value = 1, message = "取值非法（1实习中 2已结束 3已转正）")
    @Max(value = 3, message = "取值非法（1实习中 2已结束 3已转正）")
    private Integer status;

    /** 转正后关联的职工ID：状态=3 时必填（Service 校验） */
    private Long employeeId;
}
