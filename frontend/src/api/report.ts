import request from './request'
import type { ApiResult, ReportOverview } from '@/types/api'

/**
 * 报表统计接口（`/api/report/**`，需要 report:chart:* 权限；本模块只读，故只有 :list）。
 * 页面在 `src/views/report/chart/Chart.vue`（文件名 = 目录名，与后端 menu.component 一致）。
 */

/**
 * 统计总览：一次拿到一页 8 图所需的全部聚合数据。
 * `year` 只影响「入职/辞职趋势」与「离职率」的统计年份（省略用当前年后端默认）。
 */
export const reportOverview = (year?: number): Promise<ApiResult<ReportOverview>> =>
  request.get<ReportOverview>('/report/chart/overview', { params: year ? { year } : {} })
