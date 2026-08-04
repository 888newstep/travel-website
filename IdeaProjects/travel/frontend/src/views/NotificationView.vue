<template>
  <div class="app-container pb-16 pt-4 md:pt-6">
    <section class="surface-card mb-8 overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(239,246,255,0.92)_48%,rgba(250,245,255,0.88))] px-6 py-8 sm:px-8 sm:py-9">
      <div class="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
        <div>
          <div class="mb-4 flex flex-wrap gap-2">
            <span class="chip">消息中心</span>
            <span class="chip">已读状态管理</span>
            <span class="chip">时间与内容聚合</span>
          </div>
          <h1 class="text-3xl font-semibold tracking-tight text-stone-900 md:text-4xl">把通知页整理成更干净的消息收件箱</h1>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-stone-600 md:text-base">
            统一展示未读状态、时间、标题和内容摘要，让重要消息更容易被发现和处理。
          </p>
          <div class="mt-6 flex flex-wrap gap-3">
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">通知总数</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ notifications.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">未读消息</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ unreadCount }}</div>
            </div>
          </div>
        </div>

        <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
          <div class="mb-4 flex items-center justify-between gap-3">
            <div>
              <div class="text-sm font-medium text-stone-500">快捷处理</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">统一清理未读消息</div>
            </div>
            <span class="rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-600">Inbox</span>
          </div>
          <div class="rounded-2xl bg-stone-50 px-4 py-4 text-sm leading-6 text-stone-500">
            当前页面会优先展示未读标记。你可以逐条处理，也可以一键全部设为已读。
          </div>
          <button
            v-if="notifications.length"
            class="mt-4 inline-flex rounded-2xl bg-stone-900 px-5 py-3 text-sm font-medium text-white transition hover:bg-stone-800"
            @click="markAllRead"
          >全部已读</button>
        </div>
      </div>
    </section>

    <section class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="section-heading text-[1.75rem]">通知列表</h2>
        <p class="section-subtitle mt-2">更清晰地区分未读和已读消息，并强化标题与时间的层次。</p>
      </div>
      <div class="flex flex-wrap gap-2 text-xs text-stone-500">
        <span class="chip">共 {{ notifications.length }} 条通知</span>
        <span class="chip">未读 {{ unreadCount }} 条</span>
      </div>
    </section>

    <LoadingSpinner v-if="loading" />
    <template v-else>
      <div v-if="notifications.length" class="space-y-4">
        <article
          v-for="item in notifications"
          :key="item.id"
          class="surface-card rounded-[1.5rem] px-5 py-4"
          :class="!item.isRead ? 'ring-1 ring-stone-900/8' : ''"
        >
          <div class="flex items-start gap-4">
            <div class="mt-1 flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl" :class="!item.isRead ? 'bg-stone-900 text-white' : 'bg-stone-100 text-stone-400'">
              <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" />
              </svg>
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <div class="flex items-center gap-2">
                    <h3 class="text-sm font-semibold text-stone-900">{{ item.title }}</h3>
                    <span v-if="!item.isRead" class="rounded-full bg-stone-900 px-2.5 py-1 text-[11px] font-medium text-white">未读</span>
                  </div>
                  <p class="mt-2 text-sm leading-6 text-stone-500">{{ item.content }}</p>
                </div>
                <span class="shrink-0 text-xs text-stone-400">{{ item.createdAt ? new Date(item.createdAt).toLocaleDateString() : '' }}</span>
              </div>
            </div>
            <button
              v-if="!item.isRead"
              class="shrink-0 rounded-full border border-stone-200 px-3 py-1.5 text-xs font-medium text-stone-500 transition hover:bg-stone-50 hover:text-stone-700"
              @click="markRead(item.id!)"
            >标为已读</button>
          </div>
        </article>
      </div>
      <div v-else class="surface-card rounded-[1.75rem] px-6 py-14 text-center">
        <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-stone-100 text-stone-400">
          <svg class="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-stone-900">暂无通知</h3>
        <p class="mt-2 text-sm text-stone-500">当系统有新的消息、提醒或反馈时，会在这里集中展示。</p>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { notificationApi } from '../api/notification-feedback.api'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE } from '../constants'

const loading = ref(true)
const notifications = ref<any[]>([])

const unreadCount = computed(() => notifications.value.filter(item => !item.isRead).length)

onMounted(async () => {
  try {
    notifications.value = await notificationApi.getNotifications(DEFAULT_PAGE, DEFAULT_PAGE_SIZE)
  } catch { /* ignore */ }
  loading.value = false
})

async function markRead(id: number) {
  try {
    await notificationApi.markAsRead(id)
    const item = notifications.value.find(n => n.id === id)
    if (item) item.isRead = true
  } catch { /* ignore */ }
}

async function markAllRead() {
  try {
    await notificationApi.markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
  } catch { /* ignore */ }
}
</script>