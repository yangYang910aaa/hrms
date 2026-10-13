import { defineStore } from 'pinia'
import type { Component } from 'vue'
import { getMenus } from '@/api/auth'
import { useUserStore } from '@/store/user'
import router from '@/router'
import type { MenuNode } from '@/types/api'

/**
 * 权限 store：把 /api/auth/menus 的菜单树变成两样东西——
 * 1. **动态路由**：每个 C 菜单 addRoute 到布局（Root）的 children 下；
 * 2. **侧边栏数据**：原始菜单树（目录 M → 菜单 C 两层）存 state.menus，布局直接渲染。
 *
 * 组件映射规则：后端 menu.component 是 'hr/employee/index' 这种相对 views/ 的路径，
 * 用 import.meta.glob 懒加载 views 目录下的各个页面组件（相对路径 glob，key 稳定）对上去。
 * 页面还没写的菜单（后续批次）落到占位组件，显示"页面建设中"，不会白屏。
 */

// 懒加载全量页面组件：key 形如 '../views/hr/employee/index.vue'
const viewModules = import.meta.glob('../views/**/*.vue')

const Placeholder = (): Promise<Component> => import('@/views/common/placeholder.vue')

/** 已注册的动态路由名，重置时逐条 removeRoute（否则换账号登录会残留上一个账号的菜单） */
const registered: string[] = []

function resolveComponent(componentPath: string | null): (() => Promise<Component>) | null {
  if (!componentPath) return null
  const loader = viewModules[`../views/${componentPath}.vue`]
  return loader ? (loader as () => Promise<Component>) : null
}

/**
 * 把一棵菜单树注册成路由：目录（M）不进路由，只做侧边栏分组；
 * C 菜单按「目录 path + 菜单 path」拼成全路径，挂到布局 Root 的 children 下。
 */
function addMenuRoutes(tree: MenuNode[]): void {
  for (const dir of tree || []) {
    for (const menu of dir.children || []) {
      const name = `Menu_${menu.id}`
      const component = resolveComponent(menu.component)
      router.addRoute('Root', {
        path: `${dir.path}/${menu.path}`.replace(/\/+/g, '/'),
        name,
        component: component || Placeholder,
        meta: {
          title: menu.name,
          icon: menu.icon,
          perms: menu.perms,
          dir: dir.name
        }
      })
      registered.push(name)
    }
  }
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    /** 动态路由是否已生成（刷新后重置，由路由守卫重新生成） */
    loaded: false,
    /**
     * 当前这批路由属于哪个用户。
     * 退出/被踢/换账号登录时 user store 会清、但菜单不会自动消失 ——
     * 守卫比对这个 id 就能判断「菜单是不是当前用户的」，避免显示上一个账号的菜单。
     */
    ownerId: 0,
    /** 原始菜单树，侧边栏渲染用 */
    menus: [] as MenuNode[]
  }),
  actions: {
    async generateRoutes() {
      const res = await getMenus()
      const tree = res.data || []
      // 先撤后建：重复 addRoute 同名路由会让 vue-router 告警且旧记录残留
      this.resetRoutes()
      this.menus = tree
      addMenuRoutes(tree)
      this.loaded = true
      this.ownerId = Number(useUserStore().info?.id ?? 0)
    },
    /** 撤掉已注册的动态路由并清空侧边栏数据（换账号 / 退出时调用） */
    resetRoutes() {
      while (registered.length) {
        const name = registered.pop()
        if (name && router.hasRoute(name)) {
          router.removeRoute(name)
        }
      }
      this.menus = []
    },
    reset() {
      this.resetRoutes()
      this.loaded = false
      this.ownerId = 0
    }
  }
})
