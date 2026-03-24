<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteDocs, getDocs, getDownloadUrl } from '../api/rag'
import type { DocItem } from '../api/types'
import {
  IconClose,
  IconSearch,
  IconDelete,
  IconChevronLeft,
  IconChevronRight,
  IconEmpty
} from './icons'

const props = defineProps<{
  showMobileClose: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const PAGE_SIZE = 10

const state = reactive({
  currentPage: 1,
  docs: [] as DocItem[],
  keyword: '',
  deleting: false
})

const selected = ref<string[]>([])
const toDelete = ref<string[]>([])
const showConfirm = ref(false)

const toast = ref({ text: '', type: 'success' as 'success' | 'error', visible: false })

const filteredDocs = computed(() => {
  const key = state.keyword.trim().toLowerCase()
  if (!key) return state.docs
  return state.docs.filter((d) => (d.fileName || '').toLowerCase().includes(key))
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredDocs.value.length / PAGE_SIZE)))

const pagedDocs = computed(() => {
  const start = (state.currentPage - 1) * PAGE_SIZE
  return filteredDocs.value.slice(start, start + PAGE_SIZE)
})

function showToast(text: string, type: 'success' | 'error' = 'error') {
  toast.value = { text, type, visible: true }
  window.setTimeout(() => {
    toast.value.visible = false
  }, 2500)
}

async function loadDocs() {
  try {
    const data = await getDocs()
    state.docs = data?.docs || []
  } catch (error) {
    showToast(error instanceof Error ? error.message : '获取文档列表失败', 'error')
  }
}

function openDelete(md5s: string[]) {
  toDelete.value = md5s
  showConfirm.value = true
}

async function confirmDelete() {
  if (toDelete.value.length === 0 || state.deleting) return
  state.deleting = true
  try {
    await deleteDocs(toDelete.value)
    showToast(`删除成功（${toDelete.value.length}个）`, 'success')
    selected.value = []
    showConfirm.value = false
    toDelete.value = []
    await loadDocs()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '删除失败', 'error')
    showConfirm.value = false
  } finally {
    state.deleting = false
  }
}

/**
 * 下载文件 - 使用原文件名，支持浏览器选择下载位置
 */
async function downloadFile(item: DocItem) {
  try {
    const url = getDownloadUrl(item.fileMd5)
    const response = await fetch(url)
    
    if (!response.ok) {
      throw new Error('下载失败')
    }
    
    // 获取文件内容
    const blob = await response.blob()
    
    // 使用原文件名
    const fileName = item.fileName || 'document'
    
    // 创建下载链接
    const downloadUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    
    // 清理URL对象
    window.URL.revokeObjectURL(downloadUrl)
    
    showToast(`开始下载: ${fileName}`, 'success')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '下载失败', 'error')
  }
}

function toggleAll(checked: boolean) {
  if (checked) {
    selected.value = pagedDocs.value.map((d) => d.fileMd5)
  } else {
    selected.value = []
  }
}

onMounted(loadDocs)
</script>

