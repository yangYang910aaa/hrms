package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.BizException;
import com.yang.hr.common.DeleteMask;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.EmployeeDTO;
import com.yang.hr.entity.HrEmployee;
import com.yang.hr.mapper.HrEmployeeMapper;
import com.yang.hr.mapper.SysDeptMapper;
import com.yang.hr.service.EmployeeService;
import com.yang.hr.service.SalLevelService;
import com.yang.hr.vo.EmployeeOptionVO;
import com.yang.hr.vo.EmployeeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 职工主档服务实现
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    /** 逻辑删除时给唯一业务键加的后缀标记，避免二次录入撞唯一索引（规划书 §9 第 16 条） */
    /** 身份证：18 位，前 17 位数字，末位数字或 X */
    private static final Pattern ID_CARD = Pattern.compile("^\\d{17}[\\dXx]$");

    /** 身份证内嵌出生日期的解析器：STRICT 让 1990-02-30 这类假日期直接报错，而不是被 SMART 调整成 02-28 */
    private static final DateTimeFormatter ID_BIRTH =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private static final int GENDER_MALE = 1;
    private static final int GENDER_FEMALE = 2;
    private static final int EMPLOY_ON = 1;
    private static final int EMPLOY_LEFT = 3;

    private final HrEmployeeMapper employeeMapper;
    private final SysDeptMapper deptMapper;
    private final SalLevelService salLevelService;

    public EmployeeServiceImpl(HrEmployeeMapper employeeMapper, SysDeptMapper deptMapper,
                               SalLevelService salLevelService) {
        this.employeeMapper = employeeMapper;
        this.deptMapper = deptMapper;
        this.salLevelService = salLevelService;
    }

    @Override
    public Page<EmployeeVO> page(long current, long size, String name, Long deptId,
                                 Integer employType, Integer status) {
        Page<EmployeeVO> page = new Page<>(current, size);
        // 分页插件会把 records/total 回填到传入的 page 对象上，无需再转换
        employeeMapper.selectVoPage(page, StringUtils.hasText(name) ? name.trim() : null,
                deptId, employType, status);
        fillAge(page.getRecords());
        return page;
    }

    @Override
    public List<EmployeeVO> birthdays(Integer month) {
        int target = month == null ? LocalDate.now().getMonthValue() : month;
        if (target < 1 || target > 12) {
            throw new BizException(ResultCode.PARAM_ERROR, "月份必须在 1~12 之间");
        }
        List<EmployeeVO> list = employeeMapper.selectBirthdays(target);
        fillAge(list);
        return list;
    }

    @Override
    public EmployeeVO getById(Long id) {
        EmployeeVO vo = employeeMapper.selectVoById(id);
        if (vo == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "职工不存在或已删除");
        }
        // 详情也要带 age，否则同一个 VO 在列表里有值、详情里是 null，前端会踩坑
        vo.setAge(ageOf(vo.getBirthDate()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(EmployeeDTO dto) {
        assertEmpNoUnique(dto.getEmpNo(), null);
        assertIdCardMatchesBirth(dto.getIdCard(), dto.getBirthDate());
        assertGender(dto.getGender());
        assertEmployType(dto.getEmployType());
        assertDeptExists(dto.getDeptId());
        assertSalaryLevelExists(dto.getSalaryLevelId());

        int employType = dto.getEmployType() == null ? EMPLOY_ON : dto.getEmployType();
        LocalDate leaveDate = resolveLeaveDate(employType, dto.getLeaveDate(), null);
        assertDateOrder(dto.getHireDate(), leaveDate);

        HrEmployee entity = new HrEmployee();
        entity.setEmpNo(dto.getEmpNo().trim());
        entity.setName(dto.getName());
        entity.setGender(dto.getGender());
        entity.setBirthDate(dto.getBirthDate());
        entity.setIdCard(trimToNull(dto.getIdCard()));
        entity.setPhone(trimToNull(dto.getPhone()));
        entity.setEmail(trimToNull(dto.getEmail()));
        entity.setAddress(dto.getAddress());
        entity.setEduLevel(trimToNull(dto.getEduLevel()));
        entity.setDeptId(dto.getDeptId());
        entity.setPosition(dto.getPosition());
        entity.setHireDate(dto.getHireDate());
        entity.setLeaveDate(leaveDate);
        entity.setEmployType(employType);
        entity.setSalaryLevelId(dto.getSalaryLevelId());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setPhoto(dto.getPhoto());
        entity.setRemark(dto.getRemark());
        entity.setCreateBy(SecurityUtils.currentUserIdOrNull());
        employeeMapper.insert(entity);
        log.info("新增职工：{} {}（id={}）", entity.getEmpNo(), entity.getName(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(EmployeeDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少职工ID");
        }
        HrEmployee exist = requireEntity(dto.getId());

        assertEmpNoUnique(dto.getEmpNo(), dto.getId());
        assertIdCardMatchesBirth(dto.getIdCard(), dto.getBirthDate());
        assertGender(dto.getGender());
        assertEmployType(dto.getEmployType());
        assertDeptExists(dto.getDeptId());
        assertSalaryLevelExists(dto.getSalaryLevelId());

        // 在职状态是 NOT NULL：不传就沿用原值，再据此推导离职日期
        int employType = dto.getEmployType() == null ? exist.getEmployType() : dto.getEmployType();
        LocalDate leaveDate = resolveLeaveDate(employType, dto.getLeaveDate(), exist.getLeaveDate());
        assertDateOrder(dto.getHireDate(), leaveDate);

        // 显式 set：可空字段一律写入（这样清空才生效）；NOT NULL 的 employ_type/status 用条件 set；
        // leave_date 由上面推导，无条件写入（复职时会被清成 null）。
        employeeMapper.update(null, new LambdaUpdateWrapper<HrEmployee>()
                .eq(HrEmployee::getId, dto.getId())
                .set(HrEmployee::getEmpNo, dto.getEmpNo().trim())
                .set(HrEmployee::getName, dto.getName())
                .set(HrEmployee::getGender, dto.getGender())
                .set(HrEmployee::getBirthDate, dto.getBirthDate())
                .set(HrEmployee::getIdCard, trimToNull(dto.getIdCard()))
                .set(HrEmployee::getPhone, trimToNull(dto.getPhone()))
                .set(HrEmployee::getEmail, trimToNull(dto.getEmail()))
                .set(HrEmployee::getAddress, dto.getAddress())
                .set(HrEmployee::getEduLevel, trimToNull(dto.getEduLevel()))
                .set(HrEmployee::getDeptId, dto.getDeptId())
                .set(HrEmployee::getPosition, dto.getPosition())
                .set(HrEmployee::getHireDate, dto.getHireDate())
                .set(HrEmployee::getLeaveDate, leaveDate)
                .set(dto.getEmployType() != null, HrEmployee::getEmployType, employType)
                .set(HrEmployee::getSalaryLevelId, dto.getSalaryLevelId())
                .set(dto.getStatus() != null, HrEmployee::getStatus, dto.getStatus())
                .set(HrEmployee::getPhoto, dto.getPhoto())
                .set(HrEmployee::getRemark, dto.getRemark()));
        log.info("修改职工：id={} {} {}", dto.getId(), dto.getEmpNo(), dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HrEmployee employee = requireEntity(id);
        assertNoBusinessChildren(id);
        rewriteEmpNo(employee);
        employeeMapper.deleteById(id);
        log.info("删除职工：{} {}（id={}）", employee.getEmpNo(), employee.getName(), id);
    }

    // ------------------------------------------------------------------ 内部方法

    /**
     * 职工名下还有业务记录时拒绝删除，否则那些记录会变成孤儿。
     *
     * 查 11 张「以 employee_id 挂在职工名下」的子表：人事 5（社保/奖罚/调动/证书/请假）
     * + 薪酬 6（水电费/加班/计件/计时/工资档案/工资发放）。
     * **hr_intern 不算** —— 实习生是独立表，employee_id 只在「已转正」时回填，
     * 删职工不该被它挡住。清单见 HrEmployeeMapper.countBusinessChildren。
     */
    private void assertNoBusinessChildren(Long employeeId) {
        String detail = employeeMapper.countBusinessChildren(employeeId).stream()
                .filter(item -> item.getCnt() != null && item.getCnt() > 0)
                .map(item -> item.getLabel() + " " + item.getCnt() + " 条")
                .collect(Collectors.joining("、"));
        if (!detail.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "该职工名下还有业务记录（" + detail + "），请先处理后再删除");
        }
    }

    @Override
    public void assertExists(Long id) {
        if (id == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "职工ID不能为空");
        }
        if (employeeMapper.selectById(id) == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "职工不存在或已删除");
        }
    }

    @Override
    public List<EmployeeOptionVO> options() {
        // 只取下拉需要的三列，不要 select *
        List<HrEmployee> list = employeeMapper.selectList(new LambdaQueryWrapper<HrEmployee>()
                .select(HrEmployee::getId, HrEmployee::getEmpNo, HrEmployee::getName)
                .orderByAsc(HrEmployee::getEmpNo));
        return list.stream().map(item -> {
            EmployeeOptionVO vo = new EmployeeOptionVO();
            vo.setId(item.getId());
            vo.setEmpNo(item.getEmpNo());
            vo.setName(item.getName());
            return vo;
        }).toList();
    }

    private HrEmployee requireEntity(Long id) {
        HrEmployee entity = employeeMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "职工不存在或已删除");
        }
        return entity;
    }

    /** 周岁：出生日期到今天的完整年数（闰日等边界交给 Period 处理） */
    private Integer ageOf(LocalDate birthDate) {
        return birthDate == null ? null : Period.between(birthDate, LocalDate.now()).getYears();
    }

    /** 列表统一补 age，保证「列表 / 详情 / 生日提醒」三处口径一致 */
    private void fillAge(List<EmployeeVO> list) {
        list.forEach(vo -> vo.setAge(ageOf(vo.getBirthDate())));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void assertEmpNoUnique(String empNo, Long excludeId) {
        if (!StringUtils.hasText(empNo)) {
            return;
        }
        LambdaQueryWrapper<HrEmployee> wrapper = new LambdaQueryWrapper<HrEmployee>()
                .eq(HrEmployee::getEmpNo, empNo.trim());
        if (excludeId != null) {
            wrapper.ne(HrEmployee::getId, excludeId);
        }
        if (employeeMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "工号已存在：" + empNo);
        }
    }

    /**
     * 身份证：18 位格式 + 内嵌出生日期必须真实存在且不晚于今天；并与独立的 birth_date 字段交叉校验。
     * 两处都填时必须一致 —— 否则"生日提醒/年龄统计"会出现两套口径。
     * 返回身份证内嵌的出生日期（未填身份证返回 null）。
     */
    private LocalDate validateIdCard(String idCard) {
        if (!StringUtils.hasText(idCard)) {
            return null;
        }
        String value = idCard.trim();
        if (!ID_CARD.matcher(value).matches()) {
            throw new BizException(ResultCode.PARAM_ERROR, "身份证号格式不正确（应为 18 位，末位可为 X）");
        }
        String birth = value.substring(6, 14);
        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(birth, ID_BIRTH);
        } catch (DateTimeParseException e) {
            throw new BizException(ResultCode.PARAM_ERROR, "身份证号中的出生日期不合法：" + birth);
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new BizException(ResultCode.PARAM_ERROR, "身份证号中的出生日期不能晚于今天");
        }
        return birthDate;
    }

    private void assertIdCardMatchesBirth(String idCard, LocalDate birthDate) {
        LocalDate fromIdCard = validateIdCard(idCard);
        if (fromIdCard != null && birthDate != null && !fromIdCard.equals(birthDate)) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "出生日期与身份证号中的出生日期不一致（身份证为 " + fromIdCard + "）");
        }
    }

    private void assertGender(Integer gender) {
        if (gender != null && gender != GENDER_MALE && gender != GENDER_FEMALE) {
            throw new BizException(ResultCode.PARAM_ERROR, "性别取值非法（1男 2女）");
        }
    }

    private void assertEmployType(Integer employType) {
        if (employType != null && (employType < EMPLOY_ON || employType > EMPLOY_LEFT)) {
            throw new BizException(ResultCode.PARAM_ERROR, "在职状态取值非法（1在职 2实习 3离职）");
        }
    }

    private void assertDeptExists(Long deptId) {
        if (deptId == null) {
            return;
        }
        if (deptMapper.selectById(deptId) == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "所属部门不存在或已删除");
        }
    }

    /** 级别存在性统一走 SalLevelService（M5 建 sal 模块后收口，不再跨模块直查 sal_level） */
    private void assertSalaryLevelExists(Long salaryLevelId) {
        if (salaryLevelId != null) {
            salLevelService.assertExists(salaryLevelId);
        }
    }

    /**
     * 统一「在职状态 ↔ 离职日期」的一致性，返回最终应落库的离职日期。
     * - 离职(3)：必须给出离职日期（入参优先，否则沿用库中原值）
     * - 非离职：不允许填离职日期；同时清掉历史值（复职场景）
     */
    private LocalDate resolveLeaveDate(int employType, LocalDate inputLeave, LocalDate existLeave) {
        if (employType == EMPLOY_LEFT) {
            LocalDate effective = inputLeave != null ? inputLeave : existLeave;
            if (effective == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "在职状态为「离职」时必须填写离职日期");
            }
            return effective;
        }
        if (inputLeave != null) {
            throw new BizException(ResultCode.PARAM_ERROR, "非离职状态不允许填写离职日期");
        }
        return null;
    }

    private void assertDateOrder(LocalDate hireDate, LocalDate leaveDate) {
        if (hireDate != null && leaveDate != null && leaveDate.isBefore(hireDate)) {
            throw new BizException(ResultCode.PARAM_ERROR, "离职日期不能早于入职日期");
        }
    }

    /** 逻辑删除前先给唯一键让路，否则以后再录入同工号会撞唯一索引 */
    private void rewriteEmpNo(HrEmployee employee) {
        String empNo = employee.getEmpNo();
        if (!StringUtils.hasText(empNo) || empNo.contains(DeleteMask.MARK)) {
            return;
        }
        employeeMapper.update(null, new LambdaUpdateWrapper<HrEmployee>()
                .eq(HrEmployee::getId, employee.getId())
                // 列宽 30：截断后再拼，超长会 Data too long 让删除接口 500
                .set(HrEmployee::getEmpNo, DeleteMask.masked(empNo, employee.getId(), 30)));
    }
}
