<template>
  <div class="app-container pb-16 pt-4 md:pt-6">
    <section class="relative overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(255,251,235,0.92)_45%,rgba(239,246,255,0.92))] px-6 py-8 shadow-[0_25px_80px_-40px_rgba(28,25,23,0.35)] sm:px-8 sm:py-10 lg:px-10 lg:py-12">
      <div class="absolute -right-12 top-0 h-40 w-40 rounded-full bg-amber-300/20 blur-3xl"></div>
      <div class="absolute -left-10 bottom-0 h-32 w-32 rounded-full bg-sky-300/20 blur-3xl"></div>

      <div class="relative grid gap-8 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
        <div>
          <div class="mb-4 flex flex-wrap gap-2">
            <span class="chip">AI 行程规划</span>
            <span class="chip">实时出行状态</span>
            <span class="chip">旅行内容社区</span>
          </div>
          <h1 class="max-w-3xl text-4xl font-serif font-bold leading-tight text-stone-900 md:text-5xl lg:text-6xl">
            用更清晰的界面，
            <span class="text-amber-600">更轻松地开始下一段旅程</span>
          </h1>
          <p class="mt-5 max-w-2xl text-base leading-7 text-stone-600 md:text-lg">
            集中管理景点、美食、路线、游记和 AI 助手，让灵感获取、行程规划与旅途记录保持在一个连贯体验中。
          </p>

          <div class="mt-8 flex flex-col gap-3 sm:flex-row">
            <router-link
              to="/attractions"
              class="inline-flex items-center justify-center rounded-full bg-stone-900 px-6 py-3 text-sm font-medium text-white transition hover:bg-stone-800"
            >探索景点</router-link>
            <router-link
              to="/ai-chat"
              class="inline-flex items-center justify-center rounded-full border border-stone-300 bg-white/80 px-6 py-3 text-sm font-medium text-stone-700 transition hover:border-stone-400 hover:bg-white"
            >打开 AI 助手</router-link>
          </div>

          <div class="mt-8 grid gap-3 sm:grid-cols-3">
            <div class="surface-card rounded-2xl p-4">
              <div class="text-sm text-stone-500">景点数据</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ formatCount(attractions.length) }}</div>
              <div class="mt-1 text-xs text-stone-400">热门景点持续更新</div>
            </div>
            <div class="surface-card rounded-2xl p-4">
              <div class="text-sm text-stone-500">推荐路线</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ formatCount(routes.length) }}</div>
              <div class="mt-1 text-xs text-stone-400">支持城市维度浏览</div>
            </div>
            <div class="surface-card rounded-2xl p-4">
              <div class="text-sm text-stone-500">社区内容</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ formatCount(notes.length) }}</div>
              <div class="mt-1 text-xs text-stone-400">游记与互动持续沉淀</div>
            </div>
          </div>
        </div>

        <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
          <div class="mb-5 flex items-center justify-between">
            <div>
              <div class="text-sm font-medium text-stone-500">旅程起点</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">今天想怎么出发？</div>
            </div>
            <span class="rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-600">在线体验</span>
          </div>

          <div class="grid gap-3 sm:grid-cols-2">
            <router-link
              v-for="entry in quickEntries"
              :key="entry.title"
              :to="entry.to"
              class="group rounded-2xl border border-stone-200/80 bg-stone-50/80 p-4 transition-all hover:-translate-y-0.5 hover:border-stone-300 hover:bg-white"
            >
              <div class="text-2xl">{{ entry.icon }}</div>
              <div class="mt-3 text-sm font-semibold text-stone-900">{{ entry.title }}</div>
              <div class="mt-1 text-sm leading-6 text-stone-500">{{ entry.description }}</div>
              <div class="mt-3 text-xs font-medium text-stone-400 transition group-hover:text-stone-700">立即进入 →</div>
            </router-link>
          </div>
        </div>
      </div>
    </section>

    <section class="mt-8 grid gap-4 md:grid-cols-3">
      <div class="surface-card rounded-3xl p-5">
        <div class="text-sm font-medium text-stone-500">界面优化</div>
        <div class="mt-2 text-lg font-semibold text-stone-900">更清晰的导航结构</div>
        <p class="mt-2 text-sm leading-6 text-stone-500">头部增加移动端菜单、活跃态高亮与信息分组，减少内容拥挤感。</p>
      </div>
      <div class="surface-card rounded-3xl p-5">
        <div class="text-sm font-medium text-stone-500">首页层次</div>
        <div class="mt-2 text-lg font-semibold text-stone-900">更聚焦的首屏体验</div>
        <p class="mt-2 text-sm leading-6 text-stone-500">将入口、统计与推荐内容拆分，降低首次进入时的理解成本。</p>
      </div>
      <div class="surface-card rounded-3xl p-5">
        <div class="text-sm font-medium text-stone-500">视觉风格</div>
        <div class="mt-2 text-lg font-semibold text-stone-900">更柔和的卡片与背景</div>
        <p class="mt-2 text-sm leading-6 text-stone-500">统一圆角、阴影与玻璃感层次，提升整体精致度与一致性。</p>
      </div>
    </section>

    <section class="mt-14">
      <div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 class="section-heading">热门景点</h2>
          <p class="section-subtitle mt-2">优先展示高感知内容，帮助用户快速进入探索状态。</p>
        </div>
        <router-link to="/attractions" class="text-sm font-medium text-stone-500 transition hover:text-stone-900">查看全部 →</router-link>
      </div>
      <LoadingSpinner v-if="loading" />
      <div v-else-if="attractions.length" class="grid grid-cols-1 gap-6 md:grid-cols-3">
        <article
          v-for="item in attractions.slice(0, 3)"
          :key="item.id"
          class="surface-card surface-card-hover group overflow-hidden rounded-[1.75rem]"
        >
          <div class="relative h-48 overflow-hidden bg-stone-100">
            <img
              v-if="getFirstImage(item)"
              :src="getFirstImage(item)"
              :alt="item.name"
              class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
              @error="($event.target as HTMLImageElement).style.display='none'"
            />
            <div class="absolute left-4 top-4 rounded-full bg-white/85 px-3 py-1 text-xs font-medium text-stone-700 shadow-sm">
              {{ item.cityName || '精选城市' }}
            </div>
          </div>
          <div class="p-5">
            <div class="flex items-start justify-between gap-3">
              <h3 class="text-lg font-semibold text-stone-900">{{ item.name }}</h3>
              <span class="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-600">{{ item.rating ? '⭐ ' + item.rating : TEXT.NO_RATING }}</span>
            </div>
            <p class="mt-3 line-clamp-2 text-sm leading-6 text-stone-500">{{ item.description || TEXT.NO_DESCRIPTION }}</p>
            <div class="mt-4 flex items-center justify-between text-sm">
              <span class="text-stone-400">{{ item.address || '位置信息待补充' }}</span>
              <router-link :to="'/attractions'" class="font-medium text-stone-700 transition hover:text-stone-900">查看详情</router-link>
            </div>
          </div>
        </article>
      </div>
      <p v-else class="py-8 text-center text-sm text-stone-400">暂无景点数据</p>
    </section>

    <section class="mt-14">
      <div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 class="section-heading">推荐路线</h2>
          <p class="section-subtitle mt-2">用更结构化的信息展示路线概况、热度和出游难度。</p>
        </div>
        <router-link to="/routes" class="text-sm font-medium text-stone-500 transition hover:text-stone-900">查看全部 →</router-link>
      </div>
      <LoadingSpinner v-if="loadingRoutes" />
      <div v-else-if="routes.length" class="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <article
          v-for="item in routes.slice(0, 4)"
          :key="item.id"
          class="surface-card surface-card-hover rounded-[1.75rem] p-5"
        >
          <div class="flex flex-wrap items-center gap-2">
            <span class="chip">{{ item.difficulty || '轻松' }}</span>
            <span class="chip">{{ item.durationDays ? `${item.durationDays} 天` : '灵活行程' }}</span>
            <span class="chip">👁 {{ item.viewCount || 0 }}</span>
            <span class="chip">❤️ {{ item.likeCount || 0 }}</span>
          </div>
          <h3 class="mt-4 text-lg font-semibold text-stone-900">{{ item.title }}</h3>
          <p class="mt-2 line-clamp-2 text-sm leading-6 text-stone-500">{{ item.description || TEXT.NO_DESCRIPTION }}</p>
          <div class="mt-5 flex items-center justify-between text-sm">
            <span class="text-stone-400">适合收藏后继续完善</span>
            <router-link to="/routes" class="font-medium text-stone-700 transition hover:text-stone-900">查看路线</router-link>
          </div>
        </article>
      </div>
      <p v-else class="py-8 text-center text-sm text-stone-400">暂无路线数据</p>
    </section>

    <section class="mt-14">
      <div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 class="section-heading">推荐景点</h2>
          <p class="section-subtitle mt-2">保留推荐标签，让用户更容易感知平台的内容价值。</p>
        </div>
        <router-link to="/attractions" class="text-sm font-medium text-stone-500 transition hover:text-stone-900">查看全部 →</router-link>
      </div>
      <LoadingSpinner v-if="loadingRecommends" />
      <div v-else-if="recommendedAttractions.length" class="grid grid-cols-1 gap-6 md:grid-cols-3">
        <article
          v-for="item in recommendedAttractions.slice(0, 3)"
          :key="item.id"
          class="surface-card surface-card-hover group overflow-hidden rounded-[1.75rem]"
        >
          <div class="h-44 overflow-hidden bg-stone-100">
            <img
              v-if="getFirstImage(item)"
              :src="getFirstImage(item)"
              :alt="item.name"
              class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
              @error="($event.target as HTMLImageElement).style.display='none'"
            />
            <div v-else class="flex h-full items-center justify-center text-stone-300">
              <svg class="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1">
                <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 15.75l5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5l1.409-1.41a2.25 2.25 0 0 1 3.182 0l2.909 2.91m-18 3.75h16.5a1.5 1.5 0 0 0 1.5-1.5V6a1.5 1.5 0 0 0-1.5-1.5H3.75A1.5 1.5 0 0 0 2.25 6v12a1.5 1.5 0 0 0 1.5 1.5Zm10.5-11.25h.008v.008h-.008V8.25Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" />
              </svg>
            </div>
          </div>
          <div class="p-5">
            <div class="flex items-center justify-between gap-3">
              <h3 class="text-lg font-semibold text-stone-900">{{ item.name }}</h3>
              <span class="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-600">推荐</span>
            </div>
            <p class="mt-3 line-clamp-2 text-sm leading-6 text-stone-500">{{ item.description || TEXT.NO_DESCRIPTION }}</p>
            <div class="mt-4 flex items-center justify-between text-sm">
              <span class="text-stone-400">{{ item.rating ? `⭐ ${item.rating}` : TEXT.NO_RATING }}</span>
              <router-link to="/attractions" class="font-medium text-stone-700 transition hover:text-stone-900">进入景点</router-link>
            </div>
          </div>
        </article>
      </div>
      <p v-else class="py-8 text-center text-sm text-stone-400">暂无推荐数据</p>
    </section>

    <section class="mt-14">
      <div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 class="section-heading">最新游记</h2>
          <p class="section-subtitle mt-2">把社区内容入口前置，帮助用户更快发现真实旅行经验。</p>
        </div>
        <router-link to="/notes" class="text-sm font-medium text-stone-500 transition hover:text-stone-900">查看全部 →</router-link>
      </div>
      <LoadingSpinner v-if="loadingNotes" />
      <div v-else-if="notes.length" class="grid grid-cols-1 gap-6 md:grid-cols-3">
        <article
          v-for="item in notes.slice(0, 3)"
          :key="item.id"
          class="surface-card surface-card-hover rounded-[1.75rem] p-5"
        >
          <div class="mb-4 flex items-center justify-between">
            <span class="rounded-full bg-stone-100 px-3 py-1 text-xs font-medium text-stone-500">游记精选</span>
            <span class="text-xs text-stone-400">{{ item.author || TEXT.ANONYMOUS }}</span>
          </div>
          <h3 class="text-lg font-semibold text-stone-900">{{ item.title }}</h3>
          <p class="mt-3 line-clamp-3 text-sm leading-6 text-stone-500">{{ item.excerpt || item.content || TEXT.NO_CONTENT }}</p>
          <div class="mt-5 flex items-center gap-4 text-xs text-stone-400">
            <span>❤️ {{ item.likes || 0 }}</span>
            <span>💬 {{ item.comments || 0 }}</span>
            <span>适合行前参考</span>
          </div>
        </article>
      </div>
      <p v-else class="py-8 text-center text-sm text-stone-400">暂无游记数据</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
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

