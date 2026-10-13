package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.NoticeDTO;
import com.yang.hr.entity.SysNotice;
import com.yang.hr.entity.SysUser;
import com.yang.hr.mapper.SysNoticeMapper;
import com.yang.hr.mapper.SysUserMapper;
import com.yang.hr.service.NoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 通知公告服务实现（公共查询 → 通知管理）。
 *
 * 状态机：0 草稿 --发布--> 1 已发布 --下线--> 2 已下线 --发布--> 1 已发布
 * 两条硬约束：**已发布不可直接改/删**（先下线）、**状态只能走专用接口**（通用 update 不碰 status）。
 */
@Service
public class NoticeServiceImpl implements NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeServiceImpl.class);

    /** 状态：0草稿 1已发布 2已下线 */
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_OFFLINE = 2;

    private final SysNoticeMapper noticeMapper;
    private final SysUserMapper userMapper;

    public NoticeServiceImpl(SysNoticeMapper noticeMapper, SysUserMapper userMapper) {
        this.noticeMapper = noticeMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Page<SysNotice> page(long current, long size, String title, Integer noticeType, Integer status) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<SysNotice>()
                .like(StringUtils.hasText(title), SysNotice::getTitle, trim(title))
                .eq(noticeType != null, SysNotice::getNoticeType, noticeType)
                .eq(status != null, SysNotice::getStatus, status)
                .orderByDesc(SysNotice::getId);
        return noticeMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public SysNotice getById(Long id) {
        SysNotice entity = noticeMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "通知不存在或已删除");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(NoticeDTO dto) {
        SysNotice entity = new SysNotice();
        entity.setTitle(dto.getTitle().trim());
        entity.setNoticeType(dto.getNoticeType());
        entity.setContent(dto.getContent());
        entity.setStatus(STATUS_DRAFT);
        entity.setPrintCount(0);
        entity.setRemark(dto.getRemark());
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        noticeMapper.insert(entity);
        log.info("新增通知：{}（id={}，草稿）", entity.getTitle(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(NoticeDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少通知ID");
        }
        SysNotice exist = getById(dto.getId());
        if (exist.getStatus() != null && exist.getStatus() == STATUS_PUBLISHED) {
            throw new BizException(ResultCode.PARAM_ERROR, "该通知已发布，请先下线再修改");
        }

        // 显式 set：可空文本无条件写入（清空才生效）；status/publisher/publishTime/printCount 一律不碰
        noticeMapper.update(null, new LambdaUpdateWrapper<SysNotice>()
                .eq(SysNotice::getId, dto.getId())
                .set(SysNotice::getTitle, dto.getTitle().trim())
                .set(SysNotice::getNoticeType, dto.getNoticeType())
                .set(SysNotice::getContent, dto.getContent())
                .set(SysNotice::getRemark, dto.getRemark()));
        log.info("修改通知：id={} {}", dto.getId(), dto.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysNotice exist = getById(id);
        if (exist.getStatus() != null && exist.getStatus() == STATUS_PUBLISHED) {
            throw new BizException(ResultCode.PARAM_ERROR, "该通知已发布，请先下线再删除");
        }
        noticeMapper.deleteById(id);
        log.info("删除通知：id={} {}", id, exist.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        SysNotice exist = getById(id);
        if (exist.getStatus() != null && exist.getStatus() == STATUS_PUBLISHED) {
            throw new BizException(ResultCode.PARAM_ERROR, "该通知已是「已发布」，无需重复发布");
        }

        // 发布人与发布时间取服务端（当前登录用户真实姓名 + 服务端时间），不接受前端传入
        Long userId = SecurityUtils.currentUserId();
        SysUser user = userMapper.selectById(userId);
        String publisher = (user != null && StringUtils.hasText(user.getRealName()))
                ? user.getRealName() : ("用户" + userId);

        noticeMapper.update(null, new LambdaUpdateWrapper<SysNotice>()
                .eq(SysNotice::getId, id)
                .set(SysNotice::getStatus, STATUS_PUBLISHED)
                .set(SysNotice::getPublisher, publisher)
                .set(SysNotice::getPublishTime, LocalDateTime.now()));
        log.info("发布通知：id={} {} publisher={}", id, exist.getTitle(), publisher);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        SysNotice exist = getById(id);
        if (exist.getStatus() == null || exist.getStatus() != STATUS_PUBLISHED) {
            throw new BizException(ResultCode.PARAM_ERROR, "只有「已发布」的通知可以下线");
        }
        noticeMapper.update(null, new LambdaUpdateWrapper<SysNotice>()
                .eq(SysNotice::getId, id)
                .set(SysNotice::getStatus, STATUS_OFFLINE));
        log.info("下线通知：id={} {}", id, exist.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int increasePrint(Long id) {
        getById(id);
        // 用 setSql 自增，避免"读-改-写"丢更新；自增完再读回真实值返回
        noticeMapper.update(null, new LambdaUpdateWrapper<SysNotice>()
                .eq(SysNotice::getId, id)
                .setSql("print_count = print_count + 1"));
        SysNotice fresh = noticeMapper.selectById(id);
        int count = fresh == null || fresh.getPrintCount() == null ? 0 : fresh.getPrintCount();
        log.info("通知打印：id={} printCount={}", id, count);
        return count;
    }

    // ------------------------------------------------------------------ 内部方法

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
