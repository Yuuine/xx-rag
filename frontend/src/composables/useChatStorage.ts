import { ref, watch } from 'vue'

const SID_LOCALSTORAGE_KEY = 'chat_sid'
const MESSAGES_LOCALSTORAGE_KEY = 'chat_messages'
const SHOW_TIMESTAMPS_KEY = 'chat_show_timestamps'

interface MessageItem {
  role: 'user' | 'assistant'
  content: string
  rawContent?: string
  timestamp: string
  duration?: string
}

function loadWithFallback<T>(key: string, fallback: T, parse = true): T {
  try {
    const saved = localStorage.getItem(key)
    if (saved) {
      return parse ? (JSON.parse(saved) as T) : (saved as unknown as T)
    }
  } catch {
    // ignore errors
  }
  return fallback
}

function saveSafe(key: string, value: unknown, stringify = true): void {
  try {
    localStorage.setItem(key, stringify ? JSON.stringify(value) : String(value))
  } catch {
    // ignore errors
  }
}

function removeSafe(key: string): void {
  try {
    localStorage.removeItem(key)
  } catch {
    // ignore errors
  }
}

export function useChatStorage() {
  const sessionId = ref('')
  const showTimestamps = ref(loadWithFallback<boolean>(SHOW_TIMESTAMPS_KEY, false, false))
  const messages = ref<MessageItem[]>([])

  function getSessionId(): string {
    if (sessionId.value) return sessionId.value
    const saved = loadWithFallback<string | null>(SID_LOCALSTORAGE_KEY, null, false)
    if (saved) {
      sessionId.value = saved
      return saved
    }
    const sid = crypto.randomUUID()
    saveSafe(SID_LOCALSTORAGE_KEY, sid, false)
    sessionId.value = sid
    return sid
  }

  function resetSessionId(): void {
    sessionId.value = ''
    removeSafe(SID_LOCALSTORAGE_KEY)
  }

  function saveMessages(msgList: MessageItem[]): void {
    const messagesToSave = msgList.map(msg => ({
      ...msg,
      content: msg.role === 'assistant' ? (msg.rawContent || msg.content) : msg.content
    }))
    saveSafe(MESSAGES_LOCALSTORAGE_KEY, messagesToSave)
  }

  function loadMessages(
    parseMarkdown: (content: string) => string
  ): MessageItem[] {
    const saved = loadWithFallback<MessageItem[]>(MESSAGES_LOCALSTORAGE_KEY, [])
    return saved.map(msg => {
      if (msg.role === 'assistant') {
        const rawContent = msg.content
        return {
          ...msg,
          rawContent,
          content: parseMarkdown(rawContent)
        }
      }
      return msg
    })
  }

  function clearSavedMessages(): void {
    removeSafe(MESSAGES_LOCALSTORAGE_KEY)
  }

  watch(showTimestamps, (newValue) => {
    saveSafe(SHOW_TIMESTAMPS_KEY, newValue, false)
  })

  return {
    sessionId,
    showTimestamps,
    messages,
    getSessionId,
    resetSessionId,
    saveMessages,
    loadMessages,
    clearSavedMessages
  }
}
