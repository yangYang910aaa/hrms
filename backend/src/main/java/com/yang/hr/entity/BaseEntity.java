package com.yang.hr.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公共字段基类：所有业务表都有这 4 列，抽出来避免每个实体重复声明。
 *
 * - create_time / update_time 由数据库默认值维护（MyBatis-Plus 默认只插入非 null 字段，
 *   这两个字段留 null 时不会进 SQL，交给 MySQL 的 DEFAULT CURRENT_TIMESTAMP / ON UPDATE 处理）；
 * - create_by 由 Service 层写入当前登录用户；
 * - deleted 是逻辑删除标记，由 application.yml 的全局配置驱动，业务代码不要手动赋值。
 */
@Getter
@Setter
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    /** 逻辑删除标记：内部字段，不外泄给前端（@JsonIgnore，否则会随分页结果透出去） */
    @JsonIgnore
    private Integer deleted;
}
