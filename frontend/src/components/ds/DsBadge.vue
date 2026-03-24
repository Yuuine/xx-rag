<template>
  <span
    :class="[
      'ds-badge',
      `ds-badge--${variant}`,
      `ds-badge--${size}`
    ]"
  >
    <slot></slot>
  </span>
</template>

<script setup lang="ts">
interface Props {
  variant?: 'default' | 'primary' | 'success' | 'warning' | 'error' | 'outline'
  size?: 'sm' | 'md' | 'lg'
}

withDefaults(defineProps<Props>(), {
  variant: 'default',
  size: 'md'
})
</script>

<style scoped>
.ds-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--ds-font-family);
  font-weight: var(--ds-font-weight-medium);
  border-radius: var(--ds-radius-full);
  white-space: nowrap;
}

/* Variants */
.ds-badge--default {
  background: var(--ds-color-bg-secondary);
  color: var(--ds-color-text-secondary);
}

.ds-badge--primary {
  background: var(--ds-color-primary);
  color: var(--ds-color-text-inverse);
}

.ds-badge--success {
  background: var(--ds-color-success);
  color: var(--ds-color-text-inverse);
}

.ds-badge--warning {
  background: var(--ds-color-warning);
  color: var(--ds-color-text-inverse);
}

.ds-badge--error {
  background: var(--ds-color-error);
  color: var(--ds-color-text-inverse);
}

.ds-badge--outline {
  background: transparent;
  color: var(--ds-color-text-secondary);
  border: 1px solid var(--ds-color-border);
}

/* Sizes */
.ds-badge--sm {
  padding: 2px 8px;
  font-size: var(--ds-font-size-xs);
}

.ds-badge--md {
  padding: 4px 12px;
  font-size: var(--ds-font-size-sm);
}

.ds-badge--lg {
  padding: 6px 16px;
  font-size: var(--ds-font-size-md);
}
</style>
