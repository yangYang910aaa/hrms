package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 人事-证书管理（hr_certificate）
 *
 * 无唯一业务键：删除直接逻辑删除。
 */
@Getter
@Setter
@TableName("hr_certificate")
public class HrCertificate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    /** 证书名称 */
    private String certName;

    /** 证书编号 */
    private String certNo;

    /** 发证机构 */
    private String issueOrg;

    /** 发证日期 */
    private LocalDate issueDate;

    /** 有效期至 */
    private LocalDate expireDate;

    /** 扫描件地址 */
    private String fileUrl;
}
