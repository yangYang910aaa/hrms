<template>
  <div class="dashboard">
    <!-- ============ 欢迎卡 ============ -->
    <el-card class="welcome" shadow="never">
      <div class="welcome-inner">
        <el-avatar :size="56" :src="avatar || undefined" />
        <div class="welcome-text">
          <h2>{{ greeting }}，{{ realName || username }}</h2>
          <p class="sub">当前角色：{{ rolesText }}</p>
        </div>
        <div class="welcome-date">{{ todayText }}</div>
      </div>
    </el-card>

    <!-- ============ KPI 行 ============
         每张卡都先过 can()：无权限的卡直接不渲染，也不会发请求
         （薪酬专员没有 report:chart:list，硬发会 403 弹错误提示）。
         「部门数量」走 /api/common/dept-tree，只要登录就能看，保证人人有内容可看。 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col v-if="showReport" :xs="12" :sm="12" :md="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi">
            <div class="kpi-icon is-primary"><el-icon :size="22"><UserFilled /></el-icon></div>
            <div class="kpi-body">
              <div class="kpi-num">{{ kpi.total ?? '—' }}</div>
              <div class="kpi-label">在职人数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col v-if="showLeave" :xs="12" :sm="12" :md="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi">
            <div class="kpi-icon is-warning"><el-icon :size="22"><Calendar /></el-icon></div>
            <div class="kpi-body">
              <div class="kpi-num">{{ kpi.pendingLeave ?? '—' }}</div>
              <div class="kpi-label">待审批请假</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col v-if="showArchive" :xs="12" :sm="12" :md="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi">
            <div class="kpi-icon is-success"><el-icon :size="22"><Document /></el-icon></div>
            <div class="kpi-body">
              <div class="kpi-num">{{ kpi.archiveCount ?? '—' }}</div>
              <div class="kpi-label">{{ currentMonth }} 工资档案</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi">
            <div class="kpi-icon is-info"><el-icon :size="22"><OfficeBuilding /></el-icon></div>
            <div class="kpi-body">
              <div class="kpi-num">{{ kpi.deptCount ?? '—' }}</div>
              <div class="kpi-label">部门数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ============ 迷你图表 ============ -->
    <el-row v-if="showReport" :gutter="16" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="card-title">部门人员分布</span></template>
          <div ref="elDept" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="card-title">入职趋势（{{ data?.year ?? '—' }} 年）</span></template>
          <div ref="elHire" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ============ 通知 + 生日 ============ -->
    <el-row :gutter="16" class="list-row">
      <el-col v-if="showNotice" :xs="24" :md="14">
        <el-card shadow="never" class="list-card">
          <template #header>
            <div class="card-head">
              <span class="card-title">最新通知</span>
              <el-button text type="primary" size="small" @click="go('/query/notice')">更多</el-button>
            </div>
          </template>
          <ul v-if="notices.length" class="notice-list">
            <li v-for="n in notices" :key="n.id" class="notice-item">
              <el-tag :type="n.noticeType === 2 ? 'warning' : 'primary'" size="small" effect="light">
                {{ n.noticeType === 2 ? '公告' : '通知' }}
              </el-tag>
              <span class="notice-title" :title="n.title">{{ n.title }}</span>
              <span class="notice-time">{{ dateTime(n.publishTime) }}</span>
            </li>
          </ul>
          <el-empty v-else description="暂无已发布通知" :image-size="60" />
        </el-card>
      </el-col>

      <el-col v-if="showBirthday" :xs="24" :md="10">
        <el-card shadow="never" class="list-card">
          <template #header><span class="card-title">本月生日</span></template>
          <ul v-if="birthdays.length" class="birthday-list">
            <li v-for="e in birthdays" :key="e.id" class="birthday-item">
              <el-avatar :size="28" :src="e.photo || undefined" />
              <span class="birthday-name">{{ e.name }}</span>
              <span class="birthday-dept">{{ e.deptName || '未分配部门' }}</span>
              <span class="birthday-date">{{ monthDay(e.birthDate) }}</span>
            </li>
          </ul>
          <el-empty v-else description="本月没有生日提醒" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- ============ 快捷入口 ============
         菜单树已由后端按角色过滤，所以这里不需要再判权限。 -->
    <el-card shadow="never" class="quick-card">
      <template #header><span class="card-title">快捷入口</span></template>
      <div class="quick-list">
        <el-button
          v-for="m in quickMenus"
          :key="m.path"
          class="quick-btn"
          @click="go(m.path)"
        >
          <el-icon><component :is="m.icon || 'Document'" /></el-icon>
          <span>{{ m.name }}</span>
        </el-button>
        <span v-if="!quickMenus.length" class="quick-empty">当前角色没有可用的业务菜单</span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Calendar,
  Document,
  OfficeBuilding,
  UserFilled
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'
import { deptTree } from '@/api/common'
import { reportOverview } from '@/api/report'
import { leavePage } from '@/api/hr'
import { archivePage } from '@/api/sal'
import { noticePage } from '@/api/query'
import { birthdayList } from '@/api/employee'
import type { DeptNode, EmployeeVO, ReportOverview, SysNotice } from '@/types/api'
import { dateTime, monthDay } from '@/utils/format'

