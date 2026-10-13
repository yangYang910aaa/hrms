import request from './request'
import type {
  ApiResult,
  DeptDTO,
  DeptNode,
  DictDataDTO,
  DictDataVO,
  DictTypeDTO,
  DictTypeVO,
  MenuDTO,
  MenuNode,
  PageResult,
  RoleDTO,
  RolePageParams,
  RoleVO,
  SocialRateDTO,
  SocialRatePageParams,
  SocialRateVO,
  UserDTO,
  UserPageParams,
  UserVO
} from '@/types/api'

/**
 * 系统设置模块接口（/api/system/** 全部需要业务权限，与 /api/common/** 区分开）。
 * 页面在 src/views/system/**，文件名与后端 menu.component 一一对应
 *（如 system/dept/index → views/system/dept/index.vue）。
 *
 * 只写页面真正用到的接口：没被页面调用的”顺手补上”等于死代码，不加。
 */

// ------------------------------------------------------------------ 部门（组织结构）

/**
 * 部门树（管理端全量，**含停用**的部门）。
 * 与 /common/dept-tree（只含启用中）不是同一个：管理页编辑停用部门时，
 * 它的上级必须能选到，用公共接口会漏选项。
 */
export const deptTree = (): Promise<ApiResult<DeptNode[]>> =>
  request.get<DeptNode[]>('/system/dept/tree')

/** 新增（返回新记录 id） */
export const createDept = (data: DeptDTO): Promise<ApiResult<number>> =>
  request.post<number>('/system/dept', data)

/** 修改（dto.id 必填） */
export const updateDept = (data: DeptDTO): Promise<ApiResult<null>> =>
  request.put<null>('/system/dept', data)

/**
 * 删除（逻辑删除）。后端会拦三种情况并回 400：
 * 该部门下还有子部门 / 还有用户 / 还有职工。消息由 request 拦截器统一弹出。
 */
export const deleteDept = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/system/dept/${id}`)

// ------------------------------------------------------------------ 菜单

/**
 * 完整菜单树（含按钮 F 与隐藏项）—— 管理页专用。
 * 与 /api/auth/menus 不同：后者只给当前用户可见的目录与菜单，看不到按钮。
 */
export const menuTree = (): Promise<ApiResult<MenuNode[]>> =>
  request.get<MenuNode[]>('/system/menu/tree')

/** 新增（返回新记录 id） */
export const createMenu = (data: MenuDTO): Promise<ApiResult<number>> =>
  request.post<number>('/system/menu', data)

/** 修改（dto.id 必填） */
export const updateMenu = (data: MenuDTO): Promise<ApiResult<null>> =>
  request.put<null>('/system/menu', data)

/**
 * 删除（逻辑删除）。该节点下还有子节点时后端会拒（400）。
 * 注意：权限标识 perms 有唯一键，删除会把 perms 改写成 `#del#+id` 让路，
 * 所以删掉一个权限点后，同样的 perms 还能重新建回来。
 */
export const deleteMenu = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/system/menu/${id}`)

// ------------------------------------------------------------------ 角色

/** 条件分页（真分页，与部门/菜单的整棵树不同） */
export const rolePage = (params: RolePageParams): Promise<ApiResult<PageResult<RoleVO>>> =>
  request.get<PageResult<RoleVO>>('/system/role/page', { params })

/** 新增（返回新记录 id） */
export const createRole = (data: RoleDTO): Promise<ApiResult<number>> =>
  request.post<number>('/system/role', data)

/** 修改（dto.id 必填） */
export const updateRole = (data: RoleDTO): Promise<ApiResult<null>> =>
  request.put<null>('/system/role', data)

/**
 * 删除（逻辑删除）。后端会拒两种情况并回 400：
 * 内置超级管理员角色；还有用户正在使用该角色。
 */
export const deleteRole = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/system/role/${id}`)

/** 该角色已授权的菜单 id（授权树回显） */
export const roleMenuIds = (id: number): Promise<ApiResult<number[]>> =>
  request.get<number[]>(`/system/role/${id}/menus`)

/**
 * 分配菜单（覆盖式，传空数组即清空全部授权）。
 *
 * 两个必须知道的行为：
 * 1. **后端不会替你补父节点** —— 只勾了按钮而没勾它所属的目录/菜单，
 *    /api/auth/menus 会把那个菜单"上提为根"，侧边栏层级就乱了。
 *    所以提交时要带上半选（indeterminate）的父节点，见页面里的 getCheckedKeys + getHalfCheckedKeys。
 * 2. 该角色下**已登录的其他用户**会被强制下线重登（排除操作者本人），
 *    所以操作者自己不会被踢，但改到自己所属角色时前端菜单树也不会自动刷新。
 */
export const assignRoleMenus = (id: number, menuIds: number[]): Promise<ApiResult<null>> =>
  request.put<null>(`/system/role/${id}/menus`, menuIds)

