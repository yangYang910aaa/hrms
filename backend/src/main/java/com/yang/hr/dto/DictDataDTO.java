package com.yang.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 字典数据入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class DictDataDTO {

    private Long id;

    /** 所属字典类型标识，必须已存在于 sys_dict_type */
    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过 50")
    private String dictType;

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过 50")
    private String dictLabel;

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过 50")
    private String dictValue;

    private Integer sort;

    /** 1 表示设为该类型下的默认项（会取消同类型其他项的默认） */
    private Integer isDefault;

    private Integer status;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;
}
