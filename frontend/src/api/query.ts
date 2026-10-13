import request from './request'
import type {
  ApiResult,
  EmployeePageParams,
  EmployeeVO,
  NoticeDTO,
  NoticePageParams,
  PageResult,
  SysNotice
} from '@/types/api'

/**
 * 公共查询接口（`/api/query/**`）。
 * 页面在 `src/views/query/**`，文件名与后端 `sys_menu.component` 一一对应。
 *
 * ⚠️ 权限点与人事管理面分开：职工查询要 `query:employee:list`（不是 hr:employee:list），
 *    通知管理要 `query:notice:*`。这样"只有公共查询权限"的角色也能用，不用放宽管理接口。
 */

// ------------------------------------------------------------------ 职工查询（只读）

/** 条件分页：姓名模糊 / 部门 / 在职状态 / 状态（后端复用职工主档的查询口径） */
export const queryEmployeePage = (
  params: EmployeePageParams
): Promise<ApiResult<PageResult<EmployeeVO>>> =>
  request.get<PageResult<EmployeeVO>>('/query/employee/page', { params })

/** 职工详情（联表带部门名/级别名），供查询页"查看"弹窗 */
export const queryEmployeeDetail = (id: number): Promise<ApiResult<EmployeeVO>> =>
  request.get<EmployeeVO>(`/query/employee/${id}`)

// ------------------------------------------------------------------ 通知管理

/** 条件分页：标题模糊 / 类型 / 状态 */
export const noticePage = (params: NoticePageParams): Promise<ApiResult<PageResult<SysNotice>>> =>
  request.get<PageResult<SysNotice>>('/query/notice/page', { params })

export const getNotice = (id: number): Promise<ApiResult<SysNotice>> =>
  request.get<SysNotice>(`/query/notice/${id}`)

/** 新增（始终落为草稿 0，发布走 publishNotice） */
export const createNotice = (data: NoticeDTO): Promise<ApiResult<number>> =>
  request.post<number>('/query/notice', data)

/** 修改（已发布需先下线；不改状态字段） */
export const updateNotice = (data: NoticeDTO): Promise<ApiResult<null>> =>
  request.put<null>('/query/notice', data)

/** 删除（逻辑删除；已发布需先下线） */
export const deleteNotice = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/query/notice/${id}`)

/** 发布（草稿/已下线 → 已发布，服务端写发布人与发布时间） */
export const publishNotice = (id: number): Promise<ApiResult<null>> =>
  request.put<null>(`/query/notice/${id}/publish`)

/** 下线（已发布 → 已下线） */
export const offlineNotice = (id: number): Promise<ApiResult<null>> =>
  request.put<null>(`/query/notice/${id}/offline`)

/** 打印计数 +1，返回自增后的次数（能查看即能打印，与列表同权限） */
export const printNotice = (id: number): Promise<ApiResult<number>> =>
  request.put<number>(`/query/notice/${id}/print`)
