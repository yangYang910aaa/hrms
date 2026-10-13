package com.yang.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yang.hr.entity.HrEmployee;
import com.yang.hr.vo.ChildCountVO;
import com.yang.hr.vo.EmployeeVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 职工主档 Mapper
 */
public interface HrEmployeeMapper extends BaseMapper<HrEmployee> {

    /**
     * 条件分页，联表带出部门名与薪酬级别名供列表页直接展示。
     *
     * 注意：这是**只读视图**，增删改一律走 BaseMapper（不写第二个入口，避免两套口径）。
     * 联表条件里都带 `deleted = 0`，避免把已删部门/级别的名字带出来。
     */
    @Select("<script>"
            + "SELECT e.id, e.emp_no, e.name, e.gender, e.birth_date, e.id_card, e.phone, e.email, "
            + "       e.address, e.edu_level, e.dept_id, e.position, e.hire_date, e.leave_date, "
            + "       e.employ_type, e.salary_level_id, e.status, e.photo, e.remark, e.create_time, "
            + "       d.dept_name AS deptName, l.level_name AS salaryLevelName "
            + "FROM hr_employee e "
            + "LEFT JOIN sys_dept d  ON d.id = e.dept_id         AND d.deleted = 0 "
            + "LEFT JOIN sal_level l ON l.id = e.salary_level_id AND l.deleted = 0 "
            + "WHERE e.deleted = 0 "
            + "<if test=\"name != null and name != ''\"> AND e.name LIKE CONCAT('%', #{name}, '%') </if>"
            + "<if test=\"deptId != null\"> AND e.dept_id = #{deptId} </if>"
            + "<if test=\"employType != null\"> AND e.employ_type = #{employType} </if>"
            + "<if test=\"status != null\"> AND e.status = #{status} </if>"
            + "ORDER BY e.id DESC"
            + "</script>")
    IPage<EmployeeVO> selectVoPage(IPage<EmployeeVO> page,
                                   @Param("name") String name,
                                   @Param("deptId") Long deptId,
                                   @Param("employType") Integer employType,
                                   @Param("status") Integer status);

    /** 按主键取职工视图（同样带出部门名/级别名） */
    @Select("SELECT e.id, e.emp_no, e.name, e.gender, e.birth_date, e.id_card, e.phone, e.email, "
            + "       e.address, e.edu_level, e.dept_id, e.position, e.hire_date, e.leave_date, "
            + "       e.employ_type, e.salary_level_id, e.status, e.photo, e.remark, e.create_time, "
            + "       d.dept_name AS deptName, l.level_name AS salaryLevelName "
            + "FROM hr_employee e "
            + "LEFT JOIN sys_dept d  ON d.id = e.dept_id         AND d.deleted = 0 "
            + "LEFT JOIN sal_level l ON l.id = e.salary_level_id AND l.deleted = 0 "
            + "WHERE e.deleted = 0 AND e.id = #{id}")
    EmployeeVO selectVoById(@Param("id") Long id);

    /**
     * 统计该职工名下的业务子表记录数（供「职工删除保护」用）。
     *
     * 一条 UNION ALL 打 11 张子表（人事 5 + 薪酬 6），比往 Service 里注入 11 个 Mapper 逐个 count
     * 更省依赖、只跑一次往返。
     *
     * 两组口径说明：
     * - **不含 `hr_intern`** —— 实习生是独立表，`employee_id` 只在「已转正」时回填，
     *   不属于"职工名下记录"，删职工不该被它挡住。
     * - 薪酬 6 张（水电/加班/计件/计时/档案/发放）全部计入：它们都以 `employee_id` 挂在职工名下，
     *   不挡就会留下孤儿数据。其中档案/发放属 M5 批次3 才写入，此处一并纳入以免二次改本方法。
     */
    @Select("SELECT '社保记录' AS label, COUNT(*) AS cnt FROM hr_social_security WHERE employee_id = #{id} AND deleted = 0 "
            + "UNION ALL SELECT '奖罚记录', COUNT(*) FROM hr_reward_punish WHERE employee_id = #{id} AND deleted = 0 "
            + "UNION ALL SELECT '调动记录', COUNT(*) FROM hr_transfer WHERE employee_id = #{id} AND deleted = 0 "
            + "UNION ALL SELECT '证书', COUNT(*) FROM hr_certificate WHERE employee_id = #{id} AND deleted = 0 "
            + "UNION ALL SELECT '请假单', COUNT(*) FROM hr_leave WHERE employee_id = #{id} AND deleted = 0 "
            + "UNION ALL SELECT '水电费', COUNT(*) FROM sal_utility_fee WHERE employee_id = #{id} AND deleted = 0 "
            + "UNION ALL SELECT '加班记录', COUNT(*) FROM sal_overtime_record WHERE employee_id = #{id} AND deleted = 0 "
            + "UNION ALL SELECT '计件工资', COUNT(*) FROM sal_piece_wage WHERE employee_id = #{id} AND deleted = 0 "
            + "UNION ALL SELECT '计时工资', COUNT(*) FROM sal_time_wage WHERE employee_id = #{id} AND deleted = 0 "
            + "UNION ALL SELECT '工资档案', COUNT(*) FROM sal_archive WHERE employee_id = #{id} AND deleted = 0 "
            + "UNION ALL SELECT '工资发放单', COUNT(*) FROM sal_payment WHERE employee_id = #{id} AND deleted = 0")
    List<ChildCountVO> countBusinessChildren(@Param("id") Long id);

    /**
     * 取某月生日的职工（生日提醒用），联表带出部门名，按「当月第几天」升序 —— 生日临近的排前面。
     *
     * 两个刻意的过滤：
     * - **排除离职（employ_type = 3）**：给离职员工推生日提醒没有意义；
     * - 排除未填出生日期的。
     *
     * 注意 `MONTH()/DAY()` 是用不上索引的；职工表规模小（几百行）无所谓，
     * 真到几万行应加一个冗余的 `birth_month` 列并建索引。
     */
    @Select("SELECT e.id, e.emp_no, e.name, e.gender, e.birth_date, e.phone, e.dept_id, e.employ_type, "
            + "       d.dept_name AS deptName "
            + "FROM hr_employee e "
            + "LEFT JOIN sys_dept d ON d.id = e.dept_id AND d.deleted = 0 "
            + "WHERE e.deleted = 0 AND e.birth_date IS NOT NULL AND e.employ_type <> 3 "
            + "  AND MONTH(e.birth_date) = #{month} "
            + "ORDER BY DAY(e.birth_date), e.id")
    List<EmployeeVO> selectBirthdays(@Param("month") int month);
}
