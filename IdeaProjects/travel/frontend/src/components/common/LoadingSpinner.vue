<template>
  <div class="flex items-center justify-center py-12">
    <div class="flex flex-col items-center gap-3 rounded-[1.5rem] border border-white/80 bg-white/80 px-8 py-6 shadow-[0_20px_60px_-40px_rgba(15,23,42,0.45)] backdrop-blur">
      <div class="relative h-10 w-10">
        <div class="absolute inset-0 rounded-full border-2 border-stone-200" />
        <div class="absolute inset-0 rounded-full border-2 border-transparent border-t-stone-800 border-r-amber-400 animate-spin" />
      </div>
      <p class="text-sm font-medium text-stone-500">{{ text || '加载中...' }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ text?: string }>()
</script>