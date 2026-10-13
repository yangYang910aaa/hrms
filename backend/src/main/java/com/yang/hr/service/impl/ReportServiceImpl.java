package com.yang.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yang.hr.entity.HrEmployee;
import com.yang.hr.entity.SysDept;
import com.yang.hr.mapper.HrEmployeeMapper;
import com.yang.hr.mapper.SysDeptMapper;
import com.yang.hr.service.DictDataService;
import com.yang.hr.service.ReportService;
import com.yang.hr.vo.DictOptionVO;
import com.yang.hr.vo.ReportItemVO;
import com.yang.hr.vo.ReportOverviewVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 报表统计服务实现。
 *
 * 聚合放在 **Java 侧**（一次 `selectList` 拉回未删除职工，再在内存分桶），而不是写一堆
 * SQL `CASE WHEN`：
 * ① 本项目数据量小（HR 演示系统，几百行级），一次全表扫描完全够用；
 * ② 年龄/工龄分桶要在 SQL 里写 `TIMESTAMPDIFF` + 多层 `CASE`，而 MyBatis 的 `@Select`
 *    里**不能出现裸 `<`**（会被当 XML 解析导致启动失败，见项目坑表），Java 侧写反而更稳、更好测。
 * 真到几万行规模，应改成 DB 聚合 + 建索引，这里留作后续优化点。
 */
@Service
public class ReportServiceImpl implements ReportService {

    /** 计数桶里"值缺失"的分类名 */
    private static final String UNKNOWN = "未填";
    /** 部门缺失时的分类名 */
    private static final String NO_DEPT = "未分配";

    private static final String[] AGE_LABELS = {"25岁以下", "25-29岁", "30-39岁", "40-49岁", "50岁及以上", UNKNOWN};
    private static final String[] WORK_YEAR_LABELS = {"1年以内", "1-3年", "3-5年", "5-10年", "10年以上", UNKNOWN};

    /** 在职状态：3 = 离职 */
    private static final int EMPLOY_TYPE_RESIGNED = 3;

    private final HrEmployeeMapper employeeMapper;
    private final SysDeptMapper deptMapper;
    private final DictDataService dictDataService;

    public ReportServiceImpl(HrEmployeeMapper employeeMapper,
                             SysDeptMapper deptMapper,
                             DictDataService dictDataService) {
        this.employeeMapper = employeeMapper;
        this.deptMapper = deptMapper;
        this.dictDataService = dictDataService;
    }

    @Override
    public ReportOverviewVO overview(Integer year) {
        int statYear = (year == null) ? LocalDate.now().getYear() : year;

        // 一次拉回未删除职工（@TableLogic 自动过滤 deleted=1）
        List<HrEmployee> all = employeeMapper.selectList(new LambdaQueryWrapper<HrEmployee>());
        List<HrEmployee> active = new ArrayList<>();
        List<HrEmployee> resigned = new ArrayList<>();
        for (HrEmployee e : all) {
            if (isResigned(e)) {
                resigned.add(e);
            } else {
                active.add(e);
            }
        }

        ReportOverviewVO vo = new ReportOverviewVO();
        vo.setYear(statYear);
        vo.setTotal(active.size());
        vo.setResigned(resigned.size());
        vo.setTurnoverRate(turnoverRate(active.size(), resigned.size()));

        // 人员结构类：只统计在职
        vo.setGender(groupByDict(active, e -> e.getGender() == null ? null : String.valueOf(e.getGender()), "sex"));
        vo.setEducation(groupByDict(active, HrEmployee::getEduLevel, "edu_level"));
        vo.setAge(bucketAge(active));
        vo.setWorkYears(bucketWorkYears(active));
        vo.setDept(bucketDept(active));

        // 流动类：用全量（含离职）
        vo.setHireTrend(monthlyTrend(all, HrEmployee::getHireDate, statYear));
        vo.setResignTrend(monthlyTrend(all, HrEmployee::getLeaveDate, statYear));
        return vo;
    }

    // ------------------------------------------------------------------ 分桶

    /** 按字典项顺序输出分布（含 0 值项，保证图例完整）；字典里没有的值原样输出；空值归「未填」 */
    private List<ReportItemVO> groupByDict(List<HrEmployee> list,
                                           Function<HrEmployee, String> valueFn,
                                           String dictType) {
        List<DictOptionVO> options = dictDataService.options(dictType);
        Map<String, Long> counts = new HashMap<>();
        long unknown = 0;
        for (HrEmployee e : list) {
            String v = valueFn.apply(e);
            if (v == null || v.isBlank()) {
                unknown++;
            } else {
                counts.merge(v, 1L, Long::sum);
            }
        }

        List<ReportItemVO> items = new ArrayList<>();
        Set<String> emitted = new HashSet<>();
        for (DictOptionVO o : options) {
            items.add(new ReportItemVO(o.getLabel(), counts.getOrDefault(o.getValue(), 0L)));
            emitted.add(o.getValue());
        }
        // 字典里没有的历史脏值原样输出，避免各项之和对不上在职人数
        for (Map.Entry<String, Long> en : counts.entrySet()) {
            if (!emitted.contains(en.getKey())) {
                items.add(new ReportItemVO(en.getKey(), en.getValue()));
            }
        }
        if (unknown > 0) {
            items.add(new ReportItemVO(UNKNOWN, unknown));
        }
        return items;
    }

