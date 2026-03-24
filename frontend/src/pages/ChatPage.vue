<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { marked } from 'marked'
import { deleteAllSessions, deleteSession, deleteSessionBefore, uploadFiles } from '../api/rag'
import type { StreamResponse } from '../api/types'

type ModalMode = 'none' | 'session' | 'cleanup'

interface MessageItem {
  role: 'user' | 'assistant'
  content: string
  timestamp: string
  duration?: string
}

const SID_LOCALSTORAGE_KEY = 'chat_sid'
const MAX_RECONNECT_ATTEMPTS = 5
const RECONNECT_INTERVAL = 3000

const state = reactive({
  ws: null as WebSocket | null,
  wsReconnectAttempts: 0,
  sessionId: '',
  showTimestamps: false,
  isStreaming: false,
  streamBuffer: '',
  currentStreamStartTime: 0,
  uploadModalOpen: false,
  modalMode: 'none' as ModalMode,
  modalMessage: '',
  modalInput: '',
  modalAll: false,
  modalPassword: ''
})

const messages = ref<MessageItem[]>([])
const questionInput = ref('')
const toast = ref({ text: '', type: 'success' as 'success' | 'error', visible: false })
const files = ref<File[]>([])
const uploading = ref(false)
const messagesContainerRef = ref<HTMLElement | null>(null)

const canSend = computed(() => !state.isStreaming && questionInput.value.trim().length > 0)
const sendBtnText = computed(() => (state.isStreaming ? '思考中' : '发送'))

function showToast(text: string, type: 'success' | 'error' = 'error') {
  toast.value = { text, type, visible: true }
  window.setTimeout(() => {
    toast.value.visible = false
  }, 2500)
}

function getSessionId() {
  if (state.sessionId) {
    return state.sessionId
  }
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

function connectWebSocket() {
  if (state.ws && (state.ws.readyState === WebSocket.OPEN || state.ws.readyState === WebSocket.CONNECTING)) {
    return
  }
  const sid = getSessionId()
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const ws = new WebSocket(`${protocol}//${window.location.host}/ws-chat?sid=${encodeURIComponent(sid)}`)
  state.ws = ws

  ws.onopen = () => {
    state.wsReconnectAttempts = 0
    showToast('连接已建立', 'success')
  }

  ws.onmessage = (event: MessageEvent<string>) => {
    try {
      const data = JSON.parse(event.data) as StreamResponse & { type?: string; messages?: Array<{ role: string; content: string }> }
      if (data.type === 'heartbeat') {
        return
      }
      if (data.type === 'history' && data.messages) {
        data.messages.forEach((m) => {
          if (m.role === 'user' || m.role === 'assistant') {
            messages.value.push({
              role: m.role,
              content: m.content || '',
              timestamp: new Date().toLocaleString()
            })
          }
        })
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
        state.streamBuffer += data.content
        updateStreamingMessage()
      }
    } catch {
      showToast('收到无法解析的消息', 'error')
      finalizeStream()
    }
  }

  ws.onclose = (event) => {
    if (state.isStreaming) {
      finalizeStream()
    }
    if (event.code !== 1000 && state.wsReconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
      state.wsReconnectAttempts += 1
      window.setTimeout(connectWebSocket, RECONNECT_INTERVAL)
    }
  }

  ws.onerror = () => {
    showToast('连接错误', 'error')
  }
}

function forceReconnect() {
  if (state.ws) {
    state.ws.onopen = null
    state.ws.onmessage = null
    state.ws.onclose = null
    state.ws.onerror = null
    if (state.ws.readyState === WebSocket.OPEN || state.ws.readyState === WebSocket.CONNECTING) {
      state.ws.close(1000, 'client_reconnect')
    }
    state.ws = null
  }
  window.setTimeout(() => {
    state.wsReconnectAttempts = 0
    connectWebSocket()
  }, 200)
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
  const last = messages.value[messages.value.length - 1]
  if (!last || last.role !== 'assistant') {
    return
  }
  last.content = marked.parse(state.streamBuffer) as string
  scrollToBottom()
}

function finalizeStream() {
  if (!state.isStreaming) {
    return
  }
  const duration = ((Date.now() - state.currentStreamStartTime) / 1000).toFixed(2)
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'assistant') {
    last.content = marked.parse(state.streamBuffer || '') as string
    last.duration = duration
  }
  state.isStreaming = false
  state.currentStreamStartTime = 0
  state.streamBuffer = ''
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
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainerRef.value) {
      messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
    }
  })
}

