<template>
  <div class="login-page">
    <!-- 上方品牌区（居中；窄屏保留标题、隐藏装饰性脚注） -->
    <section class="login-brand">
      <div class="brand-logo">
        <el-icon :size="40"><Management /></el-icon>
      </div>
      <h1 class="brand-title">人力资源管理系统</h1>
      <p class="brand-sub">人事 · 培训 · 薪酬 · 报表 · 公共查询</p>
      <p class="brand-en">Human Resource Management System</p>
    </section>

    <!-- 下方登录卡片：指针在卡片区域移动时会轻微倾斜（跟随鼠标） -->
    <section
      class="login-form-side"
      @pointermove="handlePointerMove"
      @pointerleave="resetTilt"
    >
      <!-- 外层只负责「入场」动画，内层负责「浮动 + 倾斜」—— 分开才不会互相覆盖 transform -->
      <div class="card-wrap">
        <div ref="cardRef" class="login-card">
          <h2 class="form-title">欢迎登录</h2>
          <p class="form-desc">请输入账号密码后登录系统</p>

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
                placeholder="密码"
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
        </div>
      </div>
    </section>
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

/** 登录卡片 DOM：用于写入鼠标跟随倾斜的 CSS 变量 --rx / --ry */
const cardRef = ref<HTMLElement | null>(null)
/** 最大倾斜角度（度）：再大就会显得晃动、掉价 */
const MAX_TILT = 3

function prefersReducedMotion(): boolean {
  return typeof window !== 'undefined'
    && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

/** 指针移动 → 按指针在卡片内的相对位置算倾斜角，写进 CSS 变量 */
function handlePointerMove(e: PointerEvent) {
  const el = cardRef.value
  if (!el || prefersReducedMotion()) return
  const rect = el.getBoundingClientRect()
  const px = (e.clientX - rect.left) / rect.width  // 0 ~ 1
  const py = (e.clientY - rect.top) / rect.height  // 0 ~ 1
  el.style.setProperty('--rx', `${(-(py - 0.5) * 2 * MAX_TILT).toFixed(2)}deg`)
  el.style.setProperty('--ry', `${((px - 0.5) * 2 * MAX_TILT).toFixed(2)}deg`)
}

/** 指针离开 → 回正 */
function resetTilt() {
  const el = cardRef.value
  if (!el) return
  el.style.setProperty('--rx', '0deg')
  el.style.setProperty('--ry', '0deg')
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
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
/**
 * 登录页 ·「浅蓝氛围」清新风 · **上下单列居中 + 卡片动效**
 *
 * 布局：上方品牌区（居中）→ 下方登录卡片（居中）。单列让视觉动线是
 *   「品牌 → 标题 → 表单」一条竖线，比左右分栏更聚焦。
 *
 * 动画分四层，**各自挂在不同元素/伪元素上，避免 transform 互相覆盖**：
 *   ① `.card-wrap`        —— 入场：淡入 + 上移 + 轻微放大（只跑一次）
 *   ② `.login-card`        —— 鼠标跟随倾斜（`transform` + CSS 变量 --rx/--ry）；
 *                            **刻意不做常驻浮动**：卡片一直动会让人点不中输入框，
 *                            Playwright 直接报 element is not stable，也违反 WCAG 2.2.2
 *   ②-b `.login-card::after` —— 入场后一道高光扫过（伪元素自己动，不影响卡片命中区域）
 *   ③ `.login-page::before/::after` —— 背景两团光晕缓慢漂移
 *  另外品牌区四个元素 stagger 淡入、logo 一圈缓慢呼吸的光晕（动画 box-shadow，避开 transform），
 *  表单内部标题/说明/两个输入框/按钮再各自错峰淡入。
 *
 * ⚠️ 局部变量（氛围色/缓动/动画时长）只在本页使用，故定义在组件作用域内，不污染全局 tokens.css。
 */
.login-page {
  --login-glow-a: rgba(37, 99, 235, 0.16);
  --login-glow-b: rgba(14, 165, 233, 0.13);
  --login-bg-a: #eff6ff;
  --login-bg-b: #ffffff;
  --login-ink: #1e3a8a;
  --login-ease: cubic-bezier(0, 0, 0.2, 1);
  /* 入场用更"弹"的曲线：快出缓停，收尾有高级感 */
  --login-ease-out: cubic-bezier(0.22, 1, 0.36, 1);

  position: relative;
  overflow: hidden;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  /**
   * 刻意用 flex-start + 顶部偏移，而不是 justify-content: center：
   * 品牌区 + 卡片整列偏高（约 660px），垂直居中会把视觉重心压到下半屏，
   * 看起来"表单太靠下"。从 12vh 处起排，重心落在上 1/3，留白在下方更舒展。
   */
  justify-content: flex-start;
  gap: var(--hr-space-8);
  padding: clamp(56px, 12vh, 160px) var(--hr-space-8) var(--hr-space-8);
  background:
    radial-gradient(900px 620px at 14% 20%, var(--login-glow-a) 0%, transparent 58%),
    radial-gradient(760px 560px at 86% 80%, var(--login-glow-b) 0%, transparent 55%),
    linear-gradient(155deg, var(--login-bg-a) 0%, var(--login-bg-b) 62%);
}

/* 移动端浏览器地址栏会吃掉 100vh，支持 dvh 时优先 */
@supports (min-height: 100dvh) {
  .login-page {
    min-height: 100dvh;
  }
}

/* ---------- ③ 背景光晕漂移（常驻，慢速，制造"活跃感"） ---------- */
.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  z-index: 0;
  border-radius: 50%;
  pointer-events: none;
  will-change: transform;
}

.login-page::before {
  width: 46vw;
  height: 46vw;
  min-width: 380px;
  min-height: 380px;
  top: -12vw;
  left: -8vw;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.1) 0%, transparent 68%);
  animation: glow-drift-a 24s ease-in-out infinite alternate;
}

