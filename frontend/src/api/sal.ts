import request from './request'
import type {
  ApiResult,
  PageResult,
  SalArchiveBatchCalcDTO,
  SalArchiveCalcDTO,
  SalArchiveCalcResultVO,
  SalArchivePageParams,
  SalArchiveUpdateDTO,
  SalArchiveVO,
  SalLevel,
  SalLevelDTO,
  SalLevelPageParams,
  SalOvertimeRecord,
  SalOvertimeRecordDTO,
  SalOvertimeRecordPageParams,
  SalOvertimeStandard,
  SalOvertimeStandardDTO,
  SalOvertimeStandardPageParams,
  SalPaymentGenDTO,
  SalPaymentGenResultVO,
  SalPaymentPageParams,
  SalPaymentUpdateDTO,
  SalPaymentVO,
  SalPieceWage,
  SalPieceWageDTO,
  SalPieceWagePageParams,
  SalTimeWage,
  SalTimeWageDTO,
  SalTimeWagePageParams,
  SalUtilityFee,
  SalUtilityFeeDTO,
  SalUtilityFeePageParams
} from '@/types/api'

/**
 * 薪酬管理接口（`/api/sal/**` 全部需要业务权限）。
 * 页面在 `src/views/sal/**`，文件名与后端 `sys_menu.component` 一一对应
 *（如 `sal/overtime-standard/OvertimeStandard` → `views/sal/overtime-standard/OvertimeStandard.vue`）。
 *
 * ⚠️ 贯穿整个模块的 3 条口径（DTO 里**没有**派生字段，前端传了也不被绑定）：
 *   1. **派生金额一律服务端算**：水电合计、加班 rate/amount、计件/计时 amount、档案各项与实发。
 *   2. **工资档案 21.75 口径**：时薪 = 基本工资 ÷ 21.75 ÷ 8（后端实现，前端不参与）。
 *   3. **工资档案之外的 6 张表，只有 `sal_utility_fee` 的 `(employeeId, month)` 有唯一键**；
 *      加班/计件/计时**故意没有**（同月多条是业务设计）。
 *
 * 只写页面真正用到的接口：没被页面调用的"顺手补上"等于死代码，不加。
 */

// ------------------------------------------------------------------ 薪酬级别

/** 条件分页（按级别名称模糊） */
export const levelPage = (params: SalLevelPageParams): Promise<ApiResult<PageResult<SalLevel>>> =>
  request.get<PageResult<SalLevel>>('/sal/level/page', { params })

/** 新增。`levelNo` 唯一（删除会改写让路，删掉后同号可重建） */
export const createLevel = (data: SalLevelDTO): Promise<ApiResult<number>> =>
  request.post<number>('/sal/level', data)

export const updateLevel = (data: SalLevelDTO): Promise<ApiResult<null>> =>
  request.put<null>('/sal/level', data)

/** 删除。**被加班标准或在册职工引用则后端拒绝**（400） */
export const deleteLevel = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/sal/level/${id}`)

// ------------------------------------------------------------------ 加班标准

/** 条件分页（按薪酬级别 / 加班类型） */
export const overtimeStandardPage = (
  params: SalOvertimeStandardPageParams
): Promise<ApiResult<PageResult<SalOvertimeStandard>>> =>
  request.get<PageResult<SalOvertimeStandard>>('/sal/overtime-standard/page', { params })

/** 新增。复合唯一键 `(levelId, overtimeType)`：同一级别同一类型只能有一条，重复 400 */
export const createOvertimeStandard = (
  data: SalOvertimeStandardDTO
): Promise<ApiResult<number>> => request.post<number>('/sal/overtime-standard', data)

export const updateOvertimeStandard = (
  data: SalOvertimeStandardDTO
): Promise<ApiResult<null>> => request.put<null>('/sal/overtime-standard', data)

/** 删除（删除时把 levelId 置 -id 让出复合唯一键，所以删后可重建同组合） */
export const deleteOvertimeStandard = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/sal/overtime-standard/${id}`)

// ------------------------------------------------------------------ 水电费登记

/** 条件分页（按职工 / 月份） */
export const utilityPage = (
  params: SalUtilityFeePageParams
): Promise<ApiResult<PageResult<SalUtilityFee>>> =>
  request.get<PageResult<SalUtilityFee>>('/sal/utility/page', { params })

/**
 * 新增。`totalFee` 由服务端算（水 + 电 + 住宿）。
 * ⚠️ `(employeeId, month)` 是唯一键：同一职工同一月只能登记一条，重复 400。
 */
export const createUtility = (data: SalUtilityFeeDTO): Promise<ApiResult<number>> =>
  request.post<number>('/sal/utility', data)

/** 修改。三项费用是 NOT NULL 列，后端条件 set：传 null = 保持原值；合计按最终值重算 */
export const updateUtility = (data: SalUtilityFeeDTO): Promise<ApiResult<null>> =>
  request.put<null>('/sal/utility', data)

