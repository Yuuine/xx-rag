import { request } from './http'
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

export async function checkHealth() {
  await fetch('/actuator/health')
}
