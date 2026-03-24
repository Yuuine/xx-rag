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

export interface VectorSearchResult {
  chunkId?: string
  source?: string
  chunkIndex?: number
  content?: string
  score?: number
}

export interface StreamResponse {
  content?: string
  finishReason?: string | null
  message?: string
  references?: VectorSearchResult[]
}

export interface FileInfo {
  fileMd5: string
  fileName: string
  fileSize: number
  fileType: string
  createdAt: string
  pageCount?: number | null
  charCount?: number | null
  chunkCount?: number | null
}

