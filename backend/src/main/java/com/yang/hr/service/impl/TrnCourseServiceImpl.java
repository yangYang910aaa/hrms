package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.DeleteMask;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.TrnCourseDTO;
import com.yang.hr.entity.TrnCourse;
import com.yang.hr.mapper.TrnCourseMapper;
import com.yang.hr.service.TrnCourseService;
import com.yang.hr.vo.CourseOptionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 培训课程主档服务实现
 */
@Service
public class TrnCourseServiceImpl implements TrnCourseService {

    private static final Logger log = LoggerFactory.getLogger(TrnCourseServiceImpl.class);

    /** 逻辑删除时给唯一业务键加的后缀标记，避免二次录入撞唯一索引（规划书 §9 第 16 条） */
    private final TrnCourseMapper courseMapper;

    public TrnCourseServiceImpl(TrnCourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    @Override
    public Page<TrnCourse> page(long current, long size, String courseName, Integer courseType, Integer status) {
        LambdaQueryWrapper<TrnCourse> wrapper = new LambdaQueryWrapper<TrnCourse>()
                .like(StringUtils.hasText(courseName), TrnCourse::getCourseName, trim(courseName))
                .eq(courseType != null, TrnCourse::getCourseType, courseType)
                .eq(status != null, TrnCourse::getStatus, status)
                .orderByDesc(TrnCourse::getId);
        return courseMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public TrnCourse getById(Long id) {
        TrnCourse entity = courseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "课程不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TrnCourseDTO dto) {
        assertCourseNoUnique(dto.getCourseNo(), null);

        TrnCourse entity = new TrnCourse();
        entity.setCourseNo(dto.getCourseNo().trim());
        entity.setCourseName(dto.getCourseName());
        entity.setCourseType(dto.getCourseType());
        entity.setTeacher(trimToNull(dto.getTeacher()));
        entity.setHours(nvl(dto.getHours()));
        entity.setCredit(nvl(dto.getCredit()));
        entity.setContent(dto.getContent());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        courseMapper.insert(entity);
        log.info("新增培训课程：{} {}（id={}）", entity.getCourseNo(), entity.getCourseName(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TrnCourseDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少课程ID");
        }
        getById(dto.getId());
        assertCourseNoUnique(dto.getCourseNo(), dto.getId());

        // 显式 set：可空文本一律写入（这样清空才生效）；NOT NULL 的 hours/credit/status 用条件 set（不传保持原值）
        courseMapper.update(null, new LambdaUpdateWrapper<TrnCourse>()
                .eq(TrnCourse::getId, dto.getId())
                .set(TrnCourse::getCourseNo, dto.getCourseNo().trim())
                .set(TrnCourse::getCourseName, dto.getCourseName())
                .set(TrnCourse::getCourseType, dto.getCourseType())
                .set(TrnCourse::getTeacher, trimToNull(dto.getTeacher()))
                .set(dto.getHours() != null, TrnCourse::getHours, dto.getHours())
                .set(dto.getCredit() != null, TrnCourse::getCredit, dto.getCredit())
                .set(TrnCourse::getContent, dto.getContent())
                .set(dto.getStatus() != null, TrnCourse::getStatus, dto.getStatus())
                .set(TrnCourse::getRemark, dto.getRemark()));
        log.info("修改培训课程：id={} {} {}", dto.getId(), dto.getCourseNo(), dto.getCourseName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        TrnCourse course = getById(id);
        assertNoChildren(id);
        rewriteCourseNo(course);
        courseMapper.deleteById(id);
        log.info("删除培训课程：{} {}（id={}）", course.getCourseNo(), course.getCourseName(), id);
    }

    @Override
    public void assertExists(Long id) {
        if (id == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "课程ID不能为空");
        }
        if (courseMapper.selectById(id) == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "课程不存在或已删除");
        }
    }

    @Override
    public List<CourseOptionVO> options() {
        // 只取下拉需要的三列，不要 select *
        List<TrnCourse> list = courseMapper.selectList(new LambdaQueryWrapper<TrnCourse>()
                .select(TrnCourse::getId, TrnCourse::getCourseNo, TrnCourse::getCourseName)
                .orderByAsc(TrnCourse::getCourseNo));
        return list.stream().map(item -> {
            CourseOptionVO vo = new CourseOptionVO();
            vo.setId(item.getId());
            vo.setCourseNo(item.getCourseNo());
            vo.setCourseName(item.getCourseName());
            return vo;
        }).toList();
    }

    // ------------------------------------------------------------------ 内部方法

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void assertCourseNoUnique(String courseNo, Long excludeId) {
        if (!StringUtils.hasText(courseNo)) {
            return;
        }
        LambdaQueryWrapper<TrnCourse> wrapper = new LambdaQueryWrapper<TrnCourse>()
                .eq(TrnCourse::getCourseNo, courseNo.trim());
        if (excludeId != null) {
            wrapper.ne(TrnCourse::getId, excludeId);
        }
        if (courseMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "课程编号已存在：" + courseNo);
        }
    }

    /** 课程被任何培训期次引用时拒绝删除，否则期次会留下悬空的 course_id */
    private void assertNoChildren(Long courseId) {
        String detail = courseMapper.countChildren(courseId).stream()
                .filter(item -> item.getCnt() != null && item.getCnt() > 0)
                .map(item -> item.getLabel() + " " + item.getCnt() + " 条")
                .collect(Collectors.joining("、"));
        if (!detail.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "该课程已被培训期次引用（" + detail + "），请先处理后再删除");
        }
    }

    /** 逻辑删除前先给唯一键让路，否则以后再录入同课程编号会撞唯一索引 */
    private void rewriteCourseNo(TrnCourse course) {
        String courseNo = course.getCourseNo();
        if (!StringUtils.hasText(courseNo) || courseNo.contains(DeleteMask.MARK)) {
            return;
        }
        courseMapper.update(null, new LambdaUpdateWrapper<TrnCourse>()
                .eq(TrnCourse::getId, course.getId())
                // 列宽 30：截断后再拼，超长会 Data too long 让删除接口 500
                .set(TrnCourse::getCourseNo, DeleteMask.masked(courseNo, course.getId(), 30)));
    }
}
