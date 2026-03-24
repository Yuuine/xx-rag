import { ref } from 'vue'
import { deleteSession, deleteSessionBefore, deleteAllSessions } from '../api/rag'

type ModalMode = 'none' | 'session' | 'cleanup'

export function useSessionManagement() {
  const modalMode = ref<ModalMode>('none')
  const modalMessage = ref('')
  const modalInput = ref('')
  const modalAll = ref(false)
  const modalPassword = ref('')

  function openSessionModal(): void {
    modalMode.value = 'session'
    modalMessage.value = '请选择删除范围：输入天数删除早期记录，或勾选全部删除整个会话'
    modalInput.value = ''
    modalAll.value = false
  }

  function openCleanupModal(): void {
    modalMode.value = 'cleanup'
    modalMessage.value = '确认删除所有会话记录？请输入管理员密码'
    modalPassword.value = ''
  }

  function closeModal(): void {
    modalMode.value = 'none'
    modalInput.value = ''
    modalAll.value = false
    modalPassword.value = ''
  }

  async function handleModalConfirm(
    getSessionId: () => string,
    resetSessionId: () => void,
    clearSavedMessages: () => void,
    forceReconnect: () => void,
    showToast: (text: string, type?: 'success' | 'error') => void,
    clearMessages: () => void
  ): Promise<void> {
    if (modalMode.value === 'session') {
      const sid = getSessionId()
      try {
        if (modalAll.value) {
          await deleteSession(sid)
          showToast('会话已删除', 'success')
        } else {
          const days = Number.parseInt(modalInput.value, 10)
          if (!Number.isFinite(days) || days <= 0) {
            showToast('请输入有效天数', 'error')
            return
          }
          const before = new Date()
          before.setDate(before.getDate() - days)
          await deleteSessionBefore(sid, before.toISOString().slice(0, 19))
          showToast('早期记录已删除', 'success')
        }
        clearMessages()
        clearSavedMessages()
        resetSessionId()
        forceReconnect()
      } catch (error) {
        showToast(error instanceof Error ? error.message : '删除失败', 'error')
      } finally {
        modalMode.value = 'none'
      }
      return
    }

    if (modalMode.value === 'cleanup') {
      if (!modalPassword.value) {
        showToast('需输入密码', 'error')
        return
      }
      try {
        await deleteAllSessions(modalPassword.value)
        clearMessages()
        clearSavedMessages()
        resetSessionId()
        forceReconnect()
        showToast('所有记录已删除', 'success')
      } catch (error) {
        showToast(error instanceof Error ? error.message : '删除失败', 'error')
      } finally {
        modalMode.value = 'none'
      }
    }
  }

  return {
    modalMode,
    modalMessage,
    modalInput,
    modalAll,
    modalPassword,
    openSessionModal,
    openCleanupModal,
    closeModal,
    handleModalConfirm
  }
}
