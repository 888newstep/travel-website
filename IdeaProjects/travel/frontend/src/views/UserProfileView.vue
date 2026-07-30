<template>
  <div class="max-w-4xl mx-auto px-6 py-12">
    <h1 class="text-2xl font-semibold text-stone-900 mb-8">个人中心</h1>

    <LoadingSpinner v-if="loading" />
    <template v-else>
      <div class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
        <div class="flex items-center gap-4 mb-6">
          <div class="w-16 h-16 bg-stone-200 rounded-full flex items-center justify-center text-xl text-stone-500">
            {{ (user?.username || 'U').charAt(0).toUpperCase() }}
          </div>
          <div>
            <h2 class="text-lg font-medium text-stone-900">{{ user?.username || '用户' }}</h2>
            <p class="text-sm text-stone-500">{{ user?.phone || '' }}</p>
          </div>
        </div>

        <div class="grid grid-cols-3 gap-4 text-center">
          <div class="p-3 bg-stone-50 rounded-lg">
            <p class="text-lg font-semibold text-stone-900">{{ stats?.totalRoutes || 0 }}</p>
            <p class="text-xs text-stone-500">路线</p>
          </div>
          <div class="p-3 bg-stone-50 rounded-lg">
            <p class="text-lg font-semibold text-stone-900">{{ stats?.totalNotes || 0 }}</p>
            <p class="text-xs text-stone-500">游记</p>
          </div>
          <div class="p-3 bg-stone-50 rounded-lg">
            <p class="text-lg font-semibold text-stone-900">{{ stats?.totalCollections || 0 }}</p>
            <p class="text-xs text-stone-500">收藏</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
        <h3 class="font-medium text-stone-900 mb-4">个人信息</h3>
        <div class="space-y-4">
          <div class="flex items-center justify-between">
            <span class="text-sm text-stone-500">用户名</span>
            <span class="text-sm text-stone-900">{{ user?.username || '-' }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-sm text-stone-500">手机号</span>
            <span class="text-sm text-stone-900">{{ user?.phone || '-' }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-sm text-stone-500">角色</span>
            <span class="text-sm text-stone-900">{{ user?.role === 'admin' ? '管理员' : '普通用户' }}</span>
          </div>
        </div>
      </div>

      <!-- 我的收藏 -->
      <div class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
        <h3 class="font-medium text-stone-900 mb-4">我的收藏 ({{ collections.length }})</h3>
        <div v-if="collections.length" class="space-y-3">
          <div
            v-for="item in collections"
            :key="item.id"
            class="flex items-center justify-between p-3 bg-stone-50 rounded-lg hover:bg-stone-100 transition-colors"
          >
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-stone-900 truncate">{{ item.routeTitle || '路线 #' + item.routeId }}</p>
              <p class="text-xs text-stone-400 mt-0.5">收藏于 {{ item.collectionTime ? new Date(item.collectionTime).toLocaleDateString() : '' }}</p>
            </div>
            <button
              @click="removeCollection(item)"
              class="ml-3 p-1.5 text-stone-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors shrink-0"
              title="取消收藏"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
              </svg>
            </button>
          </div>
        </div>
        <p v-else class="text-sm text-stone-400 text-center py-6">暂无收藏</p>
      </div>

      <!-- 我的路线 -->
      <div class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
        <h3 class="font-medium text-stone-900 mb-4">我的路线 ({{ myRoutes.length }})</h3>
        <div v-if="myRoutes.length" class="space-y-3">
          <div
            v-for="item in myRoutes"
            :key="item.id"
            class="flex items-center justify-between p-3 bg-stone-50 rounded-lg hover:bg-stone-100 transition-colors"
          >
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-stone-900 truncate">{{ item.title }}</p>
              <p class="text-xs text-stone-400 mt-0.5">{{ item.description ? item.description.substring(0, EXCERPT_MAX_LENGTH) : '' }}</p>
            </div>
            <span class="text-xs px-2 py-0.5 rounded-full shrink-0" :class="item.isPublic ? 'bg-emerald-50 text-emerald-600' : 'bg-stone-100 text-stone-500'">
              {{ item.isPublic ? '公开' : '私密' }}
            </span>
          </div>
        </div>
        <p v-else class="text-sm text-stone-400 text-center py-6">暂无路线</p>
      </div>

      <!-- 我的游记 -->
      <div class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
        <h3 class="font-medium text-stone-900 mb-4">我的游记 ({{ myNotes.length }})</h3>
        <div v-if="myNotes.length" class="space-y-3">
          <div
            v-for="item in myNotes"
            :key="item.id"
            class="p-3 bg-stone-50 rounded-lg hover:bg-stone-100 transition-colors"
          >
            <p class="text-sm font-medium text-stone-900 truncate">{{ item.title }}</p>
            <p class="text-xs text-stone-400 mt-0.5 line-clamp-1">{{ item.excerpt || item.content || '' }}</p>
            <div class="flex items-center gap-3 mt-1 text-xs text-stone-400">
              <span>❤️ {{ item.likes || 0 }}</span>
              <span>💬 {{ item.comments || 0 }}</span>
              <span>{{ item.createTime ? new Date(item.createTime).toLocaleDateString() : '' }}</span>
            </div>
          </div>
        </div>
        <p v-else class="text-sm text-stone-400 text-center py-6">暂无游记</p>
      </div>

      <button
        class="w-full py-2.5 text-sm text-red-500 border border-red-200 rounded-lg hover:bg-red-50 transition-colors"
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