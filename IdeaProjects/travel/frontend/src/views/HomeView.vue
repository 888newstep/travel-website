<template>
  <div class="max-w-6xl mx-auto px-6 py-12">
    <!-- Hero -->
    <section class="text-center py-16">
      <h1 class="text-4xl md:text-5xl font-serif font-bold text-stone-900 mb-4">
        探索属于你的旅程
      </h1>
      <p class="text-lg text-stone-500 max-w-xl mx-auto mb-8">
        智能推荐景点路线，AI 助手规划行程，记录旅途中的每一刻
      </p>
      <div class="flex items-center justify-center gap-4">
        <router-link
          to="/attractions"
          class="px-6 py-2.5 bg-stone-900 text-white rounded-lg hover:bg-stone-800 transition-colors text-sm"
        >探索景点</router-link>
        <router-link
          to="/ai-chat"
          class="px-6 py-2.5 border border-stone-300 text-stone-700 rounded-lg hover:bg-stone-100 transition-colors text-sm"
        >AI 助手</router-link>
      </div>
    </section>

    <!-- 热门景点 -->
    <section class="mb-16">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-semibold text-stone-900">热门景点</h2>
        <router-link to="/attractions" class="text-sm text-stone-500 hover:text-stone-900 transition-colors">
          查看全部 →
        </router-link>
      </div>
      <LoadingSpinner v-if="loading" />
      <div v-else-if="attractions.length" class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div
          v-for="item in attractions.slice(0, 3)"
          :key="item.id"
          class="bg-white rounded-xl border border-stone-200 overflow-hidden hover:shadow-md transition-shadow group"
        >
          <div class="h-40 bg-stone-100 overflow-hidden">
            <img
              v-if="getFirstImage(item)"
              :src="getFirstImage(item)"
              :alt="item.name"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
              @error="($event.target as HTMLImageElement).style.display='none'"
            />
            <div v-else class="h-full flex items-center justify-center text-stone-300">
              <svg class="w-12 h-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1">
                <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 15.75l5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5l1.409-1.41a2.25 2.25 0 0 1 3.182 0l2.909 2.91m-18 3.75h16.5a1.5 1.5 0 0 0 1.5-1.5V6a1.5 1.5 0 0 0-1.5-1.5H3.75A1.5 1.5 0 0 0 2.25 6v12a1.5 1.5 0 0 0 1.5 1.5Zm10.5-11.25h.008v.008h-.008V8.25Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" />
              </svg>
            </div>
          </div>
          <div class="p-4">
            <h3 class="font-medium text-stone-900 mb-1">{{ item.name }}</h3>
            <p class="text-sm text-stone-500 line-clamp-2">{{ item.description || TEXT.NO_DESCRIPTION }}</p>
            <div class="flex items-center justify-between mt-3">
              <span class="text-xs text-stone-400">{{ item.rating ? '⭐ ' + item.rating : TEXT.NO_RATING }}</span>
              <router-link
                :to="`/attractions`"
                class="text-xs text-stone-600 hover:text-stone-900"
              >查看详情</router-link>
            </div>
          </div>
        </div>
      </div>
      <p v-else class="text-sm text-stone-400 text-center py-8">暂无景点数据</p>
    </section>

    <!-- 热门路线 -->
    <section class="mb-16">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-semibold text-stone-900">推荐路线</h2>
        <router-link to="/routes" class="text-sm text-stone-500 hover:text-stone-900 transition-colors">
          查看全部 →
        </router-link>
      </div>
      <LoadingSpinner v-if="loadingRoutes" />
      <div v-else-if="routes.length" class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div
          v-for="item in routes.slice(0, 4)"
          :key="item.id"
          class="bg-white rounded-xl border border-stone-200 p-5 hover:shadow-md transition-shadow"
        >
          <h3 class="font-medium text-stone-900 mb-1">{{ item.title }}</h3>
          <p class="text-sm text-stone-500 line-clamp-1">{{ item.description || TEXT.NO_DESCRIPTION }}</p>
          <div class="flex items-center gap-4 mt-3 text-xs text-stone-400">
            <span>👁 {{ item.viewCount || 0 }}</span>
            <span>❤️ {{ item.likeCount || 0 }}</span>
          </div>
        </div>
      </div>
      <p v-else class="text-sm text-stone-400 text-center py-8">暂无路线数据</p>
    </section>

    <!-- 推荐景点 -->
    <section class="mb-16">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-semibold text-stone-900">推荐景点</h2>
        <router-link to="/attractions" class="text-sm text-stone-500 hover:text-stone-900 transition-colors">
          查看全部 →
        </router-link>
      </div>
      <LoadingSpinner v-if="loadingRecommends" />
      <div v-else-if="recommendedAttractions.length" class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div
          v-for="item in recommendedAttractions.slice(0, 3)"
          :key="item.id"
          class="bg-white rounded-xl border border-stone-200 overflow-hidden hover:shadow-md transition-shadow group"
        >
          <div class="h-40 bg-stone-100 overflow-hidden">
            <img
              v-if="getFirstImage(item)"
              :src="getFirstImage(item)"
              :alt="item.name"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
              @error="($event.target as HTMLImageElement).style.display='none'"
            />
            <div v-else class="h-full flex items-center justify-center text-stone-300">
              <svg class="w-12 h-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1">
                <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 15.75l5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5l1.409-1.41a2.25 2.25 0 0 1 3.182 0l2.909 2.91m-18 3.75h16.5a1.5 1.5 0 0 0 1.5-1.5V6a1.5 1.5 0 0 0-1.5-1.5H3.75A1.5 1.5 0 0 0 2.25 6v12a1.5 1.5 0 0 0 1.5 1.5Zm10.5-11.25h.008v.008h-.008V8.25Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" />
              </svg>
            </div>
          </div>
          <div class="p-4">
            <h3 class="font-medium text-stone-900 mb-1">{{ item.name }}</h3>
            <p class="text-sm text-stone-500 line-clamp-2">{{ item.description || TEXT.NO_DESCRIPTION }}</p>
            <div class="flex items-center justify-between mt-3">
              <span class="text-xs text-stone-400">{{ item.rating ? '⭐ ' + item.rating : TEXT.NO_RATING }}</span>
              <span class="text-xs text-emerald-600 font-medium">推荐</span>
            </div>
          </div>
        </div>
      </div>
      <p v-else class="text-sm text-stone-400 text-center py-8">暂无推荐数据</p>
    </section>

    <!-- 最新游记 -->
    <section class="mb-16">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-semibold text-stone-900">最新游记</h2>
        <router-link to="/notes" class="text-sm text-stone-500 hover:text-stone-900 transition-colors">
          查看全部 →
        </router-link>
      </div>
      <LoadingSpinner v-if="loadingNotes" />
      <div v-else-if="notes.length" class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div
          v-for="item in notes.slice(0, 3)"
          :key="item.id"
          class="bg-white rounded-xl border border-stone-200 p-5 hover:shadow-md transition-shadow"
        >
          <h3 class="font-medium text-stone-900 mb-1">{{ item.title }}</h3>
          <p class="text-sm text-stone-500 line-clamp-2 mb-3">{{ item.excerpt || item.content || TEXT.NO_CONTENT }}</p>
          <div class="flex items-center gap-3 text-xs text-stone-400">
            <span>{{ item.author || TEXT.ANONYMOUS }}</span>
            <span>❤️ {{ item.likes || 0 }}</span>
            <span>💬 {{ item.comments || 0 }}</span>
          </div>
        </div>
      </div>
      <p v-else class="text-sm text-stone-400 text-center py-8">暂无游记数据</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { attractionApi } from '../api/attraction.api'
