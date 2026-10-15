<template>
  <el-container class="layout">
    <!-- 侧边栏：浅色（白底 + 右侧 1px 分割线），可折叠 232px ↔ 64px -->
    <el-aside class="aside" :class="{ 'is-collapsed': collapsed }">
      <div class="logo">
        <div class="logo-mark">
          <el-icon :size="20"><Management /></el-icon>
        </div>
        <span v-show="!collapsed" class="logo-text">人力资源管理系统</span>
      </div>
      <side-menu :collapsed="collapsed" />
    </el-aside>

    <el-container class="main-wrap">
      <el-header class="header">
        <div class="header-left">
          <el-tooltip :content="collapsed ? '展开菜单' : '折叠菜单'" placement="bottom">
            <div
              class="collapse-btn"
              role="button"
              tabindex="0"
              :aria-label="collapsed ? '展开菜单' : '折叠菜单'"
              @click="toggleCollapsed"
              @keyup.enter="toggleCollapsed"
              @keyup.space="toggleCollapsed"
            >
              <el-icon :size="18">
                <Fold v-if="!collapsed" />
                <Expand v-else />
              </el-icon>
            </div>
          </el-tooltip>

          <el-breadcrumb class="breadcrumb" separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="dirName">{{ dirName }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <span class="user-divider" />
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-trigger">
              <el-avatar :size="28" :src="avatar || undefined" />
              <span class="user-name">{{ realName || '未登录' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  {{ rolesText }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SideMenu from './components/SideMenu.vue'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'
import { logout } from '@/api/auth'
import { confirmAction } from '@/utils/confirm'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

/**
 * 侧边栏折叠：两套来源叠加（UI 阶段 4 响应式）
 *  - `userCollapsed`：用户在**宽屏**下手动切换的偏好，存 localStorage 长期保持；
 *  - `isNarrow`：窗口 ≤ `NARROW_MAX` 时**自动折叠成 64px 图标栏**，把宽度让给内容区
 *    （窄屏下宽表本来就被 fixed 列挤，这一下能多出 ~170px）。
 * ⚠️ 自动折叠**不写回 localStorage** —— 否则会把用户的宽屏偏好污染成"折叠"。
 * ⚠️ 窄屏下仍允许手动展开（`narrowOverride` 临时覆盖），跨过断点即失效、回到跟随规则。
 */
const STORAGE_KEY = 'hr-sidebar-collapsed'
const NARROW_MAX = 1199
const userCollapsed = ref(localStorage.getItem(STORAGE_KEY) === '1')
watch(userCollapsed, (v) => localStorage.setItem(STORAGE_KEY, v ? '1' : '0'))

const isNarrow = ref(false)
const narrowOverride = ref<boolean | null>(null)
const collapsed = computed(() =>
  isNarrow.value ? (narrowOverride.value ?? true) : userCollapsed.value
)

function toggleCollapsed() {
  if (isNarrow.value) narrowOverride.value = !collapsed.value
  else userCollapsed.value = !userCollapsed.value
}

let mql: MediaQueryList | null = null
function syncNarrow(e: MediaQueryList | MediaQueryListEvent) {
  isNarrow.value = e.matches
  narrowOverride.value = null // 跨断点时丢弃临时覆盖
}

onMounted(() => {
  mql = window.matchMedia(`(max-width: ${NARROW_MAX}px)`)
  syncNarrow(mql)
  mql.addEventListener('change', syncNarrow)
})

onBeforeUnmount(() => mql?.removeEventListener('change', syncNarrow))

const realName = computed(() => userStore.realName)
const avatar = computed(() => userStore.avatar)
const rolesText = computed(() => {
  const roles = userStore.roles || []
  return roles.length ? `角色：${roles.join('、')}` : '无角色'
})

// 面包屑：目录名与页面名都在 store/permission.ts 注册路由时写进 meta
const dirName = computed(() => (route.meta.dir as string) || '')
const title = computed(() => (route.meta.title as string) || '工作台')

async function onCommand(command: string | number | object) {
  if (command !== 'logout') return
  try {
    await confirmAction('确认退出登录？')
  } catch {
    return // 用户取消 —— 原来这里没接住 reject，会抛一条未处理的 Promise rejection
  }
  try {
    await logout()
  } catch {
    // 服务端注销失败也继续走本地清理（token 已失效时后端同样返回业务错误）
  }
  permissionStore.reset() // 先撤动态路由，避免下个账号看到上一个账号的菜单
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

/* ---------- 侧边栏 ---------- */
.aside {
  width: var(--hr-sidebar-width);
  background: var(--hr-bg-card);
  border-right: 1px solid var(--hr-border);
  overflow-x: hidden;
  transition: width var(--hr-duration) var(--hr-ease);
}

.aside.is-collapsed {
  width: var(--hr-sidebar-collapsed-width);
}

.logo {
  height: var(--hr-header-height);
  display: flex;
  align-items: center;
  gap: var(--hr-space-2);
  padding: 0 var(--hr-space-4);
  border-bottom: 1px solid var(--hr-border);
  white-space: nowrap;
  overflow: hidden;
}

/* logo 图标盒：与登录页 brand-logo 同一语言（主色浅底 + 主色图标） */
.logo-mark {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--hr-radius);
  background: var(--hr-primary-soft-2);
  color: var(--hr-primary);
}

.logo-text {
  font-size: var(--hr-font-lg);
  font-weight: 600;
  color: var(--hr-text-primary);
}

.main-wrap {
  min-width: 0;
}

/* ---------- 顶栏 ---------- */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--hr-space-4);
  height: var(--hr-header-height);
  padding: 0 var(--hr-space-4);
  background: var(--hr-bg-card);
  border-bottom: 1px solid var(--hr-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--hr-space-3);
  min-width: 0;
}

.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 32px;
  height: 32px;
  border-radius: var(--hr-radius);
  color: var(--hr-text-regular);
  cursor: pointer;
  outline: none;
  transition:
    background-color 0.2s var(--hr-ease),
    color 0.2s var(--hr-ease);
}

.collapse-btn:hover {
  background: var(--hr-bg-hover);
  color: var(--hr-primary);
}

.collapse-btn:focus-visible {
  box-shadow: 0 0 0 2px var(--hr-primary-soft-2);
}

/* 面包屑：前两级弱化，最后一级（当前页）加重 */
.breadcrumb {
  font-size: var(--hr-font-sm);
}

.breadcrumb :deep(.el-breadcrumb__inner) {
  color: var(--hr-text-secondary);
  font-weight: 400;
}

.breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--hr-text-primary);
  font-weight: 500;
}

