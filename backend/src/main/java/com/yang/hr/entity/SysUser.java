package com.yang.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 系统-用户（sys_user）
 *
 * 公共字段（create_time/update_time/create_by/deleted）继承自 BaseEntity；
 * 逻辑删除由 application.yml 的全局配置驱动，实体上不再重复加 @TableLogic。
 *
 * 用 @Getter/@Setter 而不用 @Data：避免自动生成 toString() 把口令哈希打进日志。
 */
@Getter
@Setter
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 口令（BCrypt $2a$ 哈希） */
    private String password;

    /** 姓名 */
    private String realName;

    /** 所属部门 */
    private Long deptId;

    private String phone;

    private String email;

    private String avatar;

    /** 状态 1正常 0停用 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 口令哈希绝不对外序列化；手写 getter 后 Lombok 不会再生成同名方法 */
    @JsonIgnore
    public String getPassword() {
        return password;
    }
}