const quickEntries = [
  { title: '景点探索', description: '浏览热门景点与目的地内容', to: '/attractions', icon: '🏞️' },
  { title: '路线规划', description: '查看推荐路线并继续完善行程', to: '/routes', icon: '🗺️' },
  { title: '实时动态', description: '快速了解景点开放与拥挤情况', to: '/realtime', icon: '📡' },
  { title: 'AI 助手', description: '获取个性化建议与行程灵感', to: '/ai-chat', icon: '✨' },
]

function formatCount(value: number) {
  if (value >= 1000) {
    return `${(value / 1000).toFixed(1)}k`
  }
  return `${value}`
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

onMounted(async () => {
  try {
    attractions.value = await attractionApi.getAttractions() as any[]
  } catch {
    attractions.value = []
  }
  loading.value = false

  try {
    routes.value = await routeCrudApi.getRoutesByCity(DEFAULT_CITY_ID) as any[]
  } catch {
    routes.value = []
  }
  loadingRoutes.value = false

  try {
    notes.value = await noteApi.getLatestNotes(3) as any[]
  } catch {
    notes.value = []
  }
  loadingNotes.value = false

  try {
    recommendedAttractions.value = await attractionApi.getRecommendations(DEFAULT_CITY_ID, 3) as any[]
  } catch {
    recommendedAttractions.value = []
  }
  loadingRecommends.value = false
})
</script>
