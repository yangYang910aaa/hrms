package com.yang.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yang.hr.entity.SysDept;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 部门 Mapper
 */
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 统计该部门下的职工数（跨表只读统计）。
     *
     * 放在这里而不是新建 HrEmployeeMapper，是为了让 M3 自成闭环、不提前铺开 M4 的产物；
     * M4 建人事模块时可以把这条挪进 HrEmployeeMapper。
     */
    @Select("SELECT COUNT(*) FROM hr_employee WHERE dept_id = #{deptId} AND deleted = 0")
    long countEmployees(@Param("deptId") Long deptId);
}
