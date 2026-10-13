package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 系统-通知公告（sys_notice）
 *
 * 这是 28 张表里第 10 张，M1 建表时就在，但直到 M6 批次5 才由「公共查询 → 通知管理」使用。
 *
 * 字段口径：
 * - notice_type 1通知 2公告（不在 sys_dict 里，前端硬编码）；
 * - status 0草稿 1已发布 2已下线（状态机：0→发布→1→下线→2→再发布→1）；
 * - publisher / publish_time 由服务端在「发布」时写入，**不接受前端传入**（避免伪造发布人/时间）；
 * - print_count 通知打印次数，由「打印」接口自增（课程要求的形式妥协，不做服务端打印）。
 */
@Getter
@Setter
@TableName("sys_notice")
public class SysNotice extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 类型 1通知 2公告 */
    private Integer noticeType;

    /** 正文 */
    private String content;

    /** 发布人（发布时由服务端写当前登录用户真实姓名） */
    private String publisher;

    /** 发布时间（发布时由服务端写） */
    private LocalDateTime publishTime;

    /** 状态 0草稿 1已发布 2已下线 */
    private Integer status;

    /** 打印次数 */
    private Integer printCount;

    private String remark;
}
