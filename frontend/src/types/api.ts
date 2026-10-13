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

// ==================================================================
// 人事模块子表（hr/**）
//
// ⚠️ 这 6 张表的**分页接口直出实体**（`R<Page<HrXxx>>`，不是 VO）——
//    字段就是表列，**没有职工姓名**：列表要显示姓名得前端拿
//    /api/common/employee-options 翻译 employeeId（见 utils/lookup.ts）。
//    日期是 `YYYY-MM-DD` 字符串；时间是 ISO `YYYY-MM-DDTHH:mm:ss`（实测）。
//
// ⚠️ DTO 字段**一律写成"必填 + 可空"（不加 `?`）**：空值统一用 `null`。
//    两个原因：① el-input-number 的 modelValue 只接受 `number | null`，可选属性会带出
//    `undefined` 而类型不过；② 后端更新一律"传 null = 保持原值"，显式 null 与不传等价。
// ==================================================================

/** 职工社保（hr_social_security）。⚠️ (employeeId, month) 是 DB 唯一键，重复登记后端回 400 */
export interface SocialSecurity {
  id: number
  employeeId: number
  ssNo: string | null
  /** 缴费基数 */
  baseAmount: number | null
  /** 单位比例（0~1 小数，0.16 = 16%） */
  companyRate: number | null
  /** 个人比例（0~1 小数） */
  personalRate: number | null
  /** 单位缴纳额；不填时后端按「基数 × 单位比例」自动算 */
  companyAmount: number | null
  /** 个人缴纳额；不填时后端按「基数 × 个人比例」自动算 */
  personalAmount: number | null
  /** 所属月份 YYYY-MM */
  month: string
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 职工社保入参（SocialSecurityDTO） */
export interface SocialSecurityDTO {
  id: number | null
  employeeId: number | null
  ssNo: string | null
  baseAmount: number | null
  companyRate: number | null
  personalRate: number | null
  companyAmount: number | null
  personalAmount: number | null
  month: string
}

/** 职工社保分页查询参数（GET /api/hr/social/page） */
export interface SocialSecurityPageParams {
  current?: number
  size?: number
  employeeId?: number | null
  month?: string | null
}

/** 奖罚登记（hr_reward_punish） */
export interface RewardPunish {
  id: number
  employeeId: number
  /** 1奖励 2处罚（字典 reward_type） */
  type: number
  reason: string
  amount: number | null
  happenDate: string | null
  operator: string | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 奖罚登记入参（RewardPunishDTO） */
export interface RewardPunishDTO {
  id: number | null
  employeeId: number | null
  type: number | null
  reason: string
  amount: number | null
  happenDate: string | null
  operator: string | null
}

/** 奖罚登记分页查询参数（GET /api/hr/reward/page） */
export interface RewardPunishPageParams {
  current?: number
  size?: number
  employeeId?: number | null
  type?: number | null
}

/**
 * 职工调动（hr_transfer）。
 * 这是一张**登记表**，不会自动改写 hr_employee 的部门/岗位。
 * `from*` 是**历史快照**：不填时后端按「入参 → 本条已有值 → 主档当前值」的链取，
 * 所以编辑时必须把已有值原样回传，否则会改写历史。
 */
export interface Transfer {
  id: number
  employeeId: number
  fromDeptId: number | null
  toDeptId: number | null
  fromPosition: string | null
  toPosition: string | null
  transferDate: string | null
  reason: string | null
  approver: string | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 职工调动入参（TransferDTO） */
export interface TransferDTO {
  id: number | null
  employeeId: number | null
  fromDeptId: number | null
  toDeptId: number | null
  fromPosition: string | null
  toPosition: string | null
  transferDate: string | null
  reason: string | null
  approver: string | null
}

/** 职工调动分页查询参数（GET /api/hr/transfer/page） */
export interface TransferPageParams {
  current?: number
  size?: number
  employeeId?: number | null
}

/** 证书管理（hr_certificate）。后端校验「有效期不得早于发证日期」 */
export interface Certificate {
  id: number
  employeeId: number
  certName: string
  certNo: string | null
  issueOrg: string | null
  issueDate: string | null
  expireDate: string | null
  fileUrl: string | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 证书管理入参（CertificateDTO） */
export interface CertificateDTO {
  id: number | null
  employeeId: number | null
  certName: string
  certNo: string | null
  issueOrg: string | null
  issueDate: string | null
  expireDate: string | null
  fileUrl: string | null
}

/** 证书管理分页查询参数（GET /api/hr/certificate/page） */
export interface CertificatePageParams {
  current?: number
  size?: number
  employeeId?: number | null
  certName?: string | null
}

/**
 * 实习生管理（hr_intern）—— **独立表**：实习生未必是职工，用姓名标识；
 * employeeId 仅在「已转正(3)」时回填，后端两条互斥校验。
 */
export interface Intern {
  id: number
  name: string
  school: string | null
  major: string | null
  deptId: number | null
  mentor: string | null
  startDate: string | null
  endDate: string | null
  /** 1实习中 2已结束 3已转正（**不在 sys_dict 里**，前端硬编码） */
  status: number
  /** 转正后关联的职工ID（仅 status=3 时有值） */
  employeeId: number | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 实习生入参（InternDTO） */
export interface InternDTO {
  id: number | null
  name: string
  school: string | null
  major: string | null
  deptId: number | null
  mentor: string | null
  startDate: string | null
  endDate: string | null
  status: number | null
  employeeId: number | null
}

/** 实习生分页查询参数（GET /api/hr/intern/page） */
export interface InternPageParams {
  current?: number
  size?: number
  name?: string
  deptId?: number | null
  status?: number | null
}

/**
 * 请假单（hr_leave）。
 * ⚠️ 状态/审批三字段**只能由审批专用接口改**，通用更新接口刻意不碰它们。
 * 状态机：0 待审批 → 1 已通过 / 2 已驳回。
 */
export interface Leave {
  id: number
  employeeId: number
  /** 请假类型（字典 leave_type） */
  leaveType: number
  /** ISO 时间字符串，如 2026-10-13T09:00:00 */
  startTime: string
  endTime: string
  days: number | null
  reason: string | null
  /** 0待审批 1已通过 2已驳回 */
  status: number
  approver: string | null
  approveTime: string | null
  approveRemark: string | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 请假单入参（LeaveDTO）—— 刻意不含 status/approver/approveTime/approveRemark */
export interface LeaveDTO {
  id: number | null
  employeeId: number | null
  leaveType: number | null
  startTime: string | null
  endTime: string | null
  days: number | null
  reason: string | null
}

/** 请假审批入参（LeaveApproveDTO）—— 审批人/审批时间由服务端写 */
export interface LeaveApproveDTO {
  /** 1通过 2驳回 */
  status: number
  approveRemark: string | null
}

/** 请假分页查询参数（GET /api/hr/leave/page） */
export interface LeavePageParams {
  current?: number
  size?: number
  employeeId?: number | null
  status?: number | null
  leaveType?: number | null
}

// ==================================================================
// 培训模块（trn/**）
//
// 同样是**分页接口直出实体**：列表显示课程名/部门名要靠
// /api/common/course-options、/api/common/dept-tree 翻译。
// 三张表**几乎没有业务校验**，靠 @NotNull / @NotBlank 兜底。
// ==================================================================

/** 培训课程（trn_course）—— 培训模块的课程主档，courseNo 唯一（删除改写让路） */
export interface TrnCourse {
  id: number
  courseNo: string
  courseName: string
  /** 类别 1内部培训 2一线工培训 3其他（不在 sys_dict 里，前端硬编码） */
  courseType: number
  teacher: string | null
  /** 课时 DECIMAL(6,1) */
  hours: number | null
  /** 学分 DECIMAL(4,1) */
  credit: number | null
  content: string | null
  /** 1启用 0停用 */
  status: number
  remark: string | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 课程入参（TrnCourseDTO） */
export interface TrnCourseDTO {
  id: number | null
  courseNo: string
  courseName: string
  courseType: number | null
  teacher: string | null
  hours: number | null
  credit: number | null
  content: string | null
  status: number | null
  remark: string | null
}

/** 课程分页查询参数（GET /api/trn/course/page） */
export interface TrnCoursePageParams {
  current?: number
  size?: number
  courseName?: string
  courseType?: number | null
  status?: number | null
}

/** 内部培训期次（trn_internal）。deptId 可空；planPersons/actualPersons/status 是 NOT NULL 列，更新不传保持原值 */
export interface TrnInternal {
  id: number
  courseId: number
  batchNo: string | null
  deptId: number | null
  trainer: string | null
  place: string | null
  startDate: string | null
  endDate: string | null
  planPersons: number | null
  actualPersons: number | null
  /** 0计划 1进行中 2已完成 */
  status: number
  remark: string | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 内部培训入参（TrnInternalDTO） */
export interface TrnInternalDTO {
  id: number | null
  courseId: number | null
  batchNo: string | null
  deptId: number | null
  trainer: string | null
  place: string | null
  startDate: string | null
  endDate: string | null
  planPersons: number | null
  actualPersons: number | null
  status: number | null
  remark: string | null
}

/** 内部培训分页查询参数（GET /api/trn/internal/page） */
export interface TrnInternalPageParams {
  current?: number
  size?: number
  courseId?: number | null
  deptId?: number | null
  status?: number | null
}

/** 一线工培训期次（trn_frontline）。与内部培训分表：deptId **必填**，用「参训/合格人数」 */
export interface TrnFrontline {
  id: number
  courseId: number
  batchNo: string | null
  /** 参训（生产）部门，NOT NULL */
  deptId: number
  trainer: string | null
  startDate: string | null
  endDate: string | null
  workerCount: number | null
  qualifiedCount: number | null
  /** 0计划 1进行中 2已完成 */
  status: number
  remark: string | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 一线工培训入参（TrnFrontlineDTO） */
export interface TrnFrontlineDTO {
  id: number | null
  courseId: number | null
  batchNo: string | null
  deptId: number | null
  trainer: string | null
  startDate: string | null
  endDate: string | null
  workerCount: number | null
  qualifiedCount: number | null
  status: number | null
  remark: string | null
}

/** 一线工培训分页查询参数（GET /api/trn/frontline/page） */
export interface TrnFrontlinePageParams {
  current?: number
  size?: number
  courseId?: number | null
  deptId?: number | null
  status?: number | null
}

// ==================================================================
// 薪酬模块（sal/**）
//
// 与 hr/trn 同样的两条约定：
// ① 基础 6 套的分页接口**直出实体**（只有 employeeId / levelId，没有姓名与级别名）
//    → 列表用 utils/lookup 翻译；
// ② 工资档案 / 工资发放的分页接口**返回 VO**（联表带出了 empNo / employeeName / levelName）。
//
// ⚠️ **派生金额一律服务端算、DTO 里没有这些字段**（utility.totalFee、overtime 的 rate/amount、
//    piece/time 的 amount、archive 的各项金额）—— 前端不传也传不了，这是故意的契约。
// ⚠️ `sal_utility_fee` / `sal_archive` / `sal_payment` 的 `(employeeId, month)` 是**唯一键**。
// ==================================================================

/** 薪酬级别（sal_level）。levelNo 唯一（删除改写让路）；被加班标准或在册职工引用则不能删 */
export interface SalLevel {
  id: number
  levelName: string
  levelNo: string | null
  /** 基本工资；也是加班时薪的计算基数 */
  baseSalary: number | null
  remark: string | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 薪酬级别入参（SalLevelDTO） */
export interface SalLevelDTO {
  id: number | null
  levelName: string
  levelNo: string | null
  baseSalary: number | null
  remark: string | null
}

/** 薪酬级别分页查询参数（GET /api/sal/level/page） */
export interface SalLevelPageParams {
  current?: number
  size?: number
  levelName?: string
}

/** 加班标准（sal_overtime_standard）。复合唯一键 (levelId, overtimeType)，删除把 levelId 置 -id 让路 */
export interface SalOvertimeStandard {
  id: number
  levelId: number
  /** 加班类型（字典 overtime_type：1工作日 2休息日 3节假日） */
  overtimeType: number
  /** 倍率，如 1.5 */
  rate: number | null
  remark: string | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 加班标准入参（SalOvertimeStandardDTO） */
export interface SalOvertimeStandardDTO {
  id: number | null
  levelId: number | null
  overtimeType: number | null
  rate: number | null
  remark: string | null
}

/** 加班标准分页查询参数（GET /api/sal/overtime-standard/page） */
export interface SalOvertimeStandardPageParams {
  current?: number
  size?: number
  levelId?: number | null
  overtimeType?: number | null
}

/** 水电费登记（sal_utility_fee）。⚠️ (employeeId, month) 唯一键；totalFee 服务端算 */
export interface SalUtilityFee {
  id: number
  employeeId: number
  month: string
  waterFee: number | null
  electricFee: number | null
  dormFee: number | null
  /** 派生：水 + 电 + 住宿 */
  totalFee: number | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 水电费入参（SalUtilityFeeDTO）—— **不含 totalFee** */
export interface SalUtilityFeeDTO {
  id: number | null
  employeeId: number | null
  month: string
  waterFee: number | null
  electricFee: number | null
  dormFee: number | null
}

/** 水电费分页查询参数（GET /api/sal/utility/page） */
export interface SalUtilityFeePageParams {
  current?: number
  size?: number
  employeeId?: number | null
  month?: string | null
}

/** 加班登记（sal_overtime_record）。rate/amount 服务端算；`(employeeId, month)` **没有**唯一键（同月可多条） */
export interface SalOvertimeRecord {
  id: number
  employeeId: number
  month: string
  overtimeType: number
  hours: number | null
  /** 派生：当时用的倍率（快照） */
  rate: number | null
  /** 派生：时薪 × 倍率 × 小时；时薪 = 基本工资 ÷ 21.75 ÷ 8 */
  amount: number | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 加班登记入参（SalOvertimeRecordDTO）—— **不含 rate/amount** */
export interface SalOvertimeRecordDTO {
  id: number | null
  employeeId: number | null
  month: string
  overtimeType: number | null
  hours: number | null
}

/** 加班登记分页查询参数（GET /api/sal/overtime/page） */
export interface SalOvertimeRecordPageParams {
  current?: number
  size?: number
  employeeId?: number | null
  month?: string | null
  overtimeType?: number | null
}

/** 计件工资（sal_piece_wage）。amount = 数量 × 单价，服务端算 */
export interface SalPieceWage {
  id: number
  employeeId: number
  month: string
  productName: string | null
  quantity: number | null
  unitPrice: number | null
  /** 派生：数量 × 单价 */
  amount: number | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 计件工资入参（SalPieceWageDTO）—— **不含 amount** */
export interface SalPieceWageDTO {
  id: number | null
  employeeId: number | null
  month: string
  productName: string | null
  quantity: number | null
  unitPrice: number | null
}

/** 计件工资分页查询参数（GET /api/sal/piece/page） */
export interface SalPieceWagePageParams {
  current?: number
  size?: number
  employeeId?: number | null
  month?: string | null
}

/** 计时工资（sal_time_wage）。amount = 工时 × 时薪，服务端算 */
export interface SalTimeWage {
  id: number
  employeeId: number
  month: string
  hourlyRate: number | null
  hours: number | null
  /** 派生：工时 × 时薪 */
  amount: number | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 计时工资入参（SalTimeWageDTO）—— **不含 amount** */
export interface SalTimeWageDTO {
  id: number | null
  employeeId: number | null
  month: string
  hourlyRate: number | null
  hours: number | null
}

/** 计时工资分页查询参数（GET /api/sal/time/page） */
export interface SalTimeWagePageParams {
  current?: number
  size?: number
  employeeId?: number | null
  month?: string | null
}

// ------------------------------------------------------------------ 工资档案 / 工资发放

/**
 * 工资档案（SalArchiveVO，**联表 VO**：带 empNo / employeeName / levelName）。
 * 档案是**可追溯快照**：levelId / baseSalary 与各项金额都存当时值，之后改标准不影响它。
 * 状态机：0 草稿 → 1 已确认 → 2 已发放；**已确认/已发放不可改、不可重算、不可删**。
 */
export interface SalArchiveVO {
  id: number
  employeeId: number
  empNo: string | null
  employeeName: string | null
  employType: number | null
  month: string
  levelId: number | null
  levelName: string | null
  baseSalary: number | null
  overtimeAmount: number | null
  pieceAmount: number | null
  timeAmount: number | null
  /** 当月奖励 − 当月处罚 */
  rewardAmount: number | null
  socialSecurity: number | null
  utilityFee: number | null
  /** 其他扣款：**唯二可手改的字段之一** */
  deductAmount: number | null
  /** 个税：**唯二可手改的字段之一** */
  tax: number | null
  netSalary: number | null
  status: number
  calcTime: string | null
  createTime: string | null
  createBy: number | null
}

/** 单人计算档案（POST /api/sal/archive，body 即此） */
export interface SalArchiveCalcDTO {
  employeeId: number | null
  month: string
}

/** 批量计算（POST /api/sal/archive/calc）。`employeeIds` 不传 = 全部未删除职工（含离职） */
export interface SalArchiveBatchCalcDTO {
  month: string
  employeeIds: number[] | null
}

/** 档案手改（PUT /api/sal/archive）—— **只开放其他扣款与个税两个口子** */
export interface SalArchiveUpdateDTO {
  id: number | null
  deductAmount: number | null
  tax: number | null
  remark: string | null
}

/** 批量计算的逐人明细（SalArchiveCalcItemVO） */
export interface SalArchiveCalcItemVO {
  employeeId: number
  empNo: string | null
  employeeName: string | null
  employType: number | null
  levelId: number | null
  levelName: string | null
  baseSalary: number | null
  overtimeAmount: number | null
  pieceAmount: number | null
  timeAmount: number | null
  rewardAmount: number | null
  socialSecurity: number | null
  /** 当月是否登记过社保（0 未登记，明细里会提示按 0 计） */
  socialSecurityRegistered: number | null
  utilityFee: number | null
  deductAmount: number | null
  tax: number | null
  netSalary: number | null
  /** 该人本次的动作：CREATED / UPDATED / SKIPPED 等 */
  action: string | null
  archiveId: number | null
  archiveStatus: number | null
  /** 逐人提示（如"没设薪酬级别，已跳过"） */
  notices: string[]
}

/** 批量计算结果（SalArchiveCalcResultVO） */
export interface SalArchiveCalcResultVO {
  month: string
  total: number
  created: number
  updated: number
  skipped: number
  /** 整批提示（含跳过原因、未登记社保的名单等） */
  notices: string[]
  items: SalArchiveCalcItemVO[]
}

/** 工资档案分页查询参数（GET /api/sal/archive/page） */
export interface SalArchivePageParams {
  current?: number
  size?: number
  employeeId?: number | null
  month?: string | null
  status?: number | null
}

/**
 * 工资发放单（SalPaymentVO，联表 VO）。
 * 状态机：0 未发放 → 1 已发放；`payDate` / `status` **只能由 `/{id}/pay` 服务端写**，
 * 所以 SalPaymentUpdateDTO 里没有这两个字段。
 */
export interface SalPaymentVO {
  id: number
  employeeId: number
  empNo: string | null
  employeeName: string | null
  month: string
  netSalary: number | null
  payDate: string | null
  payWay: string | null
  bankAccount: string | null
  /** 0未发放 1已发放 */
  status: number
  remark: string | null
  createTime: string | null
}

/** 生成发放单（POST /api/sal/payment）。只认已确认(1)的档案；`employeeIds` 不传 = 该月全部已确认档案 */
export interface SalPaymentGenDTO {
  month: string
  employeeIds: number[] | null
}

/** 发放单修改（PUT /api/sal/payment）—— **不含 payDate / status** */
export interface SalPaymentUpdateDTO {
  id: number | null
  payWay: string | null
  bankAccount: string | null
  remark: string | null
}

/** 生成发放单结果（SalPaymentGenResultVO） */
export interface SalPaymentGenResultVO {
  month: string
  total: number
  created: number
  /** 该职工该月已有发放单，跳过 */
  skippedExists: number
  /** 该月还是草稿的档案数（会提示"请先确认"） */
  draftCount: number
  paidCount: number
  notices: string[]
}

/** 工资发放分页查询参数（GET /api/sal/payment/page） */
export interface SalPaymentPageParams {
  current?: number
  size?: number
  employeeId?: number | null
  month?: string | null
  status?: number | null
}

// ==================================================================
// 报表统计（report/**）
//
// 一页 8 图**一次性载荷**（ReportOverviewVO）：8 张图共用同一个「name/value」形状
// （ReportItemVO），饼图/柱状图/折线图都吃 name+value，前端渲染代码可复用。
// ⚠️ 口径（详见后端 ReportOverviewVO 注释）：
//    · 人员结构类（性别/学历/年龄/工龄/部门）**只统计在职**，各项之和 = total；
//    · 流动类（入职/辞职/离职率）用**全量**（含离职）。
// ==================================================================

/** 报表单项（ReportItemVO） */
export interface ReportItem {
  /** 分类名，如「男」「本科」「25-29岁」「技术部」「3月」 */
  name: string
  /** 计数值（人数） */
  value: number
}

/** 统计图表总览（ReportOverviewVO） */
export interface ReportOverview {
  /** 在职人数（employ_type ≠ 3） */
  total: number
  /** 离职人数（employ_type = 3） */
  resigned: number
  /** 离职率（百分比，保留 2 位小数） */
  turnoverRate: number
  /** 趋势统计年份 */
  year: number
  gender: ReportItem[]
  education: ReportItem[]
  age: ReportItem[]
  workYears: ReportItem[]
  dept: ReportItem[]
  /** 入职趋势（指定年份 1–12 月） */
  hireTrend: ReportItem[]
  /** 辞职趋势（指定年份 1–12 月） */
  resignTrend: ReportItem[]
}

// ==================================================================
// 公共查询（query/**）
//
// 职工查询是只读的 hr_employee 视图（分页结果 = EmployeeVO，复用其类型）；
// 通知管理用 sys_notice。
// ==================================================================

/**
 * 通知公告（sys_notice）。
 * 状态机：0 草稿 → 发布 → 1 已发布 → 下线 → 2 已下线 → 再发布 → 1。
 * `publisher` / `publishTime` 由服务端在发布时写入，`printCount` 由打印接口自增。
 */
export interface SysNotice {
  id: number
  title: string
  /** 1通知 2公告（不在 sys_dict 里，前端硬编码） */
  noticeType: number
  content: string | null
  /** 发布人（服务端写） */
  publisher: string | null
  /** 发布时间（服务端写） */
  publishTime: string | null
  /** 0草稿 1已发布 2已下线 */
  status: number
  /** 打印次数 */
  printCount: number
  remark: string | null
  createTime: string | null
  updateTime: string | null
  createBy: number | null
}

/** 通知入参（NoticeDTO）—— 刻意不含 publisher/publishTime/status/printCount（走专用接口） */
export interface NoticeDTO {
  id: number | null
  title: string
  noticeType: number | null
  content: string | null
  remark: string | null
}

/** 通知分页查询参数（GET /api/query/notice/page） */
export interface NoticePageParams {
  current?: number
  size?: number
  title?: string
  noticeType?: number | null
  status?: number | null
}
