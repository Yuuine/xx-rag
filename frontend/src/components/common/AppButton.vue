<template>
  <button
    :class="[
      'app-btn',
      `app-btn--${variant}`,
      { 'app-btn--disabled': disabled, 'app-btn--loading': loading }
    ]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="app-btn__spinner" />
    <slot />
  </button>
</template>

<script setup lang="ts">
interface Props {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
  disabled?: boolean
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  disabled: false,
  loading: false
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

function handleClick(event: MouseEvent) {
  if (!props.disabled && !props.loading) {
    emit('click', event)
  }
}
</script>

<style scoped>
.app-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 20px;
  border: none;
  border-radius: var(--radius-full);
  font-size: 0.95em;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 40px;
}

.app-btn--primary {
  background: var(--apple-blue);
  color: white;
}

.app-btn--primary:hover:not(:disabled) {
  background: var(--apple-blue-hover);
  transform: scale(1.02);
}

.app-btn--secondary {
  background: var(--apple-bg-secondary);
  color: var(--apple-text);
}

.app-btn--secondary:hover:not(:disabled) {
  background: var(--apple-border-light);
}

.app-btn--danger {
  background: var(--apple-danger);
  color: white;
}

.app-btn--danger:hover:not(:disabled) {
  background: #e6352b;
}

.app-btn--ghost {
  background: transparent;
  color: var(--apple-text-secondary);
}

.app-btn--ghost:hover:not(:disabled) {
  background: var(--apple-bg-secondary);
  color: var(--apple-text);
}

.app-btn--disabled,
.app-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.app-btn__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
