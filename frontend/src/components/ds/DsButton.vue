<template>
  <button
    :class="[
      'ds-button',
      `ds-button--${variant}`,
      `ds-button--${size}`,
      { 'ds-button--disabled': disabled || loading },
      { 'ds-button--loading': loading }
    ]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="ds-button__spinner"></span>
    <span class="ds-button__content">
      <slot></slot>
    </span>
  </button>
</template>

<script setup lang="ts">
interface Props {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'success'
  size?: 'sm' | 'md' | 'lg'
  disabled?: boolean
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
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
.ds-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-sm);
  font-family: var(--ds-font-family);
  font-weight: var(--ds-font-weight-medium);
  border: none;
  border-radius: var(--ds-radius-full);
  cursor: pointer;
  transition: all var(--ds-transition-fast);
  user-select: none;
  white-space: nowrap;
}

.ds-button:focus-visible {
  outline: none;
  box-shadow: 0 0 0 4px var(--focus-ring);
}

.ds-button:hover:not(.ds-button--disabled) {
  transform: translateY(-1px);
}

.ds-button:active:not(.ds-button--disabled) {
  transform: translateY(0);
}

/* Variants */
.ds-button--primary {
  background: var(--ds-color-primary);
  color: var(--ds-color-text-inverse);
}

.ds-button--primary:hover:not(.ds-button--disabled) {
  background: var(--ds-color-primary-hover);
}

.ds-button--primary:active:not(.ds-button--disabled) {
  background: var(--ds-color-primary-active);
}

.ds-button--secondary {
  background: var(--ds-color-bg-secondary);
  color: var(--ds-color-text-primary);
}

.ds-button--secondary:hover:not(.ds-button--disabled) {
  background: var(--ds-color-bg-tertiary);
}

.ds-button--outline {
  background: transparent;
  color: var(--ds-color-primary);
  border: 1px solid var(--ds-color-border);
}

.ds-button--outline:hover:not(.ds-button--disabled) {
  background: var(--ds-color-primary-light);
  border-color: var(--ds-color-primary);
}

.ds-button--ghost {
  background: transparent;
  color: var(--ds-color-text-secondary);
}

.ds-button--ghost:hover:not(.ds-button--disabled) {
  background: var(--ds-color-bg-secondary);
  color: var(--ds-color-text-primary);
}

.ds-button--danger {
  background: var(--ds-color-error);
  color: var(--ds-color-text-inverse);
}

.ds-button--danger:hover:not(.ds-button--disabled) {
  background: var(--ds-color-error-hover);
}

.ds-button--success {
  background: var(--ds-color-success);
  color: var(--ds-color-text-inverse);
}

.ds-button--success:hover:not(.ds-button--disabled) {
  background: var(--ds-color-success-hover);
}

/* Sizes */
.ds-button--sm {
  padding: 8px 16px;
  font-size: var(--ds-font-size-sm);
}

.ds-button--md {
  padding: 12px 24px;
  font-size: var(--ds-font-size-md);
}

.ds-button--lg {
  padding: 16px 32px;
  font-size: var(--ds-font-size-lg);
}

/* States */
.ds-button--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ds-button--loading {
  pointer-events: none;
}

.ds-button__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: ds-spin 0.8s linear infinite;
}

.ds-button__content {
  display: flex;
  align-items: center;
  gap: var(--ds-space-sm);
}

@keyframes ds-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
