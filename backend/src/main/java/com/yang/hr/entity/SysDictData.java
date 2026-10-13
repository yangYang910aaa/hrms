package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统-字典数据（sys_dict_data）
 *
 * 无唯一索引，属纯子表：删除直接逻辑删除，不需要改写业务键。
 * 通过 dict_type 字符串关联 sys_dict_type.dict_type（不建物理外键）。
 */
@Getter
@Setter
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属字典类型标识，对应 sys_dict_type.dict_type */
    private String dictType;

    /** 字典标签（显示文本） */
    private String dictLabel;

    /** 字典键值（存库值，同一 dict_type 下不允许重复） */
    private String dictValue;

    private Integer sort;

    /** 是否默认 1是 0否（同一 dict_type 下至多一个为 1） */
    private Integer isDefault;

    /** 状态 1正常 0停用 */
    private Integer status;

    private String remark;
}
