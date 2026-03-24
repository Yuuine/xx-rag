<template>
  <Teleport to="body">
    <Transition name="toast">
      <div
        v-if="visible"
        class="toast"
        :class="`toast--${type}`"
      >
        {{ message }}
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
interface Props {
  visible: boolean
  message: string
  type?: 'success' | 'error' | 'info'
}

withDefaults(defineProps<Props>(), {
  type: 'info'
})
</script>

<style scoped>
.toast {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  min-width: 280px;
  padding: 14px 24px;
  border-radius: var(--radius-lg);
  color: white;
  font-size: 0.95em;
  font-weight: 500;
  text-align: center;
  box-shadow: var(--shadow-lg);
  z-index: 3000;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.toast--success {
  background: rgba(52, 199, 89, 0.95);
}

.toast--error {
  background: rgba(255, 59, 48, 0.95);
}

.toast--info {
  background: rgba(0, 113, 227, 0.95);
}

/* Transition animations */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-100px);
}
</style>