function sendQuestion() {
  const question = questionInput.value.trim()
  if (!question || state.isStreaming) {
    return
  }
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

async function doUpload() {
  if (files.value.length === 0 || uploading.value) {
    return
  }
  uploading.value = true
  try {
    await uploadFiles(files.value)
    showToast('上传成功', 'success')
    files.value = []
  } catch (error) {
    showToast(error instanceof Error ? error.message : '上传失败', 'error')
  } finally {
    uploading.value = false
  }
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
      showToast('所有记录已删除', 'success')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '删除失败', 'error')
    } finally {
      state.modalMode = 'none'
    }
  }
}



onMounted(() => {
  connectWebSocket()
})

onBeforeUnmount(() => {
  if (state.ws) {
    state.ws.close(1000, 'component_unmount')
    state.ws = null
  }
})
</script>

<template>
  <div class="chat-card">
    <header>
      潇潇知识助手
      <div class="header-controls">
        <div class="time-toggle-wrapper">
          <input id="timeToggle" v-model="state.showTimestamps" type="checkbox" class="time-toggle-checkbox">
          <label for="timeToggle" class="time-toggle-label" />
          <span class="time-toggle-text on">ON</span>
          <span class="time-toggle-text off">OFF</span>
        </div>
        <button class="upload-trigger" title="查看文档">文档</button>
        <button class="upload-trigger" title="清除会话" @click="openSessionModal">会话</button>
        <button class="upload-trigger" title="清理旧记录" @click="openCleanupModal">清理</button>
        <button class="upload-trigger" title="上传文档" @click="state.uploadModalOpen = !state.uploadModalOpen">上传</button>
      </div>
    </header>
    <div class="chat-container">
      <div id="messagesContainer" ref="messagesContainerRef">
        <div v-for="(item, idx) in messages" :key="idx" class="message" :class="item.role">
          <div v-if="item.role === 'assistant'" v-html="item.content" />
          <div v-else>{{ item.content }}</div>
          <div class="timestamp" :style="{ display: state.showTimestamps ? 'block' : 'none' }">{{ item.timestamp }}</div>
          <div v-if="item.duration" class="footnote">推理耗时: {{ item.duration }} 秒</div>
        </div>
      </div>
      <div class="input-section">
        <textarea
          id="questionInput"
          v-model="questionInput"
          placeholder="输入问题，按 Enter 发送（Shift+Enter 换行）..."
          rows="1"
          @keydown.enter.exact.prevent="sendQuestion"
        />
        <button id="sendBtn" class="btn-send" :disabled="!canSend" @click="sendQuestion">{{ sendBtnText }}</button>
      </div>
    </div>
    <div v-if="state.uploadModalOpen" class="upload-modal active">
      <h3>上传文档</h3>
      <div class="upload-area">
        <p>支持 PDF、Word、TXT、Markdown、Excel 等格式</p>
        <input
          type="file"
          multiple
          accept=".pdf,.doc,.docx,.txt,.md,.xls,.xlsx"
          @change="(e) => files = Array.from((e.target as HTMLInputElement).files || [])"
        >
      </div>
      <button class="btn-upload" :disabled="uploading" @click="doUpload">{{ uploading ? "上传中..." : "上传文件" }}</button>
    </div>
  </div>

  <div v-if="state.modalMode !== 'none'" class="modal active">
    <div class="modal-content">
      <div>{{ state.modalMessage }}</div>
      <div v-if="state.modalMode === 'session'" style="margin-top:12px;">
        <input
          v-model="state.modalInput"
          type="number"
          min="1"
          placeholder="天数，例如 30"
          style="width:100%; padding:10px; box-sizing:border-box; border-radius:8px; border:1px solid var(--border-input);"
        >
        <label style="display:flex; align-items:center; gap:8px; margin-top:8px;">
          <input v-model="state.modalAll" type="checkbox">
          <span>全部删除（删除整个会话）</span>
        </label>
      </div>
      <div v-if="state.modalMode === 'cleanup'" style="margin-top:12px;">
        <input
          v-model="state.modalPassword"
          type="password"
          placeholder="请输入管理员密码"
          style="width:100%; padding:10px; box-sizing:border-box; border-radius:8px; border:1px solid var(--border-input);"
        >
      </div>
      <div class="modal-buttons">
        <button class="modal-btn modal-cancel" @click="state.modalMode = 'none'">取消</button>
        <button class="modal-btn modal-confirm" @click="handleModalConfirm">确定</button>
      </div>
    </div>
  </div>

  <div class="toast" :class="[toast.type, { show: toast.visible }]">{{ toast.text }}</div>
</template>
