<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { deleteDocs, getDocs } from '../api/rag'
import type { DocItem } from '../api/types'

const router = useRouter()
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
  if (!key) {
    return state.docs
  }
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
  if (toDelete.value.length === 0 || state.deleting) {
    return
  }
  state.deleting = true
  try {
    await deleteDocs(toDelete.value)
    showToast(`删除成功（${toDelete.value.length}个）`, 'success')
    selected.value = []
    await loadDocs()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '删除失败', 'error')
  } finally {
    state.deleting = false
    showConfirm.value = false
    toDelete.value = []
  }
}

function toggleAll(checked: boolean) {
  if (checked) {
    selected.value = pagedDocs.value.map((d) => d.fileMd5)
  } else {
    selected.value = []
  }
}

function backToChat() {
  router.push('/')
}

onMounted(loadDocs)
</script>

<template>
  <div class="container">
    <header class="header-with-close">
      <h1>已上传文档列表</h1>
      <button class="close-btn" title="返回" @click="backToChat">×</button>
    </header>
    <div class="list">
      <div class="batch-actions">
        <div class="batch-left">
          <label style="display:flex; align-items:center; gap:8px;">
            <input
              type="checkbox"
              :checked="pagedDocs.length > 0 && selected.length === pagedDocs.length"
              @change="(e) => toggleAll((e.target as HTMLInputElement).checked)"
            >
            <span>全选</span>
          </label>
          <button class="batch-delete-btn" :disabled="selected.length === 0" @click="openDelete(selected)">批量删除</button>
        </div>
        <div class="doc-count-display">
          <span>{{ filteredDocs.length }}</span> 个文档
        </div>
        <div class="search-box">
          <input v-model="state.keyword" type="text" placeholder="搜索文档名称...">
        </div>
      </div>

      <table v-if="filteredDocs.length > 0" id="docTable">
        <thead>
          <tr>
            <th style="width: 50px;" />
            <th>文档名称</th>
            <th style="width: 180px;">创建时间</th>
            <th style="width: 80px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in pagedDocs" :key="item.fileMd5">
            <td>
              <input v-model="selected" type="checkbox" :value="item.fileMd5">
            </td>
            <td>{{ item.fileName || '未知文件名' }}</td>
            <td>{{ item.createdAt || '未知时间' }}</td>
            <td>
              <button class="delete-btn btn-danger" @click="openDelete([item.fileMd5])">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty">暂无上传文档</div>

      <div v-if="totalPages > 1" class="pagination-controls">
        <button class="page-btn" :disabled="state.currentPage <= 1" @click="state.currentPage -= 1">上一页</button>
        <span>第 {{ state.currentPage }} / {{ totalPages }} 页</span>
        <button class="page-btn" :disabled="state.currentPage >= totalPages" @click="state.currentPage += 1">下一页</button>
      </div>
    </div>
  </div>

  <div v-if="showConfirm" class="modal active">
    <div class="modal-content">
      <h3>确认删除</h3>
      <p>确定要删除选中的文档吗？<br>删除后无法恢复！</p>
      <div class="modal-buttons">
        <button class="modal-btn modal-cancel" @click="showConfirm = false">取消</button>
        <button class="modal-btn modal-confirm" :disabled="state.deleting" @click="confirmDelete">{{ state.deleting ? "删除中..." : "确认删除" }}</button>
      </div>
    </div>
  </div>

  <div class="toast" :class="[toast.type, { show: toast.visible }]">{{ toast.text }}</div>
</template>
