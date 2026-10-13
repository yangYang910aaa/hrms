package com.yang.hr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 工资发放「修改未发放单」入参（PUT /api/sal/payment）
 *
 * **刻意不含 `payDate` / `status`**：发放日期与已发放标记只能由
 * `PUT /{id}/pay` 写（服务端取当天日期），避免前端把单子标成"已发放"却没过账。
 * 仅未发放(status=0)的单子可改。
 */
@Getter
@Setter
public class SalPaymentUpdateDTO {

    @NotNull(message = "缺少工资发放单ID")
    private Long id;

    /** 发放方式 银行代发 / 现金（可空，不传保持原值） */
    @Size(max = 20, message = "发放方式长度不能超过 20")
    private String payWay;

    /** 银行账号（可空，支持清空） */
    @Size(max = 30, message = "银行账号长度不能超过 30")
    private String bankAccount;

    /** 备注（可空，支持清空） */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
