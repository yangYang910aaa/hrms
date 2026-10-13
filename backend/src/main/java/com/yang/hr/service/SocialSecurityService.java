package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.SocialSecurityDTO;
import com.yang.hr.entity.HrSocialSecurity;

/**
 * 职工社保服务
 */
public interface SocialSecurityService {

    Page<HrSocialSecurity> page(long current, long size, Long employeeId, String month);

    HrSocialSecurity getById(Long id);

    Long create(SocialSecurityDTO dto);

    void update(SocialSecurityDTO dto);

    void delete(Long id);
}