<template>
  <!-- 统一使用 modal-overlay 作为遮罩层 -->
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="docs-modal">
      <header class="docs-modal-header">
        <h1>文档列表</h1>
        <button class="close-btn" title="关闭" @click="emit('close')">
          <IconClose :size="20" />
        </button>
      </header>

      <div class="docs-modal-body">
        <div class="toolbar">
          <div class="toolbar-left">
            <label class="checkbox-wrapper">
              <input
                type="checkbox"
                :checked="pagedDocs.length > 0 && selected.length === pagedDocs.length"
                @change="(e) => toggleAll((e.target as HTMLInputElement).checked)"
              >
              <span>全选</span>
            </label>
            <button class="btn-secondary" :disabled="selected.length === 0" @click="openDelete(selected)">
              删除
            </button>
          </div>

          <div class="search-box">
            <IconSearch class="search-icon" :size="16" color="#86868B" />
            <input v-model="state.keyword" type="text" placeholder="搜索文档...">
          </div>
        </div>

        <div class="docs-count">{{ filteredDocs.length }} 个文档</div>

        <div class="docs-list">
          <div v-if="filteredDocs.length > 0" class="docs-items">
            <div
              v-for="item in pagedDocs"
              :key="item.fileMd5"
              class="doc-item"
              :class="{ selected: selected.includes(item.fileMd5) }"
            >
              <div class="doc-checkbox">
                <input v-model="selected" type="checkbox" :value="item.fileMd5">
              </div>
              <div class="doc-info">
                <div class="doc-name">{{ item.fileName || '未知文件名' }}</div>
                <div class="doc-date">{{ item.createdAt || '未知时间' }}</div>
              </div>
              <button class="doc-delete" @click="openDelete([item.fileMd5])">
                <IconDelete :size="16" />
              </button>
              <button class="doc-download" @click="downloadFile(item)" title="下载">
                ↓
              </button>
            </div>
          </div>
          <div v-else class="empty-state">
            <div class="empty-icon">
              <IconEmpty :size="48" />
            </div>
            <div class="empty-text">暂无文档</div>
          </div>
        </div>

        <!-- 分页按钮 -->
        <div v-if="totalPages > 1" class="pagination">
          <button
            class="page-btn"
            :disabled="state.currentPage <= 1"
            @click="state.currentPage -= 1"
            type="button"
          >
            <IconChevronLeft :size="18" />
          </button>
          <span class="page-info">{{ state.currentPage }} / {{ totalPages }}</span>
          <button
            class="page-btn"
            :disabled="state.currentPage >= totalPages"
            @click="state.currentPage += 1"
            type="button"
          >
            <IconChevronRight :size="18" />
          </button>
        </div>


      </div>
    </div>

    <!-- 确认删除模态框 -->
    <div v-if="showConfirm" class="confirm-modal" @click.self="showConfirm = false">
      <div class="confirm-modal-content">
        <h3>确认删除</h3>
        <p>确定要删除选中的 {{ toDelete.length }} 个文档吗？<br>此操作无法撤销。</p>
        <div class="modal-buttons">
          <button class="modal-btn modal-cancel" @click="showConfirm = false">取消</button>
          <button class="modal-btn modal-confirm" :disabled="state.deleting" @click="confirmDelete">
            {{ state.deleting ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div class="toast" :class="[toast.type, { show: toast.visible }]">
      {{ toast.text }}
    </div>
  </div>
</template>

<style scoped>
/* 统一遮罩层样式 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  animation: fadeIn 0.25s ease;
  padding: 20px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 文档模态框 - 固定尺寸 */
.docs-modal {
  background: var(--apple-bg);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  width: 600px;
  height: 600px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.docs-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--apple-border-light);
  background: var(--apple-bg);
  flex-shrink: 0;
}

.docs-modal-header h1 {
  margin: 0;
  font-size: 1.3em;
  font-weight: 600;
  color: var(--apple-text);
  letter-spacing: -0.02em;
}

.close-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--apple-bg-secondary);
  color: var(--apple-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.close-btn:hover {
  background: var(--apple-border-light);
  color: var(--apple-text);
  transform: scale(1.05);
}

.docs-modal-body {
  padding: 20px 24px 24px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 0.95em;
  color: var(--apple-text);
  font-weight: 500;
}

.checkbox-wrapper input[type="checkbox"] {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  border: 2px solid var(--apple-border);
  cursor: pointer;
  accent-color: var(--apple-blue);
}

.btn-secondary {
  padding: 8px 16px;
  border-radius: var(--radius-full);
  border: none;
  background: var(--apple-bg-secondary);
  color: var(--apple-text);
  font-size: 0.9em;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-secondary:hover:not(:disabled) {
  background: var(--apple-border-light);
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.search-box {
  position: relative;
  flex: 1;
  max-width: 240px;
}

.search-box input {
  width: 100%;
  padding: 10px 14px 10px 40px;
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-full);
  font-size: 0.95em;
  background: var(--apple-bg-secondary);
  color: var(--apple-text);
  transition: all var(--transition-fast);
  box-sizing: border-box;
}

.search-box input:focus {
  border-color: var(--apple-blue);
  background: var(--apple-bg);
  outline: none;
  box-shadow: 0 0 0 3px var(--focus-ring);
}

.search-box input::placeholder {
  color: var(--apple-text-secondary);
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
}

.docs-count {
  font-size: 0.85em;
  color: var(--apple-text-secondary);
  margin-bottom: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

.docs-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 16px;
  border-radius: var(--radius-md);
  background: var(--apple-bg-secondary);
  padding: 8px;
  min-height: 0;
}

.docs-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--apple-bg);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 2px solid transparent;
}