.login-page::after {
  width: 40vw;
  height: 40vw;
  min-width: 320px;
  min-height: 320px;
  right: -10vw;
  bottom: -14vw;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.09) 0%, transparent 68%);
  animation: glow-drift-b 30s ease-in-out infinite alternate;
}

@keyframes glow-drift-a {
  from {
    transform: translate3d(0, 0, 0) scale(1);
  }
  to {
    transform: translate3d(6vw, 4vh, 0) scale(1.12);
  }
}

@keyframes glow-drift-b {
  from {
    transform: translate3d(0, 0, 0) scale(1.05);
  }
  to {
    transform: translate3d(-5vw, -5vh, 0) scale(0.95);
  }
}

/* 内容压在光晕之上 */
.login-brand,
.login-form-side {
  position: relative;
  z-index: 1;
}

/* ---------------- 上方品牌区（居中） ---------------- */
.login-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  color: var(--login-ink);
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  margin-bottom: var(--hr-space-6);
  border-radius: var(--hr-radius-lg);
  background: var(--hr-primary-soft-2);
  color: var(--hr-primary);
}

.brand-title {
  margin: 0 0 var(--hr-space-3);
  font-size: var(--hr-font-3xl);
  font-weight: 600;
}

.brand-sub {
  margin: 0;
  font-size: var(--hr-font-lg);
  color: var(--hr-text-secondary);
}

.brand-en {
  margin: var(--hr-space-6) 0 0;
  font-size: var(--hr-font-xs);
  color: var(--hr-text-placeholder);
}

/* ---------------- 下方登录卡片 ---------------- */
.login-form-side {
  width: 100%;
  max-width: 440px;
}

