<template>
  <div class="app-container pb-16 pt-4 md:pt-6">
    <section class="surface-card mb-8 overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(255,251,235,0.92)_48%,rgba(240,249,255,0.88))] px-6 py-8 sm:px-8 sm:py-9">
      <div class="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
        <div>
          <div class="mb-4 flex flex-wrap gap-2">
            <span class="chip">实时景区状态</span>
            <span class="chip">评论与评分</span>
            <span class="chip">附近景点推荐</span>
          </div>
          <h1 class="text-3xl font-semibold tracking-tight text-stone-900 md:text-4xl">发现更适合当下出行状态的景点</h1>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-stone-600 md:text-base">
            在一个视图里查看景点热度、开放状态、评分和位置，让筛选目标与临时决策都更轻松。
          </p>
          <div class="mt-6 flex flex-wrap gap-3">
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">当前结果</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ attractions.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">实时覆盖</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ Object.keys(realtimeMap).length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">当前模式</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ keyword ? '搜索结果' : '全部景点' }}</div>
            </div>
          </div>
        </div>

        <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
          <div class="mb-4 flex items-center justify-between">
            <div>
              <div class="text-sm font-medium text-stone-500">快速检索</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">先从关键词开始</div>
            </div>
            <span class="rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-600">支持实时状态</span>
          </div>
          <div class="relative">
            <input
              v-model="keyword"
              type="text"
              placeholder="搜索景点名称、地址或描述..."
              class="w-full rounded-2xl border border-stone-200 bg-white px-12 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
              @input="doSearch"
            />
            <svg class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
            </svg>
            <button
              v-if="keyword"
              type="button"
              class="absolute right-3 top-1/2 inline-flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full text-stone-400 transition hover:bg-stone-100 hover:text-stone-700"
              @click="keyword = ''; fetchAttractions()"
            >
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div class="mt-4 grid gap-2 text-sm text-stone-500 sm:grid-cols-2">
            <div class="rounded-2xl bg-stone-50 px-4 py-3">热门探索：历史古迹、亲子景区、夜游地标</div>
            <div class="rounded-2xl bg-stone-50 px-4 py-3">搜索时自动刷新结果，并保留实时拥挤度信息</div>
          </div>
        </div>
      </div>
    </section>

    <div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="section-heading text-[1.75rem]">景点列表</h2>
        <p class="section-subtitle mt-2">点击卡片可查看详情、评分统计、用户评价和附近推荐。</p>
      </div>
      <div class="flex flex-wrap gap-2 text-xs text-stone-500">
        <span class="chip">共 {{ attractions.length }} 个景点</span>
        <span class="chip">{{ keyword ? `关键词：${keyword}` : '未设置关键词' }}</span>
      </div>
    </div>

    <LoadingSpinner v-if="loading" />
    <template v-else>
      <div v-if="attractions.length" class="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
        <article
          v-for="item in attractions"
          :key="item.id"
          class="surface-card surface-card-hover group cursor-pointer overflow-hidden rounded-[1.75rem]"
          @click="openDetail(item)"
        >
          <div class="relative h-52 overflow-hidden bg-stone-100">
            <img
              v-if="getFirstImage(item)"
              :src="getFirstImage(item)"
              :alt="item.name"
              class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
              @error="onImageError($event)"
            />
            <div v-else class="flex h-full items-center justify-center text-stone-300">
              <svg class="h-16 w-16" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1">
                <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 15.75l5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5l1.409-1.41a2.25 2.25 0 0 1 3.182 0l2.909 2.91m-18 3.75h16.5a1.5 1.5 0 0 0 1.5-1.5V6a1.5 1.5 0 0 0-1.5-1.5H3.75A1.5 1.5 0 0 0 2.25 6v12a1.5 1.5 0 0 0 1.5 1.5Zm10.5-11.25h.008v.008h-.008V8.25Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" />
              </svg>
            </div>
            <div class="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/35 to-transparent"></div>
            <div class="absolute left-4 top-4 flex flex-wrap gap-2">
              <span class="rounded-full bg-white/88 px-3 py-1 text-xs font-medium text-stone-700 shadow-sm">{{ item.cityName || '精选景点' }}</span>
              <span
                v-if="realtimeMap[item.id]"
                class="rounded-full px-3 py-1 text-xs font-medium shadow-sm"
                :class="statusClass(realtimeMap[item.id])"
              >{{ statusLabel(realtimeMap[item.id]) }}</span>
            </div>
          </div>
          <div class="p-5">
            <div class="flex items-start justify-between gap-3">
              <h3 class="text-lg font-semibold text-stone-900">{{ item.name }}</h3>
              <span class="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-600">{{ item.rating ? '⭐ ' + item.rating : TEXT.NO_RATING }}</span>
            </div>
            <p class="mt-3 line-clamp-2 text-sm leading-6 text-stone-500">{{ item.description || TEXT.NO_DESCRIPTION }}</p>
            <div class="mt-4 flex items-start justify-between gap-3 text-sm">
              <span class="line-clamp-2 text-stone-400">{{ item.address || '地址信息待补充' }}</span>
              <span class="shrink-0 font-medium text-stone-700 transition group-hover:text-stone-900">查看详情</span>
            </div>
            <div v-if="realtimeMap[item.id]" class="mt-4 grid grid-cols-2 gap-2 text-xs text-stone-500">
              <div class="rounded-2xl bg-stone-50 px-3 py-2">👥 拥挤：{{ realtimeMap[item.id].crowdCount ?? '-' }} 人</div>
              <div class="rounded-2xl bg-stone-50 px-3 py-2">⏱ 等待：{{ realtimeMap[item.id].waitTime != null ? realtimeMap[item.id].waitTime + ' min' : '-' }}</div>
              <div class="rounded-2xl bg-stone-50 px-3 py-2">🌤 天气：{{ realtimeMap[item.id].weather || '-' }}</div>
              <div class="rounded-2xl bg-stone-50 px-3 py-2">🟢 开放：{{ realtimeMap[item.id].openStatus === false ? '否' : '是' }}</div>
            </div>
          </div>
        </article>
      </div>
      <div v-else class="surface-card rounded-[1.75rem] px-6 py-14 text-center">
        <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-stone-100 text-stone-400">
          <svg class="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
            <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-stone-900">没有找到匹配景点</h3>
        <p class="mt-2 text-sm text-stone-500">试试更短的关键词，或者清空搜索后浏览全部景点。</p>
        <button
          type="button"
          class="mt-5 inline-flex rounded-full bg-stone-900 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-stone-800"
          @click="keyword = ''; fetchAttractions()"
        >重置搜索</button>
      </div>
    </template>
    <!-- 景点详情弹窗 -->
    <Teleport to="body">
      <div
        v-if="selectedAttraction"
        class="fixed inset-0 z-50 flex items-start justify-center pt-16 pb-8 px-4"
        @click.self="closeDetail"
      >
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeDetail" />
        <div class="surface-card relative max-h-full w-full max-w-2xl overflow-y-auto rounded-[1.75rem] border border-white/80 bg-white/95 shadow-[0_32px_90px_-36px_rgba(15,23,42,0.45)] backdrop-blur">
          <!-- 弹窗头部 -->
          <div class="sticky top-0 z-10 flex items-center justify-between rounded-t-[1.75rem] border-b border-stone-100/80 bg-white/90 px-6 py-4 backdrop-blur">
            <h2 class="text-lg font-semibold text-stone-900 truncate max-w-md">{{ selectedAttraction.name }}</h2>
            <button @click="closeDetail" class="p-2 text-stone-400 hover:text-stone-600 hover:bg-stone-100 rounded-lg transition-colors">
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" /></svg>
            </button>
          </div>

          <!-- 弹窗内容 -->
          <div class="p-6 space-y-6">
            <!-- 基本信息 -->
            <div>
              <div class="flex items-center gap-4 mb-3">
                <span class="text-amber-500 font-medium">{{ selectedAttraction.rating ? '⭐ ' + selectedAttraction.rating : TEXT.NO_RATING }}</span>
                <span v-if="detailData?.openingHours" class="text-xs text-stone-500">{{ detailData?.openingHours }}</span>
                <span v-if="selectedAttraction.price" class="text-xs text-stone-500">¥{{ selectedAttraction.price }}/人</span>
              </div>
              <p class="text-sm text-stone-600 leading-relaxed">{{ selectedAttraction.description || TEXT.NO_DESCRIPTION }}</p>
              <p v-if="selectedAttraction.address" class="text-xs text-stone-400 mt-2">📍 {{ selectedAttraction.address }}</p>
            </div>

            <!-- 景点图片 -->
            <div v-if="getAllImages(selectedAttraction).length" class="space-y-2">
              <div class="relative bg-stone-100 rounded-xl overflow-hidden">
                <img
                  :src="getAllImages(selectedAttraction)[currentImageIndex]"
                  :alt="selectedAttraction.name"
                  class="w-full h-56 object-cover"
                  @error="onImageError($event)"
                />
                <div v-if="getAllImages(selectedAttraction).length > 1" class="absolute inset-0 flex items-center justify-between px-3">
                  <button
                    @click="currentImageIndex = (currentImageIndex - 1 + getAllImages(selectedAttraction).length) % getAllImages(selectedAttraction).length"
                    class="w-8 h-8 rounded-full bg-white/70 hover:bg-white flex items-center justify-center text-stone-600 shadow-sm transition-colors"
                  >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" /></svg>
                  </button>
                  <button
                    @click="currentImageIndex = (currentImageIndex + 1) % getAllImages(selectedAttraction).length"
                    class="w-8 h-8 rounded-full bg-white/70 hover:bg-white flex items-center justify-center text-stone-600 shadow-sm transition-colors"
                  >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" /></svg>
                  </button>
                </div>
                <div v-if="getAllImages(selectedAttraction).length > 1" class="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1.5">
                  <button
                    v-for="(img, idx) in getAllImages(selectedAttraction)"
                    :key="idx"
                    @click="currentImageIndex = idx"
                    class="w-2 h-2 rounded-full transition-colors"
                    :class="idx === currentImageIndex ? 'bg-white' : 'bg-white/40'"
                  />
                </div>
              </div>
            </div>

            <!-- 评分统计 -->
            <div v-if="ratingStats" class="p-4 bg-stone-50 rounded-xl">
              <h3 class="text-sm font-medium text-stone-700 mb-3">评分统计</h3>
              <div class="flex items-center gap-6">
                <div class="text-center">
                  <p class="text-2xl font-bold text-stone-900">{{ ratingStats.averageRating || '-' }}</p>
                  <p class="text-xs text-stone-400">平均评分</p>
                </div>
                <div class="text-center">
                  <p class="text-2xl font-bold text-stone-900">{{ ratingStats.totalReviews || 0 }}</p>
                  <p class="text-xs text-stone-400">评价数</p>
                </div>
                <div class="flex-1 space-y-1">
                  <div v-for="star in 5" :key="star" class="flex items-center gap-2">
                    <span class="text-xs text-stone-500 w-8">{{ 6 - star }}星</span>
                    <div class="flex-1 h-2 bg-stone-200 rounded-full overflow-hidden">
                      <div class="h-full bg-amber-400 rounded-full" :style="{ width: getStarPercent(6 - star) + '%' }" />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 写评价 -->
            <div class="p-4 bg-stone-50 rounded-xl">
              <h3 class="text-sm font-medium text-stone-700 mb-3">写评价</h3>
              <div v-if="reviewSuccess" class="text-sm text-emerald-600 mb-3">评价提交成功！</div>
              <div class="space-y-3">
                <div class="flex items-center gap-1">
                  <span class="text-xs text-stone-500 mr-2">评分</span>
                  <button
                    v-for="star in 5"
                    :key="star"
                    @click="reviewRating = star"
                    class="text-xl transition-colors"
                    :class="star <= reviewRating ? 'text-amber-400' : 'text-stone-300'"
                  >★</button>
                </div>
                <textarea
                  v-model="reviewContent"
                  placeholder="分享你的游玩体验..."
                  rows="3"
                  class="w-full px-3 py-2 bg-white border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300 resize-none"
                />
                <button
                  @click="submitReview"
                  :disabled="reviewSubmitting || reviewRating === 0 || !reviewContent.trim()"
                  class="px-4 py-2 bg-stone-800 text-white text-sm rounded-lg hover:bg-stone-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >{{ reviewSubmitting ? '提交中...' : '提交评价' }}</button>
              </div>
            </div>

            <!-- 评论列表 -->
            <div>
              <h3 class="text-sm font-medium text-stone-700 mb-3">用户评价 ({{ reviews.length }})</h3>
              <div v-if="reviews.length" class="space-y-3">
                <div v-for="r in reviews" :key="r.id" class="p-3 bg-stone-50 rounded-xl">
                  <div class="flex items-center justify-between mb-1">
                    <span class="text-sm font-medium text-stone-700">用户 #{{ r.userId }}</span>
                    <span class="text-xs text-amber-500">{{ r.rating ? '★'.repeat(r.rating) + '☆'.repeat(5 - r.rating) : '' }}</span>
                  </div>
                  <p class="text-sm text-stone-600">{{ r.content || r.comment || '无内容' }}</p>
                  <p class="text-xs text-stone-400 mt-1">{{ r.createTime ? new Date(r.createTime).toLocaleDateString() : '' }}</p>
                </div>
              </div>
              <p v-else class="text-sm text-stone-400 text-center py-4">暂无评价</p>
            </div>

            <!-- 附近景点 -->
            <div v-if="similarAttractions.length">
              <h3 class="text-sm font-medium text-stone-700 mb-3">附近景点</h3>
              <div class="grid grid-cols-2 gap-3">
                <div
                  v-for="s in similarAttractions.slice(0, 4)"
                  :key="s.id"
                  class="p-3 bg-stone-50 rounded-xl cursor-pointer hover:bg-stone-100 transition-colors"
                  @click="openDetail(s)"
                >
                  <p class="text-sm font-medium text-stone-900 truncate">{{ s.name }}</p>
                  <p class="text-xs text-stone-400 mt-0.5">{{ s.rating ? '⭐ ' + s.rating : '' }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { attractionApi } from '../api/attraction.api'
import { realtimeApi } from '../api/realtime.api'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import { CROWD_LEVEL_HIGH, CROWD_LEVEL_MEDIUM, DEBOUNCE_DELAY, TEXT } from '../constants'

const loading = ref(true)
const attractions = ref<any[]>([])
const keyword = ref('')
const realtimeMap = ref<Record<number, any>>({})

// 详情弹窗
const selectedAttraction = ref<any>(null)
const detailData = ref<any>(null)
const reviews = ref<any[]>([])
const ratingStats = ref<any>(null)
const similarAttractions = ref<any[]>([])

// 评价表单
const reviewRating = ref(0)
const reviewContent = ref('')
const reviewSubmitting = ref(false)
const reviewSuccess = ref(false)
const reviewError = ref('')

// 图片轮播
const currentImageIndex = ref(0)

async function fetchAttractions() {
  loading.value = true
  try {
    attractions.value = keyword.value
      ? await attractionApi.searchAttractions(keyword.value) as any[]
      : await attractionApi.getAttractions() as any[]
    // 批量获取实时状态
    if (attractions.value.length) {
      const ids = attractions.value.map(a => a.id)
      const statusList = await realtimeApi.getBatchRealtimeStatus(ids) as any[]
      if (Array.isArray(statusList)) {
        statusList.forEach(s => {
          if (s?.attractionId != null) {
            realtimeMap.value[s.attractionId] = s
          }
        })
      }
    }
  } catch { /* ignore */ }
  loading.value = false
}

function openDetail(attraction: any) {
  selectedAttraction.value = attraction
  detailData.value = null
  reviews.value = []
  ratingStats.value = null
  similarAttractions.value = []
  reviewRating.value = 0
  reviewContent.value = ''
  reviewSubmitting.value = false
  reviewSuccess.value = false
  reviewError.value = ''
  currentImageIndex.value = 0
  fetchDetailData(attraction.id)
}

function closeDetail() {
  selectedAttraction.value = null
}

async function fetchDetailData(id: number) {
  try {
    const [detail, revs, stats, nearby] = await Promise.allSettled([
      attractionApi.getAttractionDetail(id),
      attractionApi.getAttractionReviews(id),
      attractionApi.getAttractionRatingStats(id),
      attractionApi.getAttractionNearby(id, 5000),
    ])
    if (detail.status === 'fulfilled') detailData.value = detail.value
    if (revs.status === 'fulfilled') reviews.value = (revs.value as any[]) || []
    if (stats.status === 'fulfilled') ratingStats.value = stats.value
    if (nearby.status === 'fulfilled') similarAttractions.value = (nearby.value as any[]) || []
  } catch { /* ignore */ }
}

function getStarPercent(star: number): number {
  if (!ratingStats.value) return 0
  const total = ratingStats.value.totalReviews || 1
  const key = `star${star}Count` as string
  const count = ratingStats.value[key] || 0
  return Math.round((count / total) * 100)
}

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

function onImageError(e: Event) {
  const target = e.target as HTMLImageElement
  target.style.display = 'none'
}

function getAllImages(item: any): string[] {
  if (!item.images) return []
  if (typeof item.images === 'string') {
    return item.images.split(',').map((s: string) => s.trim()).filter(Boolean)
  }
  if (Array.isArray(item.images)) {
    return item.images.filter(Boolean)
  }
  return []
}

async function submitReview() {
  if (reviewRating.value === 0 || !reviewContent.value.trim()) return
  reviewSubmitting.value = true
  reviewError.value = ''
  reviewSuccess.value = false
  try {
    await attractionApi.submitReview(selectedAttraction.value.id, reviewRating.value, reviewContent.value.trim())
    reviewSuccess.value = true
    reviewRating.value = 0
    reviewContent.value = ''
    // 刷新评论列表
    const revs = await attractionApi.getAttractionReviews(selectedAttraction.value.id)
    reviews.value = (revs as any[]) || []
    // 刷新评分统计
    const stats = await attractionApi.getAttractionRatingStats(selectedAttraction.value.id)
    ratingStats.value = stats
  } catch { /* ignore */ }
  reviewSubmitting.value = false
}

function statusClass(status: any): string {
  const level = status.crowdLevel ?? 0
  if (status.openStatus === false) return 'bg-red-50 text-red-600'
  if (level >= CROWD_LEVEL_HIGH) return 'bg-red-50 text-red-600'
  if (level >= CROWD_LEVEL_MEDIUM) return 'bg-amber-50 text-amber-600'
  return 'bg-emerald-50 text-emerald-600'
}

function statusLabel(status: any): string {
  if (status.openStatus === false) return '已关闭'
  const level = status.crowdLevel ?? 0
  if (level >= CROWD_LEVEL_HIGH) return '拥挤'
  if (level >= CROWD_LEVEL_MEDIUM) return '适中'
  return '空闲'
}

let searchTimer: ReturnType<typeof setTimeout>
function doSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(fetchAttractions, DEBOUNCE_DELAY)
}

onMounted(fetchAttractions)
</script>