.breadcrumb :deep(.el-breadcrumb__separator) {
  color: var(--hr-text-placeholder);
}

/* ---------- 用户区 ---------- */
.header-right {
  display: flex;
  align-items: center;
  gap: var(--hr-space-3);
}

/* 用户区左侧分隔线，把"账户操作"和"页面导航"在视觉上分开 */
.user-divider {
  width: 1px;
  height: 20px;
  background: var(--hr-border);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: var(--hr-space-2);
  padding: var(--hr-space-1) var(--hr-space-2);
  border-radius: var(--hr-radius);
  cursor: pointer;
  outline: none;
  transition: background-color 0.2s var(--hr-ease);
}

.user-trigger:hover {
  background: var(--hr-bg-hover);
}

.user-name {
  font-size: var(--hr-font-sm);
  color: var(--hr-text-regular);
}

/* ---------- 内容区 ---------- */
/* ⚠️ 这个 padding 是**非列表页**（dashboard / placeholder / 404）的内边距来源 ——
   它们根类不是 .page-container，靠这里撑开。列表页已收敛到 .page-container（自带 16px），
   由全局规则 `.layout .main:has(> .page-container){padding:0}` 让出；
   **那条规则长期保留、不要删**（结论见 交接文档 §9「阶段 3」的内边距一节）。 */
.main {
  background: var(--hr-bg-page);
  padding: var(--hr-space-4);
}

/* 窄屏（阶段 4 响应式）：内容区内边距收窄一档，把宽度让给表格 */
@media (max-width: 900px) {
  .main {
    padding: var(--hr-space-3);
  }
}
</style>
