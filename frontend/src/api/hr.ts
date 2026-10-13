import request from './request'
import type {
  ApiResult,
  Certificate,
  CertificateDTO,
  CertificatePageParams,
  Intern,
  InternDTO,
  InternPageParams,
  Leave,
  LeaveApproveDTO,
  LeaveDTO,
  LeavePageParams,
  PageResult,
  RewardPunish,
  RewardPunishDTO,
  RewardPunishPageParams,
  SocialSecurity,
  SocialSecurityDTO,
  SocialSecurityPageParams,
  Transfer,
  TransferDTO,
  TransferPageParams
} from '@/types/api'

/**
 * 人事管理子表接口（`/api/hr/**` 全部需要业务权限，与 `/api/common/**` 区分开）。
 * 页面在 `src/views/hr/**`，文件名与后端 `sys_menu.component` 一一对应
 *（约定：**文件名与所在目录同名**，如 `hr/social/Social` → `views/hr/social/Social.vue`）。
 *
 * ⚠️ 这 6 个模块的**分页接口直出实体**（不是 VO）：返回里只有 employeeId / deptId，
 *    没有姓名与部门名 —— 列表显示要用 `utils/lookup.ts` 翻译。
 *
 * 只写页面真正用到的接口：没被页面调用的"顺手补上"等于死代码，不加。
 */

// ------------------------------------------------------------------ 职工社保

/**
 * 条件分页（按职工 / 月份筛选）。
 * ⚠️ `hr_social_security(employee_id, month)` 是 **DB 唯一键**：
 *    同一职工同一月重复登记，新增会回 400「该职工在 X 已有社保记录，请勿重复登记」；
 *    删除走"让路"（employee_id 置 -id），所以删掉后同月能重新登记。
 */
export const socialPage = (
  params: SocialSecurityPageParams
): Promise<ApiResult<PageResult<SocialSecurity>>> =>
  request.get<PageResult<SocialSecurity>>('/hr/social/page', { params })

/** 新增。缴纳额不填时后端按「基数 × 比例」自动算（HALF_UP 2 位），前端不要自己算 */
export const createSocial = (data: SocialSecurityDTO): Promise<ApiResult<number>> =>
  request.post<number>('/hr/social', data)

/**
 * 修改。基数/比例/金额都是 NOT NULL 列，后端用**条件 set**：传 null = 不改。
 * 前端始终回传原值即可；注意"动了基数或比例才重算缴纳额"，否则保留手改值。
 */
export const updateSocial = (data: SocialSecurityDTO): Promise<ApiResult<null>> =>
  request.put<null>('/hr/social', data)

export const deleteSocial = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/hr/social/${id}`)

// ------------------------------------------------------------------ 奖罚登记

/** 条件分页（按职工 / 类型筛选） */
export const rewardPage = (
  params: RewardPunishPageParams
): Promise<ApiResult<PageResult<RewardPunish>>> =>
  request.get<PageResult<RewardPunish>>('/hr/reward/page', { params })

/** 新增（type 1奖励 2处罚） */
export const createReward = (data: RewardPunishDTO): Promise<ApiResult<number>> =>
  request.post<number>('/hr/reward', data)

export const updateReward = (data: RewardPunishDTO): Promise<ApiResult<null>> =>
  request.put<null>('/hr/reward', data)

export const deleteReward = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/hr/reward/${id}`)

// ------------------------------------------------------------------ 职工调动

/** 条件分页（按职工筛选） */
export const transferPage = (
  params: TransferPageParams
): Promise<ApiResult<PageResult<Transfer>>> =>
  request.get<PageResult<Transfer>>('/hr/transfer/page', { params })

/**
 * 新增。`fromDeptId` / `fromPosition` 不填时后端从**职工主档快照**带出。
 * 后端校验「调动前后的部门与岗位完全相同，请检查」。
 */
export const createTransfer = (data: TransferDTO): Promise<ApiResult<number>> =>
  request.post<number>('/hr/transfer', data)

/**
 * 修改。`from*` 是历史快照，后端取值的回退链是「入参 → 本条已有值 → 主档当前值」——
 * 前端编辑时**必须把本条已有的 fromDeptId/fromPosition 原样回传**，否则会改写历史。
 */
export const updateTransfer = (data: TransferDTO): Promise<ApiResult<null>> =>
  request.put<null>('/hr/transfer', data)

export const deleteTransfer = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/hr/transfer/${id}`)

// ------------------------------------------------------------------ 证书管理

/** 条件分页（按职工 / 证书名称模糊筛选） */
export const certificatePage = (
  params: CertificatePageParams
): Promise<ApiResult<PageResult<Certificate>>> =>
  request.get<PageResult<Certificate>>('/hr/certificate/page', { params })

/** 新增。后端校验「有效期不能早于发证日期」（前端也先拦一道，体验更好） */
export const createCertificate = (data: CertificateDTO): Promise<ApiResult<number>> =>
  request.post<number>('/hr/certificate', data)

export const updateCertificate = (data: CertificateDTO): Promise<ApiResult<null>> =>
  request.put<null>('/hr/certificate', data)

export const deleteCertificate = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/hr/certificate/${id}`)

// ------------------------------------------------------------------ 实习生管理

/** 条件分页（姓名模糊 / 实习部门 / 状态） */
export const internPage = (params: InternPageParams): Promise<ApiResult<PageResult<Intern>>> =>
  request.get<PageResult<Intern>>('/hr/intern/page', { params })

/**
 * 新增。`hr_intern` 是**独立表**（不在职工删除保护的 11 张子表里）。
 * 后端两条互斥校验：「已转正 ⟺ 关联了职工档案」，即 status=3 必须给 employeeId、
 * 非 3 不允许给；「结束日期不能早于开始日期」。
 */
export const createIntern = (data: InternDTO): Promise<ApiResult<number>> =>
  request.post<number>('/hr/intern', data)

export const updateIntern = (data: InternDTO): Promise<ApiResult<null>> =>
  request.put<null>('/hr/intern', data)

export const deleteIntern = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/hr/intern/${id}`)

// ------------------------------------------------------------------ 请假管理

/** 条件分页（按职工 / 状态 / 请假类型筛选） */
export const leavePage = (params: LeavePageParams): Promise<ApiResult<PageResult<Leave>>> =>
  request.get<PageResult<Leave>>('/hr/leave/page', { params })

/** 提交请假单（落库即「待审批」，status 由后端写 0） */
export const createLeave = (data: LeaveDTO): Promise<ApiResult<number>> =>
  request.post<number>('/hr/leave', data)

/**
 * 修改请假单（仅限「待审批」，已审批后端会拒）。
 * ⚠️ DTO 里**没有** status/approver/approveTime/approveRemark —— 审批只能走 approveLeave。
 */
export const updateLeave = (data: LeaveDTO): Promise<ApiResult<null>> =>
  request.put<null>('/hr/leave', data)

/**
 * 审批：status 1 通过 / 2 驳回。**只有「待审批(0)」能审**，重复审批回 400；
 * 审批人与审批时间由服务端按当前登录用户写，不接受前端传入。
 */
export const approveLeave = (id: number, data: LeaveApproveDTO): Promise<ApiResult<null>> =>
  request.put<null>(`/hr/leave/${id}/approve`, data)

export const deleteLeave = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/hr/leave/${id}`)
