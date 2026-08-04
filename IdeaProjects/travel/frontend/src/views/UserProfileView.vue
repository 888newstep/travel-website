<template>
  <div class="app-container pb-16 pt-4 md:pt-6">
    <section class="surface-card mb-8 overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(240,249,255,0.92)_45%,rgba(255,251,235,0.88))] px-6 py-8 sm:px-8 sm:py-9">
      <LoadingSpinner v-if="loading" />
      <template v-else>
        <div class="grid gap-8 xl:grid-cols-[1.05fr_0.95fr] xl:items-center">
          <div>
            <div class="mb-4 flex flex-wrap gap-2">
              <span class="chip">个人资料总览</span>
              <span class="chip">内容与路线管理</span>
              <span class="chip">收藏回看</span>
            </div>
            <div class="flex items-center gap-4">
              <div class="flex h-18 w-18 items-center justify-center rounded-[1.5rem] bg-stone-900 text-2xl font-semibold text-white shadow-lg shadow-stone-900/15">
                {{ (user?.username || 'U').charAt(0).toUpperCase() }}
              </div>
              <div>
                <h1 class="text-3xl font-semibold tracking-tight text-stone-900 md:text-4xl">{{ user?.username || '旅行用户' }}</h1>
                <p class="mt-2 text-sm text-stone-500">{{ user?.phone || '未绑定手机号' }}</p>
                <p class="mt-1 text-sm text-stone-400">{{ user?.role === 'admin' ? '管理员账号' : '普通用户账号' }}</p>
              </div>
            </div>
            <p class="mt-5 max-w-2xl text-sm leading-7 text-stone-600 md:text-base">
              个人中心现在统一展示资料、内容数据、收藏和创作成果，让用户能更快回看自己的旅行资产。
            </p>
          </div>

          <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-2">
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">路线</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ stats?.totalRoutes || 0 }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">游记</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ stats?.totalNotes || 0 }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">收藏</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ stats?.totalCollections || 0 }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">获赞</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ stats?.totalLikes || 0 }}</div>
            </div>
          </div>
        </div>
      </template>
    </section>

    <template v-if="!loading">
      <section class="mb-6 grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <div class="surface-card rounded-[1.75rem] p-6">
          <h2 class="text-lg font-semibold text-stone-900">个人信息</h2>
          <p class="mt-1 text-sm text-stone-500">查看当前账号的基础资料与身份信息。</p>
          <div class="mt-5 space-y-4">
            <div class="flex items-center justify-between rounded-2xl bg-stone-50 px-4 py-3">
              <span class="text-sm text-stone-500">用户名</span>
              <span class="text-sm font-medium text-stone-900">{{ user?.username || '-' }}</span>
            </div>
            <div class="flex items-center justify-between rounded-2xl bg-stone-50 px-4 py-3">
              <span class="text-sm text-stone-500">手机号</span>
              <span class="text-sm font-medium text-stone-900">{{ user?.phone || '-' }}</span>
            </div>
            <div class="flex items-center justify-between rounded-2xl bg-stone-50 px-4 py-3">
              <span class="text-sm text-stone-500">角色</span>
              <span class="text-sm font-medium text-stone-900">{{ user?.role === 'admin' ? '管理员' : '普通用户' }}</span>
            </div>
            <div class="flex items-center justify-between rounded-2xl bg-stone-50 px-4 py-3">
              <span class="text-sm text-stone-500">总浏览</span>
              <span class="text-sm font-medium text-stone-900">{{ stats?.totalViews || 0 }}</span>
            </div>
          </div>
        </div>

        <div class="surface-card rounded-[1.75rem] p-6">
          <h2 class="text-lg font-semibold text-stone-900">账户概览</h2>
          <p class="mt-1 text-sm text-stone-500">用更清晰的摘要帮助快速判断当前内容沉淀情况。</p>
          <div class="mt-5 grid gap-3 sm:grid-cols-2">
            <div class="rounded-2xl bg-stone-50 px-4 py-4">
              <div class="text-sm font-medium text-stone-500">我的收藏</div>
              <div class="mt-2 text-xl font-semibold text-stone-900">{{ collections.length }}</div>
              <div class="mt-1 text-xs text-stone-400">随时回看感兴趣的路线内容</div>
            </div>
            <div class="rounded-2xl bg-stone-50 px-4 py-4">
              <div class="text-sm font-medium text-stone-500">我的路线</div>
              <div class="mt-2 text-xl font-semibold text-stone-900">{{ myRoutes.length }}</div>
              <div class="mt-1 text-xs text-stone-400">继续完善公开或私密路线</div>
            </div>
            <div class="rounded-2xl bg-stone-50 px-4 py-4">
              <div class="text-sm font-medium text-stone-500">我的游记</div>
              <div class="mt-2 text-xl font-semibold text-stone-900">{{ myNotes.length }}</div>
              <div class="mt-1 text-xs text-stone-400">记录真实旅途与内容互动</div>
            </div>
            <div class="rounded-2xl bg-stone-50 px-4 py-4">
              <div class="text-sm font-medium text-stone-500">分享数</div>
              <div class="mt-2 text-xl font-semibold text-stone-900">{{ stats?.totalShares || 0 }}</div>
              <div class="mt-1 text-xs text-stone-400">衡量内容与路线传播情况</div>
            </div>
          </div>
        </div>
      </section>

      <section class="mb-6 surface-card rounded-[1.75rem] p-6">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-semibold text-stone-900">我的收藏</h2>
            <p class="mt-1 text-sm text-stone-500">聚合查看已收藏的路线，支持快速取消收藏。</p>
          </div>
          <span class="chip">{{ collections.length }} 项</span>
        </div>
        <div v-if="collections.length" class="grid gap-3">
          <div
            v-for="item in collections"
            :key="item.id"
            class="flex items-center justify-between gap-3 rounded-2xl bg-stone-50 px-4 py-4 transition hover:bg-stone-100"
          >
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium text-stone-900">{{ item.routeTitle || '路线 #' + item.routeId }}</p>
              <p class="mt-1 text-xs text-stone-400">收藏于 {{ item.collectionTime ? new Date(item.collectionTime).toLocaleDateString() : '' }}</p>
            </div>
            <button
              @click="removeCollection(item)"
              class="shrink-0 rounded-full p-2 text-stone-400 transition hover:bg-red-50 hover:text-red-500"
              title="取消收藏"
            >
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
              </svg>
            </button>
          </div>
        </div>
        <p v-else class="py-8 text-center text-sm text-stone-400">暂无收藏</p>
      </section>

      <section class="mb-6 grid gap-6 xl:grid-cols-2">
        <div class="surface-card rounded-[1.75rem] p-6">
          <div class="mb-4 flex items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-semibold text-stone-900">我的路线</h2>
              <p class="mt-1 text-sm text-stone-500">快速回看已创建路线的概况与可见性。</p>
            </div>
            <span class="chip">{{ myRoutes.length }} 条</span>
          </div>
          <div v-if="myRoutes.length" class="space-y-3">
            <div
              v-for="item in myRoutes"
              :key="item.id"
              class="flex items-center justify-between gap-3 rounded-2xl bg-stone-50 px-4 py-4 transition hover:bg-stone-100"
            >
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-medium text-stone-900">{{ item.title }}</p>
                <p class="mt-1 text-xs text-stone-400">{{ item.description ? item.description.substring(0, EXCERPT_MAX_LENGTH) : '暂无描述' }}</p>
              </div>
              <span class="shrink-0 rounded-full px-3 py-1 text-xs font-medium" :class="item.isPublic ? 'bg-emerald-50 text-emerald-600' : 'bg-stone-100 text-stone-500'">
                {{ item.isPublic ? '公开' : '私密' }}
              </span>
            </div>
          </div>
          <p v-else class="py-8 text-center text-sm text-stone-400">暂无路线</p>
        </div>

        <div class="surface-card rounded-[1.75rem] p-6">
          <div class="mb-4 flex items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-semibold text-stone-900">我的游记</h2>
              <p class="mt-1 text-sm text-stone-500">查看自己的内容沉淀、互动表现和发布时间。</p>
            </div>
            <span class="chip">{{ myNotes.length }} 篇</span>
          </div>
          <div v-if="myNotes.length" class="space-y-3">
            <div
              v-for="item in myNotes"
              :key="item.id"
              class="rounded-2xl bg-stone-50 px-4 py-4 transition hover:bg-stone-100"
            >
              <p class="truncate text-sm font-medium text-stone-900">{{ item.title }}</p>
              <p class="mt-1 line-clamp-2 text-xs text-stone-400">{{ item.excerpt || item.content || '' }}</p>
              <div class="mt-3 flex flex-wrap gap-2 text-xs text-stone-500">
                <span class="rounded-full bg-white px-3 py-1">❤️ {{ item.likes || 0 }}</span>
                <span class="rounded-full bg-white px-3 py-1">💬 {{ item.comments || 0 }}</span>
                <span class="rounded-full bg-white px-3 py-1">{{ item.createTime ? new Date(item.createTime).toLocaleDateString() : '' }}</span>
              </div>
            </div>
          </div>
          <p v-else class="py-8 text-center text-sm text-stone-400">暂无游记</p>
        </div>
      </section>

      <button
        class="w-full rounded-2xl border border-red-200 py-3 text-sm font-medium text-red-500 transition hover:bg-red-50"
        @click="handleLogout"
      >退出登录</button>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { userApi } from '../api/user.api'
