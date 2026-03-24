<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { getFileInfo, getDownloadUrl } from '../api/rag'
import type { FileInfo as FileInfoType } from '../api/types'
import { IconClose } from './icons'

const props = defineProps<{
  show: boolean
  fileMd5: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const loading = ref(false)
const fileInfo = ref<FileInfoType | null>(null)
const error = ref<string | null>(null)

const fileType = computed(() => {
  if (!fileInfo.value) return 'unknown'
  const fileName = fileInfo.value.fileName || ''
  const ext = fileName.split('.').pop()?.toLowerCase() || ''
  
  if (['pdf'].includes(ext)) return 'pdf'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(ext)) return 'image'
  if (['txt', 'md', 'json', 'xml', 'html', 'css', 'js'].includes(ext)) return 'text'
  return 'other'
})

const fileSizeText = computed(() => {
  if (!fileInfo.value?.fileSize) return ''
  const size = fileInfo.value.fileSize
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`
  return `${(size / (1024 * 1024)).toFixed(2)} MB`
})

const fileUrl = computed(() => {
  if (!props.fileMd5) return ''
  return getDownloadUrl(props.fileMd5)
})

async function loadFileInfo() {
  if (!props.fileMd5) return
  
  loading.value = true
  error.value = null
  fileInfo.value = null
  
  try {
    fileInfo.value = await getFileInfo(props.fileMd5)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function downloadFile() {
  if (!fileInfo.value) return
  const a = document.createElement('a')
  a.href = fileUrl.value
  a.download = fileInfo.value.fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

onMounted(() => {
  if (props.show && props.fileMd5) {
    loadFileInfo()
  }
})

const watchedProps = () => [props.show, props.fileMd5]
if ((watchedProps() as any).watch) {
  (watchedProps() as any).watch(() => {
    if (props.show && props.fileMd5) {
      loadFileInfo()
    }
  })
} else {
  console.log('Vue watch not available in this setup')
}
</script>

<template>
  <Teleport to="body">
    <div v-if="show" class="modal-overlay" @click.self="emit('close')">
      <div class="preview-modal">
        <div class="preview-header">
          <div class="header-left">
            <div class="file-icon" :class="fileType">
              <span class="file-icon-text">
                {{ fileType === 'pdf' ? 'PDF' : fileType === 'image' ? 'IMG' : fileType === 'text' ? 'TXT' : 'FILE' }}
              </span>
            </div>
            <div class="file-meta">
              <div class="file-name">{{ fileInfo?.fileName || '加载中...' }}</div>
              <div class="file-details" v-if="fileInfo">
                <span>{{ fileSizeText }}</span>
                <span class="separator">·</span>
                <span>{{ fileInfo.createdAt }}</span>
              </div>
            </div>
          </div>
          
          <div class="header-actions">
            <button class="action-btn download" @click="downloadFile" :disabled="loading">
              下载
            </button>
            <button class="action-btn close" @click="emit('close')">
              <IconClose :size="20" />
            </button>
          </div>
        </div>
        
        <div class="preview-body">
          <div v-if="loading" class="loading-state">
            <div class="spinner"></div>
            <span>加载中...</span>
          </div>
          
          <div v-else-if="error" class="error-state">
            <div class="error-icon">❌</div>
            <div class="error-text">{{ error }}</div>
          </div>
          
          <div v-else-if="fileType === 'pdf'" class="pdf-preview">
            <iframe :src="fileUrl" class="pdf-iframe" title="PDF Preview"></iframe>
          </div>
          
          <div v-else-if="fileType === 'image'" class="image-preview">
            <img :src="fileUrl" :alt="fileInfo?.fileName" class="preview-image" />
          </div>
          
          <div v-else-if="fileType === 'text'" class="text-preview">
            <div class="text-container">
              <div class="text-placeholder">
                <div class="placeholder-icon">📄</div>
                <div class="placeholder-text">文本预览（暂不支持在线查看，请下载查看）</div>
              </div>
            </div>
          </div>
          
          <div v-else class="other-preview">
            <div class="other-container">
              <div class="other-icon">📁</div>
              <div class="other-text">该文件类型暂不支持在线预览</div>
              <button class="download-btn" @click="downloadFile">下载文件</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  padding: 40px;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.preview-modal {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  max-width: 1100px;
  width: 100%;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.file-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 12px;
  color: white;
  flex-shrink: 0;
}

.file-icon.pdf {
  background: linear-gradient(135deg, #ff6b6b, #ee5a5a);
}

.file-icon.image {
  background: linear-gradient(135deg, #4facfe, #00f2fe);
}

.file-icon.text {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.file-icon.unknown,
.file-icon.other {
  background: linear-gradient(135deg, #a8a8a8, #888888);
}

.file-icon-text {
  letter-spacing: 0.5px;
}

.file-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.file-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 500px;
}

.file-details {
  font-size: 13px;
  color: var(--apple-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.separator {
  opacity: 0.5;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 18px;
  border-radius: 10px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn.download {
  background: var(--apple-blue);
  color: white;
}

.action-btn.download:hover:not(:disabled) {
  background: var(--apple-blue-hover);
  transform: translateY(-1px);
}

.action-btn.download:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-btn.close {
  width: 38px;
  height: 38px;
  padding: 0;
  background: transparent;
  color: var(--apple-text-secondary);
}

.action-btn.close:hover {
  background: var(--apple-gray-light);
  color: var(--apple-text);
}

.preview-body {
  flex: 1;
  min-height: 0;
  background: #f8f9fa;
  display: flex;
  align-items: stretch;
  justify-content: center;
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: var(--apple-text-secondary);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e5e7eb;
  border-top-color: var(--apple-blue);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.error-icon {
  font-size: 48px;
}

.error-text {
  font-size: 15px;
}

.pdf-preview,
.image-preview,
.text-preview,
.other-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: stretch;
  justify-content: center;
}

.pdf-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: white;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  background: white;
}

.text-container,
.other-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 40px;
}

.placeholder-icon,
.other-icon {
  font-size: 64px;
}

.placeholder-text,
.other-text {
  font-size: 15px;
  color: var(--apple-text-secondary);
  text-align: center;
}

.download-btn {
  padding: 12px 24px;
  background: var(--apple-blue);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.download-btn:hover {
  background: var(--apple-blue-hover);
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .modal-overlay {
    padding: 16px;
  }

  .file-name {
    max-width: 200px;
  }

  .preview-modal {
    max-height: 90vh;
  }
}
</style>
