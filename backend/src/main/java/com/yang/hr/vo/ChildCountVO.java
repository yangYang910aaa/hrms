package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 子表计数投影（供职工删除保护用）
 *
 * label 直接由 SQL 的字符串常量给出，保证"中文名"与统计口径只有一个来源，
 * 不会出现 Java 侧映射表与 SQL 表名对不上的漂移。
 */
@Getter
@Setter
public class ChildCountVO {

    /** 子表业务名，如「社保记录」 */
    private String label;

    /** 记录数 */
    private Long cnt;
}
