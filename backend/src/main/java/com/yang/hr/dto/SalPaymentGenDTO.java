package com.yang.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 工资发放「生成发放单」入参（POST /api/sal/payment）
 *
 * 只对**已确认(status=1)**的工资档案生成发放单；草稿 / 已发放的会在结果里说明原因并跳过。
 * `employeeIds` 为空表示该月所有已确认档案。
 */
@Getter
@Setter
public class SalPaymentGenDTO {

    /** 所属月份 YYYY-MM */
    @NotBlank(message = "所属月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "所属月份格式应为 YYYY-MM")
    private String month;

    /** 参与生成的职工ID；为空 = 该月全部已确认档案 */
    private List<Long> employeeIds;
}
