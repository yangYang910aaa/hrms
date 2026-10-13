package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 职工下拉项（/api/common/employee-options 的返回元素）
 *
 * 6 张人事子表的列表都要显示职工姓名，统一走这个"仅需登录"的轻量接口取，
 * 避免每张子表各写一套联表 VO。
 */
@Getter
@Setter
public class EmployeeOptionVO {

    private Long id;

    private String empNo;

    private String name;
}
