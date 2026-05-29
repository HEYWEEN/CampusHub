import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { useAuthStore } from '../stores/auth'
import { BizError } from '../types/api'

/**
 * Axios 实例 + 拦截器
 * — request: 自动注入 Bearer token
 * — response: 解包 ApiResponse<T>，业务错码 throw BizError
 * — 401: 清登录态 + 跳 /login
 *
 * baseURL：
 *   开发：通过 Vite proxy 把 /api 转给后端 8080（见 vite.config.ts）
 *   生产：VITE_API_BASE_URL 环境变量
 */

const baseURL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? ''

const instance = axios.create({
  baseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

instance.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => {
    const body = response.data
    // 标准 ApiResponse 解包
    if (body && typeof body === 'object' && 'code' in body && typeof body.code === 'number') {
      if (body.code === 0 || body.code === 200) {
        // 把 data 字段提到 response.data，便于 wrapper 函数统一返回
        response.data = body.data
        return response
      }
      // HTTP 2xx 但业务 code 非 0：当作 200 响应下的业务错（httpStatus = 200）
      throw new BizError(body.code, body.message ?? '未知错误', response.status)
    }
    return response
  },
  (error: AxiosError<{ code?: number; message?: string }>) => {
    if (error.response?.status === 401) {
      // token 失效或越权
      useAuthStore.getState().logout()
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    const code = error.response?.data?.code ?? error.response?.status ?? 0
    const message = error.response?.data?.message ?? error.message ?? '网络错误'
    // httpStatus 给 withMock 判断用：undefined = 网络/无响应，>= 500 才 fallback
    return Promise.reject(new BizError(code, message, error.response?.status))
  },
)

/** Typed wrapper — interceptor 已 unwrap，调用方拿到 data */
export async function apiGet<T = unknown>(url: string, params?: object): Promise<T> {
  const res = await instance.get(url, { params })
  return res.data as T
}

export async function apiPost<T = unknown>(url: string, body?: object, config?: AxiosRequestConfig): Promise<T> {
  const res = await instance.post(url, body, config)
  return res.data as T
}

export async function apiPatch<T = unknown>(url: string, body?: object): Promise<T> {
  const res = await instance.patch(url, body)
  return res.data as T
}

export async function apiDelete<T = unknown>(url: string): Promise<T> {
  const res = await instance.delete(url)
  return res.data as T
}

export default instance