    /** 年龄分桶（按出生日期算周岁，仅在职） */
    private List<ReportItemVO> bucketAge(List<HrEmployee> list) {
        long[] counts = new long[AGE_LABELS.length];
        LocalDate today = LocalDate.now();
        for (HrEmployee e : list) {
            if (e.getBirthDate() == null) {
                counts[5]++;
                continue;
            }
            long age = ChronoUnit.YEARS.between(e.getBirthDate(), today);
            if (age < 25) {
                counts[0]++;
            } else if (age < 30) {
                counts[1]++;
            } else if (age < 40) {
                counts[2]++;
            } else if (age < 50) {
                counts[3]++;
            } else {
                counts[4]++;
            }
        }
        return toItems(AGE_LABELS, counts);
    }

    /** 工龄分桶（按入职日期算工龄，仅在职） */
    private List<ReportItemVO> bucketWorkYears(List<HrEmployee> list) {
        long[] counts = new long[WORK_YEAR_LABELS.length];
        LocalDate today = LocalDate.now();
        for (HrEmployee e : list) {
            if (e.getHireDate() == null) {
                counts[5]++;
                continue;
            }
            long years = ChronoUnit.YEARS.between(e.getHireDate(), today);
            if (years < 1) {
                counts[0]++;
            } else if (years < 3) {
                counts[1]++;
            } else if (years < 5) {
                counts[2]++;
            } else if (years < 10) {
                counts[3]++;
            } else {
                counts[4]++;
            }
        }
        return toItems(WORK_YEAR_LABELS, counts);
    }

    /** 部门人员分布（按部门 id 升序，含 0 人部门；部门为空归「未分配」） */
    private List<ReportItemVO> bucketDept(List<HrEmployee> list) {
        List<SysDept> depts = deptMapper.selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getId));
        Map<Long, Long> counts = new LinkedHashMap<>();
        long noDept = 0;
        for (HrEmployee e : list) {
            if (e.getDeptId() == null) {
                noDept++;
            } else {
                counts.merge(e.getDeptId(), 1L, Long::sum);
            }
        }
        List<ReportItemVO> items = new ArrayList<>();
        for (SysDept d : depts) {
            items.add(new ReportItemVO(d.getDeptName(), counts.getOrDefault(d.getId(), 0L)));
        }
        // 挂在已删除部门下的在职职工，单列出来（不能让和数对不上）
        Set<Long> emitted = new HashSet<>();
        depts.forEach(d -> emitted.add(d.getId()));
        long orphan = counts.entrySet().stream()
                .filter(en -> !emitted.contains(en.getKey()))
                .mapToLong(Map.Entry::getValue)
                .sum();
        if (noDept > 0) {
            items.add(new ReportItemVO(NO_DEPT, noDept));
        }
        if (orphan > 0) {
            items.add(new ReportItemVO("其它部门", orphan));
        }
        return items;
    }

    /** 指定年份按月趋势（1–12 月） */
    private List<ReportItemVO> monthlyTrend(List<HrEmployee> all,
                                            Function<HrEmployee, LocalDate> dateFn,
                                            int year) {
        long[] counts = new long[12];
        for (HrEmployee e : all) {
            LocalDate d = dateFn.apply(e);
            if (d != null && d.getYear() == year) {
                counts[d.getMonthValue() - 1]++;
            }
        }
        String[] labels = new String[12];
        for (int i = 0; i < 12; i++) {
            labels[i] = (i + 1) + "月";
        }
        return toItems(labels, counts);
    }

    // ------------------------------------------------------------------ 内部工具

    private boolean isResigned(HrEmployee e) {
        return e.getEmployType() != null && e.getEmployType() == EMPLOY_TYPE_RESIGNED;
    }

    /** 离职率 = 离职 ÷ (在职 + 离职) × 100%，保留 2 位小数（无样本时为 0.00） */
    private BigDecimal turnoverRate(long active, long resigned) {
        long denom = active + resigned;
        if (denom == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(resigned * 100.0 / denom).setScale(2, RoundingMode.HALF_UP);
    }

    private List<ReportItemVO> toItems(String[] labels, long[] counts) {
        List<ReportItemVO> items = new ArrayList<>(labels.length);
        for (int i = 0; i < labels.length; i++) {
            items.add(new ReportItemVO(labels[i], counts[i]));
        }
        return items;
    }
}
