package com.yang.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.dto.NoticeDTO;
import com.yang.hr.entity.SysNotice;

/**
 * 通知公告服务（公共查询 → 通知管理）。
 */
public interface NoticeService {

    /** 条件分页：标题模糊 / 类型 / 状态 */
    Page<SysNotice> page(long current, long size, String title, Integer noticeType, Integer status);

    SysNotice getById(Long id);

    /** 新增（始终落为「草稿(0)」，发布走 publish） */
    Long create(NoticeDTO dto);

    /** 修改（仅草稿/已下线可改，已发布需先下线） */
    void update(NoticeDTO dto);

    /** 删除（逻辑删除；已发布需先下线） */
    void delete(Long id);

    /** 发布：状态置 1，并写发布人（当前登录用户）与发布时间（服务端时间）。仅草稿/已下线可发布 */
    void publish(Long id);

    /** 下线：状态置 2。仅已发布可下线 */
    void offline(Long id);

    /** 打印计数 +1，返回自增后的次数 */
    int increasePrint(Long id);
}
