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

/**
 * 菜单树节点。
 * 两个接口都用它（字段完全同构）：
 * 1. `/api/auth/menus` —— 当前用户可见菜单，按钮 F 不进树，只贡献 perms；
 * 2. `/api/system/menu/tree` —— 菜单管理页的完整树，**含按钮 F 与隐藏项**（管理员要能改它们）。
 */
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
  /** M 目录 / C 菜单 / F 按钮（F 只出现在管理端的完整树里） */
  type: 'M' | 'C' | 'F'
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

// ------------------------------------------------------------------ 系统设置

/**
 * 部门入参（DeptDTO；新增与修改共用，修改时 id 必填）。
 * parentId = 0 表示顶级部门（后端约定，不是 null）。
 */
export interface DeptDTO {
  id?: number | null
  parentId: number
  deptName: string
  deptCode?: string | null
  leader?: string | null
  phone?: string | null
  sort?: number
  status?: number
}

/**
 * 菜单入参（MenuDTO；新增与修改共用，修改时 id 必填）。parentId = 0 表示顶级。
 *
 * 层级规则（后端 assertParentAllows，前端下拉也按它过滤可选父级）：
 * 目录下可挂 目录/菜单/按钮；菜单下只能挂按钮；按钮下不能再挂任何节点。
 */
export interface MenuDTO {
  id?: number | null
  parentId: number
  menuName: string
  menuType: 'M' | 'C' | 'F'
  path?: string | null
  component?: string | null
  /** 权限标识，如 hr:employee:list；按钮 F 必填，且全表唯一 */
  perms?: string | null
  icon?: string | null
  sort?: number
  /** 是否显示 1显示 0隐藏 */
  visible?: number
  status?: number
}

/** 角色（SysRole；/api/system/role/page 的返回） */
export interface RoleVO {
  id: number
  roleName: string
  /** 角色标识，只能字母/数字/下划线；SUPER_ADMIN 是内置的超级管理员 */
  roleCode: string
  sort: number
  status: number
  remark: string | null
}

/** 角色入参（RoleDTO；新增与修改共用，修改时 id 必填） */
export interface RoleDTO {
  id?: number | null
  roleName: string
  roleCode: string
  sort?: number
  status?: number
  remark?: string | null
}

/** 角色分页查询参数（GET /api/system/role/page） */
export interface RolePageParams {
  current?: number
  size?: number
  roleName?: string
  status?: number | null
}

/**
 * 用户（SysUser；/api/system/user/page 的返回）。
 * 口令哈希被 @JsonIgnore 掉了，接口永远拿不到 password。
 * 注意**没有部门名字段**，列表要显示部门得自己拿部门树翻译 deptId。
 */
export interface UserVO {
  id: number
  username: string
  realName: string
  deptId: number | null
  phone: string | null
  email: string | null
  avatar: string | null
  status: number
  lastLoginTime: string | null
}

/**
 * 用户入参（UserDTO）。
 * 后端的三条硬规矩（见 UserDTO 注释）：
 * 1. 新增 password 必填；修改时 password 与 status 都会被忽略；
 * 2. username 不允许修改；
 * 3. roleIds 只在新增时生效，改角色必须走 PUT /{id}/roles。
 */
export interface UserDTO {
  id?: number | null
  username: string
  /** 仅新增用到；修改时后端忽略，前端也不要回显 */
  password?: string | null
  realName: string
  deptId?: number | null
  phone?: string | null
  email?: string | null
  /**
   * 后端 update 是**无条件** set 这个字段的，前端不回传原值就会被清空。
   * 本页没有头像编辑入口，所以修改时必须把原值带回去。
   */
  avatar?: string | null
  status?: number
  /** 新增时可直接分配角色；修改时后端忽略（改角色走 PUT /{id}/roles） */
  roleIds?: number[] | null
}

/** 用户分页查询参数（GET /api/system/user/page） */
export interface UserPageParams {
  current?: number
  size?: number
  username?: string
  realName?: string
  deptId?: number | null
  status?: number | null
}

/** 字典类型（SysDictType；本表没有 sort 列，后端按 id 排序） */
export interface DictTypeVO {
  id: number
  dictName: string
  /** 类型标识，如 sex；它是字典数据与前端口径的引用键，新增后不允许改 */
  dictType: string
  status: number
  remark: string | null
}

/** 字典类型入参（DictTypeDTO） */
export interface DictTypeDTO {
  id?: number | null
  dictName: string
  dictType: string
  status?: number
  remark?: string | null
}

/** 字典数据（SysDictData） */
export interface DictDataVO {
  id: number
  dictType: string
  /** 显示文本 */
  dictLabel: string
  /** 存库值（字符串，页面里做数字比较时要转） */
  dictValue: string
  sort: number
  /** 是否默认项 1是 0否（同一类型下至多一个） */
  isDefault: number
  status: number
  remark: string | null
}

/** 字典数据入参（DictDataDTO） */
export interface DictDataDTO {
  id?: number | null
  dictType: string
  dictLabel: string
  dictValue: string
  sort?: number
  isDefault?: number
  status?: number
  remark?: string | null
}

/**
 * 社保比例（SysSocialRate）。
 * ⚠️ 比例一律是 **0~1 的小数**：0.16 表示 16%（列 DECIMAL(6,4)）。
 * 前端不要乘 100，也不要做成百分比输入 —— 后端 @DecimalMax("1") 会把 16 拦下来。
 */
export interface SocialRateVO {
  id: number
  rateName: string
  rateCode: string
  companyRate: number
  personalRate: number
  /** 缴费基数下限；0 = 未配置 */
  baseMin: number
  /** 缴费基数上限；0 = 不限制 */
  baseMax: number
  /** 生效月份 YYYY-MM */
  effectiveMonth: string | null
  sort: number
  status: number
  remark: string | null
}

/** 社保比例入参（SocialRateDTO） */
export interface SocialRateDTO {
  id?: number | null
  rateName: string
  rateCode: string
  companyRate: number
  personalRate: number
  /** 0 = 未配置；仅当两端都大于 0 时才校验「下限 ≤ 上限」 */
  baseMin?: number
  baseMax?: number
  effectiveMonth?: string | null
  sort?: number
  status?: number
  remark?: string | null
}

/** 社保比例分页查询参数（GET /api/system/social-rate/page） */
export interface SocialRatePageParams {
  current?: number
  size?: number
  rateName?: string
  status?: number | null
}
