package com.yang.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 工资档案「批量计算入库」入参（POST /api/sal/archive/calc）
 *
 * `employeeIds` 为空表示**全部未删除职工**（离职的也算，方便结清当月工资）；
 * 只传一个 id 时等同单条计算。
 */
@Getter
@Setter
public class SalArchiveBatchCalcDTO {

    /** 所属月份 YYYY-MM */
    @NotBlank(message = "所属月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "所属月份格式应为 YYYY-MM")
    private String month;

    /** 参与计算的职工ID；为空 = 全部 */
    private List<Long> employeeIds;
}
