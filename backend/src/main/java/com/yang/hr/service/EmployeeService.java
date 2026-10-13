package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.EmployeeDTO;
import com.yang.hr.vo.EmployeeOptionVO;
import com.yang.hr.vo.EmployeeVO;

import java.util.List;

/**
 * 职工主档服务
 */
public interface EmployeeService {

    /** 条件分页：姓名模糊 / 部门 / 在职状态 / 状态 */
    Page<EmployeeVO> page(long current, long size, String name, Long deptId, Integer employType, Integer status);

    EmployeeVO getById(Long id);

    /** 某月生日的职工（缺省当月）；已离职与未填生日的不返回 */
    List<EmployeeVO> birthdays(Integer month);

    Long create(EmployeeDTO dto);

    void update(EmployeeDTO dto);

    void delete(Long id);

    /** 校验职工存在，不存在则抛业务异常（人事 6 张子表共用的 employeeId 校验） */
    void assertExists(Long id);

    /** 职工下拉项（供 /api/common/employee-options 使用） */
    List<EmployeeOptionVO> options();
}
