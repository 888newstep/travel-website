<template>
  <div class="app-container pb-16 pt-4 md:pt-6">
    <section class="surface-card mb-8 overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(255,247,237,0.92)_45%,rgba(254,242,242,0.88))] px-6 py-8 sm:px-8 sm:py-9">
      <div class="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
        <div>
          <div class="mb-4 flex flex-wrap gap-2">
            <span class="chip">本地美食探索</span>
            <span class="chip">菜系关键词搜索</span>
            <span class="chip">人均消费参考</span>
          </div>
          <h1 class="text-3xl font-semibold tracking-tight text-stone-900 md:text-4xl">从口味、预算到氛围，更快找到下一顿</h1>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-stone-600 md:text-base">
            把餐厅信息整理成更清晰的发现页，让用户在搜索菜系、查看评分和判断消费时少跳转一步。
          </p>
          <div class="mt-6 flex flex-wrap gap-3">
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">当前结果</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ restaurants.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">浏览模式</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ keyword ? '关键词搜索' : '城市推荐' }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">默认城市</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">#{{ DEFAULT_CITY_ID }}</div>
            </div>
          </div>
        </div>

        <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
          <div class="mb-4 flex items-center justify-between">
            <div>
              <div class="text-sm font-medium text-stone-500">快速检索</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">按名称或菜系搜索</div>
            </div>
            <span class="rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-600">即时刷新</span>
          </div>
          <div class="flex flex-col gap-3 sm:flex-row">
            <div class="relative flex-1">
              <svg class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
              </svg>
              <input
                v-model="keyword"
                type="text"
                placeholder="搜索餐厅名称、菜系或风味..."
                class="w-full rounded-2xl border border-stone-200 bg-white px-12 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
                @input="onSearch"
              />
              <button
                v-if="keyword"
                type="button"
                class="absolute right-3 top-1/2 inline-flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full text-stone-400 transition hover:bg-stone-100 hover:text-stone-700"
                @click="keyword = ''; fetchRestaurants()"
              >
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <button
              @click="fetchRestaurants"
              class="rounded-2xl bg-stone-900 px-6 py-3 text-sm font-medium text-white transition hover:bg-stone-800"
            >搜索</button>
          </div>
          <div class="mt-4 grid gap-2 text-sm text-stone-500 sm:grid-cols-2">
            <div class="rounded-2xl bg-stone-50 px-4 py-3">试试关键词：川菜、火锅、咖啡、夜宵</div>
            <div class="rounded-2xl bg-stone-50 px-4 py-3">卡片会优先展示评分、价格区间、菜系和营业时间</div>
          </div>
        </div>
      </div>
    </section>

    <div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="section-heading text-[1.75rem]">餐厅列表</h2>
        <p class="section-subtitle mt-2">更聚焦地展示评分、菜系、人均消费与位置，帮助快速做出用餐选择。</p>
      </div>
      <div class="flex flex-wrap gap-2 text-xs text-stone-500">
        <span class="chip">共 {{ restaurants.length }} 家餐厅</span>
        <span class="chip">{{ keyword ? `关键词：${keyword}` : '当前展示默认城市推荐' }}</span>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center py-20">
      <LoadingSpinner />
    </div>

    <template v-else>
      <div v-if="restaurants.length === 0" class="surface-card rounded-[1.75rem] px-6 py-14 text-center">
        <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-stone-100 text-stone-400">
          <svg class="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 8.25v-1.5m0 1.5c-1.355 0-2.697.056-4.024.166C6.845 8.51 6 9.473 6 10.608v2.513m6-4.871c1.355 0 2.697.056 4.024.166C17.155 8.51 18 9.473 18 10.608v2.513M15 8.25v-1.5m-6 1.5v-1.5m12 9.75-4.5-4.5M6 15l-4.5 4.5" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-stone-900">没有找到匹配餐厅</h3>
        <p class="mt-2 text-sm text-stone-500">试试更宽泛的菜系关键词，或清空搜索查看默认推荐。</p>
        <button
          type="button"
          class="mt-5 inline-flex rounded-full bg-stone-900 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-stone-800"
          @click="keyword = ''; fetchRestaurants()"
        >重置搜索</button>
      </div>

      <div v-else class="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
        <article
          v-for="r in restaurants"
          :key="r.id"
          class="surface-card surface-card-hover group overflow-hidden rounded-[1.75rem]"
        >
          <div class="relative aspect-[16/10] overflow-hidden bg-stone-100">
            <img
              v-if="r.imageUrl"
              :src="r.imageUrl"
              :alt="r.name"
              class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            />
            <div v-else class="flex h-full w-full items-center justify-center">
              <svg class="h-12 w-12 text-stone-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 18.75a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m3 0h6m-9 0H3.375a1.125 1.125 0 0 1-1.125-1.125V14.25m17.25 4.5a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m3 0h1.125c.621 0 1.129-.504 1.09-1.124a17.902 17.902 0 0 0-3.213-9.193 2.056 2.056 0 0 0-1.58-.86H14.25M16.5 18.75h-2.25m0-11.177v-.958c0-.568-.422-1.048-.987-1.106a48.554 48.554 0 0 0-10.026 0 1.106 1.106 0 0 0-.987 1.106v7.635m12-6.677v6.677m0 4.5v-4.5m0 0h-12" />
              </svg>
            </div>
            <div class="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/35 to-transparent"></div>
            <div class="absolute left-4 top-4 rounded-full bg-white/90 px-3 py-1 text-xs font-medium text-amber-700 shadow-sm">
              ★ {{ r.rating ?? '-' }}
            </div>
            <div v-if="r.priceLevel" class="absolute right-4 top-4 rounded-full bg-white/90 px-3 py-1 text-xs font-medium text-stone-600 shadow-sm">
              {{ r.priceLevel }}
            </div>
          </div>

          <div class="p-5">
            <div class="flex items-start justify-between gap-3">
              <h3 class="text-lg font-semibold text-stone-900">{{ r.name }}</h3>
              <span v-if="r.averageCost" class="rounded-full bg-stone-50 px-3 py-1 text-xs font-medium text-stone-600">¥{{ r.averageCost }}/人</span>
            </div>
            <div class="mt-3 flex flex-wrap items-center gap-2 text-sm text-stone-500">
              <span v-if="r.cuisineType" class="rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-700">{{ r.cuisineType }}</span>
              <span class="rounded-full bg-stone-50 px-3 py-1 text-xs font-medium text-stone-500">适合行程中补充用餐点</span>
            </div>
            <p class="mt-3 line-clamp-2 text-sm leading-6 text-stone-500">{{ r.description || r.feature || TEXT.NO_DESCRIPTION }}</p>
            <div class="mt-4 grid gap-2 text-xs text-stone-500">
              <div class="rounded-2xl bg-stone-50 px-3 py-2">📍 {{ r.address || '地址待补充' }}</div>
              <div v-if="r.openingHours" class="rounded-2xl bg-stone-50 px-3 py-2">🕒 {{ r.openingHours }}</div>
            </div>
          </div>
        </article>
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