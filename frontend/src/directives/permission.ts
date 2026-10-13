import type { App, DirectiveBinding } from 'vue'
import { useUserStore } from '@/store/user'

/**
 * 按钮级权限指令：`v-permission="'hr:employee:add'"`（字符串）或
 * `v-permission="['hr:employee:add', 'hr:employee:edit']"`（数组 = 全部具备，AND）。
 *
 * 数据源 = /api/auth/me 返回的 perms（存在 user store）。超级管理员的 perms 含 '*'，直接放行。
 * 没有权限的按钮**直接从 DOM 移除**（不是禁用）—— 禁用态还能被 DevTools 打开，没意义。
 */
export function setupPermission(app: App) {
  app.directive('permission', {
    mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
      const value = binding.value
      if (!value) return
      const perms = useUserStore().perms || []
      const has = perms.includes('*')
        ? true
        : Array.isArray(value)
          ? value.every((p) => perms.includes(p))
          : perms.includes(value)
      if (!has && el.parentNode) {
        el.parentNode.removeChild(el)
      }
    }
  })
}
