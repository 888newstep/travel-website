<template>
  <header class="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-lg border-b border-stone-200">
    <div class="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
      <router-link to="/" class="flex items-center gap-2 text-lg font-semibold text-stone-900">
        <span class="w-8 h-8 bg-stone-900 rounded-lg flex items-center justify-center text-white text-sm font-bold">T</span>
        智慧旅游
      </router-link>

      <nav class="hidden md:flex items-center gap-8">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="text-sm text-stone-500 hover:text-stone-900 transition-colors"
          active-class="text-stone-900 font-medium"
        >
          {{ item.label }}
        </router-link>
      </nav>

      <div class="flex items-center gap-3">
        <router-link
          v-if="isLoggedIn"
          to="/notifications"
          class="relative p-2 text-stone-400 hover:text-stone-600 transition-colors"
          title="通知"
        >
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" />
          </svg>
          <span
            v-if="unreadCount > 0"
            class="absolute -top-0.5 -right-0.5 w-4 h-4 bg-red-500 text-white text-[10px] font-medium rounded-full flex items-center justify-center"
          >{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </router-link>

        <template v-if="!isLoggedIn">
          <router-link
            to="/login"
            class="text-sm text-stone-500 hover:text-stone-900 transition-colors"
          >登录</router-link>
          <router-link
            to="/login"
            class="text-sm px-4 py-1.5 bg-stone-900 text-white rounded-lg hover:bg-stone-800 transition-colors"
          >注册</router-link>
        </template>
        <router-link
          v-else
          to="/profile"
          class="w-8 h-8 bg-stone-200 rounded-full flex items-center justify-center text-sm text-stone-600 hover:bg-stone-300 transition-colors"
        >
          {{ userInitial }}
        </router-link>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '../../api/user.api'
import { notificationApi } from '../../api/notification-feedback.api'
import { dictionaryApi } from '../../api/dictionary.api'

const router = useRouter()

const DEFAULT_NAV_ITEMS = [
  { path: '/', label: '首页' },
  { path: '/attractions', label: '景点' },
  { path: '/restaurants', label: '美食' },
  { path: '/routes', label: '路线' },
  { path: '/optimization', label: '优化' },
  { path: '/realtime', label: '实时' },
  { path: '/notes', label: '游记' },
  { path: '/files', label: '文件' },
  { path: '/share', label: '分享' },
  { path: '/feedback', label: '反馈' },
  { path: '/ai-chat', label: 'AI' },
]

const navItems = ref<{ path: string; label: string }[]>(DEFAULT_NAV_ITEMS)

// 启动时从后端获取最新导航配置
dictionaryApi.getByType('nav_menu').then((res: any) => {
  if (Array.isArray(res) && res.length) {
    navItems.value = res.map((item: any) => ({ path: item.value, label: item.label }))
  }
}).catch(() => {})

const isLoggedIn = ref(false)
const username = ref('')
const unreadCount = ref(0)

const userInitial = computed(() => username.value?.charAt(0)?.toUpperCase() || 'U')

function checkAuth() {
  const token = localStorage.getItem('token')
  isLoggedIn.value = !!token
  if (token) {
    userApi.getCurrentUser().then((user: any) => {
      username.value = user.username || user.nickname || ''
      localStorage.setItem('username', username.value)
      // 获取未读通知数
      notificationApi.getUnreadCount().then((count: any) => {
        unreadCount.value = typeof count === 'number' ? count : (count?.data ?? 0)
      }).catch(() => {})
    }).catch(() => {
      isLoggedIn.value = false
      localStorage.removeItem('token')
      localStorage.removeItem('username')
    })
  } else {
    username.value = ''
    unreadCount.value = 0
  }
}

onMounted(checkAuth)

// 监听路由变化，重新检查登录状态
watch(() => router.currentRoute.value.path, checkAuth)
</script>