package com.yang.hr.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.DeleteMask;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.RoleDTO;
import com.yang.hr.entity.SysMenu;
import com.yang.hr.entity.SysRole;
import com.yang.hr.mapper.SysMenuMapper;
import com.yang.hr.mapper.SysRoleMapper;
import com.yang.hr.mapper.SysRoleMenuMapper;
import com.yang.hr.mapper.SysUserRoleMapper;
import com.yang.hr.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;

/**
 * 角色服务实现
 */
@Service
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public RoleServiceImpl(SysRoleMapper roleMapper,
                           SysMenuMapper menuMapper,
                           SysRoleMenuMapper roleMenuMapper,
                           SysUserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public Page<SysRole> page(long current, long size, String roleName, Integer status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName)
                .eq(status != null, SysRole::getStatus, status)
                .orderByAsc(SysRole::getSort)
                .orderByAsc(SysRole::getId);
        return roleMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public List<SysRole> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort)
                .orderByAsc(SysRole::getId));
    }

    @Override
    public SysRole getById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "角色不存在或已删除");
        }
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleDTO dto) {
        assertRoleCodeUnique(dto.getRoleCode(), null);

        SysRole entity = new SysRole();
        copyForCreate(dto, entity);
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        roleMapper.insert(entity);
        log.info("新增角色：{}（{}，id={}）", entity.getRoleName(), entity.getRoleCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RoleDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少角色ID");
        }
        SysRole exist = getById(dto.getId());
        // 超管标识被改掉会让整套权限判定失效，直接锁死
        if (SecurityUtils.ROLE_SUPER_ADMIN.equals(exist.getRoleCode())
                && !SecurityUtils.ROLE_SUPER_ADMIN.equals(dto.getRoleCode())) {
            throw new BizException(ResultCode.FORBIDDEN, "内置超级管理员角色的标识不允许修改");
        }
        // 与 delete 的保护对称：内置超管角色也不允许停用 ——
        // 停用后 selectRoleCodesByUserId 的 status=1 过滤命中不到它，
        // isSuperAdmin 判定与 StpInterfaceImpl 的 "*" 通道会同时失效
        if (SecurityUtils.ROLE_SUPER_ADMIN.equals(exist.getRoleCode())
                && dto.getStatus() != null && dto.getStatus() == 0) {
            throw new BizException(ResultCode.FORBIDDEN, "内置超级管理员角色不允许停用");
        }
        assertRoleCodeUnique(dto.getRoleCode(), dto.getId());

        roleMapper.update(null, new LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getId, dto.getId())
                .set(SysRole::getRoleName, dto.getRoleName())
                .set(SysRole::getRoleCode, dto.getRoleCode().trim())
                .set(SysRole::getRemark, dto.getRemark())
                .set(dto.getSort() != null, SysRole::getSort, dto.getSort())
                .set(dto.getStatus() != null, SysRole::getStatus, dto.getStatus()));
        log.info("修改角色：id={} name={}", dto.getId(), dto.getRoleName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = getById(id);
        if (SecurityUtils.ROLE_SUPER_ADMIN.equals(role.getRoleCode())) {
            throw new BizException(ResultCode.FORBIDDEN, "内置超级管理员角色不允许删除");
        }
        long users = userRoleMapper.countByRoleId(id);
        if (users > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "还有 " + users + " 个用户使用该角色，请先解除关联");
        }

        int unlinked = roleMenuMapper.deleteByRoleId(id);
        rewriteRoleCode(role);
        roleMapper.deleteById(id);
        log.info("删除角色：{}（id={}），同时解除 {} 条菜单授权", role.getRoleName(), id, unlinked);
    }

    @Override
    public List<Long> menuIds(Long roleId) {
        getById(roleId);
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = getById(roleId);
        List<Long> distinctIds = menuIds == null ? List.of()
                : menuIds.stream().filter(java.util.Objects::nonNull).distinct().toList();

        if (!distinctIds.isEmpty()) {
            long exists = menuMapper.selectCount(
                    new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, distinctIds));
            if (exists != distinctIds.size()) {
                throw new BizException(ResultCode.PARAM_ERROR, "提交的菜单中包含不存在或已删除的节点");
            }
        }

        if (new HashSet<>(roleMenuMapper.selectMenuIdsByRoleId(roleId)).equals(new HashSet<>(distinctIds))) {
            log.info("角色 {} 的菜单授权没有变化，跳过写入", role.getRoleName());
            return;
        }

        roleMenuMapper.deleteByRoleId(roleId);
        if (!distinctIds.isEmpty()) {
            roleMenuMapper.batchInsert(roleId, distinctIds);
        }

        // 服务端权限是每次请求实时算的，但用户前端缓存的菜单树会过期 ——
        // 把该角色下已登录的人踢下线，让他们重新登录拉新菜单。
        // 排除当前操作者：管理员改自己所属角色时不该把自己踢出去（其服务端权限本来就是实时生效的）。
        Long currentUserId = SecurityUtils.currentUserIdOrNull();
        int kicked = 0;
        for (Long userId : userRoleMapper.selectUserIdsByRoleId(roleId)) {
            if (userId.equals(currentUserId)) {
                continue;
            }
            StpUtil.kickout(userId);
            kicked++;
        }
        log.info("角色 {} 分配了 {} 个菜单，已下线 {} 名受影响用户", role.getRoleName(), distinctIds.size(), kicked);
    }

    // ------------------------------------------------------------------ 内部方法

    private void copyForCreate(RoleDTO dto, SysRole entity) {
        entity.setRoleName(dto.getRoleName());
        entity.setRoleCode(dto.getRoleCode().trim());
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark());
    }

    private void assertRoleCodeUnique(String roleCode, Long excludeId) {
        if (!StringUtils.hasText(roleCode)) {
            return;
        }
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode.trim());
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "角色标识已存在：" + roleCode);
        }
    }

    private void rewriteRoleCode(SysRole role) {
        String code = role.getRoleCode();
        if (!StringUtils.hasText(code) || code.contains(DeleteMask.MARK)) {
            return;
        }
        roleMapper.update(null, new LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getId, role.getId())
                // 按列宽 50 截断后再拼，超长会 Data too long 让删除接口 500
                .set(SysRole::getRoleCode, DeleteMask.masked(code, role.getId(), 50)));
    }
}
