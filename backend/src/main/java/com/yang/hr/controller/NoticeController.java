package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.NoticeDTO;
import com.yang.hr.entity.SysNotice;
import com.yang.hr.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知公告接口（公共查询 → 通知管理）。
 *
 * 权限：列表/详情/打印要 query:notice:list；增/改/删/发布-下线分别要
 * query:notice:{add,edit,remove,publish}（迁移脚本 2026-10-13_add_query_perms.sql 补齐）。
 *
 * 设计要点：
 * - 状态只能通过 `/{id}/publish`（发布）、`/{id}/offline`（下线）两个专用接口流转，
 *   通用新增/修改接口刻意不碰 status/publisher/publishTime/printCount（与请假审批同一思路）；
 * - 「打印计数」是一次写操作，但**能查看即能打印**，故与列表共用 query:notice:list，不单开权限点。
 */
@RestController
@RequestMapping("/api/query/notice")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /** 条件分页：标题模糊 / 类型 / 状态 */
    @GetMapping("/page")
    @SaCheckPermission("query:notice:list")
    public R<Page<SysNotice>> page(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size,
                                   @RequestParam(required = false) String title,
                                   @RequestParam(required = false) Integer noticeType,
                                   @RequestParam(required = false) Integer status) {
        return R.ok(noticeService.page(current, size, title, noticeType, status));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("query:notice:list")
    public R<SysNotice> detail(@PathVariable Long id) {
        return R.ok(noticeService.getById(id));
    }

    @PostMapping
    @SaCheckPermission("query:notice:add")
    public R<Long> create(@Valid @RequestBody NoticeDTO dto) {
        return R.ok("新增成功", noticeService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("query:notice:edit")
    public R<Void> update(@Valid @RequestBody NoticeDTO dto) {
        noticeService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("query:notice:remove")
    public R<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return R.ok("删除成功", null);
    }

    /** 发布（草稿/已下线 → 已发布，写发布人与发布时间） */
    @PutMapping("/{id}/publish")
    @SaCheckPermission("query:notice:publish")
    public R<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return R.ok("发布成功", null);
    }

    /** 下线（已发布 → 已下线） */
    @PutMapping("/{id}/offline")
    @SaCheckPermission("query:notice:publish")
    public R<Void> offline(@PathVariable Long id) {
        noticeService.offline(id);
        return R.ok("下线成功", null);
    }

    /** 打印计数 +1，返回自增后的次数（能查看即能打印，与列表同权限） */
    @PutMapping("/{id}/print")
    @SaCheckPermission("query:notice:list")
    public R<Integer> print(@PathVariable Long id) {
        return R.ok("打印次数已更新", noticeService.increasePrint(id));
    }
}
