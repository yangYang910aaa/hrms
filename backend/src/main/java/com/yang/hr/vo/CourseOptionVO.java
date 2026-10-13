package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 课程下拉项（/api/common/course-options 的返回元素）
 *
 * 培训的期次表（内部培训 / 一线工培训）列表要显示课程名，统一走这个"仅需登录"的轻量接口取，
 * 避免给两张期次表各写一套联表 VO（与人事子表用 employee-options 同一口径）。
 */
@Getter
@Setter
public class CourseOptionVO {

    private Long id;

    private String courseNo;

    private String courseName;
}
