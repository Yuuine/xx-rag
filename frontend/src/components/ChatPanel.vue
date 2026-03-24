<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { marked } from 'marked'
import { deleteAllSessions, deleteSession, deleteSessionBefore, uploadFiles } from '../api/rag'
import type { StreamResponse } from '../api/types'

const props = defineProps<{
  docsOpen?: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle-docs'): void
}>()

type ModalMode = 'none' | 'session' | 'cleanup'

interface MessageItem {
  role: 'user' | 'assistant'
  content: string
  timestamp: string
  duration?: string
}

const SID_LOCALSTORAGE_KEY = 'chat_sid'
const MESSAGES_LOCALSTORAGE_KEY = 'chat_messages'
const SHOW_TIMESTAMPS_KEY = 'chat_show_timestamps'
const MAX_RECONNECT_ATTEMPTS = 5
const RECONNECT_INTERVAL = 3000

function encodeUtf8Base64(str: string) {
  return btoa(String.fromCharCode(...new TextEncoder().encode(str)))
}

function decodeUtf8Base64(b64: string) {
  const bytes = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

// Inject copy button into code blocks rendered by marked.
const renderer = new marked.Renderer()
renderer.code = (arg: any) => {
  const rawText = typeof arg?.text === 'string' ? arg.text : ''
  const escaped = typeof arg?.escaped === 'string' ? arg.escaped : ''
  const encoded = encodeUtf8Base64(rawText)
  return `
    <div class="code-block">
      <button class="copy-btn" type="button" data-code="${encoded}">复制</button>
      <pre><code>${escaped}</code></pre>
    </div>
  `
}
marked.use({ renderer })

function loadShowTimestamps(): boolean {
  try {
    const saved = localStorage.getItem(SHOW_TIMESTAMPS_KEY)
    return saved === 'true'
  } catch {
    return false
  }
}

function saveShowTimestamps(value: boolean) {
  try {
    localStorage.setItem(SHOW_TIMESTAMPS_KEY, String(value))
  } catch {
    // ignore
  }
}

const state = reactive({
  ws: null as WebSocket | null,
  wsStatus: 'disconnected' as 'connecting' | 'connected' | 'reconnecting' | 'error' | 'disconnected',
  wsReconnectAttempts: 0,
  sessionId: '',
  showTimestamps: loadShowTimestamps(),
  isStreaming: false,
  streamBuffer: '',
  currentStreamStartTime: 0,
  uploadModalOpen: false,
  modalMode: 'none' as ModalMode,
  modalMessage: '',
  modalInput: '',
  modalAll: false,
  modalPassword: '',
  isDragOver: false
})

const messages = ref<MessageItem[]>([])
const questionInput = ref('')
const toast = ref({ text: '', type: 'success' as 'success' | 'error', visible: false })
const files = ref<File[]>([])
const uploading = ref(false)
const messagesContainerRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

const canSend = computed(() => !state.isStreaming && questionInput.value.trim().length > 0)
const sendBtnText = computed(() => (state.isStreaming ? '思考中' : '发送'))

function showToast(text: string, type: 'success' | 'error' = 'error') {
  toast.value = { text, type, visible: true }
  window.setTimeout(() => {
    toast.value.visible = false
  }, 2500)
}

function getSessionId() {
  if (state.sessionId) return state.sessionId
  const saved = localStorage.getItem(SID_LOCALSTORAGE_KEY)
  if (saved) {
    state.sessionId = saved
    return saved
  }
  const sid = crypto.randomUUID()
  localStorage.setItem(SID_LOCALSTORAGE_KEY, sid)
  state.sessionId = sid
  return sid
}

function resetSessionId() {
  state.sessionId = ''
  localStorage.removeItem(SID_LOCALSTORAGE_KEY)
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainerRef.value) {
      messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
    }
  })
}

function startNewStream() {
  state.isStreaming = true
  state.streamBuffer = ''
  state.currentStreamStartTime = Date.now()
  messages.value.push({
    role: 'assistant',
    content: '<span class="typing-cursor"></span>',
    timestamp: new Date().toLocaleString()
  })
  scrollToBottom()
}

function updateStreamingMessage() {
  if (!state.isStreaming) return
  const last = messages.value[messages.value.length - 1]
  if (!last || last.role !== 'assistant') return
  last.content = marked.parse(state.streamBuffer) as string
  scrollToBottom()
}

