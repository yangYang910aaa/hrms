package com.yang.hr.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.DeleteMask;
import com.yang.hr.common.MenuTreeBuilder;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.MenuDTO;
import com.yang.hr.entity.SysMenu;
import com.yang.hr.mapper.SysMenuMapper;
import com.yang.hr.mapper.SysRoleMenuMapper;
import com.yang.hr.mapper.SysUserRoleMapper;
import com.yang.hr.service.MenuService;
import com.yang.hr.vo.MenuTreeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 菜单/权限点服务实现
 */
@Service
public class MenuServiceImpl implements MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuServiceImpl.class);

    private static final long ROOT_ID = 0L;
    private static final String TYPE_DIR = "M";
    private static final String TYPE_MENU = "C";
    private static final int MAX_DEPTH = 50;

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public MenuServiceImpl(SysMenuMapper menuMapper,
                           SysRoleMenuMapper roleMenuMapper,
                           SysUserRoleMapper userRoleMapper) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public Page<SysMenu> page(long current, long size, String menuName, String menuType, Integer status) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .like(StringUtils.hasText(menuName), SysMenu::getMenuName, menuName)
                .eq(StringUtils.hasText(menuType), SysMenu::getMenuType, menuType)
                .eq(status != null, SysMenu::getStatus, status)
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId);
        return menuMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public List<MenuTreeVO> tree() {
        return MenuTreeBuilder.buildFull(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId)));
    }

    @Override
    public SysMenu getById(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "菜单不存在或已删除");
        }
        return menu;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MenuDTO dto) {
        assertParentAllows(dto.getParentId(), dto.getMenuType());
        assertPermsUnique(dto.getPerms(), null);

        SysMenu entity = new SysMenu();
        copy(dto, entity);
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        menuMapper.insert(entity);
        log.info("新增菜单：{}（{}，id={}）", entity.getMenuName(), entity.getMenuType(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MenuDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少菜单ID");
        }
        getById(dto.getId());
        if (dto.getId().equals(dto.getParentId())) {
            throw new BizException(ResultCode.PARAM_ERROR, "上级菜单不能是自己");
        }
        assertParentAllows(dto.getParentId(), dto.getMenuType());
        assertNoCycle(dto.getId(), dto.getParentId());
        assertPermsUnique(dto.getPerms(), dto.getId());

        // 同 DeptServiceImpl.update：可空文本字段一律写入（支持清空），
        // sort/visible/status 条件写入（前端没传就保持原值，避免把隐藏菜单重置成显示）。
        menuMapper.update(null, new LambdaUpdateWrapper<SysMenu>()
                .eq(SysMenu::getId, dto.getId())
                .set(SysMenu::getParentId, dto.getParentId())
                .set(SysMenu::getMenuName, dto.getMenuName())
                .set(SysMenu::getMenuType, dto.getMenuType())
                .set(SysMenu::getPath, dto.getPath())
                .set(SysMenu::getComponent, dto.getComponent())
                .set(SysMenu::getPerms, trimToNull(dto.getPerms()))
                .set(SysMenu::getIcon, dto.getIcon())
                .set(dto.getSort() != null, SysMenu::getSort, dto.getSort())
                .set(dto.getVisible() != null, SysMenu::getVisible, dto.getVisible())
                .set(dto.getStatus() != null, SysMenu::getStatus, dto.getStatus()));
        log.info("修改菜单：id={} name={}", dto.getId(), dto.getMenuName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysMenu menu = getById(id);

        long children = menuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (children > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "该节点下还有 " + children + " 个子节点，请先删除子节点");
        }

        // 删菜单同样让各角色授权静默变化：服务端鉴权实时生效（用户会突然 403），
        // 但已登录用户的前端菜单缓存不刷新。与 assignMenus 同一处置：
        // 先取受影响的角色，删完授权后把这些人踢下线（排除操作者自己）。
        List<Long> affectedRoleIds = roleMenuMapper.selectRoleIdsByMenuId(id);
        int unlinked = roleMenuMapper.deleteByMenuId(id);
        rewritePerms(menu);
        menuMapper.deleteById(id);
        int kicked = kickoutAffectedUsers(affectedRoleIds);
        log.info("删除菜单：{}（id={}），同时解除 {} 条角色授权，下线 {} 名受影响用户",
                menu.getMenuName(), id, unlinked, kicked);
    }

    // ------------------------------------------------------------------ 内部方法

    /**
     * 逻辑删除前给 perms 唯一键让路（2026-10-13 复查加）：改写为「截断原值 + #del# + id」。
     * perms 是鉴权的键，软删行仍占唯一索引，不让路则删除后同 perms 不能重新建档。
     * 目录（perms 为空）无需让路 —— NULL 不参与唯一键冲突。
     */
    private void rewritePerms(SysMenu menu) {
        String perms = menu.getPerms();
        if (!StringUtils.hasText(perms) || perms.contains(DeleteMask.MARK)) {
            return;
        }
        menuMapper.update(null, new LambdaUpdateWrapper<SysMenu>()
                .eq(SysMenu::getId, menu.getId())
                // 列宽 100：截断后再拼，超长会 Data too long 让删除接口 500
                .set(SysMenu::getPerms, DeleteMask.masked(perms, menu.getId(), 100)));
    }

    /** 踢掉受影响角色下已登录的用户（排除当前操作者），返回下线人数 */
    private int kickoutAffectedUsers(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return 0;
        }
        Long currentUserId = SecurityUtils.currentUserIdOrNull();
        int kicked = 0;
        for (Long roleId : roleIds) {
            for (Long userId : userRoleMapper.selectUserIdsByRoleId(roleId)) {
                if (userId.equals(currentUserId)) {
                    continue;
                }
                StpUtil.kickout(userId);
                kicked++;
            }
        }
        return kicked;
    }

    /** 仅用于「新增」：给可空字段兜默认值 */
    private void copy(MenuDTO dto, SysMenu entity) {
        entity.setParentId(dto.getParentId());
        entity.setMenuName(dto.getMenuName());
        entity.setMenuType(dto.getMenuType());
        entity.setPath(dto.getPath());
        entity.setComponent(dto.getComponent());
        entity.setPerms(trimToNull(dto.getPerms()));
        entity.setIcon(dto.getIcon());
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        entity.setVisible(dto.getVisible() == null ? 1 : dto.getVisible());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 层级规则：目录下可挂目录/菜单/按钮；菜单下只能挂按钮；按钮不能有子节点。
     * 不拦的话会做出"目录挂在按钮下"这种前端渲染不了的脏结构。
     */
    private void assertParentAllows(Long parentId, String childType) {
        if (parentId == null || parentId == ROOT_ID) {
            return;
        }
        SysMenu parent = menuMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "上级菜单不存在或已被删除");
        }
        switch (parent.getMenuType()) {
            case TYPE_DIR -> {
                // 目录：任意类型都可以挂
            }
            case TYPE_MENU -> {
                if (TYPE_DIR.equals(childType) || TYPE_MENU.equals(childType)) {
                    throw new BizException(ResultCode.PARAM_ERROR, "菜单下只能挂按钮权限点");
                }
            }
            default -> throw new BizException(ResultCode.PARAM_ERROR, "按钮权限点下不能再挂子节点");
        }
    }

    private void assertNoCycle(Long menuId, Long parentId) {
        Long cursor = parentId;
        int depth = 0;
        while (cursor != null && cursor != ROOT_ID) {
            if (cursor.equals(menuId)) {
                throw new BizException(ResultCode.PARAM_ERROR, "不能把菜单挂到自己的下级节点下");
            }
            SysMenu parent = menuMapper.selectById(cursor);
            cursor = parent == null ? null : parent.getParentId();
            if (++depth > MAX_DEPTH) {
                throw new BizException(ResultCode.PARAM_ERROR, "菜单层级超过 " + MAX_DEPTH + " 层或存在循环引用，请检查数据");
            }
        }
    }

    private void assertPermsUnique(String perms, Long excludeId) {
        if (!StringUtils.hasText(perms)) {
            return;
        }
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPerms, perms.trim());
        if (excludeId != null) {
            wrapper.ne(SysMenu::getId, excludeId);
        }
        if (menuMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "权限标识已存在：" + perms);
        }
    }
}
