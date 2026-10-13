package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 人事-职工主档（hr_employee）
 *
 * 字段口径：
 * - gender / edu_level / employ_type 存的是 sys_dict_type 里对应字典的 dict_value；
 * - employ_type 与 leave_date 必须一致（离职才有离职日期），由 Service 保证；
 * - salary_level_id 关联 sal_level.id，供 M5 工资计算取基本工资。
 */
@Getter
@Setter
@TableName("hr_employee")
public class HrEmployee extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工号（唯一，删除时会被改写以防唯一键冲突） */
    private String empNo;

    private String name;

    /** 性别 1男 2女（字典 sex） */
    private Integer gender;

    private LocalDate birthDate;

    /** 身份证号（18 位，格式由 Service 校验） */
    private String idCard;

    private String phone;

    private String email;

    private String address;

    /** 学历（字典 edu_level 的 dict_value） */
    private String eduLevel;

    private Long deptId;

    private String position;

    private LocalDate hireDate;

    /** 离职日期，仅 employ_type=3（离职）时有值 */
    private LocalDate leaveDate;

    /** 在职状态 1在职 2实习 3离职（字典 employ_type） */
    private Integer employType;

    /** 薪酬级别（sal_level.id） */
    private Long salaryLevelId;

    /** 状态 1正常 0停用 */
    private Integer status;

    /** 照片地址 */
    private String photo;

    private String remark;
}