function saveMessages() {
  try {
    localStorage.setItem(MESSAGES_LOCALSTORAGE_KEY, JSON.stringify(messages.value))
  } catch {
    // ignore storage errors
  }
}

function loadMessages(): MessageItem[] {
  try {
    const saved = localStorage.getItem(MESSAGES_LOCALSTORAGE_KEY)
    if (saved) {
      return JSON.parse(saved) as MessageItem[]
    }
  } catch {
    // ignore parse errors
  }
  return []
}

function clearSavedMessages() {
  try {
    localStorage.removeItem(MESSAGES_LOCALSTORAGE_KEY)
  } catch {
    // ignore
  }
}

function finalizeStream() {
  if (!state.isStreaming) return
  const duration = ((Date.now() - state.currentStreamStartTime) / 1000).toFixed(2)
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'assistant') {
    last.content = marked.parse(state.streamBuffer || '') as string
    last.duration = duration
  }
  state.isStreaming = false
  state.currentStreamStartTime = 0
  state.streamBuffer = ''
  // 保存消息到 localStorage
  saveMessages()
}

function appendAssistantMessage(content: string, duration = '0.00') {
  messages.value.push({
    role: 'assistant',
    content: marked.parse(content) as string,
    timestamp: new Date().toLocaleString(),
    duration
  })
  scrollToBottom()
}

function appendUserMessage(content: string) {
  messages.value.push({
    role: 'user',
    content,
    timestamp: new Date().toLocaleString()
  })
  scrollToBottom()
  // 保存消息到 localStorage
  saveMessages()
}

function forceReconnect() {
  if (state.ws) {
    try {
      state.ws.close(1000, 'force_reconnect')
    } catch {
      // ignore
    }
    state.ws = null
  }
  state.wsReconnectAttempts = 0
  connectWebSocket(true)
}

function connectWebSocket(force = false) {
  if (!force && state.ws && (state.ws.readyState === WebSocket.OPEN || state.ws.readyState === WebSocket.CONNECTING)) return

  state.wsStatus = 'connecting'
  const sid = getSessionId()
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const ws = new WebSocket(`${protocol}//${window.location.host}/ws-chat?sid=${encodeURIComponent(sid)}`)
  state.ws = ws

  ws.onopen = () => {
    state.wsStatus = 'connected'
    state.wsReconnectAttempts = 0
    showToast('连接已建立', 'success')
  }

  ws.onmessage = (event: MessageEvent<string>) => {
    try {
      const data = JSON.parse(event.data) as StreamResponse & { type?: string; messages?: Array<{ role: string; content: string }> }

      if (data.type === 'heartbeat') return

      if (data.type === 'history' && data.messages) {
        // 优先使用本地存储的消息（包含耗时信息）
        const savedMessages = loadMessages()
        if (savedMessages.length > 0) {
          messages.value = savedMessages
        } else {
          // 如果没有本地存储的消息，使用服务器返回的历史消息
          messages.value = []
          data.messages.forEach((m) => {
            if (m.role === 'user' || m.role === 'assistant') {
              messages.value.push({
                role: m.role,
                content: m.content || '',
                timestamp: new Date().toLocaleString()
              })
            }
          })
        }
        scrollToBottom()
        return
      }

      if (data.finishReason === 'stop') {
        finalizeStream()
        return
      }

      if (data.message?.startsWith('Error:')) {
        appendAssistantMessage(`错误：${data.message.replace('Error: ', '')}`, '0.00')
        finalizeStream()
        return
      }

      if (data.content) {
        if (!state.isStreaming) return
        state.streamBuffer += data.content
        updateStreamingMessage()
      }
    } catch {
      showToast('收到无法解析的消息', 'error')
      finalizeStream()
    }
  }

  ws.onclose = (event) => {
    if (state.isStreaming) finalizeStream()

    if (event.code !== 1000 && state.wsReconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
      state.wsStatus = 'reconnecting'
      state.wsReconnectAttempts += 1
      window.setTimeout(() => connectWebSocket(), RECONNECT_INTERVAL)
      return
    }

    state.wsStatus = 'disconnected'
  }

  ws.onerror = () => {
    state.wsStatus = 'error'
    showToast('连接错误', 'error')
  }
}

