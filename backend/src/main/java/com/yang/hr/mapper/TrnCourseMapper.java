package com.yang.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yang.hr.entity.TrnCourse;
import com.yang.hr.vo.ChildCountVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 培训课程主档 Mapper
 *
 * 纯 BaseMapper 即可：课程列表无联表需求（期次表要显示课程名，统一走
 * `/api/common/course-options` 那个"仅需登录"的下拉接口，不写联表 VO）。
 */
public interface TrnCourseMapper extends BaseMapper<TrnCourse> {

    /**
     * 统计该课程被多少个培训期次引用（供「课程删除保护」用）。
     *
     * 一条 UNION ALL 打两张期次表（内部培训 / 一线工培训），比在 Service 里注入两个 Mapper 逐个 count
     * 更省依赖、只跑一次往返；口径与职工删除保护（HrEmployeeMapper.countBusinessChildren）一致。
     */
    @Select("SELECT '内部培训' AS label, COUNT(*) AS cnt FROM trn_internal  WHERE course_id = #{id} AND deleted = 0 "
            + "UNION ALL SELECT '一线工培训', COUNT(*) FROM trn_frontline WHERE course_id = #{id} AND deleted = 0")
    List<ChildCountVO> countChildren(@Param("id") Long id);
}
