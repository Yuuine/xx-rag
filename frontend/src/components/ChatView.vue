<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { marked } from 'marked'
import type { StreamResponse } from '../api/types'
import { uploadFiles } from '../api/rag'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  rawContent?: string
  timestamp: Date
  duration?: number
}

const MAX_RECONNECT_ATTEMPTS = 5
const RECONNECT_INTERVAL = 3000

const messages = ref<Message[]>([])
const inputMessage = ref('')
const isStreaming = ref(false)
const isConnected = ref(false)
const isConnecting = ref(false)
const showSidebar = ref(true)
const showUploadModal = ref(false)
const uploadFilesList = ref<File[]>([])
const isUploading = ref(false)
const isDragging = ref(false)
const showNewChatHover = ref(false)

let ws: WebSocket | null = null
let reconnectAttempts = 0
let reconnectTimer: number | null = null
let streamStartTime = 0
let currentStreamMessage = ''

const messagesContainerRef = ref<HTMLDivElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const textareaRef = ref<HTMLTextAreaElement | null>(null)

const canSend = computed(() =>
  !isStreaming.value &&
  inputMessage.value.trim().length > 0 &&
  isConnected.value
)

const connectionStatus = computed(() => {
  if (isConnected.value) return 'connected'
  if (isConnecting.value) return 'connecting'
  return 'disconnected'
})

function addMessage(role: 'user' | 'assistant', content: string, duration?: number): Message {
  const message: Message = {
    id: crypto.randomUUID(),
    role,
    content: role === 'assistant' ? marked.parse(content) as string : content,
    rawContent: role === 'assistant' ? content : undefined,
    timestamp: new Date(),
    duration
  }
  messages.value.push(message)
  scrollToBottom()
  return message
}

function updateLastMessage(content: string) {
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage && lastMessage.role === 'assistant') {
    lastMessage.content = marked.parse(content) as string
    lastMessage.rawContent = content
  }
}

function finalizeStream() {
  if (!isStreaming.value) return
  const duration = (Date.now() - streamStartTime) / 1000
  const lastMessage = messages.value[messages.value.length - 1]
  if (lastMessage && lastMessage.role === 'assistant') {
    lastMessage.duration = duration
  }
  isStreaming.value = false
  currentStreamMessage = ''
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainerRef.value) {
      messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
    }
  })
}

function connectWebSocket() {
  if (ws?.readyState === WebSocket.OPEN || ws?.readyState === WebSocket.CONNECTING) {
    return
  }

  isConnecting.value = true
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'

  try {
    ws = new WebSocket(`${protocol}//${window.location.host}/ws-chat`)

    ws.onopen = () => {
      isConnected.value = true
      isConnecting.value = false
      reconnectAttempts = 0
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as StreamResponse & { type?: string }

        if (data.type === 'heartbeat') return

        if (data.finishReason === 'stop') {
          finalizeStream()
          return
        }

        if (data.message?.startsWith('Error:')) {
          addMessage('assistant', `错误：${data.message.replace('Error: ', '')}`)
          finalizeStream()
          return
        }

        if (data.content) {
          if (!isStreaming.value) {
            isStreaming.value = true
            streamStartTime = Date.now()
            currentStreamMessage = ''
            addMessage('assistant', '')
          }
          currentStreamMessage += data.content
          updateLastMessage(currentStreamMessage)
          scrollToBottom()
        }
      } catch (error) {
        console.error('WebSocket message error:', error)
      }
    }

    ws.onclose = (event) => {
      isConnected.value = false
      isConnecting.value = false

      if (isStreaming.value) {
        finalizeStream()
      }

      if (event.code !== 1000 && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
        reconnectAttempts++
        reconnectTimer = window.setTimeout(() => {
          connectWebSocket()
        }, RECONNECT_INTERVAL)
      }
    }

    ws.onerror = () => {
      isConnected.value = false
      isConnecting.value = false
    }
  } catch (error) {
    console.error('WebSocket connection error:', error)
    isConnecting.value = false
  }
}

function disconnectWebSocket() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  if (ws) {
    ws.onopen = null
    ws.onmessage = null
    ws.onclose = null
    ws.onerror = null

    try {
      ws.close(1000, 'manual_disconnect')
    } catch {
    }
    ws = null
  }

  isConnected.value = false
  isConnecting.value = false
}

function reconnectWebSocket() {
  disconnectWebSocket()
  reconnectAttempts = 0
  setTimeout(() => connectWebSocket(), 100)
}

