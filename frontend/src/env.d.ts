/// <reference types="vite/client" />

/**
 * .vue 单文件组件的类型声明（vue-tsc + Volar 下其实能自动识别，
 * 这里补一份让 `pnpm type-check` 与 IDE 在任何配置下都不报红）。
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>
  export default component
}