function stopGenerating() {
  if (!state.isStreaming) return
  state.isStreaming = false
  state.streamBuffer = ''
  try {
    if (state.ws && state.ws.readyState === WebSocket.OPEN) {
      state.ws.send(JSON.stringify({ type: 'cancel' }))
    }
  } catch {
    // ignore
  }
  showToast('已停止生成', 'success')
}

function sendQuestion() {
  const question = questionInput.value.trim()
  if (!question || state.isStreaming) return

  appendUserMessage(question)
  questionInput.value = ''
  startNewStream()

  if (state.ws && state.ws.readyState === WebSocket.OPEN) {
    state.ws.send(question)
  } else {
    showToast('连接未就绪', 'error')
    finalizeStream()
  }
}

// 文件上传相关
function handleFileSelect(selectedFiles: FileList | null) {
  if (!selectedFiles) return
  const validFiles = Array.from(selectedFiles).filter(file => {
    const validTypes = ['.pdf', '.doc', '.docx', '.txt', '.md', '.xls', '.xlsx']
    return validTypes.some(type => file.name.toLowerCase().endsWith(type))
  })
  if (validFiles.length > 0) {
    files.value = validFiles
  }
}

function onDragOver(e: DragEvent) {
  e.preventDefault()
  e.stopPropagation()
  state.isDragOver = true
}

function onDragLeave(e: DragEvent) {
  e.preventDefault()
  e.stopPropagation()
  state.isDragOver = false
}

function onDrop(e: DragEvent) {
  e.preventDefault()
  e.stopPropagation()
  state.isDragOver = false
  handleFileSelect(e.dataTransfer?.files || null)
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

async function doUpload() {
  if (files.value.length === 0 || uploading.value) return
  uploading.value = true
  try {
    await uploadFiles(files.value)
    showToast('上传成功', 'success')
    files.value = []
    state.uploadModalOpen = false
  } catch (error) {
    showToast(error instanceof Error ? error.message : '上传失败', 'error')
  } finally {
    uploading.value = false
  }
}

// 弹窗关闭处理
function closeUploadModal() {
  state.uploadModalOpen = false
  files.value = []
  state.isDragOver = false
}

function closeModal() {
  state.modalMode = 'none'
  state.modalInput = ''
  state.modalAll = false
  state.modalPassword = ''
}

function openSessionModal() {
  state.modalMode = 'session'
  state.modalMessage = '请选择删除范围：输入天数删除早期记录，或勾选全部删除整个会话'
  state.modalInput = ''
  state.modalAll = false
}

function openCleanupModal() {
  state.modalMode = 'cleanup'
  state.modalMessage = '确认删除所有会话记录？请输入管理员密码'
  state.modalPassword = ''
}

async function handleModalConfirm() {
  if (state.modalMode === 'session') {
    const sid = getSessionId()
    try {
      if (state.modalAll) {
        await deleteSession(sid)
        showToast('会话已删除', 'success')
      } else {
        const days = Number.parseInt(state.modalInput, 10)
        if (!Number.isFinite(days) || days <= 0) {
          showToast('请输入有效天数', 'error')
          return
        }
        const before = new Date()
        before.setDate(before.getDate() - days)
        await deleteSessionBefore(sid, before.toISOString().slice(0, 19))
        showToast('早期记录已删除', 'success')
      }
      messages.value = []
      clearSavedMessages()
      resetSessionId()
      forceReconnect()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '删除失败', 'error')
    } finally {
      state.modalMode = 'none'
    }
    return
  }

  if (state.modalMode === 'cleanup') {
    if (!state.modalPassword) {
      showToast('需输入密码', 'error')
      return
    }
    try {
      await deleteAllSessions(state.modalPassword)
      messages.value = []
      clearSavedMessages()
      showToast('所有记录已删除', 'success')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '删除失败', 'error')
    } finally {
      state.modalMode = 'none'
    }
  }
}

function onCopyClick(e: MouseEvent) {
  const target = e.target as HTMLElement | null
  const btn = target?.closest?.('.copy-btn') as HTMLButtonElement | null
  if (!btn) return
  const encoded = btn.getAttribute('data-code')
  if (!encoded) return

  const codeText = decodeUtf8Base64(encoded)
  navigator.clipboard.writeText(codeText).then(() => {
    const old = btn.textContent || '复制'
    btn.textContent = '已复制'
    btn.classList.add('copied')
    window.setTimeout(() => {
      btn.textContent = old
      btn.classList.remove('copied')
    }, 2000)
    showToast('已复制', 'success')
  }).catch(() => {
    showToast('复制失败', 'error')
  })
}

