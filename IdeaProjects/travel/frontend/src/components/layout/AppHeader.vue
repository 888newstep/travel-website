<template>
  <header class="fixed inset-x-0 top-0 z-50 px-3 pt-3 sm:px-4">
    <div class="app-container">
      <div class="glass-panel rounded-2xl px-4 sm:px-5">
        <div class="flex h-16 items-center justify-between gap-3">
          <router-link to="/" class="flex min-w-0 items-center gap-3 text-stone-900">
            <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-stone-900 text-sm font-bold text-white shadow-lg shadow-stone-900/20">T</span>
            <div class="min-w-0">
              <div class="truncate text-base font-semibold sm:text-lg">智慧旅游</div>
              <div class="hidden text-xs text-stone-500 sm:block">AI 驱动的路线规划与旅行内容平台</div>
            </div>
          </router-link>

          <nav class="hidden min-w-0 flex-1 md:flex md:justify-center">
            <div class="scrollbar-hide flex max-w-[42rem] items-center gap-1 overflow-x-auto rounded-full border border-stone-200/80 bg-white/70 px-2 py-2">
              <router-link
                v-for="item in navItems"
                :key="item.path"
                :to="item.path"
                class="shrink-0 rounded-full px-3 py-2 text-sm font-medium transition-all"
                :class="isActive(item.path)
                  ? 'bg-stone-900 text-white shadow-sm'
                  : 'text-stone-500 hover:bg-stone-100 hover:text-stone-900'"
              >
                {{ item.label }}
              </router-link>
            </div>
          </nav>

          <div class="flex items-center gap-2 sm:gap-3">
            <router-link
              to="/ai-chat"
              class="hidden rounded-full border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-medium text-amber-700 transition hover:border-amber-300 hover:bg-amber-100 md:inline-flex"
            >AI 行程助手</router-link>

            <router-link
              v-if="isLoggedIn"
              to="/notifications"
              class="relative rounded-full p-2 text-stone-400 transition hover:bg-stone-100 hover:text-stone-700"
              title="通知"
            >
              <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" />
              </svg>
              <span
                v-if="unreadCount > 0"
                class="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-medium text-white"
              >{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </router-link>

            <template v-if="!isLoggedIn">
              <router-link
                to="/login"
                class="hidden text-sm font-medium text-stone-500 transition hover:text-stone-900 sm:inline-flex"
              >登录</router-link>
              <router-link
                to="/login"
                class="hidden rounded-full bg-stone-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-stone-800 sm:inline-flex"
              >开始使用</router-link>
            </template>

            <router-link
              v-else
              to="/profile"
              class="flex h-10 w-10 items-center justify-center rounded-full bg-stone-900 text-sm font-semibold text-white shadow-lg shadow-stone-900/15 transition hover:scale-105"
            >
              {{ userInitial }}
            </router-link>

            <button
              type="button"
              class="inline-flex rounded-full border border-stone-200 bg-white/90 p-2 text-stone-600 transition hover:bg-stone-100 md:hidden"
              @click="mobileMenuOpen = !mobileMenuOpen"
            >
              <svg v-if="!mobileMenuOpen" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
              </svg>
              <svg v-else class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <transition name="page">
          <div v-if="mobileMenuOpen" class="border-t border-stone-200/80 py-4 md:hidden">
            <div class="grid grid-cols-2 gap-2">
              <router-link
                v-for="item in navItems"
                :key="item.path"
                :to="item.path"
                class="rounded-2xl border px-3 py-3 text-sm font-medium transition-all"
                :class="isActive(item.path)
                  ? 'border-stone-900 bg-stone-900 text-white'
                  : 'border-stone-200 bg-white text-stone-600 hover:border-stone-300 hover:text-stone-900'"
              >
                {{ item.label }}
              </router-link>
            </div>
            <div v-if="!isLoggedIn" class="mt-3 grid grid-cols-2 gap-2">
              <router-link to="/login" class="rounded-2xl border border-stone-200 bg-white px-3 py-3 text-center text-sm font-medium text-stone-700">登录</router-link>
              <router-link to="/login" class="rounded-2xl bg-stone-900 px-3 py-3 text-center text-sm font-medium text-white">立即体验</router-link>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { userApi } from '../../api/user.api'
import { notificationApi } from '../../api/notification-feedback.api'
import { dictionaryApi } from '../../api/dictionary.api'

const route = useRoute()

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
const isLoggedIn = ref(false)
const username = ref('')
const unreadCount = ref(0)
const mobileMenuOpen = ref(false)

const userInitial = computed(() => username.value?.charAt(0)?.toUpperCase() || 'U')

function isActive(path: string) {
  return route.path === path
}

async function loadNavItems() {
  try {
    const res = await dictionaryApi.getByType('nav_menu')
    if (Array.isArray(res) && res.length) {
      navItems.value = res.map((item: any) => ({ path: item.value, label: item.label }))
    }
  } catch {
    navItems.value = DEFAULT_NAV_ITEMS
  }
}

function resetAuthState() {
  isLoggedIn.value = false
  username.value = ''
  unreadCount.value = 0
  localStorage.removeItem('token')
  localStorage.removeItem('username')
}

async function checkAuth() {
  const token = localStorage.getItem('token')
  isLoggedIn.value = !!token

  if (!token) {
    username.value = ''
    unreadCount.value = 0
    return
  }

  try {
    const user = await userApi.getCurrentUser()
    username.value = user.username || ''
    localStorage.setItem('username', username.value)
  } catch {
    resetAuthState()
    return
  }

  try {
    const count = await notificationApi.getUnreadCount()
    unreadCount.value = Number(count ?? 0)
  } catch {
    unreadCount.value = 0
  }
}

onMounted(() => {
  loadNavItems()
  checkAuth()
})

watch(() => route.fullPath, () => {
  mobileMenuOpen.value = false
  checkAuth()
})
</script>

