import request from './request'
import type {
  ApiResult,
  PageResult,
  TrnCourse,
  TrnCourseDTO,
  TrnCoursePageParams,
  TrnFrontline,
  TrnFrontlineDTO,
  TrnFrontlinePageParams,
  TrnInternal,
  TrnInternalDTO,
  TrnInternalPageParams
} from '@/types/api'

/**
 * 培训管理接口（`/api/trn/**`，全部需要业务权限）。
 * 页面在 `src/views/trn/**`，文件名与后端 `sys_menu.component` 一一对应。
 *
 * ⚠️ 分页同样**直出实体**：`trn_internal` / `trn_frontline` 只带 courseId / deptId，
 *    列表要显示课程名与部门名，用 `utils/lookup.ts` 翻译。
 * ⚠️ 期次表单的「课程下拉 / 部门下拉」走**公共接口**（`/api/common/course-options`、
 *    `/api/common/dept-tree`，只需登录），已在 `api/common.ts` 里。
 */

// ------------------------------------------------------------------ 课程管理

/** 条件分页（课程名模糊 / 类别 / 状态） */
export const coursePage = (
  params: TrnCoursePageParams
): Promise<ApiResult<PageResult<TrnCourse>>> =>
  request.get<PageResult<TrnCourse>>('/trn/course/page', { params })

/**
 * 新增。课程编号 `course_no` 唯一（删除会改写让路，删掉后同号可重建）；
 * 类别 ∈{1内部培训 2一线工培训 3其他}。
 */
export const createCourse = (data: TrnCourseDTO): Promise<ApiResult<number>> =>
  request.post<number>('/trn/course', data)

export const updateCourse = (data: TrnCourseDTO): Promise<ApiResult<null>> =>
  request.put<null>('/trn/course', data)

/** 删除（逻辑删除）。**被任何期次引用则后端拒绝**（400） */
export const deleteCourse = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/trn/course/${id}`)

// ------------------------------------------------------------------ 内部培训

/** 条件分页（课程 / 部门 / 状态） */
export const internalPage = (
  params: TrnInternalPageParams
): Promise<ApiResult<PageResult<TrnInternal>>> =>
  request.get<PageResult<TrnInternal>>('/trn/internal/page', { params })

/**
 * 新增。`courseId` 必须存在；`deptId` **可空**（传则校验、不传即清空）；
 * `planPersons` / `actualPersons` / `status` 是 NOT NULL 列 → 更新不传保持原值。
 */
export const createInternal = (data: TrnInternalDTO): Promise<ApiResult<number>> =>
  request.post<number>('/trn/internal', data)

export const updateInternal = (data: TrnInternalDTO): Promise<ApiResult<null>> =>
  request.put<null>('/trn/internal', data)

export const deleteInternal = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/trn/internal/${id}`)

// ------------------------------------------------------------------ 一线工培训

/** 条件分页（课程 / 部门 / 状态） */
export const frontlinePage = (
  params: TrnFrontlinePageParams
): Promise<ApiResult<PageResult<TrnFrontline>>> =>
  request.get<PageResult<TrnFrontline>>('/trn/frontline/page', { params })

/**
 * 新增。与内部培训分表：`deptId` 这里**必填**；
 * 额外约束「合格人数 ≤ 参训人数」（更新时按最终值交叉校验，前端也先拦一道）。
 */
export const createFrontline = (data: TrnFrontlineDTO): Promise<ApiResult<number>> =>
  request.post<number>('/trn/frontline', data)

export const updateFrontline = (data: TrnFrontlineDTO): Promise<ApiResult<null>> =>
  request.put<null>('/trn/frontline', data)

export const deleteFrontline = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/trn/frontline/${id}`)
