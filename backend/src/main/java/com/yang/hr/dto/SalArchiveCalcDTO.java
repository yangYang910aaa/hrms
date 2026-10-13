package com.yang.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 工资档案「单条计算入库」入参（POST /api/sal/archive）
 *
 * 只需要职工 + 月份：所有金额由服务端从各子表汇总算出，前端不参与计算。
 */
@Getter
@Setter
public class SalArchiveCalcDTO {

    @NotNull(message = "职工不能为空")
    private Long employeeId;

    /** 所属月份 YYYY-MM */
    @NotBlank(message = "所属月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "所属月份格式应为 YYYY-MM")
    private String month;
}
