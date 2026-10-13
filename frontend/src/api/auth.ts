import request from './request'
import type { ApiResult, LoginResult, MenuNode, UserInfo } from '@/types/api'

/** 登录：入参 {username,password}，返回 {tokenName, tokenValue, expiresIn, userInfo} */
export const login = (
  data: { username: string; password: string }
): Promise<ApiResult<LoginResult>> => request.post<LoginResult>('/auth/login', data)

/** 退出登录（注销当前 token；失败也不阻塞前端清理） */
export const logout = (): Promise<ApiResult<null>> => request.post<null>('/auth/logout')

/** 当前用户信息 + 角色编码 + 权限码（perms 是 v-permission 的数据源） */
export const getProfile = (): Promise<ApiResult<UserInfo>> => request.get<UserInfo>('/auth/me')

/** 当前用户菜单树（动态路由的数据源；按钮 F 不进树，只贡献 perms） */
export const getMenus = (): Promise<ApiResult<MenuNode[]>> => request.get<MenuNode[]>('/auth/menus')
