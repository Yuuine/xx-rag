import {reactive} from 'vue'

interface ToastState {
  visible: boolean
  message: string
  type: 'success' | 'error' | 'info'
}

const toastState = reactive<ToastState>({
  visible: false,
  message: '',
  type: 'info'
})

let toastTimer: number | null = null

/**
 * Toast 提示组合式函数
 * @returns Toast 状态和控制函数
 */
export function useToast() {
  /**
   * 显示 Toast 提示
   * @param message 提示消息
   * @param type 提示类型
   * @param duration 显示时长（毫秒）
   */
  function showToast(
    message: string,
    type: 'success' | 'error' | 'info' = 'info',
    duration: number = 2500
  ) {
    // 清除之前的定时器
    if (toastTimer) {
      clearTimeout(toastTimer)
    }
    
    toastState.message = message
    toastState.type = type
    toastState.visible = true
    
    toastTimer = window.setTimeout(() => {
      toastState.visible = false
    }, duration)
  }
  
  /**
   * 隐藏 Toast 提示
   */
  function hideToast() {
    toastState.visible = false
    if (toastTimer) {
      clearTimeout(toastTimer)
      toastTimer = null
    }
  }
  
  return {
    toast: toastState,
    showToast,
    hideToast
  }
}
