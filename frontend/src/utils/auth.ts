/**
 * 登录态 token 的本地存储。
 *
 * 与后端约定（交接文档 §5）：token 放 localStorage，请求头名 satoken
 * （头名以后端 sa-token.token-name 为准，当前配置就是 satoken）。
 */
const TOKEN_KEY = 'hr_token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}
