<template>
  <div class="max-w-4xl mx-auto px-6 py-12">
    <h1 class="text-2xl font-semibold text-stone-900 mb-8">实时状态</h1>

    <!-- 景点实时状态查询 -->
    <section class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-lg font-medium text-stone-900 mb-4">景点实时状态查询</h2>
      <div class="flex gap-3">
        <input
          v-model="singleAttractionId"
          type="text"
          placeholder="输入 attractionId"
          class="flex-1 px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
        />
        <button
          class="bg-stone-900 text-white rounded-lg px-6 py-2 text-sm font-medium hover:bg-stone-800 transition-colors"
          @click="fetchSingleStatus"
        >查询</button>
      </div>
      <div v-if="singleResult" class="mt-4">
        <RealtimeCard :data="singleResult" />
      </div>
    </section>

    <!-- 批量查询 -->
    <section class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-lg font-medium text-stone-900 mb-4">批量查询</h2>
      <div class="flex gap-3">
        <input
          v-model="batchIds"
          type="text"
          placeholder="输入 attractionId，逗号分隔（如 1,2,3）"
          class="flex-1 px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
        />
        <button
          class="bg-stone-900 text-white rounded-lg px-6 py-2 text-sm font-medium hover:bg-stone-800 transition-colors"
          @click="fetchBatchStatus"
        >查询</button>
      </div>
      <div v-if="batchResults.length" class="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
        <RealtimeCard v-for="item in batchResults" :key="item.attractionId" :data="item" />
      </div>
    </section>

    <!-- 拥挤景点列表 -->
    <section class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-lg font-medium text-stone-900 mb-4">拥挤景点列表</h2>
      <div class="flex gap-3">
        <input
          v-model="minCrowdLevel"
          type="number"
          placeholder="最小拥挤等级（默认 3）"
          class="flex-1 px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
        />
        <button
          class="bg-stone-900 text-white rounded-lg px-6 py-2 text-sm font-medium hover:bg-stone-800 transition-colors"
          @click="fetchCrowdedList"
        >查询</button>
      </div>
      <div v-if="crowdedList.length" class="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
        <RealtimeCard v-for="item in crowdedList" :key="item.attractionId" :data="item" />
      </div>
    </section>

    <!-- 历史人流均值 -->
    <section class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-lg font-medium text-stone-900 mb-4">历史人流均值</h2>
      <div class="flex gap-3">
        <input
          v-model="historicalAvgId"
          type="text"
          placeholder="输入 attractionId"
          class="flex-1 px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
        />
        <button
          class="bg-stone-900 text-white rounded-lg px-6 py-2 text-sm font-medium hover:bg-stone-800 transition-colors"
          @click="fetchHistoricalAvg"
        >查询</button>
      </div>
      <div v-if="historicalAvgResult != null" class="mt-4 p-4 bg-stone-50 rounded-lg">
        <p class="text-sm text-stone-600">历史人流均值：<span class="font-semibold text-stone-900">{{ historicalAvgResult }}</span> 人</p>
      </div>
    </section>

    <!-- 近7天人流均值 -->
    <section class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-lg font-medium text-stone-900 mb-4">近7天人流均值</h2>
      <div class="flex gap-3">
        <input
          v-model="sevenDaysAvgId"
          type="text"
          placeholder="输入 attractionId"
          class="flex-1 px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
        />
        <button
          class="bg-stone-900 text-white rounded-lg px-6 py-2 text-sm font-medium hover:bg-stone-800 transition-colors"
          @click="fetchSevenDaysAvg"
        >查询</button>
      </div>
      <div v-if="sevenDaysAvgResult != null" class="mt-4 p-4 bg-stone-50 rounded-lg">
        <p class="text-sm text-stone-600">近7天人流均值：<span class="font-semibold text-stone-900">{{ sevenDaysAvgResult }}</span> 人</p>
      </div>
    </section>

    <!-- 活跃预警信息 -->
    <section class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-medium text-stone-900">活跃预警信息</h2>
        <button
          class="bg-stone-900 text-white rounded-lg px-6 py-2 text-sm font-medium hover:bg-stone-800 transition-colors"
          @click="fetchWarns"
        >刷新</button>
      </div>
      <LoadingSpinner v-if="warnsLoading" />
      <template v-else>
        <div v-if="warns.length" class="space-y-3">
          <div
            v-for="(warn, idx) in warns"
            :key="idx"
            class="p-4 rounded-lg border"
            :class="warn.severity === 'high' ? 'bg-red-50 border-red-200' : warn.severity === 'medium' ? 'bg-amber-50 border-amber-200' : 'bg-blue-50 border-blue-200'"
          >
            <div class="flex items-start justify-between">
              <div>
                <p class="text-sm font-medium text-stone-900">{{ warn.title || warn.message || '预警信息' }}</p>
                <p v-if="warn.attractionName" class="text-xs text-stone-500 mt-1">景点：{{ warn.attractionName }}</p>
                <p v-if="warn.createTime" class="text-xs text-stone-400 mt-1">{{ warn.createTime }}</p>
              </div>
              <span
                class="text-xs px-2 py-0.5 rounded-full shrink-0"
                :class="warn.severity === 'high' ? 'bg-red-200 text-red-700' : warn.severity === 'medium' ? 'bg-amber-200 text-amber-700' : 'bg-blue-200 text-blue-700'"
              >{{ warn.severity === 'high' ? '高危' : warn.severity === 'medium' ? '中危' : '低危' }}</span>
            </div>
          </div>
        </div>
        <p v-else class="text-sm text-stone-400 text-center py-8">暂无活跃预警</p>
      </template>
    </section>

    <!-- 需要同步的状态 -->
    <section class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-lg font-medium text-stone-900 mb-4">需要同步的状态</h2>
      <div class="flex gap-3">
        <input
          v-model="needSyncMinutes"
          type="number"
          placeholder="多少分钟内未同步（默认 30）"
          class="flex-1 px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
        />
        <button
          class="bg-stone-900 text-white rounded-lg px-6 py-2 text-sm font-medium hover:bg-stone-800 transition-colors"
          @click="fetchNeedSync"
        >查询</button>
      </div>
      <div v-if="needSyncList.length" class="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
        <RealtimeCard v-for="item in needSyncList" :key="item.attractionId" :data="item" />
      </div>
    </section>

    <!-- 交通信息查询 -->
    <section class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-lg font-medium text-stone-900 mb-4">交通信息查询</h2>
      <div class="flex gap-3">
        <input
          v-model="trafficAttractionId"
          type="text"
          placeholder="输入 attractionId"
          class="flex-1 px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
        />
        <button
          class="bg-stone-900 text-white rounded-lg px-6 py-2 text-sm font-medium hover:bg-stone-800 transition-colors"
          @click="fetchTraffic"
        >查询</button>
      </div>
      <div v-if="trafficResult" class="mt-4 p-4 bg-stone-50 rounded-lg border border-stone-200">
        <div class="space-y-2 text-sm">
          <p class="text-stone-700">交通状况: <span :class="trafficStatusColor">{{ trafficResult.status || trafficResult.level || '未知' }}</span></p>
          <p v-if="trafficResult.speed" class="text-stone-600">平均速度: {{ trafficResult.speed }} km/h</p>
          <p v-if="trafficResult.congestion" class="text-stone-600">拥堵指数: {{ trafficResult.congestion }}</p>
          <p v-if="trafficResult.description" class="text-stone-500 text-xs">{{ trafficResult.description }}</p>
        </div>
      </div>
    </section>

    <!-- 批量更新状态 -->
    <section class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-medium text-stone-900">批量更新状态</h2>
        <button
          class="bg-stone-900 text-white rounded-lg px-6 py-2 text-sm font-medium hover:bg-stone-800 transition-colors"
          :disabled="batchUpdateLoading"
          @click="batchUpdateStatus"
        >{{ batchUpdateLoading ? '更新中...' : '批量更新所有景点' }}</button>
      </div>
      <p v-if="batchUpdateResult" class="text-sm" :class="batchUpdateResult.includes('成功') ? 'text-emerald-600' : 'text-stone-600'">{{ batchUpdateResult }}</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { realtimeApi } from '../api/realtime.api'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import RealtimeCard from '../components/RealtimeCard.vue'