import { routeCrudApi } from '../api/route.api'
import { noteApi } from '../api/note.api'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import { DEFAULT_CITY_ID, TEXT } from '../constants'

const loading = ref(true)
const loadingRoutes = ref(true)
const loadingNotes = ref(true)
const loadingRecommends = ref(true)
const attractions = ref<any[]>([])
const routes = ref<any[]>([])
const notes = ref<any[]>([])
const recommendedAttractions = ref<any[]>([])

function getFirstImage(item: any): string {
  if (!item.images) return ''
  if (typeof item.images === 'string') {
    return item.images.split(',').map((s: string) => s.trim()).filter(Boolean)[0] || ''
  }
  if (Array.isArray(item.images)) {
    return item.images[0] || ''
  }
  return ''
}

onMounted(async () => {
  try {
    attractions.value = await attractionApi.getAttractions() as any[]
  } catch { /* ignore */ }
  loading.value = false

  try {
    routes.value = await routeCrudApi.getRoutesByCity(DEFAULT_CITY_ID) as any[]
  } catch { /* ignore */ }
  loadingRoutes.value = false

  try {
    notes.value = await noteApi.getLatestNotes(3) as any[]
  } catch { /* ignore */ }
  loadingNotes.value = false

  try {
    recommendedAttractions.value = await attractionApi.getRecommendations(DEFAULT_CITY_ID, 3) as any[]
  } catch { /* ignore */ }
  loadingRecommends.value = false
})
</script>