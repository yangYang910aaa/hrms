import { computed, ref } from 'vue'
import { courseOptions, deptTree, employeeOptions } from '@/api/common'
import { flattenTree } from '@/utils/tree'
import type { CourseOption, DeptNode, EmployeeOption } from '@/types/api'

/**
 * 常用下拉源 + `id → 名称` 翻译（人事 6 套 / 培训 3 套页面共用）。
 *
 * 为什么需要它：这 9 张表的**分页接口直出实体**，只带 employeeId / deptId / courseId，
 * **不带姓名 / 部门名 / 课程名** —— 列表要显示名称只能自己翻译。
 * 6 个页面各抄一份「拉 options + 建 Map」就是 6 份重复，所以收进这里。
 *
 * 数据放在**模块级 ref**（一次会话里同时只有一个路由页面挂载，共享安全）；
 * 每次挂载都重新拉，**不做缓存** —— 职工/课程可能在别的页面刚被增删，
 * 缓存会让下拉与列表翻译显示过期数据。与样板页 Employee.vue 的做法一致。
 */

const employees = ref<EmployeeOption[]>([])
const depts = ref<DeptNode[]>([])
const courses = ref<CourseOption[]>([])

/** 职工下拉（也供列表里 employeeId → 姓名） */
export async function loadEmployees(): Promise<void> {
  employees.value = (await employeeOptions()).data || []
}

/** 部门树（下拉用 `deptOptions`；也供 deptId → 部门名） */
export async function loadDepts(): Promise<void> {
  depts.value = (await deptTree()).data || []
}

/** 课程下拉（也供列表里 courseId → 课程名） */
export async function loadCourses(): Promise<void> {
  courses.value = (await courseOptions()).data || []
}

/**
 * 部门下拉选项（树拍平成带缩进的平表）。
 * 放在这里是因为**调动 / 实习生 / 内部培训 / 一线工培训 4 个页面都要用**，
 * 各写一份 `computed(() => flattenTree(depts.value, …))` 就是 4 份重复。
 */
export const deptOptions = computed(() => flattenTree(depts.value, (n) => n.deptName))

/** 查不到时的占位：空值给「—」，有 id 但列表里查不到给 `#id`（多半是数据不一致，别静默留白） */
const EMPTY = '—'

/** employeeId → 姓名（列表列用） */
export function empName(id: number | null | undefined): string {
  if (id === null || id === undefined) return EMPTY
  return employees.value.find((e) => e.id === id)?.name ?? `#${id}`
}

/** 职工下拉的选项文案「工号 姓名」—— 9 个页面共 11 处用到，收成一处好统一改 */
export function employeeLabel(emp: EmployeeOption): string {
  return `${emp.empNo} ${emp.name}`
}

/** 课程下拉的选项文案「编号 课程名」 */
export function courseLabel(course: CourseOption): string {
  return `${course.courseNo} ${course.courseName}`
}

/** deptId → 部门名（部门树是嵌套的，递归找） */
export function deptName(id: number | null | undefined): string {
  if (id === null || id === undefined) return EMPTY
  const find = (list: DeptNode[]): DeptNode | undefined => {
    for (const n of list) {
      if (n.id === id) return n
      const hit = find(n.children || [])
      if (hit) return hit
    }
    return undefined
  }
  return find(depts.value)?.deptName ?? `#${id}`
}

/** courseId → 课程名（列表列用） */
export function courseName(id: number | null | undefined): string {
  if (id === null || id === undefined) return EMPTY
  return courses.value.find((c) => c.id === id)?.courseName ?? `#${id}`
}

// 只导出页面真正用到的：部门树本身不导出（页面统一用拍平后的 `deptOptions`），
// 避免又出现一个"看着有用、其实没人用"的导出。
export { courses, employees }
