<template>
  <div class="placeholder">
    <el-empty description="页面建设中">
      <template #description>
        <p class="title">{{ title }}</p>
        <p class="sub">该菜单对应的页面将在后续批次实现，后端接口已就绪。</p>
      </template>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => (route.meta.title as string) || '页面')
</script>

<style scoped>
.placeholder {
  background: #fff;
  border-radius: 4px;
  padding: 48px 0;
}

.title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.sub {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
</style>
