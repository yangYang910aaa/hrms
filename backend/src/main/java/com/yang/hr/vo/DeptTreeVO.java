package com.yang.hr.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点
 */
@Getter
@Setter
public class DeptTreeVO {

    private Long id;

    private Long parentId;

    private String deptName;

    private String deptCode;

    private String leader;

    private String phone;

    private Integer sort;

    private Integer status;

    private List<DeptTreeVO> children = new ArrayList<>();
}
