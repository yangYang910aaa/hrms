package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.SalPaymentGenDTO;
import com.yang.hr.dto.SalPaymentUpdateDTO;
import com.yang.hr.entity.SalArchive;
import com.yang.hr.entity.SalPayment;
import com.yang.hr.mapper.SalArchiveMapper;
import com.yang.hr.mapper.SalPaymentMapper;
import com.yang.hr.service.SalPaymentService;
import com.yang.hr.vo.SalPaymentGenResultVO;
import com.yang.hr.vo.SalPaymentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 工资发放服务实现
 */
@Service
public class SalPaymentServiceImpl implements SalPaymentService {

    private static final Logger log = LoggerFactory.getLogger(SalPaymentServiceImpl.class);

    /** 工资档案状态：草稿 */
    private static final int ARCHIVE_DRAFT = 0;

    /** 工资档案状态：已确认 */
    private static final int ARCHIVE_CONFIRMED = 1;

    /** 工资档案状态：已发放 */
    private static final int ARCHIVE_PAID = 2;

    /** 发放单状态：未发放 */
    private static final int PAY_UNPAID = 0;

    private final SalPaymentMapper paymentMapper;
    private final SalArchiveMapper archiveMapper;

    public SalPaymentServiceImpl(SalPaymentMapper paymentMapper, SalArchiveMapper archiveMapper) {
        this.paymentMapper = paymentMapper;
        this.archiveMapper = archiveMapper;
    }

    @Override
    public Page<SalPaymentVO> page(long current, long size, Long employeeId, String month, Integer status) {
        Page<SalPaymentVO> page = new Page<>(current, size);
        paymentMapper.selectVoPage(page, employeeId, StringUtils.hasText(month) ? month.trim() : null, status);
        return page;
    }

