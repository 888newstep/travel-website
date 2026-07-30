<template>
  <div class="max-w-4xl mx-auto px-6 py-12">
    <h1 class="text-2xl font-semibold text-stone-900 mb-8">路线优化</h1>

    <!-- 路线选择 -->
    <div class="mb-8">
      <label class="block text-sm font-medium text-stone-700 mb-2">选择要优化的路线</label>
      <select
        v-model="selectedRouteId"
        @change="loadOptimizationData"
        class="w-full max-w-md px-4 py-2 border border-stone-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-500"
      >
        <option :value="0" disabled>-- 请选择路线 --</option>
        <option v-for="route in routes" :key="route.id" :value="route.id">
          {{ route.title }} ({{ route.cityName || '' }})
        </option>
      </select>
    </div>

    <!-- 优化建议 -->
    <div v-if="selectedRouteId && suggestions.length > 0" class="mb-8">
      <h2 class="text-lg font-medium text-stone-800 mb-4">优化建议</h2>
      <div class="space-y-3">
        <div
          v-for="(suggestion, index) in suggestions"
          :key="index"
          class="p-4 bg-white border border-stone-200 rounded-lg hover:border-stone-300 transition-colors"
        >
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <h3 class="text-sm font-medium text-stone-800">
                {{ suggestion.title || suggestion.type || '优化建议 #' + (index + 1) }}
              </h3>
              <p class="text-sm text-stone-500 mt-1">{{ suggestion.description || suggestion.detail || '' }}</p>
              <div v-if="suggestion.benefit" class="mt-2 flex gap-4 text-xs text-stone-400">
                <span v-if="suggestion.timeSaved">节省时间: {{ suggestion.timeSaved }}分钟</span>
                <span v-if="suggestion.costSaved">节省费用: ¥{{ suggestion.costSaved }}</span>
                <span v-if="suggestion.scoreImprovement">评分提升: +{{ suggestion.scoreImprovement }}</span>
              </div>
            </div>
            <button
              @click="applyOptimization(suggestion)"
              class="ml-4 px-3 py-1.5 text-xs bg-stone-900 text-white rounded-lg hover:bg-stone-800 transition-colors whitespace-nowrap"
            >
              应用优化
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="selectedRouteId && suggestions.length === 0 && !loading" class="text-center py-12 text-stone-400">
      <p>暂无优化建议</p>
    </div>

    <!-- 优化历史 -->
    <div v-if="selectedRouteId && history.length > 0" class="mb-8">
      <h2 class="text-lg font-medium text-stone-800 mb-4">优化历史</h2>
      <div class="space-y-2">
        <div
          v-for="(item, index) in history"
          :key="index"
          class="p-3 bg-stone-50 border border-stone-200 rounded-lg text-sm"
        >
          <div class="flex justify-between text-stone-600">
            <span>{{ item.description || item.type || '优化记录' }}</span>
            <span class="text-stone-400">{{ item.createdAt || item.time || '' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-12 text-stone-400">
      <p>加载中...</p>
    </div>

    <!-- 未选择路线 -->
    <div v-if="!selectedRouteId" class="text-center py-16 text-stone-400">
      <p class="text-lg">请选择一条路线开始优化</p>
      <p class="text-sm mt-2">系统将基于您的路线提供智能优化建议</p>
    </div>
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