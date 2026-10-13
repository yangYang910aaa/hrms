package com.yang.hr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 职工入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 *
 * 只做「单字段格式」校验；跨字段的业务规则（工号唯一、身份证合法性、在职状态与离职日期一致性等）
 * 放 Service，那里能拿到库里的既有值做合并判断。
 */
@Getter
@Setter
public class EmployeeDTO {

    private Long id;

    @NotBlank(message = "不能为空")
    @Size(max = 30, message = "长度不能超过 30")
    private String empNo;

    @NotBlank(message = "不能为空")
    @Size(max = 30, message = "长度不能超过 30")
    private String name;

    /** 性别 1男 2女（字典 sex） */
    private Integer gender;

    @PastOrPresent(message = "不能晚于今天")
    private LocalDate birthDate;

    /** 身份证号：18 位，最后一位可为 X；格式与内嵌出生日期由 Service 校验 */
    @Size(max = 18, message = "长度不能超过 18")
    private String idCard;

    @Size(max = 20, message = "长度不能超过 20")
    private String phone;

    @Email(message = "格式不正确")
    @Size(max = 50, message = "长度不能超过 50")
    private String email;

    @Size(max = 200, message = "长度不能超过 200")
    private String address;

    /** 学历（字典 edu_level 的 dict_value） */
    @Size(max = 20, message = "长度不能超过 20")
    private String eduLevel;

    private Long deptId;

    @Size(max = 50, message = "长度不能超过 50")
    private String position;

    private LocalDate hireDate;

    /** 离职日期：仅在职状态=3（离职）时允许填写 */
    private LocalDate leaveDate;

    /** 在职状态 1在职 2实习 3离职（字典 employ_type） */
    private Integer employType;

    private Long salaryLevelId;

    private Integer status;

    @Size(max = 255, message = "长度不能超过 255")
    private String photo;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;
}