import { userStatsApi } from '../api/user-stats.api'
import { collectionApi } from '../api/collection.api'
import { routeCrudApi } from '../api/route.api'
import { noteApi } from '../api/note.api'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import { EXCERPT_MAX_LENGTH } from '../constants'
const loading = ref(true)
const user = ref<any>(null)
const stats = ref<any>({})
const collections = ref<any[]>([])
const myRoutes = ref<any[]>([])
const myNotes = ref<any[]>([])

onMounted(async () => {
  try {
    user.value = await userApi.getCurrentUser()
    const userId = user.value?.id
    if (userId) {
      const [s, c, r, n] = await Promise.allSettled([
        userStatsApi.getCurrentUserStats(),
        collectionApi.getUserCollections(userId),
        routeCrudApi.getMyRoutes(userId),
        noteApi.getUserTravelNotes(userId),
      ])
      if (s.status === 'fulfilled') stats.value = s.value
      if (c.status === 'fulfilled') collections.value = (c.value as any[]) || []
      if (r.status === 'fulfilled') myRoutes.value = (r.value as any[]) || []
      if (n.status === 'fulfilled') myNotes.value = (n.value as any[]) || []
    }
  } catch { /* ignore */ }
  loading.value = false
})

async function removeCollection(item: any) {
  try {
    if (item.routeId && user.value?.id) {
      await collectionApi.removeCollection(user.value.id, item.routeId)
      collections.value = collections.value.filter(c => c.id !== item.id)
    }
  } catch { /* ignore */ }
}

function handleLogout() {
  userApi.logout()
}
</script>