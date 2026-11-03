import axios, { type AxiosRequestConfig } from 'axios'

interface ApiBody<T> {
  code: number
  msg: string
  data: T
}

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

const http = axios.create({ baseURL: API_BASE, timeout: 30000 })

const AUTH_KEYS = [
  'admin_token', 'admin_user', 'admin_perms', 'admin_menus',
  'admin_role_code', 'admin_role_name', 'admin_must_change'
]

function redirectToLogin() {
  const onLoginPage = window.location.hash.startsWith('#/login')
  AUTH_KEYS.forEach((key) => localStorage.removeItem(key))
  if (!onLoginPage) {
    window.location.replace(`${import.meta.env.BASE_URL}#/login`)
    window.location.reload()
  }
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiBody<unknown>
    if (body && typeof body.code === 'number') {
      if (body.code === 200) {
        return body.data as never
      }
      if (body.code === 401) {
        redirectToLogin()
      }
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return response.data as never
  },
  (error: { response?: { status?: number; data?: ApiBody<unknown> } }) => {
    const status = error.response?.status
    const msg = error.response?.data?.msg || (status === 403 ? '没有操作权限' : '网络异常，请稍后重试')
    if (status === 401) {
      redirectToLogin()
    }
    return Promise.reject(new Error(msg))
  }
)

export function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return http.get(url, { params }) as unknown as Promise<T>
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return http.post(url, data, config) as unknown as Promise<T>
}

export function put<T>(url: string, data?: unknown): Promise<T> {
  return http.put(url, data) as unknown as Promise<T>
}

export function del<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return http.delete(url, { params }) as unknown as Promise<T>
}

export async function upload<T>(url: string, file: File, extra?: Record<string, string>): Promise<T> {
  const form = new FormData()
  form.append('file', file)
  Object.entries(extra || {}).forEach(([key, value]) => form.append(key, value))
  return http.post(url, form, { headers: { 'Content-Type': 'multipart/form-data' } }) as unknown as Promise<T>
}

export async function download(url: string, params?: Record<string, unknown>, fallback = 'export.xlsx'): Promise<void> {
  const query = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.append(key, String(value))
  })
  const token = localStorage.getItem('admin_token')
  const response = await fetch(`${API_BASE}${url}${query.size ? `?${query.toString()}` : ''}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (response.status === 401) {
    redirectToLogin()
    throw new Error('登录已过期，请重新登录')
  }
  if (!response.ok) {
    throw new Error('导出失败，请稍后重试')
  }
  const blob = await response.blob()
  const disposition = response.headers.get('content-disposition') || ''
  let filename = fallback
  const match = /filename\*=utf-8''([^;]+)/i.exec(disposition)
  if (match) filename = decodeURIComponent(match[1])
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}

export default http
