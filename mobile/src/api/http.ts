import axios from 'axios'
import { showToast } from 'vant'

const http = axios.create({
  baseURL: '/api',
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
    showToast(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      redirectToLogin()
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