/* ① 入场：淡入 + 上移 + 轻微放大，只跑一次 */
.card-wrap {
  animation: card-in 0.7s 0.12s var(--login-ease-out) both;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(28px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* ② 鼠标跟随倾斜：只响应指针移动，指针停下卡片就静止（不会一直晃）。
   ⚠️  deliberately 不做"常驻上下浮动"：卡片在动的时候用户点不中输入框
   （Playwright 直接报 element is not stable、连点 56 次点不中），
   而且违反 WCAG 2.2.2「超过 5s 的移动内容必须可暂停」。 */
.login-card {
  position: relative;
  overflow: hidden;
  padding: var(--hr-space-8);
  border: 1px solid var(--hr-border);
  border-radius: var(--hr-radius-lg);
  background: var(--hr-bg-card);
  box-shadow: var(--hr-shadow-md);
  transform: perspective(1000px) rotateX(var(--rx, 0deg)) rotateY(var(--ry, 0deg));
  transition: transform 0.35s var(--login-ease), box-shadow 0.35s var(--login-ease);
}

/* ②-b 入场后一道高光扫过卡片（只跑一次；伪元素自己动，不影响卡片本身的命中区域） */
.login-card::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(
    105deg,
    transparent 35%,
    rgba(255, 255, 255, 0.55) 50%,
    transparent 65%
  );
  transform: translateX(-130%);
  animation: card-sheen 1s 0.7s var(--login-ease-out) both;
}

@keyframes card-sheen {
  from {
    transform: translateX(-130%);
  }
  to {
    transform: translateX(130%);
  }
}

.form-title {
  margin: 0 0 var(--hr-space-2);
  font-size: var(--hr-font-2xl);
  font-weight: 600;
  color: var(--hr-text-primary);
}

.form-desc {
  margin: 0 0 var(--hr-space-6);
  font-size: var(--hr-font-sm);
  color: var(--hr-text-secondary);
}

/* 输入框：Apple 式填充态。
   EP 默认用 `box-shadow: 0 0 0 1px … inset` 画边，所以必须覆盖 box-shadow 而不是 border。 */
.login-card :deep(.el-input__wrapper) {
  height: 52px;
  padding: 0 16px;
  border-radius: var(--hr-radius);
  background: var(--hr-bg-sunken);
  box-shadow: none;
  transition:
    background-color 0.2s var(--login-ease),
    box-shadow 0.2s var(--login-ease);
}

.login-card :deep(.el-input__wrapper:hover) {
  background: var(--hr-primary-soft);
  box-shadow: none;
}

.login-card :deep(.el-input__wrapper.is-focus) {
  background: var(--hr-bg-card);
  box-shadow: 0 0 0 1.5px var(--hr-primary) inset;
}

.login-card :deep(.el-input__inner) {
  font-size: var(--hr-font-lg);
}

/* 表单内部错峰淡入（在卡片入场落定后开始） */
.form-title,
.form-desc {
  animation: fade-up 0.5s var(--login-ease-out) both;
}

.form-title {
  animation-delay: 0.45s;
}

.form-desc {
  animation-delay: 0.55s;
}

.login-card :deep(.el-form-item) {
  margin-bottom: var(--hr-space-6);
  animation: fade-up 0.5s var(--login-ease-out) both;
}

.login-card :deep(.el-form-item:nth-of-type(1)) {
  animation-delay: 0.65s;
}

.login-card :deep(.el-form-item:nth-of-type(2)) {
  animation-delay: 0.75s;
}

/* 登录按钮：页面唯一饱和色块，hover 上浮 + 主色光晕。
   ⚠️ 这里只动画 opacity、不用 fade-up：fade-up 动画的是 transform，
   fill-mode:both 的终值会把 hover 的 translateY 覆盖掉，按钮就"点不动"了。 */
.submit {
  width: 100%;
  height: 50px;
  margin-top: var(--hr-space-3);
  border-radius: var(--hr-radius);
  font-size: var(--hr-font-lg);
  font-weight: 600;
  letter-spacing: 4px;
  /* letter-spacing 会在末字后也留一份间距导致视觉偏左，用等量 text-indent 补回居中 */
  text-indent: 4px;
  animation: fade-in 0.5s 0.9s var(--login-ease-out) both;
  transition:
    transform 0.15s var(--login-ease),
    box-shadow 0.2s var(--login-ease);
}

.submit:hover {
  transform: translateY(-2px);
  box-shadow: var(--hr-shadow-primary);
}

.submit:active {
  transform: translateY(0) scale(0.99);
}

/* ---------------- 品牌区 stagger 淡入 ---------------- */
.brand-logo,
.brand-title,
.brand-sub,
.brand-en {
  animation: fade-up 0.6s var(--login-ease-out) both;
}

.brand-title {
  animation-delay: 0.2s;
}

.brand-sub {
  animation-delay: 0.3s;
}

.brand-en {
  animation-delay: 0.4s;
}

/**
 * logo 额外加一圈缓慢呼吸的光晕。
 * ⚠️ 故意动画 `box-shadow` 而不是 `transform`：入场动画 fade-up 占着 transform，
 * 同一个属性上叠两条动画会互相覆盖（后声明者胜），必须错开属性。
 */
.brand-logo {
  animation:
    fade-up 0.6s var(--login-ease-out) both,
    logo-glow 4.5s ease-in-out 0.6s infinite alternate;
}

@keyframes logo-glow {
  from {
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0);
  }
  to {
    box-shadow: 0 0 0 12px rgba(37, 99, 235, 0.07);
  }
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 尊重系统"减少动态效果"设置：全部动画/过渡关掉，倾斜也不再响应 */
@media (prefers-reduced-motion: reduce) {
  .login-page *,
  .login-page *::before,
  .login-page *::after {
    animation: none !important;
    transition: none !important;
  }
}

/* ---------------- 窄屏：压缩间距 ---------------- */
@media (max-width: 600px) {
  .login-page {
    gap: var(--hr-space-6);
    padding: clamp(40px, 10vh, 96px) var(--hr-space-4) var(--hr-space-4);
  }

  .login-card {
    padding: var(--hr-space-6);
  }

  .brand-en {
    display: none;
  }
}
</style>
