package com.yang.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yang.hr.entity.SalPayment;
import com.yang.hr.vo.SalPaymentVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工资发放 Mapper
 */
public interface SalPaymentMapper extends BaseMapper<SalPayment> {

    /** 条件分页，联表带出职工工号/姓名（只读视图，增删改走 BaseMapper） */
    @Select("<script>"
            + "SELECT p.id, p.employee_id AS employeeId, p.month, p.net_salary AS netSalary, "
            + "       p.pay_date AS payDate, p.pay_way AS payWay, p.bank_account AS bankAccount, "
            + "       p.status, p.remark, p.create_time AS createTime, "
            + "       e.emp_no AS empNo, e.name AS employeeName "
            + "FROM sal_payment p "
            + "LEFT JOIN hr_employee e ON e.id = p.employee_id AND e.deleted = 0 "
            + "WHERE p.deleted = 0 "
            + "<if test=\"employeeId != null\"> AND p.employee_id = #{employeeId} </if>"
            + "<if test=\"month != null and month != ''\"> AND p.month = #{month} </if>"
            + "<if test=\"status != null\"> AND p.status = #{status} </if>"
            + "ORDER BY p.month DESC, p.id ASC"
            + "</script>")
    IPage<SalPaymentVO> selectVoPage(IPage<SalPaymentVO> page,
                                     @Param("employeeId") Long employeeId,
                                     @Param("month") String month,
                                     @Param("status") Integer status);

    @Select("SELECT p.id, p.employee_id AS employeeId, p.month, p.net_salary AS netSalary, "
            + "       p.pay_date AS payDate, p.pay_way AS payWay, p.bank_account AS bankAccount, "
            + "       p.status, p.remark, p.create_time AS createTime, "
            + "       e.emp_no AS empNo, e.name AS employeeName "
            + "FROM sal_payment p "
            + "LEFT JOIN hr_employee e ON e.id = p.employee_id AND e.deleted = 0 "
            + "WHERE p.deleted = 0 AND p.id = #{id}")
    SalPaymentVO selectVoById(@Param("id") Long id);
}