// 监听 showTimestamps 变化并保存到 localStorage
watch(() => state.showTimestamps, (newValue) => {
  saveShowTimestamps(newValue)
})

onMounted(() => {
  connectWebSocket()
  document.addEventListener('click', onCopyClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onCopyClick)
  if (state.ws) {
    try {
      state.ws.close(1000, 'component_unmount')
    } catch {
      // ignore
    }
    state.ws = null
  }
})
</script>

<template>
  <div class="chat-card">
    <!-- 顶部导航栏 - 重新设计 -->
    <header class="chat-header">
      <div class="header-left">
        <div class="logo">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <circle cx="14" cy="14" r="12" fill="var(--apple-blue)" />
            <path d="M10 14L13 17L18 11" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span class="logo-text">潇潇知识助手</span>
        </div>
      </div>

      <div class="header-center">
        <div class="time-toggle-wrapper">
          <span class="toggle-label">时间戳</span>
          <input id="timeToggle" v-model="state.showTimestamps" type="checkbox" class="time-toggle-checkbox">
          <label for="timeToggle" class="time-toggle-label" />
        </div>
      </div>

      <div class="header-right">
        <button class="header-btn" :class="{ active: props.docsOpen }" title="查看文档" @click="emit('toggle-docs')">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path d="M3 3H15V15H3V3Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
            <path d="M6 6H12M6 9H12M6 12H9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
          <span>文档</span>
        </button>

        <button class="header-btn" title="清除会话" @click="openSessionModal">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path d="M3 6H15M6 6V4C6 3.44772 6.44772 3 7 3H11C11.5523 3 12 3.44772 12 4V6M7 9V13M11 9V13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span>会话</span>
        </button>

        <button class="header-btn" title="清理旧记录" @click="openCleanupModal">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path d="M9 16C12.866 16 16 12.866 16 9C16 5.13401 12.866 2 9 2C5.13401 2 2 5.13401 2 9C2 12.866 5.13401 16 9 16Z" stroke="currentColor" stroke-width="1.5" />
            <path d="M6 9H12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
          <span>清理</span>
        </button>

        <button class="header-btn" title="上传文档" @click="state.uploadModalOpen = true">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path d="M9 2V12M9 2L5 6M9 2L13 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M3 12V14C3 14.5523 3.44772 15 4 15H14C14.5523 15 15 14.5523 15 14V12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span>上传</span>
        </button>

        <button class="header-btn stop-btn" title="停止生成" :disabled="!state.isStreaming" @click="stopGenerating">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <rect x="4" y="4" width="10" height="10" rx="2" fill="currentColor" />
          </svg>
          <span>停止</span>
        </button>
      </div>
    </header>

    <!-- WebSocket 状态提示 -->
    <div v-if="state.wsStatus !== 'connected'" class="ws-banner">
      <div class="ws-banner-text">
        {{ state.wsStatus === 'connecting' ? '连接中...' : state.wsStatus === 'reconnecting' ? '重连中...' : state.wsStatus === 'error' ? '连接错误' : '已断开' }}
      </div>
      <button class="ws-banner-btn" @click="forceReconnect">立即重连</button>
    </div>

    <!-- 消息区域 -->
    <div class="chat-container">
      <div id="messagesContainer" ref="messagesContainerRef">
        <div v-for="(item, idx) in messages" :key="idx" class="message" :class="item.role">
          <div v-if="item.role === 'assistant'" v-html="item.content" />
          <div v-else>{{ item.content }}</div>
          <div class="timestamp" :style="{ display: state.showTimestamps ? 'block' : 'none' }">{{ item.timestamp }}</div>
          <div v-if="item.duration" class="footnote">推理耗时: {{ item.duration }} 秒</div>
        </div>
      </div>
    </div>

    <!-- 底部输入框 -->
    <div class="input-section">
      <div class="input-wrapper">
        <textarea
          id="questionInput"
          v-model="questionInput"
          placeholder="输入问题，按 Enter 发送（Shift+Enter 换行）..."
          rows="1"
          @keydown.enter.exact.prevent="sendQuestion"
        />
        <button id="sendBtn" class="btn-send" :disabled="!canSend" @click="sendQuestion">
          <svg v-if="!state.isStreaming" width="20" height="20" viewBox="0 0 20 20" fill="none">
            <path d="M3 10L17 3L10 17L8 12L3 10Z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span v-else>{{ sendBtnText }}</span>
        </button>
      </div>
    </div>

    <!-- 上传弹窗 - 支持拖拽 -->
    <div v-if="state.uploadModalOpen" class="modal-overlay" @click.self="closeUploadModal">
      <div class="upload-modal">
        <div class="upload-modal-header">
          <h3>上传文档</h3>
          <button class="close-btn" @click="closeUploadModal">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M15 5L5 15M5 5L15 15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </button>
        </div>

        <div
          class="upload-area"
          :class="{ 'drag-over': state.isDragOver }"
          @dragover="onDragOver"
          @dragleave="onDragLeave"
          @drop="onDrop"
          @click="triggerFileInput"
        >
          <input
            ref="fileInputRef"
            type="file"
            multiple
            accept=".pdf,.doc,.docx,.txt,.md,.xls,.xlsx"
            style="display: none"
            @change="(e) => handleFileSelect((e.target as HTMLInputElement).files)"
          >
          <div class="upload-icon">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <path d="M24 8V32M24 8L14 18M24 8L34 18" stroke="var(--apple-blue)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M8 32V36C8 38.2091 9.79086 40 12 40H36C38.2091 40 40 38.2091 40 36V32" stroke="var(--apple-blue)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </div>
          <p class="upload-title">点击或拖拽文件到此处上传</p>
          <p class="upload-desc">支持 PDF、Word、TXT、Markdown、Excel 等格式</p>
        </div>

        <div v-if="files.length > 0" class="file-list">
          <div v-for="(file, idx) in files" :key="idx" class="file-item">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M11 2H5C4.46957 2 3.96086 2.21071 3.58579 2.58579C3.21071 2.96086 3 3.46957 3 4V16C3 16.5304 3.21071 17.0391 3.58579 17.4142C3.96086 17.7893 4.46957 18 5 18H15C15.5304 18 16.0391 17.7893 16.4142 17.4142C16.7893 17.0391 17 16.5304 17 16V8L11 2Z" stroke="var(--apple-blue)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M11 2V8H17" stroke="var(--apple-blue)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            <span class="file-name">{{ file.name }}</span>
            <span class="file-size">{{ (file.size / 1024).toFixed(1) }} KB</span>
          </div>
        </div>

        <button class="btn-upload" :disabled="uploading || files.length === 0" @click="doUpload">
          {{ uploading ? '上传中...' : `上传 ${files.length > 0 ? files.length + ' 个' : ''}文件` }}
        </button>
      </div>
    </div>

    <!-- 会话管理弹窗 -->
    <div v-if="state.modalMode !== 'none'" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <h3>{{ state.modalMode === 'session' ? '会话管理' : '清理记录' }}</h3>
        <p class="modal-desc">{{ state.modalMessage }}</p>

        <div v-if="state.modalMode === 'session'" class="modal-input-group">
          <input
            v-model="state.modalInput"
            type="number"
            min="1"
            placeholder="天数，例如 30"
            class="modal-input"
          >
          <label class="checkbox-label">
            <input v-model="state.modalAll" type="checkbox">
            <span>全部删除（删除整个会话）</span>
          </label>
        </div>

        <div v-if="state.modalMode === 'cleanup'" class="modal-input-group">
          <input
            v-model="state.modalPassword"
            type="password"
            placeholder="请输入管理员密码"
            class="modal-input"
          >
        </div>

        <div class="modal-buttons">
          <button class="modal-btn modal-cancel" @click="closeModal">取消</button>
          <button class="modal-btn modal-confirm" @click="handleModalConfirm">确定</button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div class="toast" :class="[toast.type, { show: toast.visible }]">{{ toast.text }}</div>
  </div>
