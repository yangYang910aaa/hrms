<template>
  <el-menu
    class="side-menu"
    :class="{ 'is-collapsed': collapsed }"
    :default-active="activeMenu"
    :collapse="collapsed"
    :collapse-transition="false"
    unique-opened
    router
  >
    <!-- 工作台是静态路由，不来自菜单树 -->
    <el-menu-item index="/dashboard">
      <el-icon><HomeFilled /></el-icon>
      <!-- 折叠态 tooltip 的来源：文字必须放在 #title 里，直接写在插槽外折叠后会没有提示 -->
      <template #title>工作台</template>
    </el-menu-item>

    <!-- 业务菜单：目录(M) → 菜单(C) 两层，数据来自 /api/auth/menus -->
    <el-sub-menu v-for="dir in menus" :key="dir.id" :index="dir.path">
      <template #title>
        <el-icon><component :is="dir.icon || 'Folder'" /></el-icon>
        <span>{{ dir.name }}</span>
      </template>
      <el-menu-item
        v-for="menu in dir.children || []"
        :key="menu.id"
        :index="fullPath(dir.path, menu.path)"
      >
        <el-icon><component :is="menu.icon || 'Document'" /></el-icon>
        <template #title>{{ menu.name }}</template>
      </el-menu-item>
    </el-sub-menu>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { usePermissionStore } from '@/store/permission'

defineProps<{ collapsed: boolean }>()

const route = useRoute()
const permissionStore = usePermissionStore()

const menus = computed(() => permissionStore.menus)

// 当前高亮：完整路径（el-menu 的 router 模式会拿 index 当跳转路径）
const activeMenu = computed(() => route.path)

/** 与 store/permission.ts 注册路由时的拼接规则保持一致，否则高亮会错位 */
function fullPath(dirPath: string, menuPath: string): string {
  return `${dirPath}/${menuPath}`.replace(/\/+/g, '/')
}
</script>

<style scoped>
/**
 * 浅色侧边栏（白底 + 右侧 1px 分割线，与登录页同一套浅色语言）。
 *
 * ⚠️ 主题变量为什么写在 `.side-menu`（= el-menu 根元素）上而不是 :root：
 *   EP 把 --el-menu-* 声明在 :root，而「元素自身的声明值恒胜过从祖先继承来的值」——
 *   写在这些组件自己的选择器上即可覆盖，同时不污染全局令牌（本项目菜单只此一处）。
 */
.side-menu {
  --el-menu-bg-color: var(--hr-bg-card);
  --el-menu-text-color: var(--hr-text-regular);
  --el-menu-hover-bg-color: var(--hr-bg-hover);
  --el-menu-active-color: var(--hr-primary);
  --el-menu-item-height: 48px;
  --el-menu-item-font-size: var(--hr-font-base);

  /* EP 默认给菜单加右侧竖线，浅色侧边栏用 .aside 的分割线就够了 */
  border-right: none;
}

/* ---------- 选中态：主色浅底 + 左侧 3px 主色强调条 ---------- */
/* EP 的 .el-menu-item.is-active 只改 color、不改背景，背景要自己给。
   注意 EP 有条 `color: …!important` 的规则，但只作用于 el-menu--horizontal，竖向侧边栏不受影响。 */
.side-menu :deep(.el-menu-item) {
  position: relative;
  margin: 2px var(--hr-space-2);
  border-radius: var(--hr-radius);
}

.side-menu :deep(.el-menu-item.is-active) {
  background: var(--hr-primary-soft);
  font-weight: 500;
}

.side-menu :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--hr-primary);
}

/* 折叠态（64px）里强调条会挤得突兀，隐藏 */
.side-menu.is-collapsed :deep(.el-menu-item.is-active)::before {
  display: none;
}

/* ---------- hover ---------- */
.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  background: var(--hr-bg-hover);
  color: var(--hr-text-primary);
}

/* ---------- 子菜单标题 ---------- */
.side-menu :deep(.el-sub-menu__title) {
  margin: 2px var(--hr-space-2);
  border-radius: var(--hr-radius);
  font-size: var(--hr-font-base);
}

/* 子菜单展开时给个浅底，和选中项区分开 */
.side-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: var(--hr-text-primary);
}

/* 图标与文字间距统一 */
.side-menu :deep(.el-menu-item .el-icon),
.side-menu :deep(.el-sub-menu__title .el-icon) {
  margin-right: var(--hr-space-2);
}
</style>
