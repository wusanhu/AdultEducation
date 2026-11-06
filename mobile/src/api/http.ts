import axios from 'axios'
import { showToast } from 'vant'

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

/** 学习会话失效事件：由学习页监听并弹窗退出，避免只弹 toast 后停留在失效页面 */
export const STUDY_SESSION_INVALID_EVENT = 'study-session-invalid'
const STUDY_SESSION_INVALID_MSG = '学习会话已失效'

function isStudySessionInvalid(code: unknown, msg: unknown): boolean {
  return Number(code) === 409 && typeof msg === 'string' && msg.includes(STUDY_SESSION_INVALID_MSG)
}

const http = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
})

function redirectToLogin() {
  const onLoginPage = window.location.hash.startsWith('#/login')
  localStorage.removeItem('mobile_token')
  localStorage.removeItem('mobile_user')
  if (!onLoginPage) {
    window.location.replace(`${import.meta.env.BASE_URL}#/login`)
    window.location.reload()
  }
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('mobile_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      redirectToLogin()
      return Promise.reject(new Error(res.msg || '登录已过期'))
    }
    // 仅会话失效这一种 409 交给学习页弹窗处理，其余错误仍走 toast
    if (isStudySessionInvalid(res.code, res.msg)) {
      window.dispatchEvent(new Event(STUDY_SESSION_INVALID_EVENT))
      return Promise.reject(new Error(res.msg))
    }
    showToast(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      redirectToLogin()
    } else if (isStudySessionInvalid(error.response?.status, error.response?.data?.msg)) {
      window.dispatchEvent(new Event(STUDY_SESSION_INVALID_EVENT))
    } else {
      showToast(error.response?.data?.msg || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default http

export function get<T = any>(url: string, params?: any): Promise<T> {
  return http.get(url, { params }) as any
}

/**
 * 私有附件（证书、核验抓拍）需要登录态，img/a 标签无法携带请求头，改用 token 查询参数。
 */
export function fileUrl(url?: string | null): string {
  if (!url) return ''
  if (/^(https?:|data:|blob:)/i.test(url)) return url
  const token = localStorage.getItem('mobile_token')
  if (!token) return url
  return `${url}${url.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}`
}

export function post<T = any>(url: string, data?: any): Promise<T> {
  return http.post(url, data) as any
}

export function put<T = any>(url: string, data?: any): Promise<T> {
  return http.put(url, data) as any
}

export function del<T = any>(url: string, params?: any): Promise<T> {
  return http.delete(url, { params }) as any
}

export function upload<T = any>(url: string, file: File, type = 'common'): Promise<T> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  return http.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }) as any
}
