import axios from 'axios'
import type { Result } from './types'

const client = axios.create({
  baseURL: '/',
  timeout: 60000
})

export async function request<T>(config: Parameters<typeof client.request>[0]): Promise<T> {
  const response = await client.request<Result<T>>(config)
  const payload = response.data
  if (payload.code !== 0) {
    throw new Error(payload.message || `业务错误(${payload.code})`)
  }
  return payload.data
}

export default client
