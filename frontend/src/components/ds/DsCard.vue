<template>
  <div
    :class="[
      'ds-card',
      `ds-card--${variant}`,
      { 'ds-card--clickable': clickable },
      { 'ds-card--elevated': elevated }
    ]"
    @click="handleClick"
  >
    <div v-if="$slots.header" class="ds-card__header">
      <slot name="header"></slot>
    </div>
    <div class="ds-card__body">
      <slot></slot>
    </div>
    <div v-if="$slots.footer" class="ds-card__footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  variant?: 'default' | 'outlined' | 'filled'
  clickable?: boolean
  elevated?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  clickable: false,
  elevated: false
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

function handleClick(event: MouseEvent) {
  if (props.clickable) {
    emit('click', event)
  }
}
</script>

<style scoped>
.ds-card {
  background: var(--ds-color-bg-primary);
  border-radius: var(--ds-radius-lg);
  transition: all var(--ds-transition-normal);
}

/* Variants */
.ds-card--default {
  border: 1px solid var(--ds-color-border-light);
}

.ds-card--outlined {
  border: 1px solid var(--ds-color-border);
}

.ds-card--filled {
  background: var(--ds-color-bg-secondary);
  border: none;
}

/* States */
.ds-card--clickable {
  cursor: pointer;
}

.ds-card--clickable:hover {
  transform: translateY(-2px);
}

.ds-card--elevated {
  box-shadow: var(--ds-shadow-md);
}

.ds-card--elevated:hover {
  box-shadow: var(--ds-shadow-lg);
}

/* Sections */
.ds-card__header {
  padding: var(--ds-space-lg) var(--ds-space-xl) var(--ds-space-md);
  border-bottom: 1px solid var(--ds-color-border-light);
}

.ds-card__body {
  padding: var(--ds-space-xl);
}

.ds-card__footer {
  padding: var(--ds-space-md) var(--ds-space-xl) var(--ds-space-lg);
  border-top: 1px solid var(--ds-color-border-light);
  display: flex;
  align-items: center;
  gap: var(--ds-space-sm);
}
</style>