/**
 * 工作台（业务化）。
 *
 * 改造前这里显示的是「可用菜单 27 / 权限点 110 / 可见范围」+ 一段开发笔记
 * （/doc.html、v-permission…）—— 对 HR 用户毫无意义。现在换成真业务内容。
 *
 * 🔴 硬约束：**必须按 perms 优雅降级**。
 *    工作台对所有登录用户可见，但上面这些数据接口各自要权限点
 *    （report:chart:list / hr:leave:list / sal:archive:list / query:notice:list / hr:employee:list）。
 *    薪酬专员没有 report:chart:list，直接请求会 403 并弹错误提示。
 *    所以每一块都先用 can() 判权限，无权限的**整块不渲染、一个请求都不发**。
 *    SUPER_ADMIN 的 perms 含 '*'，can() 里把它当全通过。
 */

const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const realName = computed(() => userStore.realName)
const avatar = computed(() => userStore.avatar)
const username = computed(() => userStore.info?.username || '')
const rolesText = computed(() => {
  const roles = userStore.roles || []
  return roles.length ? roles.join('、') : '无角色'
})

// ---------- 权限判断 ----------
const perms = computed(() => userStore.perms)
function can(perm: string): boolean {
  const list = perms.value
  return list.includes('*') || list.includes(perm)
}

const showReport = computed(() => can('report:chart:list'))
const showLeave = computed(() => can('hr:leave:list'))
const showArchive = computed(() => can('sal:archive:list'))
const showNotice = computed(() => can('query:notice:list'))
const showBirthday = computed(() => can('hr:employee:list'))

// ---------- 状态 ----------
const kpi = reactive<{
  total: number | null
  pendingLeave: number | null
  archiveCount: number | null
  deptCount: number | null
}>({ total: null, pendingLeave: null, archiveCount: null, deptCount: null })

const data = ref<ReportOverview | null>(null)
const notices = ref<SysNotice[]>([])
const birthdays = ref<EmployeeVO[]>([])

const now = new Date()
const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
const todayText = `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 ${now.getDate()} 日`

const greeting = computed(() => {
  const h = now.getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

// ---------- 图表 ----------
const elDept = ref<HTMLElement>()
const elHire = ref<HTMLElement>()
/** 容器 → 实例。用 Map 而不是数组，重绘时能按容器找到旧实例直接 setOption，避免重复 init 告警 */
const chartMap = new Map<HTMLElement, echarts.ECharts>()

/**
 * ECharts 画在 canvas 上，**读不到 CSS 变量**，所以这里按 tokens.css 的实际值硬编码一份。
 * 改 tokens.css 主色/语义色时，记得同步这里。
 */
const CHART_PALETTE = ['#2563EB', '#10b981', '#f59e0b', '#ef4444', '#6b7280', '#8b5cf6', '#06b6d4', '#f472b6']

function upsertChart(el: HTMLElement | undefined, option: echarts.EChartsOption) {
  if (!el) return
  let chart = chartMap.get(el)
  if (!chart) {
    chart = echarts.init(el)
    chartMap.set(el, chart)
  }
  chart.setOption(option, true)
}

/** 部门人员分布（迷你横向柱状图） */
function deptChartOption(items: ReportOverview['dept']): echarts.EChartsOption {
  const rows = (items || [])
    .slice()
    .sort((a, b) => b.value - a.value)
    .slice(0, 8)
  return {
    color: CHART_PALETTE,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 20, top: 10, bottom: 8, containLabel: true },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: {
      type: 'category',
      data: rows.map((r) => r.name),
      axisLabel: { width: 72, overflow: 'truncate' }
    },
    series: [{ type: 'bar', data: rows.map((r) => r.value), barMaxWidth: 18, itemStyle: { borderRadius: [0, 4, 4, 0] } }]
  }
}

/** 入职趋势（迷你折线图，1-12 月） */
function hireChartOption(items: ReportOverview['hireTrend']): echarts.EChartsOption {
  const rows = items || []
  return {
    color: CHART_PALETTE,
    tooltip: { trigger: 'axis' },
    grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: rows.map((r) => r.name), boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        type: 'line',
        data: rows.map((r) => r.value),
        smooth: true,
        symbolSize: 6,
        lineStyle: { width: 2.5 },
        areaStyle: { opacity: 0.12 }
      }
    ]
  }
}

function renderCharts() {
  if (!showReport.value || !data.value) return
  upsertChart(elDept.value, deptChartOption(data.value.dept))
  upsertChart(elHire.value, hireChartOption(data.value.hireTrend))
}

function onResize() {
  chartMap.forEach((c) => c.resize())
}

