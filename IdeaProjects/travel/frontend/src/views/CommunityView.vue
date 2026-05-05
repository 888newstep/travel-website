<template>
  <div class="space-y-8">
    <!-- 页面标题 -->
    <section class="bg-white rounded-xl shadow-md p-8">
      <h2 class="text-3xl font-bold text-gray-800 mb-4">旅行社区</h2>
      <p class="text-gray-600">分享你的旅行故事，发现精彩旅程</p>
    </section>

    <!-- 搜索栏 -->
    <section class="bg-white rounded-xl shadow-md p-6">
      <div class="flex gap-4">
        <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索游记..."
            class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            @keyup.enter="handleSearch"
        />
        <button
            @click="handleSearch"
            :disabled="isLoading"
            class="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ isLoading ? '搜索中...' : '搜索' }}
        </button>
      </div>
    </section>

    <!-- 错误提示 -->
    <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4 text-red-600">
      {{ error }}
    </div>

    <!-- 加载中 -->
    <div v-if="isLoading && !hotNotes.length && !latestNotes.length" class="flex justify-center items-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
    </div>

    <!-- 热门游记 -->
    <section v-else>
      <h3 class="text-2xl font-bold text-gray-800 mb-4">热门游记</h3>
      <div v-if="hotNotes.length === 0" class="text-center py-12 text-gray-500">
        暂无热门游记
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
            v-for="note in hotNotes"
            :key="note.id"
            class="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow cursor-pointer"
            @click="viewDetail(note.id)"
        >
          <img
              v-if="note.coverImage"
              :src="note.coverImage"
              :alt="note.title"
              class="w-full h-48 object-cover"
          />
          <div v-else class="w-full h-48 bg-gray-200 flex items-center justify-center">
            <svg class="w-16 h-16 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd"/>
            </svg>
          </div>
          <div class="p-4">
            <h4 class="text-xl font-semibold text-gray-800 mb-2">{{ note.title }}</h4>
            <p class="text-gray-600 text-sm mb-3 line-clamp-2">{{ note.content }}</p>
            <div class="flex items-center justify-between text-sm text-gray-500">
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M10 12a2 2 0 100-4 2 2 0 000 4z"/>
                  <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd"/>
                </svg>
                {{ note.viewCount || 0 }}
              </span>
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clip-rule="evenodd"/>
                </svg>
                {{ note.likeCount || 0 }}
              </span>
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z" clip-rule="evenodd"/>
                </svg>
                {{ note.commentCount || 0 }}
              </span>
            </div>
            <div class="mt-2 text-xs text-gray-400">
              {{ formatTime(note.createTime) }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 最新游记 -->
    <section>
      <h3 class="text-2xl font-bold text-gray-800 mb-4">最新发布</h3>
      <div v-if="latestNotes.length === 0" class="text-center py-12 text-gray-500">
        暂无最新游记
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
            v-for="note in latestNotes"
            :key="note.id"
            class="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow cursor-pointer"
            @click="viewDetail(note.id)"
        >
          <img
              v-if="note.coverImage"
              :src="note.coverImage"
              :alt="note.title"
              class="w-full h-48 object-cover"
          />
          <div v-else class="w-full h-48 bg-gray-200 flex items-center justify-center">
            <svg class="w-16 h-16 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd"/>
            </svg>
          </div>
          <div class="p-4">
            <h4 class="text-xl font-semibold text-gray-800 mb-2">{{ note.title }}</h4>
            <p class="text-gray-600 text-sm mb-3 line-clamp-2">{{ note.content }}</p>
            <div class="flex items-center justify-between text-sm text-gray-500">
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M10 12a2 2 0 100-4 2 2 0 000 4z"/>
                  <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd"/>
                </svg>
                {{ note.viewCount || 0 }}
              </span>
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clip-rule="evenodd"/>
                </svg>
                {{ note.likeCount || 0 }}
              </span>
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z" clip-rule="evenodd"/>
                </svg>
                {{ note.commentCount || 0 }}
              </span>
            </div>
            <div class="mt-2 text-xs text-gray-400">
              {{ formatTime(note.createTime) }}
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { noteApi } from '@/api/note';
import type { TravelNote } from '@/api/note';

const router = useRouter();
const searchKeyword = ref('');
const hotNotes = ref<TravelNote[]>([]);
const latestNotes = ref<TravelNote[]>([]);
const isLoading = ref(false);
const error = ref<string | null>(null);

// 加载热门游记
const loadHotNotes = async () => {
  try {
    isLoading.value = true;
    error.value = null;
    const data = await noteApi.getHotNotes(6);
    hotNotes.value = Array.isArray(data) ? data : [];
  } catch (err: any) {
    console.error('加载热门游记失败:', err);
    error.value = err.message || '加载热门游记失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
};

// 加载最新游记
const loadLatestNotes = async () => {
  try {
    isLoading.value = true;
    error.value = null;
    const data = await noteApi.getLatestNotes(6);
    latestNotes.value = Array.isArray(data) ? data : [];
  } catch (err: any) {
    console.error('加载最新游记失败:', err);
    error.value = err.message || '加载最新游记失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
};

// 搜索游记
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    return;
  }
  try {
    isLoading.value = true;
    error.value = null;
    const data = await noteApi.search(searchKeyword.value, 1, 12);
    hotNotes.value = Array.isArray(data) ? data : [];
  } catch (err: any) {
    console.error('搜索游记失败:', err);
    error.value = err.message || '搜索失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
};

// 查看详情
const viewDetail = async (_id: number) => {
  try {
    await noteApi.incrementView(_id);
    router.push(`/community/${_id}`);
  } catch (err) {
    console.error('增加浏览量失败:', err);
    router.push(`/community/${_id}`);
  }
};

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return '';
  const date = new Date(time);
  return date.toLocaleDateString('zh-CN');
};

onMounted(() => {
  loadHotNotes();
  loadLatestNotes();
});
</script>
