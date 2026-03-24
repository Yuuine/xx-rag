import {onBeforeUnmount, ref} from 'vue'
import {WS_CONFIG} from '../constants'

type WebSocketStatus = 'connecting' | 'connected' | 'reconnecting' | 'error' | 'disconnected'

interface UseWebSocketOptions {
  onMessage?: (data: unknown) => void
  onOpen?: () => void
  onClose?: (event: CloseEvent) => void
  onError?: () => void
}

/**
 * WebSocket 组合式函数
 * @param url WebSocket URL
 * @param options 回调函数选项
 * @returns WebSocket 状态和控制函数
 */
export function useWebSocket(url: string, options: UseWebSocketOptions = {}) {
  const ws = ref<WebSocket | null>(null)
  const status = ref<WebSocketStatus>('disconnected')
  const reconnectAttempts = ref(0)
  
  let reconnectTimer: number | null = null
  
  /**
   * 连接 WebSocket
   * @param force 是否强制重新连接
   */
  function connect(force = false) {
    if (!force && ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) {
      return
    }
    
    status.value = 'connecting'
    
    const socket = new WebSocket(url)
    ws.value = socket
    
    socket.onopen = () => {
      status.value = 'connected'
      reconnectAttempts.value = 0
      options.onOpen?.()
    }
    
    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        options.onMessage?.(data)
      } catch {
        options.onMessage?.(event.data)
      }
    }
    
    socket.onclose = (event) => {
      ws.value = null
      
      if (event.code !== 1000 && reconnectAttempts.value < WS_CONFIG.MAX_RECONNECT_ATTEMPTS) {
        status.value = 'reconnecting'
        reconnectAttempts.value++
        reconnectTimer = window.setTimeout(() => connect(), WS_CONFIG.RECONNECT_INTERVAL)
        return
      }
      
      status.value = 'disconnected'
      options.onClose?.(event)
    }
    
    socket.onerror = () => {
      status.value = 'error'
      options.onError?.()
    }
  }
  
  /**
   * 断开 WebSocket 连接
   * @param code 关闭代码
   * @param reason 关闭原因
   */
  function disconnect(code = 1000, reason = 'manual_disconnect') {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    
    if (ws.value) {
      try {
        ws.value.close(code, reason)
      } catch {
        // 忽略错误
      }
      ws.value = null
    }
  }
  
  /**
   * 发送消息
   * @param data 要发送的数据
   */
  function send(data: string | object) {
    if (ws.value?.readyState === WebSocket.OPEN) {
      const message = typeof data === 'string' ? data : JSON.stringify(data)
      ws.value.send(message)
      return true
    }
    return false
  }
  
  /**
   * 强制重新连接
   */
  function reconnect() {
    disconnect(1000, 'force_reconnect')
    reconnectAttempts.value = 0
    connect(true)
  }
  
  // 组件卸载时自动断开连接
  onBeforeUnmount(() => {
    disconnect(1000, 'component_unmount')
  })
  
  return {
    ws,
    status,
    reconnectAttempts,
    connect,
    disconnect,
    send,
    reconnect
  }
}
