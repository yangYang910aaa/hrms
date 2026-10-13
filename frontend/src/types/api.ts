/**
 * 与后端约定的接口类型（VO / DTO / 信封 / 分页），逐字段对齐 backend 的 vo/ 与 dto/。
 * 新增页面时从这里取类型，不要在各页面里重复声明。
 */

/** 统一响应信封：HTTP 恒 200，成败看 code（200/400/401/403/404/500） */
export interface ApiResult<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 分页结果（MyBatis-Plus Page 的 5 字段） */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/** 当前登录用户（/api/auth/me 与登录响应的 userInfo） */
export interface UserInfo {
  id: number
  username: string
  realName: string
  deptId: number | null
  /** 角色编码列表，如 ["SUPER_ADMIN"] */
  roles: string[]
  /** 权限标识列表，如 ["hr:employee:add"]；按钮级控制用 */
  perms: string[]
}

/** 登录成功返回（tokenName 一般是 satoken） */
export interface LoginResult {
  tokenName: string
  tokenValue: string
  expiresIn: number
  userInfo: UserInfo
}

/** 菜单树节点（/api/auth/menus；按钮 F 不进树，只贡献 perms） */
export interface MenuNode {
  id: number
  parentId: number
  name: string
  path: string
  /** 前端组件相对 views/ 的路径，如 'hr/employee/index'；目录为 null */
  component: string | null
  perms: string | null
  /** Element Plus 图标组件名，如 'Postcard' */
  icon: string | null
  /** M 目录 / C 菜单 */
  type: 'M' | 'C'
  sort: number
  visible: number
  status: number
  children: MenuNode[]
}

/** 字典项（/api/common/dict/{type}）—— 后端 isDefault 是 1/0，不是布尔 */
export interface DictOption {
  label: string
  value: string
  isDefault: number
}

/** 部门树节点（/api/common/dept-tree，只含启用中的部门） */
export interface DeptNode {
  id: number
  parentId: number | null
  deptName: string
  deptCode: string | null
  leader: string | null
  phone: string | null
  sort: number
  status: number
  children: DeptNode[]
}

/** 职工下拉项（/api/common/employee-options） */
export interface EmployeeOption {
  id: number
  empNo: string
  name: string
}

/** 课程下拉项（/api/common/course-options） */
export interface CourseOption {
  id: number
  courseNo: string
  courseName: string
}

/** 薪酬级别下拉项（/api/common/salary-level-options） */
export interface SalaryLevelOption {
  id: number
  levelNo: string
  levelName: string
  baseSalary: number
}

/** 职工视图（EmployeeVO；字典类字段只回原值，显示文本前端用字典翻译） */
export interface EmployeeVO {
  id: number
  empNo: string
  name: string
  gender: number | null
  birthDate: string | null
  /** 派生字段：周岁，服务端算好 */
  age: number | null
  idCard: string | null
  phone: string | null
  email: string | null
  address: string | null
  eduLevel: string | null
  deptId: number | null
  deptName: string | null
  position: string | null
  hireDate: string | null
  leaveDate: string | null
  employType: number | null
  salaryLevelId: number | null
  salaryLevelName: string | null
  status: number | null
  photo: string | null
  remark: string | null
  createTime: string | null
}

/** 职工入参（EmployeeDTO；新增与修改共用，修改时 id 必填） */
export interface EmployeeDTO {
  id?: number | null
  empNo: string
  name: string
  gender?: number | null
  birthDate?: string | null
  idCard?: string | null
  phone?: string | null
  email?: string | null
  address?: string | null
  eduLevel?: string | null
  deptId?: number | null
  position?: string | null
  hireDate?: string | null
  leaveDate?: string | null
  employType?: number | null
  salaryLevelId?: number | null
  status?: number | null
  photo?: string | null
  remark?: string | null
}

/** 职工分页查询参数（GET /api/hr/employee/page） */
export interface EmployeePageParams {
  current?: number
  size?: number
  name?: string
  deptId?: number | null
  employType?: number | null
  status?: number | null
}
