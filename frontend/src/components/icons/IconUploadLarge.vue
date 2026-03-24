<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 48 48"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      d="M24 8V32M24 8L14 18M24 8L34 18"
      :stroke="color"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
    <path
      d="M8 32V36C8 38.2091 9.79086 40 12 40H36C38.2091 40 40 38.2091 40 36V32"
      :stroke="color"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
  </svg>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  size?: number
  color?: string
}>(), {
  size: 48,
  color: 'var(--apple-blue)'
})
</script>
