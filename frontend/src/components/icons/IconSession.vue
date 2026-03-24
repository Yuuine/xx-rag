<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 18 18"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      d="M3 6H15M6 6V4C6 3.44772 6.44772 3 7 3H11C11.5523 3 12 3.44772 12 4V6M7 9V13M11 9V13"
      :stroke="color"
      stroke-width="1.5"
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
  size: 18,
  color: 'currentColor'
})
</script>
