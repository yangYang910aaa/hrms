import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端约定：端口 8080、接口统一前缀 /api；职工照片是 /uploads 下的相对 URL。
// 这两个前缀都必须代理过去，否则 <img src="/uploads/..."> 在前端域下会 404。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    // 全量引入 Element Plus 会把主包撑到 1.2MB：把框架与 UI 库拆成独立 chunk，
    // 业务代码改动时这两个包能命中浏览器缓存，首屏也少下一遍
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          element: ['element-plus', '@element-plus/icons-vue']
        }
      }
    },
    // Element Plus 全量引入 ≈1.05MB（gzip 后 ~330KB），课设规模下首屏完全可接受；
    // 已拆成独立 chunk，业务代码改动不会让它重新下载。真要瘦身再换按需引入
    chunkSizeWarningLimit: 1200
  }
})
