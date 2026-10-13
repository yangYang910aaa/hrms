<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="22"><Management /></el-icon>
        <span>人力资源管理系统</span>
      </div>
      <side-menu />
    </el-aside>

    <el-container class="main-wrap">
      <el-header class="header" height="56px">
        <div class="page-title">{{ currentTitle }}</div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-trigger">
              <el-avatar :size="28" icon="UserFilled" />
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import SideMenu from './components/SideMenu.vue'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'
import { logout } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const realName = computed(() => userStore.realName)
const rolesText = computed(() => {
  const roles = userStore.roles || []
  return roles.length ? `角色：${roles.join('、')}` : '无角色'
})
const currentTitle = computed(() => route.meta.title || '工作台')

async function onCommand(command: string | number | object) {
  if (command !== 'logout') return
  await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
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

.aside {
  background: #001529;
  color: #fff;
  overflow-x: hidden;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  white-space: nowrap;
  overflow: hidden;
}

.main-wrap {
  min-width: 0;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 20px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-name {
  font-size: 14px;
  color: #606266;
}

.main {
  background: #f0f2f5;
  padding: 16px;
}
</style>
