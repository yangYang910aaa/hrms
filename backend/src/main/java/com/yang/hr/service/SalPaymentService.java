package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.SalPaymentGenDTO;
import com.yang.hr.dto.SalPaymentUpdateDTO;
import com.yang.hr.vo.SalPaymentGenResultVO;
import com.yang.hr.vo.SalPaymentVO;

/**
 * 工资发放服务（M5 批次3）
 *
 * 与工资档案的衔接：**只对已确认(status=1)的档案生成发放单**；
 * 发放时服务端写当天日期，并把对应档案置为 2 已发放 —— 之后该档案不可改、不可重算。
 */
public interface SalPaymentService {

    /** 条件分页：职工 / 月份 / 状态 */
    Page<SalPaymentVO> page(long current, long size, Long employeeId, String month, Integer status);

    SalPaymentVO getById(Long id);

    /** 生成发放单：从已确认档案带入实发工资，状态为未发放(0) */
    SalPaymentGenResultVO generate(SalPaymentGenDTO dto);

    /** 修改未发放单（发放方式 / 银行账号 / 备注） */
    void update(SalPaymentUpdateDTO dto);

    /** 发放：0 未发放 → 1 已发放，同时把对应工资档案置为 2 已发放 */
    void pay(Long id);

    /** 删除未发放单（删除时给 (employee_id, month) 唯一键让路） */
    void delete(Long id);
}
