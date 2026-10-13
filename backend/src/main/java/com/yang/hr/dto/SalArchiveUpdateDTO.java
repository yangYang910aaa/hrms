package com.yang.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 工资档案「修改草稿」入参（PUT /api/sal/archive）
 *
 * **刻意只开放 `deductAmount`（其他扣款）与 `tax`（个税）两个可改字段**：
 * 其余列（职工、月份、基本工资、加班、计件、计时、奖罚、社保、水电、实发）全部由服务端算，
 * 改了就等于绕过计算口径。仅草稿(status=0)可改，已确认/已发放一律拒绝。
 *
 * 2026-10-13 复查收口：移除了此前的 `employeeId` / `month` 两个字段 ——
 * 职工与月份在计算时即确定，改它们等于把档案改挂到别人名下，属约定外开放面；
 * 真算错了月份，删掉草稿重算即可（草稿可删）。
 */
@Getter
@Setter
public class SalArchiveUpdateDTO {

    @NotNull(message = "缺少工资档案ID")
    private Long id;

    /** 其他扣款（对应列 DECIMAL(12,2)）；不传保持原值，显式传 0 才清零 */
    @DecimalMin(value = "0", message = "其他扣款不能为负数")
    @Digits(integer = 10, fraction = 2, message = "其他扣款最多 10 位整数、2 位小数")
    private BigDecimal deductAmount;

    /** 个税（对应列 DECIMAL(12,2)）；不传保持原值，显式传 0 才清零 */
    @DecimalMin(value = "0", message = "个税不能为负数")
    @Digits(integer = 10, fraction = 2, message = "个税最多 10 位整数、2 位小数")
    private BigDecimal tax;

    /** 修改原因（写进日志，便于追溯手改记录） */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
