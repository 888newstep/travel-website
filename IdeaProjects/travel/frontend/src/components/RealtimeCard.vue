<template>
  <div class="bg-stone-50 rounded-xl p-4 border border-stone-100">
    <div class="flex items-center justify-between mb-3">
      <span class="text-sm font-medium text-stone-900">景点 #{{ data.attractionId }}</span>
      <span
        class="text-xs px-2 py-0.5 rounded-full"
        :class="statusClass"
      >{{ statusLabel }}</span>
    </div>
    <div class="grid grid-cols-2 gap-2 text-sm">
      <div class="flex items-center gap-1">
        <span class="text-stone-400">拥挤度</span>
        <span class="text-stone-700 font-medium">{{ data.crowdCount ?? '-' }}人 (Lv{{ data.crowdLevel ?? '-' }})</span>
      </div>
      <div class="flex items-center gap-1">
        <span class="text-stone-400">等待时间</span>
        <span class="text-stone-700 font-medium">{{ data.waitTime != null ? data.waitTime + '分钟' : '-' }}</span>
      </div>
      <div class="flex items-center gap-1">
        <span class="text-stone-400">温度</span>
        <span class="text-stone-700 font-medium">{{ data.temperature != null ? data.temperature + '°C' : '-' }}</span>
      </div>
      <div class="flex items-center gap-1">
        <span class="text-stone-400">天气</span>
        <span class="text-stone-700 font-medium">{{ data.weather || '-' }}</span>
      </div>
      <div class="flex items-center gap-1">
        <span class="text-stone-400">状态</span>
        <span class="text-stone-700 font-medium">{{ data.status || '-' }}</span>
      </div>
      <div class="flex items-center gap-1">
        <span class="text-stone-400">开放</span>
        <span :class="data.openStatus ? 'text-emerald-600' : 'text-red-500'" class="font-medium">
          {{ data.openStatus ? '开放中' : '已关闭' }}
        </span>
      </div>
    </div>
    <div v-if="data.lastUpdateTime" class="mt-2 text-xs text-stone-400">
      更新于 {{ new Date(data.lastUpdateTime).toLocaleString() }}
    </div>
  </div>
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
  return '空闲'
})

const statusClass = computed(() => {
  if (!props.data.openStatus) return 'bg-red-50 text-red-600'
  const level = props.data.crowdLevel ?? 0
  if (level >= CROWD_LEVEL_HIGH) return 'bg-red-50 text-red-600'
  if (level >= CROWD_LEVEL_MEDIUM) return 'bg-amber-50 text-amber-600'
  return 'bg-emerald-50 text-emerald-600'
})
</script>