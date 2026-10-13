package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统-角色（sys_role）
 */
@Getter
@Setter
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleName;

    /** 角色标识（唯一，删除时会被改写以防唯一键冲突） */
    private String roleCode;

    private Integer sort;

    /** 状态 1正常 0停用 */
    private Integer status;

    private String remark;
}
