<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 18 18"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      d="M3 3H15V15H3V3Z"
      :stroke="color"
      stroke-width="1.5"
      stroke-linejoin="round"
    />
    <path
      d="M6 6H12M6 9H12M6 12H9"
      :stroke="color"
      stroke-width="1.5"
      stroke-linecap="round"
    />
  </svg>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  size?: number
  color?: string
}>(), {
  size: 18,
  color: 'currentColor'
})
</script>
