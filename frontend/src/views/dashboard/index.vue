<template>
  <div class="dashboard">
    <el-card class="welcome">
      <div class="welcome-inner">
        <el-avatar :size="56" icon="UserFilled" />
        <div class="welcome-text">
          <h2>{{ greeting }}，{{ realName || username }}</h2>
          <p class="sub">欢迎使用人力资源管理系统 · 当前角色：{{ rolesText }}</p>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat">
            <div class="stat-num">{{ menuCount }}</div>
            <div class="stat-label">可用菜单</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat">
            <div class="stat-num">{{ perms.length }}</div>
            <div class="stat-label">权限点</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat">
            <div class="stat-num">{{ isSuperAdmin ? '全部' : menuCount }}</div>
            <div class="stat-label">可见范围</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="notice">
      <template #header>
        <span>系统说明</span>
      </template>
      <ul class="desc">
        <li>左侧菜单按当前角色动态生成，按钮级权限由后端权限点控制（<code>v-permission</code>）。</li>
        <li>薪酬模块支持按月份批量计算工资档案并生成发放单，已发放的记录不可改动。</li>
        <li>接口文档见后端 <code>/doc.html</code>（Knife4j）。</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'

const userStore = useUserStore()
const permissionStore = usePermissionStore()

const realName = computed(() => userStore.realName)
const username = computed(() => userStore.info?.username || '')
const rolesText = computed(() => {
  const roles = userStore.roles || []
  return roles.length ? roles.join('、') : '无'
})
const perms = computed(() => userStore.perms)
const isSuperAdmin = computed(() => (userStore.roles || []).includes('SUPER_ADMIN'))
const menuCount = computed(() =>
  permissionStore.menus.reduce((sum, dir) => sum + (dir.children?.length || 0), 0)
)
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
</script>

<style scoped>
.welcome-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome-text h2 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #303133;
}

.sub {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.stat-row {
  margin-top: 16px;
}

.stat {
  text-align: center;
  padding: 8px 0;
}

.stat-num {
  font-size: 28px;
  font-weight: 600;
  color: #409eff;
}

.stat-label {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.notice {
  margin-top: 16px;
}

.desc {
  margin: 0;
  padding-left: 20px;
  color: #606266;
  font-size: 13px;
  line-height: 2;
}

code {
  background: #f4f4f5;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 12px;
  color: #c0341d;
}
</style>
