<script setup lang="ts">
import { ref } from 'vue'
import ChatPanel from '../components/ChatPanel.vue'
import DocsPanel from '../components/DocsPanel.vue'

const docsOpen = ref(false)

function toggleDocs() {
  docsOpen.value = !docsOpen.value
}

function closeDocs() {
  docsOpen.value = false
}
</script>

<template>
  <div class="workspace-shell">
    <main class="chat-main">
      <ChatPanel :docs-open="docsOpen" @toggle-docs="toggleDocs" />
    </main>

    <!-- 使用 Teleport 将 DocsPanel 渲染到 body，确保居中显示 -->
    <Teleport to="body">
      <DocsPanel v-if="docsOpen" :show-mobile-close="true" @close="closeDocs" />
    </Teleport>
  </div>
</template>
