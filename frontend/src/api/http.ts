import axios, { type AxiosError } from 'axios'
import type { Result } from './types'

const client = axios.create({
  baseURL: '/',
  timeout: 60000
})

client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

client.interceptors.response.use(
  (response) => {
    return response
  },
  (error: AxiosError<Result<unknown>>) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        console.warn('认证失败，请重新登录')
        localStorage.removeItem('auth_token')
        window.location.href = '/'
      } else if (status === 403) {
        console.warn('没有权限执行此操作')
      } else if (status >= 500) {
        console.error('服务器错误:', status)
      }
    } else if (error.request) {
      console.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export async function request<T>(config: Parameters<typeof client.request>[0]): Promise<T> {
  const response = await client.request<Result<T>>(config)
  const payload = response.data
  if (payload.code !== 0) {
    throw new Error(payload.message || `业务错误(${payload.code})`)
  }
  return payload.data
}

export default client