export const deleteUtility = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/sal/utility/${id}`)

// ------------------------------------------------------------------ 加班登记

/** 条件分页（按职工 / 月份 / 加班类型） */
export const overtimePage = (
  params: SalOvertimeRecordPageParams
): Promise<ApiResult<PageResult<SalOvertimeRecord>>> =>
  request.get<PageResult<SalOvertimeRecord>>('/sal/overtime/page', { params })

/**
 * 新增。`rate` 与 `amount` 由服务端算（金额 = 基本工资 ÷ 21.75 ÷ 8 × 倍率 × 小时）。
 * 职工**没有薪酬级别**、或该级别 + 该类型**没配加班标准** → 后端回 400 并提示去配置，
 * **不会静默按 0 算**。同月可多条（这张表没有唯一键）。
 */
export const createOvertime = (data: SalOvertimeRecordDTO): Promise<ApiResult<number>> =>
  request.post<number>('/sal/overtime', data)

/** 修改。换职工 / 换类型 / 改小时都会**按最终值重算** rate 与 amount */
export const updateOvertime = (data: SalOvertimeRecordDTO): Promise<ApiResult<null>> =>
  request.put<null>('/sal/overtime', data)

export const deleteOvertime = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/sal/overtime/${id}`)

// ------------------------------------------------------------------ 计件工资

/** 条件分页（按职工 / 月份） */
export const piecePage = (
  params: SalPieceWagePageParams
): Promise<ApiResult<PageResult<SalPieceWage>>> =>
  request.get<PageResult<SalPieceWage>>('/sal/piece/page', { params })

/** 新增。`amount = 数量 × 单价`（服务端算）。同月可多条 */
export const createPiece = (data: SalPieceWageDTO): Promise<ApiResult<number>> =>
  request.post<number>('/sal/piece', data)

/** 修改。`productName` 可空文本**无条件写**（不传即清空）；数量/单价不传保持原值，合计按最终值重算 */
export const updatePiece = (data: SalPieceWageDTO): Promise<ApiResult<null>> =>
  request.put<null>('/sal/piece', data)

export const deletePiece = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/sal/piece/${id}`)

// ------------------------------------------------------------------ 计时工资

/** 条件分页（按职工 / 月份） */
export const timePage = (
  params: SalTimeWagePageParams
): Promise<ApiResult<PageResult<SalTimeWage>>> =>
  request.get<PageResult<SalTimeWage>>('/sal/time/page', { params })

/** 新增。`amount = 工时 × 时薪`（服务端算，时薪由用户填，不是 21.75 那套） */
export const createTime = (data: SalTimeWageDTO): Promise<ApiResult<number>> =>
  request.post<number>('/sal/time', data)

export const updateTime = (data: SalTimeWageDTO): Promise<ApiResult<null>> =>
  request.put<null>('/sal/time', data)

export const deleteTime = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/sal/time/${id}`)

// ------------------------------------------------------------------ 工资档案

/** 条件分页（按职工 / 月份 / 状态）。返回 VO，已带 empNo / employeeName / levelName */
export const archivePage = (
  params: SalArchivePageParams
): Promise<ApiResult<PageResult<SalArchiveVO>>> =>
  request.get<PageResult<SalArchiveVO>>('/sal/archive/page', { params })

/** 单人计算（生成该职工该月档案）。没设薪酬级别的职工会 400 提示 */
export const calcArchive = (data: SalArchiveCalcDTO): Promise<ApiResult<number>> =>
  request.post<number>('/sal/archive', data)

/**
 * 批量计算（工资档案页的重点动作）。
 * `employeeIds` **不传 = 全部未删除职工**（含离职，便于结清当月工资）；
 * 没设薪酬级别的**跳过并在 notices 里提示**；**单人失败不拖垮整批**（记 SKIPPED，其他人照算）。
 * 重算**只覆盖草稿(0)**，且**不冲掉已手改的 deductAmount / tax**。
 */
export const batchCalcArchive = (
  data: SalArchiveBatchCalcDTO
): Promise<ApiResult<SalArchiveCalcResultVO>> => request.post<SalArchiveCalcResultVO>('/sal/archive/calc', data)

/** 手改（**只开放 `deductAmount` / `tax`**）；实发由服务端按九项口径重算 */
export const updateArchive = (data: SalArchiveUpdateDTO): Promise<ApiResult<null>> =>
  request.put<null>('/sal/archive', data)

/** 确认草稿 → 已确认（确认后不可改、不可重算） */
export const confirmArchive = (id: number): Promise<ApiResult<null>> =>
  request.put<null>(`/sal/archive/${id}/confirm`)

/** 删除（草稿才可删；已确认/已发放会被拒） */
export const deleteArchive = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/sal/archive/${id}`)

// ------------------------------------------------------------------ 工资发放

/** 条件分页（按职工 / 月份 / 状态）。返回 VO */
export const paymentPage = (
  params: SalPaymentPageParams
): Promise<ApiResult<PageResult<SalPaymentVO>>> =>
  request.get<PageResult<SalPaymentVO>>('/sal/payment/page', { params })

/**
 * 生成发放单。**只认已确认(1)的档案**；`employeeIds` 不传 = 该月全部已确认档案。
 * 该月还有草稿档案时会在 notices 里提示"请先确认"。
 */
export const generatePayment = (
  data: SalPaymentGenDTO
): Promise<ApiResult<SalPaymentGenResultVO>> =>
  request.post<SalPaymentGenResultVO>('/sal/payment', data)

/** 修改（发发放方式 / 银行账号 / 备注）。**不含 payDate 与 status** */
export const updatePayment = (data: SalPaymentUpdateDTO): Promise<ApiResult<null>> =>
  request.put<null>('/sal/payment', data)

/** 发放：服务端写当天 payDate、发放单转已发放，并把对应档案置 2；档案不在或不是已确认 → 400 */
export const payPayment = (id: number): Promise<ApiResult<null>> =>
  request.put<null>(`/sal/payment/${id}/pay`)

/** 删除（已发放会被拒） */
export const deletePayment = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/sal/payment/${id}`)