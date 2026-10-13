import axios, {
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'
import router from '@/router'
import type { ApiResult } from '@/types/api'

/**
 * axios 实例：所有业务接口走这里。
 *
 * 与后端契约（交接文档 §5，写死的三条）：
 * 1. baseURL '/api' —— vite.config.ts 已代理到 8080；
 * 2. 请求头 `satoken` 携带 token（头名 = 后端 sa-token.token-name）；
 * 3. **HTTP 恒 200，成败只看响应体 code** —— 拦截器校验 code：成功把信封交回
 *    调用方，失败统一弹错误消息。401（未登录/被踢）→ 清 token 回登录页。
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers['satoken'] = token
  }
  return config
})

service.interceptors.response.use(
  (res: AxiosResponse<ApiResult>) => {
    const body = res.data
    if (body && body.code === 200) {
      return res
    }
    // 业务失败：HTTP 仍是 200，走这里
    if (body && body.code === 401) {
      removeToken()
      const current = router.currentRoute.value
      if (!current.path.startsWith('/login')) {
        router.push({ path: '/login', query: { redirect: current.fullPath } })
      }
    }
    ElMessage.error((body && body.msg) || '操作失败')
    return Promise.reject(new Error((body && body.msg) || '操作失败'))
  },
  (err) => {
    ElMessage.error(err.message || '网络异常，请稍后重试')
    return Promise.reject(err)
  }
)

/**
 * 拆包后的请求助手：统一返回 Promise<ApiResult<T>>（信封），
 * 调用方拿 res.data 即业务数据。拦截器已保证只有 code=200 才会 resolve。
 */
const request = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    service.get<ApiResult<T>>(url, config).then((r) => r.data),
  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    service.post<ApiResult<T>>(url, data, config).then((r) => r.data),
  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    service.put<ApiResult<T>>(url, data, config).then((r) => r.data),
  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    service.delete<ApiResult<T>>(url, config).then((r) => r.data)
}

export default request