import { CROWD_LEVEL_MEDIUM } from '../constants'

// 单个查询
const singleAttractionId = ref('')
const singleResult = ref<any>(null)

function fetchSingleStatus() {
  const id = singleAttractionId.value.trim()
  if (!id) return
  realtimeApi.getAttractionRealtimeStatus(Number(id))
    .then((res) => { singleResult.value = res })
    .catch(() => { singleResult.value = null })
}

// 批量查询
const batchIds = ref('')
const batchResults = ref<any[]>([])

function fetchBatchStatus() {
  const ids = batchIds.value.split(',').map(s => s.trim()).filter(Boolean).map(Number)
  if (!ids.length) return
  realtimeApi.getBatchRealtimeStatus(ids)
    .then((res) => { batchResults.value = Array.isArray(res) ? res : [] })
    .catch(() => { batchResults.value = [] })
}

// 拥挤景点列表
const minCrowdLevel = ref(String(CROWD_LEVEL_MEDIUM))
const crowdedList = ref<any[]>([])

function fetchCrowdedList() {
  const level = Number(minCrowdLevel.value) || CROWD_LEVEL_MEDIUM
  realtimeApi.getCrowdedAttractions(level)
    .then((res) => { crowdedList.value = Array.isArray(res) ? res : [] })
    .catch(() => { crowdedList.value = [] })
}

