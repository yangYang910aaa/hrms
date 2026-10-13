package com.yang.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 证书入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class CertificateDTO {

    private Long id;

    @NotNull(message = "不能为空")
    private Long employeeId;

    @NotBlank(message = "不能为空")
    @Size(max = 100, message = "长度不能超过 100")
    private String certName;

    @Size(max = 50, message = "长度不能超过 50")
    private String certNo;

    @Size(max = 100, message = "长度不能超过 100")
    private String issueOrg;

    private LocalDate issueDate;

    /** 有效期至：与发证日期都填时不得早于发证日期（Service 校验） */
    private LocalDate expireDate;

    @Size(max = 255, message = "长度不能超过 255")
    private String fileUrl;
}
