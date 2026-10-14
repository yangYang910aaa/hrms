import { defineStore } from 'pinia'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { getProfile } from '@/api/auth'
import type { UserInfo } from '@/types/api'

/**
 * 用户 store：token + 当前用户信息（含 roles / perms）。
 *
 * perms 是按钮级权限的唯一数据源（v-permission 指令读它）；
 * roles 里的 SUPER_ADMIN 走"全权限"通道（perms 会含 '*'）。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() as string,
    // {id, username, realName, deptId, roles: [], perms: []}
    info: null as UserInfo | null
  }),
  getters: {
    perms: (state): string[] => (state.info && state.info.perms) || [],
    roles: (state): string[] => (state.info && state.info.roles) || [],
    realName: (state): string => (state.info && state.info.realName) || '',
    avatar: (state): string => (state.info && state.info.avatar) || ''
  },
  actions: {
    /** 登录成功：落 token 与登录响应里带的 userInfo（含 perms，不必再查一次 me） */
    login(tokenValue: string, userInfo: UserInfo) {
      this.token = tokenValue
      setToken(tokenValue)
      this.info = userInfo
    },
    /** 刷新页面后 pinia state 丢失，用 token 换回用户信息（路由守卫调用） */
    async loadProfile() {
      const res = await getProfile()
      this.info = res.data
    },
    /** 退出：清本地态（服务端注销失败不阻塞） */
    logout() {
      this.token = ''
      this.info = null
      removeToken()
    }
  }
})