// 历史人流均值
const historicalAvgId = ref('')
const historicalAvgResult = ref<number | null>(null)

function fetchHistoricalAvg() {
  const id = historicalAvgId.value.trim()
  if (!id) return
  realtimeApi.getHistoricalAvgCrowdCount(Number(id))
    .then((res) => { historicalAvgResult.value = Number(res) || 0 })
    .catch(() => { historicalAvgResult.value = null })
}

// 近7天人流均值
const sevenDaysAvgId = ref('')
const sevenDaysAvgResult = ref<number | null>(null)

function fetchSevenDaysAvg() {
  const id = sevenDaysAvgId.value.trim()
  if (!id) return
  realtimeApi.get7DaysAvgCrowdCount(Number(id))
    .then((res) => { sevenDaysAvgResult.value = Number(res) || 0 })
    .catch(() => { sevenDaysAvgResult.value = null })
}

// 活跃预警
const warns = ref<any[]>([])
const warnsLoading = ref(false)

function fetchWarns() {
  warnsLoading.value = true
  realtimeApi.getActiveWarns()
    .then((res) => { warns.value = Array.isArray(res) ? res : [] })
    .catch(() => { warns.value = [] })
    .finally(() => { warnsLoading.value = false })
}

// 需要同步的状态
const DEFAULT_SYNC_MINUTES = 30
const needSyncMinutes = ref(String(DEFAULT_SYNC_MINUTES))
const needSyncList = ref<any[]>([])

function fetchNeedSync() {
  const minutes = Number(needSyncMinutes.value) || DEFAULT_SYNC_MINUTES
  realtimeApi.getNeedSyncStatus(minutes)
    .then((res) => { needSyncList.value = Array.isArray(res) ? res : [] })
    .catch(() => { needSyncList.value = [] })
}

// 交通信息
const trafficAttractionId = ref('')
const trafficResult = ref<any>(null)
const trafficStatusColor = ref('text-stone-600')

function fetchTraffic() {
  const id = trafficAttractionId.value.trim()
  if (!id) return
  trafficResult.value = null
  realtimeApi.getTrafficInfo(Number(id))
    .then((res) => {
      trafficResult.value = res
      const status = (res.status || res.level || '').toString()
      if (status.includes('拥堵') || status.includes('严重')) trafficStatusColor.value = 'text-red-600'
      else if (status.includes('缓慢')) trafficStatusColor.value = 'text-amber-600'
      else trafficStatusColor.value = 'text-emerald-600'
    })
    .catch(() => { trafficResult.value = null })
}

// 批量更新
const batchUpdateLoading = ref(false)
const batchUpdateResult = ref('')

function batchUpdateStatus() {
  batchUpdateResult.value = ''
  batchUpdateLoading.value = true
  realtimeApi.triggerBatchUpdate()
    .then(() => { batchUpdateResult.value = '批量更新成功' })
    .catch(() => { batchUpdateResult.value = '批量更新失败' })
    .finally(() => { batchUpdateLoading.value = false })
}

onMounted(() => {
  fetchWarns()
})
</script>