function sendMessage() {
  const message = inputMessage.value.trim()
  if (!message || !canSend.value) return

  addMessage('user', message)
  inputMessage.value = ''
  resizeTextarea()

  if (ws?.readyState === WebSocket.OPEN) {
    ws.send(message)
  }
}

function stopStreaming() {
  if (!isStreaming.value) return

  if (ws?.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'cancel' }))
  }

  finalizeStream()
}

function startNewChat() {
  messages.value = []
  reconnectWebSocket()
}

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files) {
    uploadFilesList.value = Array.from(input.files)
  }
}

function handleDrop(event: DragEvent) {
  event.preventDefault()
  isDragging.value = false

  if (event.dataTransfer?.files) {
    uploadFilesList.value = Array.from(event.dataTransfer.files)
  }
}

async function uploadSelectedFiles() {
  if (uploadFilesList.value.length === 0 || isUploading.value) return

  isUploading.value = true
  try {
    await uploadFiles(uploadFilesList.value)
    showUploadModal.value = false
    uploadFilesList.value = []
  } catch (error) {
    console.error('Upload error:', error)
  } finally {
    isUploading.value = false
  }
}

function resizeTextarea() {
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
      textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 200) + 'px'
    }
  })
}

function handleKeyDown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

watch(inputMessage, () => {
  resizeTextarea()
})

onMounted(() => {
  connectWebSocket()
  resizeTextarea()
})

onUnmounted(() => {
  disconnectWebSocket()
})
</script>

<template>
  <div class="app-container">
    <aside
      class="sidebar"
      :class="{ collapsed: !showSidebar }"
    >
      <div class="sidebar-header">
        <button
          class="new-chat-btn"
          @click="startNewChat"
          @mouseenter="showNewChatHover = true"
          @mouseleave="showNewChatHover = false"
        >
          <svg class="icon-plus" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          <span v-if="showSidebar">新建对话</span>
        </button>

        <button
          v-if="!showSidebar"
          class="sidebar-toggle-btn"
          @click="showSidebar = true"
          title="展开侧边栏"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </div>

      <div v-if="showSidebar" class="sidebar-content">
        <div class="conversation-list">
          <div class="conversation-item active">
            <svg class="conversation-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            <span class="conversation-title">新对话</span>
          </div>
        </div>
      </div>

      <div v-if="showSidebar" class="sidebar-footer">
        <button class="sidebar-btn" @click="showUploadModal = true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          <span>上传文档</span>
        </button>
      </div>
    </aside>

    <main class="main-content">
      <header class="top-bar">
        <button
          v-if="showSidebar"
          class="sidebar-toggle-btn"
          @click="showSidebar = false"
          title="收起侧边栏"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>

        <div class="logo">
          <svg class="logo-icon" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" fill="var(--color-accent)"/>
            <path d="M8 12L11 15L16 9" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="logo-text">潇潇助手</span>
        </div>

        <div class="connection-status" :class="connectionStatus">
          <span class="status-dot"/>
        </div>
      </header>

      <div class="messages-container" ref="messagesContainerRef">
        <div v-if="messages.length === 0" class="welcome-screen">
          <div class="welcome-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
          </div>
          <h1 class="welcome-title">有什么可以帮到你的？</h1>
          <p class="welcome-subtitle">我是基于知识库的智能助手，可以为你提供精准的答案</p>
        </div>

        <div v-else class="messages-list">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-wrapper"
            :class="message.role"
          >
            <div class="message">
              <div class="message-avatar">
                <svg v-if="message.role === 'assistant'" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" fill="var(--color-accent)"/>
                  <path d="M8 12L11 15L16 9" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
              </div>

              <div class="message-content">
                <div class="message-text" v-html="message.content"/>
              </div>
            </div>
          </div>

          <div v-if="isStreaming" class="typing-indicator">
            <span/><span/><span/>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrapper">
          <div class="input-box">
            <textarea
              ref="textareaRef"
              v-model="inputMessage"
              placeholder="输入消息..."
              rows="1"
              @keydown="handleKeyDown"
            />

            <div class="input-actions">
              <button
                v-if="isStreaming"
                class="stop-btn"
                @click="stopStreaming"
              >
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <rect x="6" y="6" width="12" height="12" rx="2"/>
                </svg>
                <span>停止</span>
              </button>

              <button
                class="send-btn"
                :class="{ disabled: !canSend && !isStreaming }"
                :disabled="!canSend && !isStreaming"
                @click="isStreaming ? stopStreaming() : sendMessage()"
              >
                <svg v-if="!isStreaming" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="22" y1="2" x2="11" y2="13"/>
                  <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                </svg>
              </button>
            </div>
          </div>

          <div class="input-footer">
            <span class="input-hint">按 Enter 发送，Shift + Enter 换行</span>
          </div>
        </div>
      </div>
    </main>

    <Teleport to="body">
      <div v-if="showUploadModal" class="modal-overlay" @click.self="showUploadModal = false">
        <div class="modal">
          <div class="modal-header">
            <h3>上传文档</h3>
            <button class="modal-close" @click="showUploadModal = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <div
            class="upload-zone"
            :class="{ dragging: isDragging }"
            @dragenter.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @dragover.prevent
            @drop.prevent="handleDrop"
            @click="fileInputRef?.click()"
          >
            <input
              ref="fileInputRef"
              type="file"
              multiple
              accept=".pdf,.doc,.docx,.txt,.md,.xls,.xlsx"
              @change="handleFileSelect"
            />
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
            <p>点击或拖拽文件到此处</p>
            <span class="upload-hint">支持 PDF、Word、Excel、Markdown、TXT</span>
          </div>

          <div v-if="uploadFilesList.length > 0" class="file-list">
            <div v-for="(file, index) in uploadFilesList" :key="index" class="file-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
              <span class="file-name">{{ file.name }}</span>
              <span class="file-size">{{ (file.size / 1024).toFixed(1) }} KB</span>
            </div>
          </div>

          <div class="modal-footer">
            <button class="modal-btn" @click="showUploadModal = false">取消</button>
            <button
              class="modal-btn primary"
              :disabled="uploadFilesList.length === 0 || isUploading"
              @click="uploadSelectedFiles"
            >
              {{ isUploading ? '上传中...' : `上传 ${uploadFilesList.length} 个文件` }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: var(--color-bg-primary);
  overflow: hidden;
}

