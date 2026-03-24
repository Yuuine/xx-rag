import {onBeforeUnmount, ref} from 'vue'
import {WS_CONFIG} from '../constants'

type WebSocketStatus = 'connecting' | 'connected' | 'reconnecting' | 'error' | 'disconnected'

interface UseWebSocketOptions {
  onMessage?: (data: unknown) => void
  onOpen?: () => void
  onClose?: (event: CloseEvent) => void
  onError?: () => void
}

export function useWebSocket(options: UseWebSocketOptions = {}) {
  const ws = ref<WebSocket | null>(null)
  const status = ref<WebSocketStatus>('disconnected')
  const reconnectAttempts = ref(0)

  let reconnectTimer: number | null = null

  function connect(url: string) {
    if (ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) {
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
        reconnectTimer = window.setTimeout(() => connect(url), WS_CONFIG.RECONNECT_INTERVAL)
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

  function disconnect(code = 1000, reason = 'manual_disconnect') {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    if (ws.value) {
      try {
        ws.value.close(code, reason)
      } catch {
      }
      ws.value = null
    }
  }

  function send(data: string | object) {
    if (ws.value?.readyState === WebSocket.OPEN) {
      const message = typeof data === 'string' ? data : JSON.stringify(data)
      ws.value.send(message)
      return true
    }
    return false
  }

  function reconnect(url: string) {
    disconnect(1000, 'force_reconnect')
    reconnectAttempts.value = 0
    connect(url)
  }

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
