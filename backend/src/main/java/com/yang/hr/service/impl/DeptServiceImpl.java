package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.DeleteMask;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.DeptDTO;
import com.yang.hr.entity.SysDept;
import com.yang.hr.entity.SysUser;
import com.yang.hr.mapper.SysDeptMapper;
import com.yang.hr.mapper.SysUserMapper;
import com.yang.hr.service.DeptService;
import com.yang.hr.vo.DeptTreeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 组织结构（部门）服务实现
 */
@Service
public class DeptServiceImpl implements DeptService {

    private static final Logger log = LoggerFactory.getLogger(DeptServiceImpl.class);

    private static final long ROOT_ID = 0L;
    /** 逻辑删除时给唯一业务键加的后缀标记，避免二次录入撞唯一索引（规划书 §9 第 16 条） */
    private static final int MAX_DEPTH = 50;

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    public DeptServiceImpl(SysDeptMapper deptMapper, SysUserMapper userMapper) {
        this.deptMapper = deptMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Page<SysDept> page(long current, long size, String deptName, Integer status) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<SysDept>()
                .like(StringUtils.hasText(deptName), SysDept::getDeptName, deptName)
                .eq(status != null, SysDept::getStatus, status)
                .orderByAsc(SysDept::getSort)
                .orderByAsc(SysDept::getId);
        return deptMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public List<DeptTreeVO> tree() {
        List<SysDept> all = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSort)
                .orderByAsc(SysDept::getId));
        return buildTree(all);
    }

    @Override
    public List<DeptTreeVO> enabledTree() {
        List<SysDept> all = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, 1)
                .orderByAsc(SysDept::getSort)
                .orderByAsc(SysDept::getId));
        return buildTree(all);
    }

    /** 父子列表 → 树；父节点不在结果里（被删/被停用过滤掉）时上提为根，避免整棵子树消失 */
    private List<DeptTreeVO> buildTree(List<SysDept> all) {
        Map<Long, DeptTreeVO> byId = new LinkedHashMap<>();
        for (SysDept dept : all) {
            byId.put(dept.getId(), toVo(dept));
        }

        List<DeptTreeVO> roots = new ArrayList<>();
        for (SysDept dept : all) {
            DeptTreeVO node = byId.get(dept.getId());
            DeptTreeVO parent = dept.getParentId() == null ? null : byId.get(dept.getParentId());
            if (parent == null || parent == node) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    @Override
    public SysDept getById(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "部门不存在或已删除");
        }
        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DeptDTO dto) {
        assertParentExists(dto.getParentId());
        assertDeptCodeUnique(dto.getDeptCode(), null);

        SysDept entity = new SysDept();
        copy(dto, entity);
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        deptMapper.insert(entity);
        log.info("新增部门：{}（id={}）", entity.getDeptName(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DeptDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少部门ID");
        }
        getById(dto.getId());

        assertParentExists(dto.getParentId());
        assertNoCycle(dto.getId(), dto.getParentId());
        assertDeptCodeUnique(dto.getDeptCode(), dto.getId());

        // 用显式 set 而不是 updateById：
        // ① deptCode/leader/phone 一律写入，这样把负责人清空才真的能清空（updateById 会跳过 null）；
        // ② sort/status 用条件 set，前端没传就保持原值，不会被默认值悄悄覆盖。
        deptMapper.update(null, new LambdaUpdateWrapper<SysDept>()
                .eq(SysDept::getId, dto.getId())
                .set(SysDept::getParentId, dto.getParentId())
                .set(SysDept::getDeptName, dto.getDeptName())
                .set(SysDept::getDeptCode, trimToNull(dto.getDeptCode()))
                .set(SysDept::getLeader, dto.getLeader())
                .set(SysDept::getPhone, dto.getPhone())
                .set(dto.getSort() != null, SysDept::getSort, dto.getSort())
                .set(dto.getStatus() != null, SysDept::getStatus, dto.getStatus()));
        log.info("修改部门：id={} name={}", dto.getId(), dto.getDeptName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDept dept = getById(id);

        long children = deptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (children > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "该部门下还有 " + children + " 个子部门，请先删除子部门");
        }

        long users = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (users > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "该部门下还有 " + users + " 个用户，请先调整归属");
        }

        long employees = deptMapper.countEmployees(id);
        if (employees > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "该部门下还有 " + employees + " 名职工，请先调整归属");
        }

        rewriteDeptCode(dept);
        deptMapper.deleteById(id);
        log.info("删除部门：{}（id={}）", dept.getDeptName(), id);
    }

    // ------------------------------------------------------------------ 内部方法

    /** 仅用于「新增」：给可空字段兜默认值 */
    private void copy(DeptDTO dto, SysDept entity) {
        entity.setParentId(dto.getParentId());
        entity.setDeptName(dto.getDeptName());
        entity.setDeptCode(trimToNull(dto.getDeptCode()));
        entity.setLeader(dto.getLeader());
        entity.setPhone(dto.getPhone());
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void assertParentExists(Long parentId) {
        if (parentId == null || parentId == ROOT_ID) {
            return;
        }
        if (deptMapper.selectById(parentId) == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "上级部门不存在或已被删除");
        }
    }

    /** 顺着 parentId 往上走，若走回自己说明要形成环 */
    private void assertNoCycle(Long deptId, Long parentId) {
        Long cursor = parentId;
        int depth = 0;
        while (cursor != null && cursor != ROOT_ID) {
            if (cursor.equals(deptId)) {
                throw new BizException(ResultCode.PARAM_ERROR, "不能把部门挂到自己的下级部门下");
            }
            SysDept parent = deptMapper.selectById(cursor);
            cursor = parent == null ? null : parent.getParentId();
            if (++depth > MAX_DEPTH) {
                throw new BizException(ResultCode.PARAM_ERROR, "部门层级超过 " + MAX_DEPTH + " 层或存在循环引用，请检查数据");
            }
        }
    }

    private void assertDeptCodeUnique(String deptCode, Long excludeId) {
        if (!StringUtils.hasText(deptCode)) {
            return;
        }
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptCode, deptCode.trim());
        if (excludeId != null) {
            wrapper.ne(SysDept::getId, excludeId);
        }
        if (deptMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "部门编码已存在：" + deptCode);
        }
    }

    /** 逻辑删除前先给唯一键让路，否则以后再建同名编码会撞唯一索引 */
    private void rewriteDeptCode(SysDept dept) {
        String code = dept.getDeptCode();
        if (!StringUtils.hasText(code) || code.contains(DeleteMask.MARK)) {
            return;
        }
        deptMapper.update(null, new LambdaUpdateWrapper<SysDept>()
                .eq(SysDept::getId, dept.getId())
                // 列宽 50：截断后再拼，超长会 Data too long 让删除接口 500
                .set(SysDept::getDeptCode, DeleteMask.masked(code, dept.getId(), 50)));
    }

    private DeptTreeVO toVo(SysDept dept) {
        DeptTreeVO vo = new DeptTreeVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setDeptName(dept.getDeptName());
        vo.setDeptCode(dept.getDeptCode());
        vo.setLeader(dept.getLeader());
        vo.setPhone(dept.getPhone());
        vo.setSort(dept.getSort());
        vo.setStatus(dept.getStatus());
        return vo;
    }
}
