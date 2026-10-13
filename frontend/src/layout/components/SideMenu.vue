<template>
  <el-menu
    class="side-menu"
    :default-active="activeMenu"
    background-color="#001529"
    text-color="#c0c4cc"
    active-text-color="#ffffff"
    router
  >
    <!-- 工作台是静态路由，不来自菜单树 -->
    <el-menu-item index="/dashboard">
      <el-icon><HomeFilled /></el-icon>
      <span>工作台</span>
    </el-menu-item>

    <!-- 业务菜单：目录(M) → 菜单(C) 两层，数据来自 /api/auth/menus -->
    <el-sub-menu
      v-for="dir in menus"
      :key="dir.id"
      :index="dir.path"
    >
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
        <span>{{ menu.name }}</span>
      </el-menu-item>
    </el-sub-menu>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { usePermissionStore } from '@/store/permission'

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
/* 宽度交给 layout 的 el-aside 控制；本项目侧边栏不做折叠 */
.side-menu {
  border-right: none;
}
</style>
