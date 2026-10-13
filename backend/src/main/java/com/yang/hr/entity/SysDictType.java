package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统-字典类型（sys_dict_type）
 *
 * 注意：本表没有 sort 列（见 db/schema.sql），列表按 id 排序。
 */
@Getter
@Setter
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典名称，如「性别」 */
    private String dictName;

    /** 字典类型标识（唯一，删除时会被改写以防唯一键冲突），如 sex */
    private String dictType;

    /** 状态 1正常 0停用 */
    private Integer status;

    private String remark;
}