    @Override
    public SalPaymentVO getById(Long id) {
        SalPaymentVO vo = paymentMapper.selectVoById(id);
        if (vo == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "工资发放单不存在或已删除");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalPaymentGenResultVO generate(SalPaymentGenDTO dto) {
        String month = trim(dto.getMonth());
        List<Long> wanted = dto.getEmployeeIds() == null ? List.of()
                : dto.getEmployeeIds().stream().filter(Objects::nonNull).distinct().toList();

        List<SalArchive> archives = archiveMapper.selectList(new LambdaQueryWrapper<SalArchive>()
                .eq(SalArchive::getMonth, month)
                .orderByAsc(SalArchive::getId));

        SalPaymentGenResultVO result = new SalPaymentGenResultVO();
        result.setMonth(month);

        Set<Long> found = new HashSet<>();
        for (SalArchive archive : archives) {
            found.add(archive.getEmployeeId());
            if (!wanted.isEmpty() && !wanted.contains(archive.getEmployeeId())) {
                continue;
            }
            if (archive.getStatus() == null || archive.getStatus() != ARCHIVE_CONFIRMED) {
                // 草稿不能发钱、已发放的不用重复生成
                if (archive.getStatus() != null && archive.getStatus() == ARCHIVE_PAID) {
                    result.setPaidCount(result.getPaidCount() + 1);
                } else {
                    result.setDraftCount(result.getDraftCount() + 1);
                }
                continue;
            }
            result.setTotal(result.getTotal() + 1);
            if (findByEmployeeMonth(archive.getEmployeeId(), month) != null) {
                result.setSkippedExists(result.getSkippedExists() + 1);
                continue;
            }
            SalPayment payment = new SalPayment();
            payment.setEmployeeId(archive.getEmployeeId());
            payment.setMonth(month);
            payment.setNetSalary(archive.getNetSalary());
            payment.setStatus(PAY_UNPAID);
            payment.setCreateBy(SecurityUtils.currentUserIdOrNull());
            paymentMapper.insert(payment);
            result.setCreated(result.getCreated() + 1);
            log.info("生成工资发放单：employeeId={} month={} 实发={}（id={}）",
                    payment.getEmployeeId(), month, payment.getNetSalary(), payment.getId());
        }

        if (!wanted.isEmpty()) {
            for (Long id : wanted) {
                if (!found.contains(id)) {
                    result.getNotices().add("职工ID " + id + " 在 " + month + " 没有工资档案，已跳过");
                }
            }
        }
        if (result.getDraftCount() > 0) {
            result.getNotices().add("有 " + result.getDraftCount()
                    + " 条档案还是草稿，请先确认后再生成发放单");
        }
        if (result.getPaidCount() > 0) {
            result.getNotices().add("有 " + result.getPaidCount() + " 条档案已发放，本次未重复生成");
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SalPaymentUpdateDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少工资发放单ID");
        }
        SalPayment payment = paymentMapper.selectById(dto.getId());
        if (payment == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "工资发放单不存在或已删除");
        }
        assertUnpaid(payment, "修改");

        paymentMapper.update(null, new LambdaUpdateWrapper<SalPayment>()
                .eq(SalPayment::getId, dto.getId())
                .set(SalPayment::getPayWay, trimToNull(dto.getPayWay()))
                .set(SalPayment::getBankAccount, trimToNull(dto.getBankAccount()))
                .set(SalPayment::getRemark, trimToNull(dto.getRemark())));
        log.info("修改工资发放单：id={} payWay={} bankAccount={}", dto.getId(),
                trimToNull(dto.getPayWay()), trimToNull(dto.getBankAccount()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long id) {
        SalPayment payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "工资发放单不存在或已删除");
        }
        assertUnpaid(payment, "发放");

        // 发放必须能把对应档案置为「已发放」—— 档案不在了就说明数据不一致，明确报错而不是悄悄发出去
        SalArchive archive = findArchiveByEmployeeMonth(payment.getEmployeeId(), payment.getMonth());
        if (archive == null || archive.getStatus() == null || archive.getStatus() != ARCHIVE_CONFIRMED) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "对应的工资档案不存在或不是已确认状态，无法发放");
        }

        paymentMapper.update(null, new LambdaUpdateWrapper<SalPayment>()
                .eq(SalPayment::getId, id)
                .set(SalPayment::getStatus, 1)
                // 发放日期由服务端取当天，不接受前端传入
                .set(SalPayment::getPayDate, LocalDate.now()));
        archiveMapper.update(null, new LambdaUpdateWrapper<SalArchive>()
                .eq(SalArchive::getId, archive.getId())
                .set(SalArchive::getStatus, ARCHIVE_PAID));
        log.info("发放工资：paymentId={} employeeId={} month={} 实发={} payDate={}",
                id, payment.getEmployeeId(), payment.getMonth(), payment.getNetSalary(), LocalDate.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalPayment payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "工资发放单不存在或已删除");
        }
        assertUnpaid(payment, "删除");
        releaseUniqueKey(payment);
        paymentMapper.deleteById(id);
        log.info("删除工资发放单：id={} employeeId={} month={}", id, payment.getEmployeeId(), payment.getMonth());
    }

    // ------------------------------------------------------------------ 内部方法

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private SalPayment findByEmployeeMonth(Long employeeId, String month) {
        return paymentMapper.selectOne(new LambdaQueryWrapper<SalPayment>()
                .eq(SalPayment::getEmployeeId, employeeId)
                .eq(SalPayment::getMonth, month));
    }

    /** 按职工 + 月份找工资档案（发放时要把档案置为已发放） */
    private SalArchive findArchiveByEmployeeMonth(Long employeeId, String month) {
        return archiveMapper.selectOne(new LambdaQueryWrapper<SalArchive>()
                .eq(SalArchive::getEmployeeId, employeeId)
                .eq(SalArchive::getMonth, month));
    }

    private void assertUnpaid(SalPayment payment, String action) {
        if (payment.getStatus() == null || payment.getStatus() != PAY_UNPAID) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "该发放单已发放，不能" + action + "（已发放的工资不允许改动）");
        }
    }

    /**
     * 逻辑删除前让出复合唯一键 (employee_id, month)：把 `employee_id` 置为 **`-id`**。
     * 与批次1/批次2 的"让路"约定一致。
     */
    private void releaseUniqueKey(SalPayment entity) {
        Long employeeId = entity.getEmployeeId();
        if (employeeId == null || employeeId < 0) {
            return;
        }
        paymentMapper.update(null, new LambdaUpdateWrapper<SalPayment>()
                .eq(SalPayment::getId, entity.getId())
                .set(SalPayment::getEmployeeId, -entity.getId()));
    }
}
