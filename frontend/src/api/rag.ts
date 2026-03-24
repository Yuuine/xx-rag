import client, { request } from './http'
import type { DocListData } from './types'

export async function uploadFiles(files: File[]) {
  const form = new FormData()
  files.forEach((f) => form.append('files', f))
  await request<unknown>({
    url: '/xx/upload',
    method: 'post',
    data: form
  })
}

export async function getDocs() {
  return request<DocListData>({
    url: '/xx/getDoc',
    method: 'get'
  })
}

export async function deleteDocs(fileMd5s: string[]) {
  return request<unknown>({
    url: '/xx/delete',
    method: 'post',
    data: fileMd5s
  })
}

export async function deleteSession(sessionId: string) {
  return request<unknown>({
    url: '/xx/deleteSession',
    method: 'post',
    data: { sessionId }
  })
}

export async function deleteSessionBefore(sessionId: string, beforeDate: string) {
  return request<unknown>({
    url: '/xx/deleteSessionBefore',
    method: 'post',
    data: { sessionId, beforeDate }
  })
}

export async function deleteAllSessions(password: string) {
  return request<unknown>({
    url: '/xx/deleteAllSessions',
    method: 'post',
    data: { password }
  })
}

export async function checkHealth() {
  await client.get('/actuator/health')
}
