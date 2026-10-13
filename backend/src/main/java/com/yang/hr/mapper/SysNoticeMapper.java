package com.yang.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yang.hr.entity.SysNotice;

/**
 * 通知公告 Mapper（sys_notice）。
 *
 * 增删改查与状态流转都走 BaseMapper + LambdaUpdateWrapper（不写第二个 SQL 入口，
 * 与实际数据一致的单一口径），因此这里只声明泛型。
 */
public interface SysNoticeMapper extends BaseMapper<SysNotice> {
}