// ---------- 数据加载 ----------
function countDepts(nodes: DeptNode[]): number {
  return nodes.reduce((sum, n) => sum + 1 + countDepts(n.children || []), 0)
}

async function load() {
  const tasks: Promise<void>[] = []

  // 部门数量：/api/common/** 只要登录就能看，无权限的用户也有内容
  tasks.push(
    deptTree().then((r) => {
      kpi.deptCount = countDepts(r.data || [])
    })
  )

  if (showReport.value) {
    tasks.push(
      reportOverview().then((r) => {
        data.value = r.data
        kpi.total = r.data?.total ?? null
        renderCharts()
      })
    )
  }

  if (showLeave.value) {
    tasks.push(
      // 只要 total，size=1 少传数据
      leavePage({ status: 0, current: 1, size: 1 }).then((r) => {
        kpi.pendingLeave = r.data?.total ?? null
      })
    )
  }

  if (showArchive.value) {
    tasks.push(
      archivePage({ month: currentMonth, current: 1, size: 1 }).then((r) => {
        kpi.archiveCount = r.data?.total ?? null
      })
    )
  }

  if (showNotice.value) {
    tasks.push(
      noticePage({ status: 1, current: 1, size: 5 }).then((r) => {
        notices.value = r.data?.records || []
      })
    )
  }

  if (showBirthday.value) {
    tasks.push(
      birthdayList().then((r) => {
        birthdays.value = r.data || []
      })
    )
  }

  // 单个区块失败不拖垮其它区块（拦截器已弹错误，这里静默兜住）
  await Promise.all(tasks.map((t) => t.catch(() => undefined)))
}

// ---------- 快捷入口 ----------
/** 全路径 = 目录 path + 菜单 path，拼接规则与 store/permission.ts 注册路由时保持一致 */
const quickMenus = computed(() =>
  permissionStore.menus
    .flatMap((dir) =>
      (dir.children || []).map((m) => ({
        path: `${dir.path}/${m.path}`.replace(/\/+/g, '/'),
        name: m.name,
        icon: m.icon
      }))
    )
    .slice(0, 8)
)

function go(path: string) {
  router.push(path)
}

onMounted(async () => {
  await load()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chartMap.forEach((c) => c.dispose())
  chartMap.clear()
})
</script>

<style scoped>
.kpi-row {
  margin-top: var(--hr-space-4);
}

.kpi-card :deep(.el-card__body) {
  padding: var(--hr-space-4);
}

.kpi {
  display: flex;
  align-items: center;
  gap: var(--hr-space-3);
}

.kpi-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 46px;
  height: 46px;
  border-radius: var(--hr-radius);
}

.kpi-icon.is-primary {
  background: var(--hr-primary-soft);
  color: var(--hr-primary);
}

.kpi-icon.is-warning {
  background: #fef3e2;
  color: var(--hr-warning);
}

.kpi-icon.is-success {
  background: #e7f8f2;
  color: var(--hr-success);
}

.kpi-icon.is-info {
  background: var(--hr-bg-sunken);
  color: var(--hr-text-secondary);
}

.kpi-num {
  font-size: var(--hr-font-2xl);
  font-weight: 600;
  line-height: 1.2;
  color: var(--hr-text-primary);
  font-variant-numeric: tabular-nums;
}

.kpi-label {
  margin-top: 2px;
  font-size: var(--hr-font-sm);
  color: var(--hr-text-secondary);
}

.chart-row,
.list-row {
  margin-top: var(--hr-space-4);
}

.card-title {
  font-size: var(--hr-font-base);
  font-weight: 600;
  color: var(--hr-text-primary);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart {
  height: 240px;
}

.notice-list,
.birthday-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: var(--hr-space-2);
  padding: var(--hr-space-2) 0;
  border-bottom: 1px solid var(--hr-border-light);
  font-size: var(--hr-font-sm);
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-title {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--hr-text-regular);
}

.notice-time {
  flex: 0 0 auto;
  font-size: var(--hr-font-xs);
  color: var(--hr-text-placeholder);
}

.birthday-item {
  display: flex;
  align-items: center;
  gap: var(--hr-space-2);
  padding: var(--hr-space-2) 0;
  border-bottom: 1px solid var(--hr-border-light);
  font-size: var(--hr-font-sm);
}

.birthday-item:last-child {
  border-bottom: none;
}

.birthday-name {
  font-weight: 500;
  color: var(--hr-text-primary);
}

.birthday-dept {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--hr-text-secondary);
}

.birthday-date {
  flex: 0 0 auto;
  color: var(--hr-text-placeholder);
  font-variant-numeric: tabular-nums;
}

.quick-card {
  margin-top: var(--hr-space-4);
}

.quick-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--hr-space-2);
}

.quick-btn {
  margin: 0;
}

.quick-empty {
  font-size: var(--hr-font-sm);
  color: var(--hr-text-placeholder);
}
</style>
