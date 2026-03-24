/**
 * 本地存储工具函数
 * 提供类型安全的 localStorage 操作
 */

/**
 * 从 localStorage 获取数据
 * @param key 存储键
 * @returns 存储的数据，如果不存在返回 null
 */
export function getStorageItem<T>(key: string): T | null {
  try {
    const item = localStorage.getItem(key)
    if (!item) return null
    return JSON.parse(item) as T
  } catch {
    return null
  }
}

/**
 * 保存数据到 localStorage
 * @param key 存储键
 * @param value 要存储的数据
 */
export function setStorageItem<T>(key: string, value: T): void {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // 忽略存储错误（如存储空间不足）
  }
}

/**
 * 从 localStorage 删除数据
 * @param key 存储键
 */
export function removeStorageItem(key: string): void {
  try {
    localStorage.removeItem(key)
  } catch {
    // 忽略错误
  }
}

/**
 * 生成唯一ID
 * @returns UUID 字符串
 */
export function generateId(): string {
  return crypto.randomUUID()
}

/**
 * Base64 编码（支持 UTF-8）
 * @param str 要编码的字符串
 * @returns Base64 编码后的字符串
 */
export function encodeBase64(str: string): string {
  return btoa(String.fromCharCode(...new TextEncoder().encode(str)))
}

/**
 * Base64 解码（支持 UTF-8）
 * @param b64 Base64 编码的字符串
 * @returns 解码后的字符串
 */
export function decodeBase64(b64: string): string {
  const bytes = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}