</template>

<style scoped>
/* 弹窗遮罩层 - 阻止点击穿透 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 上传弹窗 */
.upload-modal {
  background: var(--apple-bg);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  width: 480px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.upload-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--apple-border-light);
}

.upload-modal-header h3 {
  margin: 0;
  font-size: 1.2em;
  font-weight: 600;
  color: var(--apple-text);
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: var(--apple-bg-secondary);
  color: var(--apple-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: var(--apple-border-light);
  color: var(--apple-text);
}

/* 拖拽上传区域 */
.upload-area {
  margin: 24px;
  padding: 40px 24px;
  border: 2px dashed var(--apple-border);
  border-radius: var(--radius-lg);
  background: var(--apple-bg-secondary);
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.upload-area:hover {
  border-color: var(--apple-blue);
  background: var(--apple-blue-light);
}

.upload-area.drag-over {
  border-color: var(--apple-blue);
  background: var(--apple-blue-light);
  transform: scale(1.02);
}

.upload-icon {
  margin-bottom: 16px;
}

.upload-title {
  margin: 0 0 8px 0;
  font-size: 1em;
  font-weight: 500;
  color: var(--apple-text);
}

.upload-desc {
  margin: 0;
  font-size: 0.85em;
  color: var(--apple-text-secondary);
}

/* 文件列表 */
.file-list {
  margin: 0 24px 16px;
  max-height: 150px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--apple-bg-secondary);
  border-radius: var(--radius-md);
  margin-bottom: 8px;
}

.file-item:last-child {
  margin-bottom: 0;
}

.file-name {
  flex: 1;
  font-size: 0.9em;
  color: var(--apple-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 0.8em;
  color: var(--apple-text-secondary);
}

/* 上传按钮 */
.btn-upload {
  margin: 0 24px 24px;
  padding: 14px;
  background: var(--apple-blue);
  color: white;
  border: none;
  border-radius: var(--radius-full);
  font-size: 1em;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-upload:hover:not(:disabled) {
  background: var(--apple-blue-hover);
}

.btn-upload:disabled {
  background: var(--apple-border);
  color: var(--apple-text-secondary);
  cursor: not-allowed;
}

/* 会话管理弹窗 */
.modal-content {
  background: var(--apple-bg);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  width: 400px;
  max-width: 90vw;
  padding: 28px;
  animation: slideUp 0.3s ease;
}

.modal-content h3 {
  margin: 0 0 8px 0;
  font-size: 1.2em;
  font-weight: 600;
  color: var(--apple-text);
}

.modal-desc {
  margin: 0 0 20px 0;
  font-size: 0.9em;
  color: var(--apple-text-secondary);
  line-height: 1.5;
}

.modal-input-group {
  margin-bottom: 20px;
}

.modal-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-md);
  font-size: 1em;
  background: var(--apple-bg-secondary);
  color: var(--apple-text);
  box-sizing: border-box;
  margin-bottom: 12px;
}

.modal-input:focus {
  border-color: var(--apple-blue);
  outline: none;
  box-shadow: 0 0 0 3px var(--focus-ring);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 0.9em;
  color: var(--apple-text);
}

.checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: var(--apple-blue);
}

