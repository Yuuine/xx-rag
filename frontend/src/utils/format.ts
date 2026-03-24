/**
 * 格式化工具函数
 */

/**
 * 格式化文件大小
 * @param bytes 字节数
 * @returns 格式化后的字符串（如 "1.5 KB"）
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`
}

/**
 * 格式化日期
 * @param date 日期对象或日期字符串
 * @returns 格式化后的日期字符串
 */
export function formatDate(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 获取日期分组键（用于消息分组）
 * @param timestamp 时间戳字符串
 * @returns 分组键（今天/昨天/具体日期）
 */
export function getDateGroupKey(timestamp: string): string {
  try {
    const date = new Date(timestamp)
    const today = new Date()
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)
    
    if (date.toDateString() === today.toDateString()) {
      return '今天'
    } else if (date.toDateString() === yesterday.toDateString()) {
      return '昨天'
    } else {
      return date.toLocaleDateString('zh-CN', { 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric' 
      })
    }
  } catch {
    return '历史消息'
  }
}

/**
 * 格式化耗时
 * @param startTime 开始时间戳
 * @param endTime 结束时间戳（默认为当前时间）
 * @returns 格式化后的耗时字符串（秒）
 */
export function formatDuration(startTime: number, endTime: number = Date.now()): string {
  return ((endTime - startTime) / 1000).toFixed(2)
}