.sidebar {
  display: flex;
  flex-direction: column;
  width: var(--sidebar-width);
  background: var(--color-bg-secondary);
  border-right: 1px solid var(--color-border-light);
  transition: width var(--transition-base);
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
}

.sidebar-header {
  padding: var(--space-3);
  border-bottom: 1px solid var(--color-border-light);
}

.new-chat-btn {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: transparent;
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: all var(--transition-fast);
}

.new-chat-btn:hover {
  background: var(--color-bg-hover);
}

.icon-plus {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.sidebar-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.sidebar-toggle-btn:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.sidebar-toggle-btn svg {
  width: 20px;
  height: 20px;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-3);
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.conversation-item:hover {
  background: var(--color-bg-hover);
}

.conversation-item.active {
  background: var(--color-bg-tertiary);
}

.conversation-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: var(--color-text-secondary);
}

.conversation-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-footer {
  padding: var(--space-3);
  border-top: 1px solid var(--color-border-light);
}

.sidebar-btn {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-align: left;
  transition: all var(--transition-fast);
}

.sidebar-btn:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.sidebar-btn svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--color-bg-primary);
}

.top-bar {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-6);
  border-bottom: 1px solid var(--color-border-light);
  background: var(--color-bg-primary);
  flex-shrink: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex: 1;
}

.logo-icon {
  width: 28px;
  height: 28px;
}

.logo-text {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.connection-status {
  display: flex;
  align-items: center;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-text-tertiary);
}

.connection-status.connected .status-dot {
  background: var(--color-success);
}

.connection-status.connecting .status-dot {
  background: var(--color-warning);
  animation: pulse 1.5s ease-in-out infinite;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: var(--space-8);
  text-align: center;
}

.welcome-icon {
  width: 64px;
  height: 64px;
  margin-bottom: var(--space-6);
  color: var(--color-text-tertiary);
}

.welcome-title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-3);
}

.welcome-subtitle {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  max-width: 500px;
}

.messages-list {
  display: flex;
  flex-direction: column;
  max-width: var(--chat-max-width);
  margin: 0 auto;
  padding: var(--space-6);
}

.message-wrapper {
  display: flex;
  margin-bottom: var(--space-6);
  animation: slideUp var(--transition-base) ease-out;
}

.message-wrapper.user {
  justify-content: flex-end;
}

.message {
  display: flex;
  gap: var(--space-4);
  max-width: 85%;
}