.doc-item:hover {
  background: var(--apple-bg);
  border-color: var(--apple-border);
}

.doc-item.selected {
  border-color: var(--apple-blue);
  background: var(--apple-blue-light);
}

.doc-checkbox input[type="checkbox"] {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  border: 2px solid var(--apple-border);
  cursor: pointer;
  accent-color: var(--apple-blue);
  flex-shrink: 0;
}

.doc-info {
  flex: 1;
  min-width: 0;
}

.doc-name {
  font-size: 0.95em;
  font-weight: 500;
  color: var(--apple-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.doc-date {
  font-size: 0.8em;
  color: var(--apple-text-secondary);
}

.doc-delete {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--apple-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  opacity: 0;
  flex-shrink: 0;
}

.doc-item:hover .doc-delete {
  opacity: 1;
}

.doc-delete:hover {
  background: var(--apple-danger);
  color: white;
  transform: scale(1.1);
}

.doc-download {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--apple-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  opacity: 0;
  flex-shrink: 0;
}

.doc-item:hover .doc-download {
  opacity: 1;
}

.doc-download:hover {
  background: var(--apple-blue);
  color: white;
  transform: scale(1.1);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  color: var(--apple-text-secondary);
}

.empty-icon {
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 1em;
  font-weight: 500;
}

/* 分页按钮 - 修复样式 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  flex-shrink: 0;
  padding-top: 8px;
  border-top: 1px solid var(--apple-border-light);
}

.page-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: var(--apple-bg-secondary);
  color: var(--apple-text);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.page-btn:hover:not(:disabled) {
  background: var(--apple-border-light);
  transform: scale(1.08);
}

.page-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.page-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
  background: var(--apple-border-light);
}

.page-info {
  font-size: 0.95em;
  color: var(--apple-text);
  font-weight: 600;
  min-width: 80px;
  text-align: center;
  letter-spacing: 0.5px;
}

/* 头部关闭按钮 */
.close-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--apple-bg-secondary);
  color: var(--apple-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.close-btn:hover {
  background: var(--apple-border-light);
  color: var(--apple-text);
  transform: scale(1.05);
}

/* 确认删除模态框 */
.confirm-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2100;
  animation: fadeIn 0.2s ease;
}

.confirm-modal-content {
  background: var(--apple-bg);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  width: 380px;
  max-width: 90vw;
  padding: 28px;
  text-align: center;
  animation: scaleIn 0.25s ease;
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.confirm-modal-content h3 {
  margin: 0 0 12px 0;
  font-size: 1.25em;
  font-weight: 600;
  color: var(--apple-text);
}

.confirm-modal-content p {
  margin: 0 0 24px 0;
  color: var(--apple-text-secondary);
  line-height: 1.6;
  font-size: 0.95em;
}

.modal-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.modal-btn {
  padding: 12px 28px;
  border: none;
  border-radius: var(--radius-full);
  font-size: 0.95em;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.modal-btn:hover {
  transform: scale(1.02);
}

.modal-cancel {
  background: var(--apple-bg-secondary);
  color: var(--apple-text);
}

.modal-cancel:hover {
  background: var(--apple-border-light);
}

.modal-confirm {
  background: var(--apple-danger);
  color: white;
}

.modal-confirm:hover:not(:disabled) {
  background: #e6352b;
}

.modal-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
  transition: all var(--transition-normal);
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

/* 移动端适配 */
@media (max-width: 768px) {
  .modal-overlay {
    padding: 16px;
  }

  .docs-modal {
    width: 100%;
    height: 80vh;
  }

  .docs-modal-header {
    padding: 16px 20px;
  }

  .docs-modal-body {
    padding: 16px 20px 20px;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .search-box {
    max-width: none;
  }

  .doc-delete,
  .doc-download {
    opacity: 1;
  }

  .pagination {
    gap: 16px;
  }

  .page-btn {
    width: 44px;
    height: 44px;
  }
}

@media (max-width: 480px) {
  .modal-overlay {
    padding: 12px;
  }

  .docs-modal-header h1 {
    font-size: 1.15em;
  }

  .doc-item {
    padding: 10px 12px;
  }

  .doc-date {
    display: none;
  }

  .page-info {
    font-size: 0.9em;
  }
}
</style>
