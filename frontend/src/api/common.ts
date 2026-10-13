import request from './request'
import type { ApiResult, CourseOption, DeptNode, DictOption, EmployeeOption, SalaryLevelOption } from '@/types/api'

/**
 * 公共只读选项接口（仅需登录，不要业务权限）——
 * 业务表单的下拉数据源，见后端 CommonController 的设计意图。
 */

/** 按字典类型取启用中的字典项：[{label, value, isDefault}] */
export const dictOptions = (dictType: string): Promise<ApiResult<DictOption[]>> =>
  request.get<DictOption[]>(`/common/dict/${dictType}`)

/** 部门树（只含启用中的部门；父被过滤时子节点上提为根） */
export const deptTree = (): Promise<ApiResult<DeptNode[]>> =>
  request.get<DeptNode[]>('/common/dept-tree')

/** 职工下拉项（id / empNo / name） */
export const employeeOptions = (): Promise<ApiResult<EmployeeOption[]>> =>
  request.get<EmployeeOption[]>('/common/employee-options')

/** 课程下拉项（id / courseNo / courseName） */
export const courseOptions = (): Promise<ApiResult<CourseOption[]>> =>
  request.get<CourseOption[]>('/common/course-options')

/** 薪酬级别下拉项（id / levelNo / levelName / baseSalary） */
export const salaryLevelOptions = (): Promise<ApiResult<SalaryLevelOption[]>> =>
  request.get<SalaryLevelOption[]>('/common/salary-level-options')
