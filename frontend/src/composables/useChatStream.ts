import { computed, nextTick, ref } from 'vue'
import type { Ref } from 'vue'
import { marked } from 'marked'

interface MessageItem {
  role: 'user' | 'assistant'
  content: string
  rawContent?: string
  timestamp: string
  duration?: string
}

interface UseChatStreamOptions {
  messagesContainerRef?: Ref<HTMLElement | null>
}

export function useChatStream(options: UseChatStreamOptions = {}) {
  const isStreaming = ref(false)
  const streamBuffer = ref('')
  const currentStreamStartTime = ref(0)

  const canSend = computed(() => !isStreaming.value)
  const sendBtnText = computed(() => (isStreaming.value ? '思考中' : '发送'))

  function scrollToBottom(): void {
    nextTick(() => {
      if (options.messagesContainerRef?.value) {
        options.messagesContainerRef.value.scrollTop =
          options.messagesContainerRef.value.scrollHeight
      }
    })
  }

  function startNewStream(messages: Ref<MessageItem[]>): void {
    isStreaming.value = true
    streamBuffer.value = ''
    currentStreamStartTime.value = Date.now()
    messages.value.push({
      role: 'assistant',
      content: '<span class="typing-cursor"></span>',
      timestamp: new Date().toLocaleString()
    })
    scrollToBottom()
  }

  function updateStreamingMessage(messages: Ref<MessageItem[]>): void {
    if (!isStreaming.value) return
    const last = messages.value[messages.value.length - 1]
    if (!last || last.role !== 'assistant') return
    last.content = marked.parse(streamBuffer.value) as string
    scrollToBottom()
  }

  function finalizeStream(
    messages: Ref<MessageItem[]>,
    saveMessages?: (msgList: MessageItem[]) => void
  ): void {
    if (!isStreaming.value) return
    const duration = ((Date.now() - currentStreamStartTime.value) / 1000).toFixed(2)
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      last.rawContent = streamBuffer.value || ''
      last.content = marked.parse(last.rawContent) as string
      last.duration = duration
    }
    isStreaming.value = false
    currentStreamStartTime.value = 0
    streamBuffer.value = ''
    saveMessages?.(messages.value)
  }

  function appendAssistantMessage(
    messages: Ref<MessageItem[]>,
    content: string,
    duration = '0.00'
  ): void {
    messages.value.push({
      role: 'assistant',
      rawContent: content,
      content: marked.parse(content) as string,
      timestamp: new Date().toLocaleString(),
      duration
    })
    scrollToBottom()
  }

  function appendUserMessage(
    messages: Ref<MessageItem[]>,
    content: string,
    saveMessages?: (msgList: MessageItem[]) => void
  ): void {
    messages.value.push({
      role: 'user',
      content,
      timestamp: new Date().toLocaleString()
    })
    scrollToBottom()
    saveMessages?.(messages.value)
  }

  function stopStreaming(
    sendCancel: () => void,
    showToast: (text: string, type?: 'success' | 'error') => void
  ): void {
    if (!isStreaming.value) return
    isStreaming.value = false
    streamBuffer.value = ''
    try {
      sendCancel()
    } catch {
      // ignore
    }
    showToast('已停止生成', 'success')
  }

  return {
    isStreaming,
    streamBuffer,
    currentStreamStartTime,
    canSend,
    sendBtnText,
    scrollToBottom,
    startNewStream,
    updateStreamingMessage,
    finalizeStream,
    appendAssistantMessage,
    appendUserMessage,
    stopStreaming
  }
}
