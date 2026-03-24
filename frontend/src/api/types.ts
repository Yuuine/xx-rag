export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface DocItem {
  fileMd5: string
  fileName: string
  createdAt: string
}

export interface DocListData {
  docs: DocItem[]
}

export interface StreamResponse {
  content?: string
  finishReason?: string | null
  message?: string
}

