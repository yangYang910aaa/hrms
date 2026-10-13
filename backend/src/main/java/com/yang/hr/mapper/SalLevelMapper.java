package com.yang.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yang.hr.entity.SalLevel;
import com.yang.hr.vo.ChildCountVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 薪酬级别 Mapper
 */
public interface SalLevelMapper extends BaseMapper<SalLevel> {

    /**
     * 统计该级别被谁引用（供「级别删除保护」用）。
     *
     * 一条 UNION ALL 打两张表：加班标准（配置引用）+ 职工主档（在册职工引用）。
     * 口径与职工删除保护（HrEmployeeMapper.countBusinessChildren）一致。
     * **不含 sal_archive** —— 工资档案是历史快照，级别被删也不该改写历史。
     */
    @Select("SELECT '加班标准' AS label, COUNT(*) AS cnt FROM sal_overtime_standard WHERE level_id = #{id} AND deleted = 0 "
            + "UNION ALL SELECT '在册职工', COUNT(*) FROM hr_employee WHERE salary_level_id = #{id} AND deleted = 0")
    List<ChildCountVO> countChildren(@Param("id") Long id);
}
