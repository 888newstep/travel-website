<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <!-- 页面标题 -->
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-stone-900">美食餐厅</h1>
      <p class="mt-2 text-stone-500">发现周边美食，品味地道佳肴</p>
    </div>

    <!-- 搜索栏 -->
    <div class="mb-8">
      <div class="flex gap-3">
        <div class="flex-1 relative">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
          </svg>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索餐厅名称或菜系..."
            class="w-full pl-10 pr-4 py-2.5 bg-white border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 transition-all"
            @input="onSearch"
          />
        </div>
        <button
          @click="fetchRestaurants"
          class="px-6 py-2.5 bg-amber-500 text-white rounded-xl text-sm font-medium hover:bg-amber-600 transition-colors"
        >
          搜索
        </button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center py-20">
      <LoadingSpinner />
    </div>

    <!-- 餐厅列表 -->
    <template v-else>
      <div v-if="restaurants.length === 0" class="text-center py-20">
        <svg class="w-16 h-16 mx-auto text-stone-300 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 8.25v-1.5m0 1.5c-1.355 0-2.697.056-4.024.166C6.845 8.51 6 9.473 6 10.608v2.513m6-4.871c1.355 0 2.697.056 4.024.166C17.155 8.51 18 9.473 18 10.608v2.513M15 8.25v-1.5m-6 1.5v-1.5m12 9.75-4.5-4.5M6 15l-4.5 4.5" />
        </svg>
        <p class="text-stone-400 text-lg">暂无餐厅数据</p>
        <p class="text-stone-300 text-sm mt-1">试试搜索其他关键词</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          v-for="r in restaurants"
          :key="r.id"
          class="bg-white rounded-2xl border border-stone-200 overflow-hidden hover:shadow-lg hover:border-amber-200 transition-all duration-300 group"
        >
          <!-- 图片区域 -->
          <div class="aspect-[16/10] bg-stone-100 overflow-hidden">
            <img
              v-if="r.imageUrl"
              :src="r.imageUrl"
              :alt="r.name"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
            />
            <div v-else class="w-full h-full flex items-center justify-center">
              <svg class="w-12 h-12 text-stone-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 18.75a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m3 0h6m-9 0H3.375a1.125 1.125 0 0 1-1.125-1.125V14.25m17.25 4.5a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m3 0h1.125c.621 0 1.129-.504 1.09-1.124a17.902 17.902 0 0 0-3.213-9.193 2.056 2.056 0 0 0-1.58-.86H14.25M16.5 18.75h-2.25m0-11.177v-.958c0-.568-.422-1.048-.987-1.106a48.554 48.554 0 0 0-10.026 0 1.106 1.106 0 0 0-.987 1.106v7.635m12-6.677v6.677m0 4.5v-4.5m0 0h-12" />
              </svg>
            </div>
            <!-- 评分标签 -->
            <div class="absolute top-3 left-3 bg-white/90 backdrop-blur-sm rounded-lg px-2.5 py-1 text-sm font-medium text-amber-600">
              <span>★</span> {{ r.rating ?? '-' }}
            </div>
            <!-- 价格标签 -->
            <div v-if="r.priceLevel" class="absolute top-3 right-3 bg-white/90 backdrop-blur-sm rounded-lg px-2.5 py-1 text-xs font-medium text-stone-500">
              {{ r.priceLevel }}
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="p-4">
            <h3 class="text-lg font-semibold text-stone-900 mb-1">{{ r.name }}</h3>
            <div class="flex items-center gap-2 text-sm text-stone-500 mb-2">
              <span v-if="r.cuisineType" class="px-2 py-0.5 bg-amber-50 text-amber-700 rounded-md text-xs font-medium">{{ r.cuisineType }}</span>
              <span v-if="r.averageCost" class="text-stone-400">¥{{ r.averageCost }}/人</span>
            </div>
            <p class="text-sm text-stone-400 line-clamp-2 mb-3">{{ r.description || r.feature || TEXT.NO_DESCRIPTION }}</p>
            <div class="flex items-center justify-between text-xs text-stone-400">
              <span class="truncate max-w-[200px]">{{ r.address }}</span>
              <span v-if="r.openingHours" class="shrink-0 ml-2">{{ r.openingHours }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { restaurantApi } from '../api/restaurant.api'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import { DEFAULT_CITY_ID, DEBOUNCE_DELAY, TEXT } from '../constants'

const restaurants = ref<any[]>([])
const keyword = ref('')
const loading = ref(true)
let searchTimer: ReturnType<typeof setTimeout> | null = null

async function fetchRestaurants() {
  loading.value = true
  try {
    if (keyword.value.trim()) {
      restaurants.value = await restaurantApi.search(DEFAULT_CITY_ID, keyword.value.trim()) as any[]
    } else {
      restaurants.value = await restaurantApi.getByCity(DEFAULT_CITY_ID) as any[]
    }
  } catch { /* ignore */ }
  loading.value = false
}

function onSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(fetchRestaurants, DEBOUNCE_DELAY)
}

onMounted(fetchRestaurants)
</script>