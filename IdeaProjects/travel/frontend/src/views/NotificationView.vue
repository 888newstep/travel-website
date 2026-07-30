<template>
  <div class="max-w-4xl mx-auto px-6 py-12">
    <div class="flex items-center justify-between mb-8">
      <h1 class="text-2xl font-semibold text-stone-900">通知</h1>
      <button
        v-if="notifications.length"
        class="text-sm text-stone-500 hover:text-stone-900 transition-colors"
        @click="markAllRead"
      >全部已读</button>
    </div>

    <LoadingSpinner v-if="loading" />
    <template v-else>
      <div v-if="notifications.length" class="space-y-3">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="bg-white rounded-xl border border-stone-200 p-4 flex items-start gap-3 hover:shadow-sm transition-shadow"
          :class="!item.isRead ? 'border-l-2 border-l-stone-900' : ''"
        >
          <div class="flex-1">
            <div class="flex items-start justify-between mb-1">
              <h3 class="text-sm font-medium text-stone-900">{{ item.title }}</h3>
              <span class="text-xs text-stone-400">{{ item.createdAt ? new Date(item.createdAt).toLocaleDateString() : '' }}</span>
            </div>
            <p class="text-sm text-stone-500">{{ item.content }}</p>
          </div>
          <button
            v-if="!item.isRead"
            class="text-xs text-stone-400 hover:text-stone-600 whitespace-nowrap"
            @click="markRead(item.id!)"
          >标为已读</button>
        </div>
      </div>
      <p v-else class="text-sm text-stone-400 text-center py-16">暂无通知</p>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { notificationApi } from '../api/notification-feedback.api'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE } from '../constants'

const loading = ref(true)
const notifications = ref<any[]>([])

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