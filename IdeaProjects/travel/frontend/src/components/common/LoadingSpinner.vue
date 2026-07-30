<template>
  <div class="flex items-center justify-center py-12">
    <div class="flex flex-col items-center gap-3">
      <div class="w-8 h-8 border-2 border-stone-200 border-t-stone-800 rounded-full animate-spin" />
      <p class="text-sm text-stone-500">{{ text || '加载中...' }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ text?: string }>()
</script>