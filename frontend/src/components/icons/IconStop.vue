<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 18 18"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <rect
      x="4"
      y="4"
      width="10"
      height="10"
      rx="2"
      :fill="color"
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
