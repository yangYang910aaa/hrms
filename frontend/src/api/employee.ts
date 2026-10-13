import request from './request'
import type { ApiResult, EmployeeDTO, EmployeePageParams, EmployeeVO, PageResult } from '@/types/api'

/**
 * 职工主档接口（/api/hr/employee）。
 * 页面文件在 views/hr/employee/Employee.vue（路径 + 文件名与后端 menu.component 字符串一致）。
 */

/** 条件分页：name 模糊 / deptId / employType / status */
export const employeePage = (
  params: EmployeePageParams
): Promise<ApiResult<PageResult<EmployeeVO>>> =>
  request.get<PageResult<EmployeeVO>>('/hr/employee/page', { params })

/** 详情 */
export const getEmployee = (id: number): Promise<ApiResult<EmployeeVO>> =>
  request.get<EmployeeVO>(`/hr/employee/${id}`)

/** 某月生日职工（缺省当月） */
export const birthdayList = (month?: number): Promise<ApiResult<EmployeeVO[]>> =>
  request.get<EmployeeVO[]>('/hr/employee/birthday', { params: month ? { month } : {} })

/** 新增（返回新记录 id） */
export const createEmployee = (data: EmployeeDTO): Promise<ApiResult<number>> =>
  request.post<number>('/hr/employee', data)

/** 修改（dto.id 必填） */
export const updateEmployee = (data: EmployeeDTO): Promise<ApiResult<null>> =>
  request.put<null>('/hr/employee', data)

/** 删除（逻辑删除） */
export const deleteEmployee = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/hr/employee/${id}`)

/**
 * 上传照片，返回可对外访问的 URL（再写进职工的 photo 字段）。
 * 走 el-upload 直传（带 satoken 头），不经 axios 实例。
 */
export const EMPLOYEE_PHOTO_URL = '/api/hr/employee/photo'
