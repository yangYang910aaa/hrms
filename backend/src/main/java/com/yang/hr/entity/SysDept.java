package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统-组织结构/部门（sys_dept）
 */
@Getter
@Setter
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父部门ID，0 表示根 */
    private Long parentId;

    private String deptName;

    /** 部门编码（唯一，删除时会被改写以防唯一键冲突） */
    private String deptCode;

    /** 负责人 */
    private String leader;

    private String phone;

    private Integer sort;

    /** 状态 1正常 0停用 */
    private Integer status;
}
