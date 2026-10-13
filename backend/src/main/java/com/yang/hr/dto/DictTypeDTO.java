package com.yang.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 字典类型入参（新增与修改共用；修改时 id 必填，由 Service 校验）
 */
@Getter
@Setter
public class DictTypeDTO {

    private Long id;

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过 50")
    private String dictName;

    /**
     * 字典类型标识，如 sex。
     * 新增后不允许修改（它是字典数据与前端代码共同的引用键，改掉会孤儿化已有数据项）。
     */
    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过 50")
    private String dictType;

    private Integer status;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;
}
