<template>
  <article class="surface-card surface-card-hover overflow-hidden rounded-[1.5rem] p-4 sm:p-5">
    <div class="flex items-start justify-between gap-3">
      <div>
        <p class="text-xs font-medium uppercase tracking-[0.24em] text-stone-400">Realtime Status</p>
        <h3 class="mt-2 text-base font-semibold text-stone-900">景点 #{{ data.attractionId }}</h3>
      </div>
      <span class="rounded-full px-3 py-1 text-xs font-medium" :class="statusClass">{{ statusLabel }}</span>
    </div>

    <div class="mt-4 grid gap-3 sm:grid-cols-2">
      <div class="rounded-2xl bg-stone-50/80 px-4 py-3">
        <p class="text-xs text-stone-400">拥挤人数</p>
        <p class="mt-1 text-sm font-semibold text-stone-700">{{ data.crowdCount ?? '-' }} 人 · Lv{{ data.crowdLevel ?? '-' }}</p>
      </div>
      <div class="rounded-2xl bg-stone-50/80 px-4 py-3">
        <p class="text-xs text-stone-400">等待时间</p>
        <p class="mt-1 text-sm font-semibold text-stone-700">{{ data.waitTime != null ? `${data.waitTime} 分钟` : '-' }}</p>
      </div>
      <div class="rounded-2xl bg-stone-50/80 px-4 py-3">
        <p class="text-xs text-stone-400">温度</p>
        <p class="mt-1 text-sm font-semibold text-stone-700">{{ data.temperature != null ? `${data.temperature}°C` : '-' }}</p>
      </div>
      <div class="rounded-2xl bg-stone-50/80 px-4 py-3">
        <p class="text-xs text-stone-400">天气</p>
        <p class="mt-1 text-sm font-semibold text-stone-700">{{ data.weather || '-' }}</p>
      </div>
    </div>

    <div class="mt-4 flex flex-wrap items-center gap-2 text-xs text-stone-500">
      <span class="rounded-full bg-stone-100 px-3 py-1.5">状态：{{ data.status || '-' }}</span>
      <span class="rounded-full px-3 py-1.5" :class="data.openStatus ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-600'">
        {{ data.openStatus ? '营业中' : '已关闭' }}
      </span>
    </div>

    <p v-if="data.lastUpdateTime" class="mt-4 text-xs text-stone-400">
      最近更新：{{ new Date(data.lastUpdateTime).toLocaleString() }}
    </p>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CROWD_LEVEL_HIGH, CROWD_LEVEL_MEDIUM } from '../constants'

const props = defineProps<{
  data: Record<string, any>
}>()

const statusLabel = computed(() => {
  if (!props.data.openStatus) return '已关闭'
  const level = props.data.crowdLevel ?? 0
  if (level >= CROWD_LEVEL_HIGH) return '拥挤'
  if (level >= CROWD_LEVEL_MEDIUM) return '适中'
  return '舒适'
})

const statusClass = computed(() => {
  if (!props.data.openStatus) return 'bg-rose-50 text-rose-600'
  const level = props.data.crowdLevel ?? 0
  if (level >= CROWD_LEVEL_HIGH) return 'bg-rose-50 text-rose-600'
  if (level >= CROWD_LEVEL_MEDIUM) return 'bg-amber-50 text-amber-700'
  return 'bg-emerald-50 text-emerald-700'
})
</script>