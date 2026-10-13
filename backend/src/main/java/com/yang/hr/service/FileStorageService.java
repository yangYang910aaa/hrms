package com.yang.hr.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务
 *
 * 目前只用于职工照片；证书扫描件（hr_certificate.file_url）、通知附件等后续可直接复用。
 */
public interface FileStorageService {

    /**
     * 保存图片，返回可对外访问的相对 URL（如 `/uploads/202610/9f3a....jpg`）。
     * 调用方负责把这个 URL 存进业务字段（如 hr_employee.photo）。
     */
    String storeImage(MultipartFile file);
}
