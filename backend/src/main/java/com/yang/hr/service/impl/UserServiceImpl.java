package com.yang.hr.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.DeleteMask;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.UserDTO;
import com.yang.hr.entity.SysRole;
import com.yang.hr.entity.SysUser;
import com.yang.hr.mapper.SysDeptMapper;
import com.yang.hr.mapper.SysRoleMapper;
import com.yang.hr.mapper.SysUserMapper;
import com.yang.hr.mapper.SysUserRoleMapper;
import com.yang.hr.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 用户服务实现
 *
 * 三条"防自锁"铁律（都是真会把自己关在门外的操作）：
 * 1. 不能删除 / 停用自己；
 * 2. 不能删除 / 停用系统中最后一个启用状态的超级管理员；
 * 3. 不能修改自己的角色。
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;

    public UserServiceImpl(SysUserMapper userMapper,
                           SysRoleMapper roleMapper,
                           SysUserRoleMapper userRoleMapper,
                           SysDeptMapper deptMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.deptMapper = deptMapper;
    }

    @Override
    public Page<SysUser> page(long current, long size, String username, String realName, Long deptId, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(username), SysUser::getUsername, username)
                .like(StringUtils.hasText(realName), SysUser::getRealName, realName)
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .eq(status != null, SysUser::getStatus, status)
                .orderByAsc(SysUser::getId);
        return userMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public SysUser getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "用户不存在或已删除");
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserDTO dto) {
        String username = dto.getUsername().trim();
        assertUsernameUnique(username, null);
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BizException(ResultCode.PARAM_ERROR, "新增用户必须设置初始口令");
        }
        assertDeptUsable(dto.getDeptId());
        List<Long> roleIds = distinctIds(dto.getRoleIds());
        assertRolesExist(roleIds);

        SysUser entity = new SysUser();
        entity.setUsername(username);
        entity.setPassword(encode(dto.getPassword()));
        entity.setRealName(dto.getRealName());
        entity.setDeptId(dto.getDeptId());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setAvatar(dto.getAvatar());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        userMapper.insert(entity);

        if (!roleIds.isEmpty()) {
            userRoleMapper.batchInsert(entity.getId(), roleIds);
        }
        log.info("新增用户：{}（id={}），角色 {} 个", username, entity.getId(), roleIds.size());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少用户ID");
        }
        SysUser exist = getById(dto.getId());
        // 登录账号是身份标识，改名等于换账号，直接拒绝而不是静默忽略
        if (!exist.getUsername().equals(dto.getUsername().trim())) {
            throw new BizException(ResultCode.PARAM_ERROR, "登录账号不允许修改");
        }
        assertDeptUsable(dto.getDeptId());

        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, dto.getId())
                .set(SysUser::getRealName, dto.getRealName())
                .set(SysUser::getDeptId, dto.getDeptId())
                .set(SysUser::getPhone, dto.getPhone())
                .set(SysUser::getEmail, dto.getEmail())
                .set(SysUser::getAvatar, dto.getAvatar()));
        // 刻意不在这里改 status：停用需要连带 kickout 和"不能停用自己/最后一个超管"的保护，
        // 一律走 changeStatus()，否则这个通用更新接口就成了绕过保护的后门。
        log.info("修改用户：id={} realName={}", dto.getId(), dto.getRealName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = getById(id);
        assertNotSelf(id, "删除");
        assertNotLastSuperAdmin(id, "删除");

        // 先解除角色关联（物理删除关联表），再改写唯一键、最后逻辑删除主档
        userRoleMapper.deleteByUserId(id);
        rewriteUsername(user);
        userMapper.deleteById(id);
        StpUtil.kickout(id);
        log.info("删除用户：{}（id={}）", user.getUsername(), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new BizException(ResultCode.PARAM_ERROR, "新口令不能为空");
        }
        getById(id);

        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getPassword, encode(newPassword)));
        StpUtil.kickout(id);
        log.info("重置用户口令：id={}（已强制下线）", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态只能是 0(停用) 或 1(正常)");
        }
        SysUser user = getById(id);
        if (status == 0) {
            assertNotSelf(id, "停用");
            assertNotLastSuperAdmin(id, "停用");
        }

        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getStatus, status));
        if (status == 0) {
            // 只挡"下次请求"会有空窗期，这里直接把已签发的会话踢掉
            StpUtil.kickout(id);
        }
        log.info("用户 {} 状态改为 {}", user.getUsername(), status);
    }

    @Override
    public List<Long> roleIds(Long userId) {
        getById(userId);
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        SysUser user = getById(userId);
        assertNotSelf(userId, "修改角色");

        List<Long> distinctIds = distinctIds(roleIds);
        assertRolesExist(distinctIds);

        Set<Long> before = new HashSet<>(userRoleMapper.selectRoleIdsByUserId(userId));
        Set<Long> after = new HashSet<>(distinctIds);
        if (before.equals(after)) {
            log.info("用户 {} 的角色没有变化，跳过写入与下线", user.getUsername());
            return;
        }

        userRoleMapper.deleteByUserId(userId);
        if (!distinctIds.isEmpty()) {
            userRoleMapper.batchInsert(userId, distinctIds);
        }
        // 权限变了，旧会话里的菜单已经过期，必须重新登录
        StpUtil.kickout(userId);
        log.info("用户 {} 分配角色 {} 个（已强制下线以便新权限生效）", user.getUsername(), distinctIds.size());
    }

    // ------------------------------------------------------------------ 内部方法

    @SuppressWarnings("deprecation")   // Sa-Token 的 BCrypt 整类已 @Deprecated 且无替代实现；功能正常，仅消除编辑器告警
    private String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
    }

    private List<Long> distinctIds(List<Long> ids) {
        return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private void assertUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUser::getId, excludeId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "登录账号已存在：" + username);
        }
    }

    /** 部门可以为空；填了就必须存在（用 DeptMapper 直查，避免 Service 之间互相依赖） */
    private void assertDeptUsable(Long deptId) {
        if (deptId == null) {
            return;
        }
        if (deptMapper.selectById(deptId) == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "所选部门不存在或已被删除");
        }
    }

    private void assertRolesExist(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds));
        if (exists != roleIds.size()) {
            throw new BizException(ResultCode.PARAM_ERROR, "提交的角色中包含不存在或已删除的角色");
        }
    }

    private void assertNotSelf(Long id, String action) {
        Long current = SecurityUtils.currentUserIdOrNull();
        if (current != null && current.equals(id)) {
            throw new BizException(ResultCode.FORBIDDEN, "不能对当前登录账号自己执行「" + action + "」");
        }
    }

    private void assertNotLastSuperAdmin(Long id, String action) {
        boolean targetIsSuper = SecurityUtils.isSuperAdmin(userMapper.selectRoleCodesByUserId(id));
        if (!targetIsSuper) {
            return;
        }
        if (userRoleMapper.countEnabledSuperAdmins() <= 1) {
            throw new BizException(ResultCode.FORBIDDEN,
                    "该用户是系统中最后一个启用的超级管理员，" + action + "后将无人能管理系统");
        }
    }

    private void rewriteUsername(SysUser user) {
        String username = user.getUsername();
        if (!StringUtils.hasText(username) || username.contains(DeleteMask.MARK)) {
            return;
        }
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                // 列宽 50：截断后再拼，超长会 Data too long 让删除接口 500
                .set(SysUser::getUsername, DeleteMask.masked(username, user.getId(), 50)));
    }
}
