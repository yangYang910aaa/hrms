package com.yang.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yang.hr.entity.TrnFrontline;

/**
 * 一线工培训期次 Mapper（纯 BaseMapper）
 *
 * 列表要显示课程名/部门名的，前端统一用 /api/common/course-options + /api/common/dept-tree 取，
 * 不写联表 VO（与人事子表用 employee-options 同一口径）。
 */
public interface TrnFrontlineMapper extends BaseMapper<TrnFrontline> {
}
