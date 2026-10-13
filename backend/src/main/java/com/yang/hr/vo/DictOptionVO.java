package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 字典项下拉项（/api/common/dict/{type} 的返回元素）
 *
 * 只暴露前端渲染下拉真正需要的三个字段，不把整行实体（状态、创建人等）泄给业务表单。
 */
@Getter
@Setter
public class DictOptionVO {

    /** 显示文本 */
    private String label;

    /** 提交值 */
    private String value;

    /** 是否默认项 1是 0否 */
    private Integer isDefault;
}
