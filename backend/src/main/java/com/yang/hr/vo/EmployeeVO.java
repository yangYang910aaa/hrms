package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 职工视图：主档字段 + 联表带出的部门名/薪酬级别名，供列表页与详情页直接渲染。
 *
 * 字典类字段（gender / eduLevel / employType）只回原值，显示文本由前端用
 * `/api/common/dict/{type}` 翻译 —— 与字典模块的设计保持一致。
 */
@Getter
@Setter
public class EmployeeVO {

    private Long id;

    private String empNo;

    private String name;

    private Integer gender;

    private LocalDate birthDate;

    /** 派生字段：按出生日期算出的周岁（birthDate 为空则 null）。列表页与生日提醒共用 */
    private Integer age;

    private String idCard;

    private String phone;

    private String email;

    private String address;

    private String eduLevel;

    private Long deptId;

    /** 联表带出：部门名称 */
    private String deptName;

    private String position;

    private LocalDate hireDate;

    private LocalDate leaveDate;

    private Integer employType;

    private Long salaryLevelId;

    /** 联表带出：薪酬级别名称 */
    private String salaryLevelName;

    private Integer status;

    private String photo;

    private String remark;

    private LocalDateTime createTime;
}
