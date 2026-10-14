import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 中文语言包：不配的话分页会显示英文（Total / Go to）、日期选择器也是英文
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
// 全局样式必须在 element-plus 的 css 之后引入：里面覆盖了 EP 的主题变量（--el-color-primary 等）
import './styles/index.css'
import { setupPermission } from './directives/permission'

const app = createApp(App)

// 图标全局注册：布局与各页面里直接 <el-icon><User /></el-icon> 用
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// v-permission 指令：按钮级权限控制（数据源 = /api/auth/me 返回的 perms）
setupPermission(app)

// 必须等路由就绪再挂载：动态菜单是守卫里异步 addRoute 的，
// 不等的话刷新子页面时首屏会先渲染一次 404 再跳到真实页面
router.isReady().then(() => {
  app.mount('#app')
})
