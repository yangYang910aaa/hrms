package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.TrnCourseDTO;
import com.yang.hr.entity.TrnCourse;
import com.yang.hr.vo.CourseOptionVO;

import java.util.List;

/**
 * 培训课程主档服务
 */
public interface TrnCourseService {

    /** 条件分页：课程名模糊 / 类别 / 状态 */
    Page<TrnCourse> page(long current, long size, String courseName, Integer courseType, Integer status);

    TrnCourse getById(Long id);

    Long create(TrnCourseDTO dto);

    void update(TrnCourseDTO dto);

    void delete(Long id);

    /** 校验课程存在，不存在则抛业务异常（内部培训 / 一线工培训两张期次表共用的 courseId 校验） */
    void assertExists(Long id);

    /** 课程下拉项（供 /api/common/course-options 使用） */
    List<CourseOptionVO> options();
}
