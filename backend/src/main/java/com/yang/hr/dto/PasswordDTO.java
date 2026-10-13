package com.yang.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 重置口令入参
 */
@Getter
@Setter
public class PasswordDTO {

    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 100, message = "长度需在 6-100 之间")
    private String newPassword;
}
