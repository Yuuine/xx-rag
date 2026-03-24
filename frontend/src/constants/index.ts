/**
 * 全局常量定义
 */

// 存储键名
export const STORAGE_KEYS = {
  SESSION_ID: 'chat_sid',
  MESSAGES: 'chat_messages',
  SHOW_TIMESTAMPS: 'chat_show_timestamps'
} as const

// WebSocket 配置
export const WS_CONFIG = {
  MAX_RECONNECT_ATTEMPTS: 5,
  RECONNECT_INTERVAL: 3000
} as const

// 消息懒加载配置
export const LAZY_LOAD_CONFIG = {
  MESSAGES_PER_PAGE: 20
} as const

// 文件上传配置
export const UPLOAD_CONFIG = {
  VALID_FILE_TYPES: ['.pdf', '.doc', '.docx', '.txt', '.md', '.xls', '.xlsx'],
  MAX_FILE_SIZE: 50 * 1024 * 1024 // 50MB
} as const
