<template>
  <div class="app-container pb-16 pt-4 md:pt-6">
    <section class="surface-card mb-8 overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(255,251,235,0.92)_45%,rgba(240,249,255,0.9))] px-6 py-8 sm:px-8 sm:py-9">
      <div class="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
        <div>
          <div class="mb-4 flex flex-wrap gap-2">
            <span class="chip">路线优化建议</span>
            <span class="chip">历史优化记录</span>
            <span class="chip">智能推荐应用</span>
          </div>
          <h1 class="text-3xl font-semibold tracking-tight text-stone-900 md:text-4xl">让路线优化从“有建议”变成“更好决策”</h1>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-stone-600 md:text-base">
            用更清晰的筛选、建议和历史分区帮助用户理解每条路线可优化的方向，并快速应用调整方案。
          </p>
          <div class="mt-6 grid gap-3 sm:grid-cols-3">
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">可选路线</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ routes.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">优化建议</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ suggestions.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">历史记录</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ history.length }}</div>
            </div>
          </div>
        </div>

        <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
          <div class="mb-4">
            <div class="text-sm font-medium text-stone-500">选择路线</div>
            <div class="mt-1 text-xl font-semibold text-stone-900">从现有路线开始优化</div>
          </div>
          <select
            v-model="selectedRouteId"
            @change="loadOptimizationData"
            class="w-full rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
          >
            <option :value="0" disabled>-- 请选择路线 --</option>
            <option v-for="route in routes" :key="route.id" :value="route.id">
              {{ route.title }} ({{ route.cityName || '' }})
            </option>
          </select>
          <div class="mt-4 grid gap-2 text-sm text-stone-500 sm:grid-cols-2">
            <div class="rounded-2xl bg-stone-50 px-4 py-3">系统将综合时间、费用和可行性给出建议</div>
            <div class="rounded-2xl bg-stone-50 px-4 py-3">应用建议后会刷新推荐与历史记录</div>
          </div>
        </div>
      </div>
    </section>

    <div v-if="loading" class="surface-card rounded-[1.75rem] px-6 py-14 text-center text-stone-400">
      <p>加载中...</p>
    </div>

    <template v-else>
      <div v-if="!selectedRouteId" class="surface-card rounded-[1.75rem] px-6 py-14 text-center text-stone-400">
        <p class="text-lg">请选择一条路线开始优化</p>
        <p class="mt-2 text-sm">系统将基于你的路线提供智能优化建议</p>
      </div>

      <template v-else>
        <section class="mb-6 surface-card rounded-[1.75rem] p-6">
          <div class="mb-4 flex items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-semibold text-stone-900">优化建议</h2>
              <p class="mt-1 text-sm text-stone-500">优先展示可操作建议及其收益，帮助快速判断是否采用。</p>
            </div>
            <span class="chip">{{ suggestions.length }} 条建议</span>
          </div>

          <div v-if="suggestions.length" class="space-y-4">
            <article
              v-for="(suggestion, index) in suggestions"
              :key="index"
              class="surface-card surface-card-hover rounded-[1.5rem] px-5 py-4"
            >
              <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div class="min-w-0 flex-1">
                  <h3 class="text-sm font-semibold text-stone-800">
                    {{ suggestion.title || suggestion.type || '优化建议 #' + (index + 1) }}
                  </h3>
                  <p class="mt-2 text-sm leading-6 text-stone-500">{{ suggestion.description || suggestion.detail || '' }}</p>
                  <div v-if="suggestion.benefit || suggestion.timeSaved || suggestion.costSaved || suggestion.scoreImprovement" class="mt-3 flex flex-wrap gap-2 text-xs text-stone-500">
                    <span v-if="suggestion.timeSaved" class="rounded-full bg-stone-50 px-3 py-1.5">节省时间 {{ suggestion.timeSaved }} 分钟</span>
                    <span v-if="suggestion.costSaved" class="rounded-full bg-stone-50 px-3 py-1.5">节省费用 ¥{{ suggestion.costSaved }}</span>
                    <span v-if="suggestion.scoreImprovement" class="rounded-full bg-stone-50 px-3 py-1.5">评分提升 +{{ suggestion.scoreImprovement }}</span>
                  </div>
                </div>
                <button
                  @click="applyOptimization(suggestion)"
                  class="shrink-0 rounded-2xl bg-stone-900 px-4 py-2.5 text-xs font-medium text-white transition hover:bg-stone-800"
                >
                  应用优化
                </button>
              </div>
            </article>
          </div>
          <p v-else class="py-8 text-center text-sm text-stone-400">暂无优化建议</p>
        </section>

        <section v-if="history.length" class="surface-card rounded-[1.75rem] p-6">
          <div class="mb-4 flex items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-semibold text-stone-900">优化历史</h2>
              <p class="mt-1 text-sm text-stone-500">帮助你回看过去的调整动作与时间记录。</p>
            </div>
            <span class="chip">{{ history.length }} 条记录</span>
          </div>
          <div class="space-y-3">
            <div
              v-for="(item, index) in history"
              :key="index"
              class="rounded-2xl border border-white/80 bg-stone-50/80 px-4 py-4 text-sm text-stone-600"
            >
              <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <span>{{ item.description || item.type || '优化记录' }}</span>
                <span class="text-xs text-stone-400">{{ item.createdAt || item.time || '' }}</span>
              </div>
            </div>
          </div>
        </section>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { routeCrudApi, intelligentRouteApi } from '../api/route.api'
import { DEFAULT_CITY_ID } from '../constants'

const routes = ref<any[]>([])
const selectedRouteId = ref(0)
const suggestions = ref<any[]>([])
const history = ref<any[]>([])
const loading = ref(false)

async function loadRoutes() {
  try {
    const data = await routeCrudApi.getRoutesByCity(DEFAULT_CITY_ID) as any
    routes.value = Array.isArray(data) ? data : (data.routes || [])
  } catch {
    routes.value = []
  }
}

async function loadOptimizationData() {
  if (!selectedRouteId.value) return
  loading.value = true
  try {
    const [suggestionData, historyData] = await Promise.all([
      intelligentRouteApi.getOptimizationSuggestionsForRoute(selectedRouteId.value),
      intelligentRouteApi.getOptimizationHistory(selectedRouteId.value),
    ])
    suggestions.value = Array.isArray(suggestionData) ? suggestionData : []
    history.value = Array.isArray(historyData) ? historyData : []
  } catch {
    suggestions.value = []
    history.value = []
  } finally {
    loading.value = false
  }
}

async function applyOptimization(suggestion: any) {
  try {
    await intelligentRouteApi.applyOptimizationSuggestion(selectedRouteId.value, suggestion.id, suggestion)
    alert('优化方案已应用')
    loadOptimizationData()
  } catch {
    alert('应用优化失败，请重试')
  }
}

loadRoutes()
</script>