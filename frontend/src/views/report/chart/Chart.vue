<template>
  <div class="report-chart-page">
    <!-- 顶部：KPI + 年份切换 -->
    <el-card shadow="never" class="head-card">
      <div class="head">
        <div class="kpi">
          <span class="kpi-label">在职人数</span>
          <span class="kpi-value">{{ data ? data.total : '—' }}</span>
        </div>
        <div class="kpi">
          <span class="kpi-label">离职人数</span>
          <span class="kpi-value">{{ data ? data.resigned : '—' }}</span>
        </div>
        <div class="kpi">
          <span class="kpi-label">离职率</span>
          <span class="kpi-value">{{ data ? `${data.turnoverRate}%` : '—' }}</span>
        </div>
        <div class="spacer"></div>
        <span class="tip">结构类（性别/学历/年龄/工龄/部门）只统计在职；趋势类含离职</span>
        <el-select v-model="year" style="width: 118px" @change="load">
          <el-option v-for="y in yearOptions" :key="y" :label="`${y} 年`" :value="y" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </el-card>

    <div v-loading="loading" class="grid">
      <el-card shadow="never" class="chart-card">
        <template #header><span class="chart-title">性别统计</span></template>
        <div ref="elGender" class="chart"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header><span class="chart-title">学历比率</span></template>
        <div ref="elEducation" class="chart"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header><span class="chart-title">年龄比率统计</span></template>
        <div ref="elAge" class="chart"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header><span class="chart-title">工龄统计</span></template>
        <div ref="elWorkYears" class="chart"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header><span class="chart-title">部门人员统计</span></template>
        <div ref="elDept" class="chart"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header><span class="chart-title">离职率统计</span></template>
        <div ref="elTurnover" class="chart"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>
          <span class="chart-title">入职统计（{{ year }} 年）</span>
        </template>
        <div ref="elHire" class="chart"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>
          <span class="chart-title">辞职统计（{{ year }} 年）</span>
        </template>
        <div ref="elResign" class="chart"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { reportOverview } from '@/api/report'
import type { ReportItem, ReportOverview } from '@/types/api'

/**
 * 报表统计（一页 8 图）。
 *
 * 8 张图共用后端同一个 `name/value` 形状，这里用 4 个小工厂函数
 * （pie / bar / line / gauge）统一出 option，避免 8 段各写一遍 echarts 配置。
 * 图表实例在每次重绘前统一 dispose，卸载时清理并摘掉 resize 监听，防止内存泄漏。
 */

const data = ref<ReportOverview | null>(null)
const loading = ref(false)
const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const yearOptions = Array.from({ length: 6 }, (_, i) => currentYear - i)

// 8 个容器
const elGender = ref<HTMLElement>()
const elEducation = ref<HTMLElement>()
const elAge = ref<HTMLElement>()
const elWorkYears = ref<HTMLElement>()
const elDept = ref<HTMLElement>()
const elTurnover = ref<HTMLElement>()
const elHire = ref<HTMLElement>()
const elResign = ref<HTMLElement>()

let instances: echarts.ECharts[] = []

// ---------- option 工厂 ----------
const palette = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9B8CFF', '#36CFC9', '#FF85C0']

function toPairs(items: ReportItem[] = []): { name: string; value: number }[] {
  return items.map((i) => ({ name: i.name, value: i.value }))
}

/** 饼图：占比类（性别 / 学历） */
function pieOption(items: ReportItem[]): echarts.EChartsOption {
  return {
    color: palette,
    tooltip: { trigger: 'item', formatter: '{b}：{c} 人（{d}%）' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '66%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b}\n{c}' },
        data: toPairs(items)
      }
    ]
  }
}

/** 柱状图：分布类（年龄 / 工龄 / 部门） */
function barOption(items: ReportItem[]): echarts.EChartsOption {
  const names = items.map((i) => i.name)
  return {
    color: palette,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 16, top: 20, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { interval: 0, rotate: names.length > 6 ? 28 : 0 }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: items.map((i) => i.value), barMaxWidth: 40 }]
  }
}

/** 折线图：趋势类（入职 / 辞职，按月） */
function lineOption(items: ReportItem[]): echarts.EChartsOption {
  return {
    color: palette,
    tooltip: { trigger: 'axis' },
    grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: items.map((i) => i.name) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        type: 'line',
        smooth: true,
        symbolSize: 7,
        areaStyle: { opacity: 0.12 },
        data: items.map((i) => i.value)
      }
    ]
  }
}

/** 仪表盘：离职率 */
function gaugeOption(rate: number): echarts.EChartsOption {
  return {
    series: [
      {
        type: 'gauge',
        min: 0,
        max: 100,
        radius: '92%',
        progress: { show: true, width: 14 },
        axisLine: { lineStyle: { width: 14 } },
        axisTick: { show: false },
        splitLine: { length: 10 },
        axisLabel: { distance: 16, fontSize: 10 },
        pointer: { width: 4 },
        detail: { valueAnimation: true, formatter: '{value}%', fontSize: 22, offsetCenter: [0, '52%'] },
        title: { offsetCenter: [0, '76%'], fontSize: 12 },
        data: [{ value: Number(rate || 0), name: '离职率' }]
      }
    ]
  }
}

// ---------- 渲染 ----------
function draw(el: HTMLElement | undefined, option: echarts.EChartsOption) {
  if (!el) return
  const chart = echarts.init(el)
  chart.setOption(option)
  instances.push(chart)
}

function disposeAll() {
  instances.forEach((c) => c.dispose())
  instances = []
}

function render() {
  const d = data.value
  disposeAll()
  if (!d) return
  draw(elGender.value, pieOption(d.gender))
  draw(elEducation.value, pieOption(d.education))
  draw(elAge.value, barOption(d.age))
  draw(elWorkYears.value, barOption(d.workYears))
  draw(elDept.value, barOption(d.dept))
  draw(elTurnover.value, gaugeOption(d.turnoverRate))
  draw(elHire.value, lineOption(d.hireTrend))
  draw(elResign.value, lineOption(d.resignTrend))
}

function onResize() {
  instances.forEach((c) => c.resize())
}

async function load() {
  loading.value = true
  try {
    const res = await reportOverview(year.value)
    data.value = res.data
    await nextTick()
    render()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await load()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  disposeAll()
})
</script>

<style scoped>
.head-card {
  margin-bottom: 12px;
}

.head {
  display: flex;
  align-items: center;
  gap: 28px;
  flex-wrap: wrap;
}

.kpi {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.kpi-label {
  font-size: 12px;
  color: #909399;
}

.kpi-value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.spacer {
  flex: 1;
}

.tip {
  font-size: 12px;
  color: #909399;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.chart-title {
  font-weight: 600;
}

.chart {
  height: 260px;
  width: 100%;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
