<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="32" color="#409eff"><Management /></el-icon>
        <h1>人力资源管理系统</h1>
        <p class="subtitle">Vue 3 + TypeScript + Element Plus + Spring Boot</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="onSubmit"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="账号" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="口令"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-button
          class="submit"
          type="primary"
          size="large"
          :loading="loading"
          @click="onSubmit"
        >
          登 录
        </el-button>
      </el-form>

      <p class="tip">演示账号：admin / ***REMOVED***（超级管理员，可见全部菜单）</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入口令', trigger: 'blur' }]
}

async function onSubmit() {
  if (!formRef.value) return
  // 用 Promise 形式：validate(callback) 里的异步错误不在 await 链上，会漏成未处理的 rejection
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const res = await login({ username: form.username.trim(), password: form.password })
    // 登录响应自带 userInfo（含 roles/perms），不必再查一次 me
    userStore.login(res.data.tokenValue, res.data.userInfo)
    // 拉菜单树 → 注册动态路由，再进首页（守卫里因此不会重复生成一次）
    await permissionStore.generateRoutes()
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch {
    // 错误消息由 axios 拦截器统一弹出（含"输错 3 次锁定 5 分钟"等后端提示）
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #001529 100%);
}

.login-card {
  width: 380px;
  padding: 36px 32px 28px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-header h1 {
  margin: 8px 0 4px;
  font-size: 20px;
  color: #303133;
}

.subtitle {
  font-size: 12px;
  color: #909399;
}

.submit {
  width: 100%;
}

.tip {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: #909399;
}
</style>