.modal-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.modal-btn {
  padding: 10px 24px;
  border: none;
  border-radius: var(--radius-full);
  font-size: 0.95em;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-cancel {
  background: var(--apple-bg-secondary);
  color: var(--apple-text);
}

.modal-cancel:hover {
  background: var(--apple-border-light);
}

.modal-confirm {
  background: var(--apple-blue);
  color: white;
}

.modal-confirm:hover {
  background: var(--apple-blue-hover);
}

/* WebSocket 状态横幅 */
.ws-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 20px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 0.9em;
}

.ws-banner-btn {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: none;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  cursor: pointer;
  font-size: 0.85em;
  font-weight: 500;
  transition: all 0.2s ease;
}

.ws-banner-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* Toast */
.toast {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%) translateY(-100px);
  min-width: 280px;
  padding: 14px 24px;
  border-radius: var(--radius-lg);
  color: white;
  font-size: 0.95em;
  font-weight: 500;
  text-align: center;
  box-shadow: var(--shadow-lg);
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s ease;
  z-index: 3000;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.toast.success {
  background: rgba(52, 199, 89, 0.95);
}

.toast.error {
  background: rgba(255, 59, 48, 0.95);
}

.toast.show {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(0);
}

/* 响应式 */
@media (max-width: 768px) {
  .upload-modal,
  .modal-content {
    width: 90vw;
  }

  .upload-area {
    padding: 30px 20px;
  }
}
</style>
