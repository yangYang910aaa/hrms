package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.CertificateDTO;
import com.yang.hr.entity.HrCertificate;
import com.yang.hr.mapper.HrCertificateMapper;
import com.yang.hr.service.CertificateService;
import com.yang.hr.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * 证书管理服务实现
 */
@Service
public class CertificateServiceImpl implements CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateServiceImpl.class);

    private final HrCertificateMapper certificateMapper;
    private final EmployeeService employeeService;

    public CertificateServiceImpl(HrCertificateMapper certificateMapper, EmployeeService employeeService) {
        this.certificateMapper = certificateMapper;
        this.employeeService = employeeService;
    }

    @Override
    public Page<HrCertificate> page(long current, long size, Long employeeId, String certName) {
        LambdaQueryWrapper<HrCertificate> wrapper = new LambdaQueryWrapper<HrCertificate>()
                .eq(employeeId != null, HrCertificate::getEmployeeId, employeeId)
                .like(StringUtils.hasText(certName), HrCertificate::getCertName, certName)
                .orderByDesc(HrCertificate::getIssueDate)
                .orderByDesc(HrCertificate::getId);
        return certificateMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public HrCertificate getById(Long id) {
        HrCertificate entity = certificateMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "证书记录不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CertificateDTO dto) {
        employeeService.assertExists(dto.getEmployeeId());
        assertDateOrder(dto.getIssueDate(), dto.getExpireDate());

        HrCertificate entity = new HrCertificate();
        copy(dto, entity);
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        certificateMapper.insert(entity);
        log.info("新增证书：employeeId={} certName={}（id={}）",
                entity.getEmployeeId(), entity.getCertName(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CertificateDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少证书记录ID");
        }
        getById(dto.getId());
        employeeService.assertExists(dto.getEmployeeId());
        assertDateOrder(dto.getIssueDate(), dto.getExpireDate());

        // 除主键外全部无条件写入：这些列都可空，这样清空才真的生效
        certificateMapper.update(null, new LambdaUpdateWrapper<HrCertificate>()
                .eq(HrCertificate::getId, dto.getId())
                .set(HrCertificate::getEmployeeId, dto.getEmployeeId())
                .set(HrCertificate::getCertName, dto.getCertName())
                .set(HrCertificate::getCertNo, trimToNull(dto.getCertNo()))
                .set(HrCertificate::getIssueOrg, trimToNull(dto.getIssueOrg()))
                .set(HrCertificate::getIssueDate, dto.getIssueDate())
                .set(HrCertificate::getExpireDate, dto.getExpireDate())
                .set(HrCertificate::getFileUrl, trimToNull(dto.getFileUrl())));
        log.info("修改证书：id={} employeeId={} certName={}",
                dto.getId(), dto.getEmployeeId(), dto.getCertName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HrCertificate entity = getById(id);
        certificateMapper.deleteById(id);
        log.info("删除证书：id={} employeeId={}", id, entity.getEmployeeId());
    }

    // ------------------------------------------------------------------ 内部方法

    private void copy(CertificateDTO dto, HrCertificate entity) {
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setCertName(dto.getCertName());
        entity.setCertNo(trimToNull(dto.getCertNo()));
        entity.setIssueOrg(trimToNull(dto.getIssueOrg()));
        entity.setIssueDate(dto.getIssueDate());
        entity.setExpireDate(dto.getExpireDate());
        entity.setFileUrl(trimToNull(dto.getFileUrl()));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void assertDateOrder(LocalDate issueDate, LocalDate expireDate) {
        if (issueDate != null && expireDate != null && expireDate.isBefore(issueDate)) {
            throw new BizException(ResultCode.PARAM_ERROR, "有效期不能早于发证日期");
        }
    }
}
