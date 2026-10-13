package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.SalPaymentGenDTO;
import com.yang.hr.dto.SalPaymentUpdateDTO;
import com.yang.hr.service.SalPaymentService;
import com.yang.hr.vo.SalPaymentGenResultVO;
import com.yang.hr.vo.SalPaymentVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工资发放接口（薪酬管理 → 工资发放）
 *
 * | 方法 | 路径 | 权限 | 说明 |
 * |---|---|---|---|
 * | GET | `/page` | `sal:payment:list` | 条件分页 |
 * | GET | `/{id}` | `sal:payment:list` | 详情 |
 * | POST | `/` | `sal:payment:add` | **生成发放单**（只对已确认档案） |
 * | PUT | `/` | `sal:payment:edit` | 改未发放单（方式/账号/备注） |
 * | PUT | `/{id}/pay` | `sal:payment:pay` | 发放（写当天日期，档案转已发放） |
 * | DELETE | `/{id}` | `sal:payment:remove` | 删除（仅未发放） |
 *
 * 发放单 DTO 刻意不含 `payDate` / `status` —— 这两个只能由 `/{id}/pay` 服务端写。
 */
@RestController
@RequestMapping("/api/sal/payment")
public class SalPaymentController {

    private final SalPaymentService paymentService;

    public SalPaymentController(SalPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/page")
    @SaCheckPermission("sal:payment:list")
    public R<Page<SalPaymentVO>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) Long employeeId,
                                      @RequestParam(required = false) String month,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(paymentService.page(current, size, employeeId, month, status));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("sal:payment:list")
    public R<SalPaymentVO> detail(@PathVariable Long id) {
        return R.ok(paymentService.getById(id));
    }

    /** 生成发放单：从已确认档案带入实发工资 */
    @PostMapping
    @SaCheckPermission("sal:payment:add")
    public R<SalPaymentGenResultVO> generate(@Valid @RequestBody SalPaymentGenDTO dto) {
        return R.ok("生成完成", paymentService.generate(dto));
    }

    /** 改未发放单 */
    @PutMapping
    @SaCheckPermission("sal:payment:edit")
    public R<Void> update(@Valid @RequestBody SalPaymentUpdateDTO dto) {
        paymentService.update(dto);
        return R.ok("修改成功", null);
    }

    /** 发放：未发放 → 已发放，同时把对应工资档案置为已发放 */
    @PutMapping("/{id}/pay")
    @SaCheckPermission("sal:payment:pay")
    public R<Void> pay(@PathVariable Long id) {
        paymentService.pay(id);
        return R.ok("已发放", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("sal:payment:remove")
    public R<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return R.ok("删除成功", null);
    }
}
