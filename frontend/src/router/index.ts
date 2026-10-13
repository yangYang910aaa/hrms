import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'

/**
 * 静态路由：登录页 / 工作台 / 404。
 *
 * 业务菜单（/system/**、/hr/** 等）**不在这里写死** —— 登录后从
 * GET /api/auth/menus 拉当前角色的菜单树，由 store/permission.ts 动态
 * addRoute 进来（按钮 F 不进树，只贡献给 v-permission 的 perms）。
 */
const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', noLayout: true }
  },
  {
    // 布局骨架：侧边栏 + 顶栏 + 内容区。动态菜单与工作台都挂它的 children 下
    path: '/',
    name: 'Root',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'HomeFilled', affix: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', noLayout: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes
})

/**
 * 全局守卫：
 * 1. 无 token → 一律去登录页（带上 redirect，登录后跳回原页面）；
 * 2. 有 token 但 store 是空的（刷新页面后 pinia state 丢失）→ 先拉 /api/auth/me 补回用户信息；
 * 3. 动态菜单没生成过 → 拉 /api/auth/menus 并 addRoute，然后 replace 重新进一次守卫
 *    （不 replace 的话本次导航的目标组件可能还没注册，白屏）；
 * 4. 已登录再访问 /login → 回首页。
 */
router.beforeEach(async (to) => {
  const token = getToken()
  if (to.path === '/login') {
    return token ? { path: '/' } : true
  }
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const userStore = useUserStore()
  if (!userStore.info) {
    try {
      await userStore.loadProfile()
    } catch {
      // token 失效/被踢：清掉回登录页
      userStore.logout()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  const permissionStore = usePermissionStore()
  // ownerId：菜单是不是当前用户的？退出后再用另一个账号登录，
  // user store 已清但菜单还在 —— 不比对就会显示上一个账号的菜单
  const ownerId = Number(userStore.info?.id ?? 0)
  if (!permissionStore.loaded || permissionStore.ownerId !== ownerId) {
    await permissionStore.generateRoutes()
    // 路由刚注册完，本次导航的目标组件可能还没匹配上 —— replace 重进一次守卫
    return { path: to.fullPath, replace: true }
  }
  return true
})

/** 文档标题：取当前路由 meta.title */
router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 人力资源管理系统` : '人力资源管理系统'
})

/** 路由 meta 的扩展字段（声明合并，让 to.meta.title 等有类型） */
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** 后端 icon / perms 列都可能为 null，故显式允许 */
    icon?: string | null
    perms?: string | null
    noLayout?: boolean
    affix?: boolean
    /** 所属目录名（动态菜单路由携带，面包屑用） */
    dir?: string
  }
}

export default router
