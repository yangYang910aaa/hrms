<template>
  <div class="not-found">
    <div class="code">404</div>
    <p class="msg">页面不存在或已被移除</p>
    <el-button type="primary" @click="goHome">返回工作台</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.replace('/')
}
</script>

<style scoped>
.not-found {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: #f0f2f5;
}

.code {
  font-size: 72px;
  font-weight: 700;
  color: #409eff;
  line-height: 1;
}

.msg {
  margin: 0 0 8px;
  color: #909399;
  font-size: 14px;
}
</style>