/** 全部启用角色（用户表单 / 分配角色的下拉源） */
export const roleList = (): Promise<ApiResult<RoleVO[]>> =>
  request.get<RoleVO[]>('/system/role/list')

// ------------------------------------------------------------------ 用户

/** 条件分页 */
export const userPage = (params: UserPageParams): Promise<ApiResult<PageResult<UserVO>>> =>
  request.get<PageResult<UserVO>>('/system/user/page', { params })

/** 新增（password 必填，可同时带 roleIds；返回新记录 id） */
export const createUser = (data: UserDTO): Promise<ApiResult<number>> =>
  request.post<number>('/system/user', data)

/**
 * 修改。**刻意不提交 password / status / roleIds**：
 * 后端会忽略 password 与 status，改口令走 reset-pwd、改角色走 /{id}/roles、
 * 改状态走 /{id}/status —— 那三个才带"踢下线"和防自锁保护。
 */
export const updateUser = (data: UserDTO): Promise<ApiResult<null>> =>
  request.put<null>('/system/user', data)

/** 删除（逻辑删除） */
export const deleteUser = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/system/user/${id}`)

/** 重置口令（该用户会被强制下线）；长度 6-100 */
export const resetUserPwd = (id: number, newPassword: string): Promise<ApiResult<null>> =>
  request.put<null>(`/system/user/${id}/reset-pwd`, { newPassword })

/** 启用 / 停用（停用后该用户会被强制下线）。改自己会被防自锁拦成 403 */
export const changeUserStatus = (id: number, status: number): Promise<ApiResult<null>> =>
  request.put<null>(`/system/user/${id}/status?status=${status}`)

/** 该用户已拥有的角色 id */
export const userRoleIds = (id: number): Promise<ApiResult<number[]>> =>
  request.get<number[]>(`/system/user/${id}/roles`)

/** 分配角色（覆盖式；该用户会被强制下线以便新权限立即生效） */
export const assignUserRoles = (id: number, roleIds: number[]): Promise<ApiResult<null>> =>
  request.put<null>(`/system/user/${id}/roles`, roleIds)

// ------------------------------------------------------------------ 数据字典（类型 + 数据两级）

/** 字典类型分页。本页左侧一次拉全（size 给大值），字典类型数量有限 */
export const dictTypePage = (
  params: { current?: number; size?: number; dictName?: string; dictType?: string; status?: number | null }
): Promise<ApiResult<PageResult<DictTypeVO>>> =>
  request.get<PageResult<DictTypeVO>>('/system/dict/type/page', { params })

/** 新增字典类型 */
export const createDictType = (data: DictTypeDTO): Promise<ApiResult<number>> =>
  request.post<number>('/system/dict/type', data)

/** 修改字典类型（dictType 标识不允许改，后端会拒） */
export const updateDictType = (data: DictTypeDTO): Promise<ApiResult<null>> =>
  request.put<null>('/system/dict/type', data)

/** 删除字典类型。该类型下还有字典数据时后端会拒（400） */
export const deleteDictType = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/system/dict/type/${id}`)

/** 字典数据分页（按 dictType 过滤） */
export const dictDataPage = (
  params: { current?: number; size?: number; dictType?: string; dictLabel?: string; status?: number | null }
): Promise<ApiResult<PageResult<DictDataVO>>> =>
  request.get<PageResult<DictDataVO>>('/system/dict/data/page', { params })

/** 新增字典数据 */
export const createDictData = (data: DictDataDTO): Promise<ApiResult<number>> =>
  request.post<number>('/system/dict/data', data)

/** 修改字典数据 */
export const updateDictData = (data: DictDataDTO): Promise<ApiResult<null>> =>
  request.put<null>('/system/dict/data', data)

/** 删除字典数据（纯子表，无唯一键，不需要让路） */
export const deleteDictData = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/system/dict/data/${id}`)

// ------------------------------------------------------------------ 社保比例

/** 条件分页 */
export const socialRatePage = (
  params: SocialRatePageParams
): Promise<ApiResult<PageResult<SocialRateVO>>> =>
  request.get<PageResult<SocialRateVO>>('/system/social-rate/page', { params })

/** 新增（比例传 0~1 小数；返回新记录 id） */
export const createSocialRate = (data: SocialRateDTO): Promise<ApiResult<number>> =>
  request.post<number>('/system/social-rate', data)

/**
 * 修改。比例与基数都是 NOT NULL 列，后端对它们用**条件 set**：
 * 传 null = 不改。前端始终回传原值即可。
 */
export const updateSocialRate = (data: SocialRateDTO): Promise<ApiResult<null>> =>
  request.put<null>('/system/social-rate', data)

/** 删除（逻辑删除；险种编码是唯一键，删除会改写让路） */
export const deleteSocialRate = (id: number): Promise<ApiResult<null>> =>
  request.delete<null>(`/system/social-rate/${id}`)