.message-wrapper.user .message {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.message-avatar svg {
  width: 100%;
  height: 100%;
}

.message-content {
  min-width: 0;
}

.message-text {
  padding: var(--space-4) var(--space-5);
  border-radius: var(--radius-xl);
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
  line-height: var(--line-height-relaxed);
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.message-wrapper.user .message-text {
  background: var(--color-accent);
  color: white;
}

.message-text :deep(pre) {
  margin: var(--space-4) 0;
  padding: var(--space-4);
  background: var(--color-code-bg);
  border: 1px solid var(--color-code-border);
  border-radius: var(--radius-md);
  overflow-x: auto;
}

.message-text :deep(code) {
  font-family: var(--font-family-mono);
  font-size: 0.9em;
}

.message-text :deep(p) {
  margin-bottom: var(--space-3);
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.message-text :deep(ul),
.message-text :deep(ol) {
  margin: var(--space-3) 0;
  padding-left: var(--space-6);
}

.message-text :deep(li) {
  margin-bottom: var(--space-2);
}

.typing-indicator {
  display: flex;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-5);
  margin-left: calc(32px + var(--space-4));
  margin-top: calc(-1 * var(--space-6));
  margin-bottom: var(--space-6);
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: var(--color-text-tertiary);
  border-radius: 50%;
  animation: typing 1.4s ease-in-out infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-8px); }
}

.input-area {
  flex-shrink: 0;
  padding: var(--space-4) var(--space-6);
  background: var(--color-bg-primary);
}

.input-wrapper {
  max-width: var(--chat-max-width);
  margin: 0 auto;
}

.input-box {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  transition: all var(--transition-fast);
}

.input-box:focus-within {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px var(--color-accent-light);
}

.input-box textarea {
  width: 100%;
  min-height: 24px;
  max-height: 200px;
  padding: var(--space-2) var(--space-3);
  font-size: var(--font-size-base);
  line-height: var(--line-height-normal);
  color: var(--color-text-primary);
  resize: none;
}

.input-box textarea::placeholder {
  color: var(--color-text-tertiary);
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3);
}

.stop-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: all var(--transition-fast);
}

.stop-btn:hover {
  background: var(--color-bg-tertiary);
  color: var(--color-text-primary);
}

.stop-btn svg {
  width: 16px;
  height: 16px;
}

.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: var(--color-accent);
  color: white;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.send-btn:hover:not(.disabled) {
  background: var(--color-accent-hover);
  transform: scale(1.05);
}

.send-btn.disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}

.send-btn svg {
  width: 18px;
  height: 18px;
}

.input-footer {
  margin-top: var(--space-3);
  text-align: center;
}

.input-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-modal);
  animation: fadeIn var(--transition-fast);
}

.modal {
  width: 100%;
  max-width: 480px;
  margin: var(--space-4);
  background: var(--color-bg-primary);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  animation: scaleIn var(--transition-base);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-5) var(--space-6);
  border-bottom: 1px solid var(--color-border-light);
}

.modal-header h3 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.modal-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.modal-close:hover {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
}

.modal-close svg {
  width: 20px;
  height: 20px;
}

.modal-body {
  padding: var(--space-6);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--color-border-light);
}

.upload-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-10);
  margin: var(--space-6);
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.upload-zone:hover,
.upload-zone.dragging {
  border-color: var(--color-accent);
  background: var(--color-accent-light);
}

.upload-zone input {
  display: none;
}

.upload-zone svg {
  width: 48px;
  height: 48px;
  color: var(--color-accent);
}

.upload-zone p {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.upload-hint {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.file-list {
  padding: 0 var(--space-6) var(--space-4);
}

.file-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-2);
}

.file-item svg {
  width: 20px;
  height: 20px;
  color: var(--color-accent);
}

.file-name {
  flex: 1;
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.modal-btn {
  padding: var(--space-3) var(--space-5);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  background: transparent;
  transition: all var(--transition-fast);
}

.modal-btn:hover {
  background: var(--color-bg-secondary);
}

.modal-btn.primary {
  background: var(--color-accent);
  color: white;
}

.modal-btn.primary:hover:not(:disabled) {
  background: var(--color-accent-hover);
}

.modal-btn.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: var(--z-sticky);
    transform: translateX(-100%);
    transition: transform var(--transition-base);
  }

  .sidebar:not(.collapsed) {
    transform: translateX(0);
  }

  .sidebar.collapsed {
    width: 0;
    overflow: hidden;
  }

  .top-bar {
    padding: var(--space-3) var(--space-4);
  }

  .messages-list {
    padding: var(--space-4);
  }

  .message {
    max-width: 95%;
  }

  .input-area {
    padding: var(--space-3) var(--space-4);
  }

  .welcome-title {
    font-size: var(--font-size-xl);
  }
}
</style>
