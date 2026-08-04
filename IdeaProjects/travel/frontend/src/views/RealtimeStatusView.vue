<template>
  <div class="app-container pb-16 pt-4 md:pt-6">
    <section class="surface-card mb-8 overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(239,246,255,0.92)_48%,rgba(236,253,245,0.88))] px-6 py-8 sm:px-8 sm:py-9">
      <div class="grid gap-8 xl:grid-cols-[1.15fr_0.85fr] xl:items-center">
        <div>
          <div class="mb-4 flex flex-wrap gap-2">
            <span class="chip">景区实时状态</span>
            <span class="chip">拥挤与预警</span>
            <span class="chip">同步与交通信息</span>
          </div>
          <h1 class="text-3xl font-semibold tracking-tight text-stone-900 md:text-4xl">把实时状态信息整理成更易读的运维与查询面板</h1>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-stone-600 md:text-base">
            支持单点查询、批量状态、拥挤列表、历史均值、交通信息和预警浏览，让信息定位与处理更高效。
          </p>
          <div class="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">活跃预警</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ warns.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">批量结果</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ batchResults.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">拥挤景点</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ crowdedList.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">待同步状态</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ needSyncList.length }}</div>
            </div>
          </div>
        </div>

        <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
          <div class="mb-5 flex items-center justify-between gap-3">
            <div>
              <div class="text-sm font-medium text-stone-500">快捷入口</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">先查单个景点状态</div>
            </div>
            <span class="rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-600">实时反馈</span>
          </div>
          <div class="flex flex-col gap-3 sm:flex-row">
            <input
              v-model="singleAttractionId"
              type="text"
              placeholder="输入 attractionId"
              class="flex-1 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
            />
            <button
              class="rounded-2xl bg-stone-900 px-6 py-3 text-sm font-medium text-white transition hover:bg-stone-800"
              @click="fetchSingleStatus"
            >查询</button>
          </div>
          <div class="mt-4 grid gap-2 text-sm text-stone-500 sm:grid-cols-2">
            <div class="rounded-2xl bg-stone-50 px-4 py-3">可继续查看批量状态、拥挤等级和交通信息</div>
            <div class="rounded-2xl bg-stone-50 px-4 py-3">预警区会自动展示页面加载时获取到的活跃信息</div>
          </div>
          <div v-if="singleResult" class="mt-4">
            <RealtimeCard :data="singleResult" />
          </div>
        </div>
      </div>
    </section>

    <section class="mb-6 grid gap-6 xl:grid-cols-2">
      <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-stone-900">批量查询</h2>
          <p class="mt-1 text-sm text-stone-500">适合一次查看多个景点的开放、天气与拥挤状态。</p>
        </div>
        <div class="flex flex-col gap-3 sm:flex-row">
          <input
            v-model="batchIds"
            type="text"
            placeholder="输入 attractionId，逗号分隔（如 1,2,3）"
            class="flex-1 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
          />
          <button class="rounded-2xl bg-stone-900 px-6 py-3 text-sm font-medium text-white transition hover:bg-stone-800" @click="fetchBatchStatus">查询</button>
        </div>
        <div v-if="batchResults.length" class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
          <RealtimeCard v-for="item in batchResults" :key="item.attractionId" :data="item" />
        </div>
        <p v-else class="mt-4 text-sm text-stone-400">输入多个 attractionId 后可在这里看到批量结果。</p>
      </div>

      <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-stone-900">拥挤景点列表</h2>
          <p class="mt-1 text-sm text-stone-500">根据最小拥挤等级快速筛出需要关注的景点。</p>
        </div>
        <div class="flex flex-col gap-3 sm:flex-row">
          <input
            v-model="minCrowdLevel"
            type="number"
            placeholder="最小拥挤等级（默认 3）"
            class="flex-1 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
          />
          <button class="rounded-2xl bg-stone-900 px-6 py-3 text-sm font-medium text-white transition hover:bg-stone-800" @click="fetchCrowdedList">查询</button>
        </div>
        <div v-if="crowdedList.length" class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
          <RealtimeCard v-for="item in crowdedList" :key="item.attractionId" :data="item" />
        </div>
        <p v-else class="mt-4 text-sm text-stone-400">尚未筛出拥挤景点，或者还没有发起查询。</p>
      </div>
    </section>

    <section class="mb-6 grid gap-6 xl:grid-cols-2">
      <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-stone-900">历史人流均值</h2>
          <p class="mt-1 text-sm text-stone-500">查询长期平均人流，为异常波动判断提供参考。</p>
        </div>
        <div class="flex flex-col gap-3 sm:flex-row">
          <input
            v-model="historicalAvgId"
            type="text"
            placeholder="输入 attractionId"
            class="flex-1 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
          />
          <button class="rounded-2xl bg-stone-900 px-6 py-3 text-sm font-medium text-white transition hover:bg-stone-800" @click="fetchHistoricalAvg">查询</button>
        </div>
        <div v-if="historicalAvgResult != null" class="mt-4 rounded-2xl bg-stone-50 px-4 py-4 text-sm text-stone-600">
          历史人流均值：<span class="font-semibold text-stone-900">{{ historicalAvgResult }}</span> 人
        </div>
      </div>

      <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-stone-900">近 7 天人流均值</h2>
          <p class="mt-1 text-sm text-stone-500">用于判断最近一周是否持续升温或回落。</p>
        </div>
        <div class="flex flex-col gap-3 sm:flex-row">
          <input
            v-model="sevenDaysAvgId"
            type="text"
            placeholder="输入 attractionId"
            class="flex-1 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
          />
          <button class="rounded-2xl bg-stone-900 px-6 py-3 text-sm font-medium text-white transition hover:bg-stone-800" @click="fetchSevenDaysAvg">查询</button>
        </div>
        <div v-if="sevenDaysAvgResult != null" class="mt-4 rounded-2xl bg-stone-50 px-4 py-4 text-sm text-stone-600">
          近 7 天人流均值：<span class="font-semibold text-stone-900">{{ sevenDaysAvgResult }}</span> 人
        </div>
      </div>
    </section>

    <section class="mb-6 grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
      <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-semibold text-stone-900">活跃预警</h2>
            <p class="mt-1 text-sm text-stone-500">展示当前需要重点关注的拥挤、风险或系统预警。</p>
          </div>
          <button class="rounded-full border border-stone-200 bg-white px-4 py-2 text-sm font-medium text-stone-700 transition hover:bg-stone-50" @click="fetchWarns">刷新</button>
        </div>
        <LoadingSpinner v-if="warnsLoading" />
        <template v-else>
          <div v-if="warns.length" class="space-y-3">
            <div
              v-for="(warn, idx) in warns"
              :key="idx"
              class="rounded-2xl border p-4"
              :class="warn.severity === 'high' ? 'border-red-200 bg-red-50' : warn.severity === 'medium' ? 'border-amber-200 bg-amber-50' : 'border-blue-200 bg-blue-50'"
            >
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="text-sm font-medium text-stone-900">{{ warn.title || warn.message || '预警信息' }}</p>
                  <p v-if="warn.attractionName" class="mt-1 text-xs text-stone-500">景点：{{ warn.attractionName }}</p>
                  <p v-if="warn.createTime" class="mt-1 text-xs text-stone-400">{{ warn.createTime }}</p>
                </div>
                <span
                  class="shrink-0 rounded-full px-2.5 py-1 text-xs font-medium"
                  :class="warn.severity === 'high' ? 'bg-red-200 text-red-700' : warn.severity === 'medium' ? 'bg-amber-200 text-amber-700' : 'bg-blue-200 text-blue-700'"
                >{{ warn.severity === 'high' ? '高危' : warn.severity === 'medium' ? '中危' : '低危' }}</span>
              </div>
            </div>
          </div>
          <p v-else class="py-8 text-center text-sm text-stone-400">暂无活跃预警</p>
        </template>
      </div>

      <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-stone-900">交通信息查询</h2>
          <p class="mt-1 text-sm text-stone-500">快速查看指定景点周边交通状态、速度和拥堵指数。</p>
        </div>
        <div class="flex flex-col gap-3 sm:flex-row">
          <input
            v-model="trafficAttractionId"
            type="text"
            placeholder="输入 attractionId"
            class="flex-1 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
          />
          <button class="rounded-2xl bg-stone-900 px-6 py-3 text-sm font-medium text-white transition hover:bg-stone-800" @click="fetchTraffic">查询</button>
        </div>
        <div v-if="trafficResult" class="mt-4 rounded-2xl border border-stone-200 bg-stone-50 p-4">
          <div class="space-y-2 text-sm">
            <p class="text-stone-700">交通状况：<span :class="trafficStatusColor">{{ trafficResult.status || trafficResult.level || '未知' }}</span></p>
            <p v-if="trafficResult.speed" class="text-stone-600">平均速度：{{ trafficResult.speed }} km/h</p>
            <p v-if="trafficResult.congestion" class="text-stone-600">拥堵指数：{{ trafficResult.congestion }}</p>
            <p v-if="trafficResult.description" class="text-xs text-stone-500">{{ trafficResult.description }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="mb-6 grid gap-6 xl:grid-cols-[1fr_0.95fr]">
      <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-stone-900">需要同步的状态</h2>
          <p class="mt-1 text-sm text-stone-500">找出超过指定分钟未同步的景点状态，便于人工或任务补偿。</p>
        </div>
        <div class="flex flex-col gap-3 sm:flex-row">
          <input
            v-model="needSyncMinutes"
            type="number"
            placeholder="多少分钟内未同步（默认 30）"
            class="flex-1 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
          />
          <button class="rounded-2xl bg-stone-900 px-6 py-3 text-sm font-medium text-white transition hover:bg-stone-800" @click="fetchNeedSync">查询</button>
        </div>
        <div v-if="needSyncList.length" class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
          <RealtimeCard v-for="item in needSyncList" :key="item.attractionId" :data="item" />
        </div>
        <p v-else class="mt-4 text-sm text-stone-400">还没有查询到待同步状态。</p>
      </div>

      <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-semibold text-stone-900">批量更新状态</h2>
            <p class="mt-1 text-sm text-stone-500">一键触发所有景点的批量状态更新任务。</p>
          </div>
          <button
            class="rounded-2xl bg-stone-900 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-stone-800 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="batchUpdateLoading"
            @click="batchUpdateStatus"
          >{{ batchUpdateLoading ? '更新中...' : '批量更新所有景点' }}</button>
        </div>
        <div class="rounded-2xl border border-dashed border-stone-200 bg-stone-50 px-4 py-4 text-sm text-stone-500">
          建议在需要同步状态较多或出现预警集中时执行批量更新，结果会在下方反馈。
        </div>
        <p v-if="batchUpdateResult" class="mt-4 text-sm" :class="batchUpdateResult.includes('成功') ? 'text-emerald-600' : 'text-stone-600'">{{ batchUpdateResult }}</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { realtimeApi } from '../api/realtime.api'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import RealtimeCard from '../components/RealtimeCard.vue'
import { CROWD_LEVEL_MEDIUM } from '../constants'

const singleAttractionId = ref('')
const singleResult = ref<any>(null)

function fetchSingleStatus() {
  const id = singleAttractionId.value.trim()
  if (!id) return
  realtimeApi.getAttractionRealtimeStatus(Number(id))
    .then((res) => { singleResult.value = res })
    .catch(() => { singleResult.value = null })
}

const batchIds = ref('')
const batchResults = ref<any[]>([])

function fetchBatchStatus() {
  const ids = batchIds.value.split(',').map(s => s.trim()).filter(Boolean).map(Number)
  if (!ids.length) return
  realtimeApi.getBatchRealtimeStatus(ids)
    .then((res) => { batchResults.value = Array.isArray(res) ? res : [] })
    .catch(() => { batchResults.value = [] })
}

const minCrowdLevel = ref(String(CROWD_LEVEL_MEDIUM))
const crowdedList = ref<any[]>([])

function fetchCrowdedList() {
  const level = Number(minCrowdLevel.value) || CROWD_LEVEL_MEDIUM
  realtimeApi.getCrowdedAttractions(level)
    .then((res) => { crowdedList.value = Array.isArray(res) ? res : [] })
    .catch(() => { crowdedList.value = [] })
}

const historicalAvgId = ref('')
const historicalAvgResult = ref<number | null>(null)

function fetchHistoricalAvg() {
  const id = historicalAvgId.value.trim()
  if (!id) return
  realtimeApi.getHistoricalAvgCrowdCount(Number(id))
    .then((res) => { historicalAvgResult.value = Number(res) || 0 })
    .catch(() => { historicalAvgResult.value = null })
}

const sevenDaysAvgId = ref('')
const sevenDaysAvgResult = ref<number | null>(null)

function fetchSevenDaysAvg() {
  const id = sevenDaysAvgId.value.trim()
  if (!id) return
  realtimeApi.get7DaysAvgCrowdCount(Number(id))
    .then((res) => { sevenDaysAvgResult.value = Number(res) || 0 })
    .catch(() => { sevenDaysAvgResult.value = null })
}

const warns = ref<any[]>([])
const warnsLoading = ref(false)

function fetchWarns() {
  warnsLoading.value = true
  realtimeApi.getActiveWarns()
    .then((res) => { warns.value = Array.isArray(res) ? res : [] })
    .catch(() => { warns.value = [] })
    .finally(() => { warnsLoading.value = false })
}

const DEFAULT_SYNC_MINUTES = 30
const needSyncMinutes = ref(String(DEFAULT_SYNC_MINUTES))
const needSyncList = ref<any[]>([])

function fetchNeedSync() {
  const minutes = Number(needSyncMinutes.value) || DEFAULT_SYNC_MINUTES
  realtimeApi.getNeedSyncStatus(minutes)
    .then((res) => { needSyncList.value = Array.isArray(res) ? res : [] })
    .catch(() => { needSyncList.value = [] })
}

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