package com.yang.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yang.hr.entity.SalArchive;
import com.yang.hr.vo.SalArchiveCalcItemVO;
import com.yang.hr.vo.SalArchiveVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工资档案 Mapper
 *
 * 含一条「月度汇总」SQL：以职工为行，用子查询把当月应发/应扣项一次算出来，
 * 避免 Service 里注入 6 个 Mapper 各查一次（口径与职工删除保护同源：一条 SQL 打多表）。
 */
public interface SalArchiveMapper extends BaseMapper<SalArchive> {

    /** 条件分页，联表带出职工工号/姓名与薪酬级别名（只读视图，增删改走 BaseMapper） */
    @Select("<script>"
            + "SELECT a.id, a.employee_id AS employeeId, a.month, a.level_id AS levelId, "
            + "       a.base_salary AS baseSalary, a.overtime_amount AS overtimeAmount, "
            + "       a.piece_amount AS pieceAmount, a.time_amount AS timeAmount, "
            + "       a.reward_amount AS rewardAmount, a.social_security AS socialSecurity, "
            + "       a.utility_fee AS utilityFee, a.deduct_amount AS deductAmount, a.tax, "
            + "       a.net_salary AS netSalary, a.status, a.calc_time AS calcTime, "
            + "       a.create_time AS createTime, a.create_by AS createBy, "
            + "       e.emp_no AS empNo, e.name AS employeeName, e.employ_type AS employType, "
            + "       l.level_name AS levelName "
            + "FROM sal_archive a "
            + "LEFT JOIN hr_employee e ON e.id = a.employee_id AND e.deleted = 0 "
            + "LEFT JOIN sal_level l   ON l.id = a.level_id       AND l.deleted = 0 "
            + "WHERE a.deleted = 0 "
            + "<if test=\"employeeId != null\"> AND a.employee_id = #{employeeId} </if>"
            + "<if test=\"month != null and month != ''\"> AND a.month = #{month} </if>"
            + "<if test=\"status != null\"> AND a.status = #{status} </if>"
            + "ORDER BY a.month DESC, a.id ASC"
            + "</script>")
    IPage<SalArchiveVO> selectVoPage(IPage<SalArchiveVO> page,
                                     @Param("employeeId") Long employeeId,
                                     @Param("month") String month,
                                     @Param("status") Integer status);

    @Select("SELECT a.id, a.employee_id AS employeeId, a.month, a.level_id AS levelId, "
            + "       a.base_salary AS baseSalary, a.overtime_amount AS overtimeAmount, "
            + "       a.piece_amount AS pieceAmount, a.time_amount AS timeAmount, "
            + "       a.reward_amount AS rewardAmount, a.social_security AS socialSecurity, "
            + "       a.utility_fee AS utilityFee, a.deduct_amount AS deductAmount, a.tax, "
            + "       a.net_salary AS netSalary, a.status, a.calc_time AS calcTime, "
            + "       a.create_time AS createTime, a.create_by AS createBy, "
            + "       e.emp_no AS empNo, e.name AS employeeName, e.employ_type AS employType, "
            + "       l.level_name AS levelName "
            + "FROM sal_archive a "
            + "LEFT JOIN hr_employee e ON e.id = a.employee_id AND e.deleted = 0 "
            + "LEFT JOIN sal_level l   ON l.id = a.level_id       AND l.deleted = 0 "
            + "WHERE a.deleted = 0 AND a.id = #{id}")
    SalArchiveVO selectVoById(@Param("id") Long id);

    /**
     * 汇总某月「所有未删除职工」的应发/应扣项（工资计算的数据源）。
     *
     * 一条 SQL 打 7 张表，口径与 {@code HrEmployeeMapper.countBusinessChildren} 一致：
     * - 加班 / 计件 / 计时：直接 SUM 批次2 **已算好落库的金额**，不重算；
     * - 奖罚：按 {@code happen_date} 落在当月的合计（奖励 +、处罚 −）；
     * - 社保：取该职工当月 {@code hr_social_security.personal_amount}（无记录则 0，
     *   并用 {@code socialSecurityRegistered} 告诉 Service「未登记」，由它在结果里提示）；
     * - 基本工资：取职工当前薪酬级别的 {@code base_salary}（未设级别则 levelId 为 null，由 Service 跳过并提示）。
     *
     * ⚠️ 不在此处排除离职职工：离职当月的工资仍要结清，交由前端按 employType 自行处理。
     *
     * @param employeeId 为空 = 全部职工；传值则只算该职工（单条计算复用同一条 SQL）
     */
    @Select("<script>"
            + "SELECT e.id AS employeeId, e.emp_no AS empNo, e.name AS employeeName, "
            + "       e.employ_type AS employType, e.salary_level_id AS levelId, "
            + "       l.level_name AS levelName, l.base_salary AS baseSalary, "
            + "       (SELECT COALESCE(SUM(o.amount), 0) FROM sal_overtime_record o "
            + "         WHERE o.employee_id = e.id AND o.month = #{month} AND o.deleted = 0) AS overtimeAmount, "
            + "       (SELECT COALESCE(SUM(p.amount), 0) FROM sal_piece_wage p "
            + "         WHERE p.employee_id = e.id AND p.month = #{month} AND p.deleted = 0) AS pieceAmount, "
            + "       (SELECT COALESCE(SUM(t.amount), 0) FROM sal_time_wage t "
            + "         WHERE t.employee_id = e.id AND t.month = #{month} AND t.deleted = 0) AS timeAmount, "
            + "       ((SELECT COALESCE(SUM(r.amount), 0) FROM hr_reward_punish r "
            + "          WHERE r.employee_id = e.id AND r.type = 1 AND r.deleted = 0 "
            + "            AND DATE_FORMAT(r.happen_date, '%Y-%m') = #{month}) "
            + "        - (SELECT COALESCE(SUM(r.amount), 0) FROM hr_reward_punish r "
            + "          WHERE r.employee_id = e.id AND r.type = 2 AND r.deleted = 0 "
            + "            AND DATE_FORMAT(r.happen_date, '%Y-%m') = #{month})) AS rewardAmount, "
            + "       COALESCE((SELECT s.personal_amount FROM hr_social_security s "
            + "         WHERE s.employee_id = e.id AND s.month = #{month} AND s.deleted = 0 "
            + "         ORDER BY s.id DESC LIMIT 1), 0) AS socialSecurity, "
            + "       (SELECT COUNT(*) FROM hr_social_security s "
            + "         WHERE s.employee_id = e.id AND s.month = #{month} AND s.deleted = 0) AS socialSecurityRegistered, "
            + "       (SELECT COALESCE(SUM(u.total_fee), 0) FROM sal_utility_fee u "
            + "         WHERE u.employee_id = e.id AND u.month = #{month} AND u.deleted = 0) AS utilityFee "
            + "FROM hr_employee e "
            + "LEFT JOIN sal_level l ON l.id = e.salary_level_id AND l.deleted = 0 "
            + "WHERE e.deleted = 0 "
            + "<if test=\"employeeId != null\"> AND e.id = #{employeeId} </if>"
            + "ORDER BY e.id"
            + "</script>")
    List<SalArchiveCalcItemVO> selectCalcRows(@Param("month") String month,
                                              @Param("employeeId") Long employeeId);
}
