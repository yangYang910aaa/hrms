package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.CertificateDTO;
import com.yang.hr.entity.HrCertificate;

/**
 * 证书管理服务
 */
public interface CertificateService {

    Page<HrCertificate> page(long current, long size, Long employeeId, String certName);

    HrCertificate getById(Long id);

    Long create(CertificateDTO dto);

    void update(CertificateDTO dto);

    void delete(Long id);
}
