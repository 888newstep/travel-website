<template>
  <div class="space-y-8">
    <!-- 页面标题 -->
    <section class="bg-white rounded-xl shadow-md p-8">
      <div class="flex justify-between items-center">
        <div>
          <h2 class="text-3xl font-bold text-gray-800 mb-2">我的行程</h2>
          <p class="text-gray-600">管理和规划您的旅行行程</p>
        </div>
        <button
            @click="showCreateModal = true"
            class="px-6 py-2 bg-linear-to-r from-indigo-600 to-blue-600 text-white rounded-lg hover:from-indigo-700 hover:to-blue-700 transition-all shadow-md"
        >
          ✨ 创建新行程
        </button>
      </div>
    </section>

    <!-- 错误提示 -->
    <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4 text-red-600">
      {{ error }}
    </div>

    <!-- 加载中 -->
    <div v-if="isLoading" class="flex justify-center items-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
    </div>

    <!-- 行程列表 -->
    <section v-else>
      <div v-if="itineraries.length === 0" class="text-center py-12 text-gray-500">
        <p class="text-lg">暂无行程</p>
        <p class="text-sm mt-2">点击"创建新行程"开始规划您的旅行</p>
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
            v-for="itinerary in itineraries"
            :key="itinerary.id"
            class="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow cursor-pointer"
            @click="viewDetail(itinerary.id)"
        >
          <div class="h-48 bg-linear-to-br from-indigo-500 to-blue-500 flex items-center justify-center">
            <svg class="w-20 h-20 text-white opacity-50" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clip-rule="evenodd"/>
            </svg>
          </div>
          <div class="p-4">
            <h3 class="text-xl font-semibold text-gray-800 mb-2">{{ itinerary.title }}</h3>
            <div class="space-y-2 text-sm text-gray-600">
              <div class="flex items-center gap-2">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd"/>
                </svg>
                <span>{{ itinerary.destination }}</span>
              </div>
              <div class="flex items-center gap-2">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clip-rule="evenodd"/>
                </svg>
                <span>{{ itinerary.days }} 天</span>
              </div>
              <div class="flex items-center gap-2">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z"/>
                  <path fill-rule="evenodd" d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm3 4a1 1 0 000 2h.01a1 1 0 100-2H7zm3 0a1 1 0 000 2h3a1 1 0 100-2h-3zm-3 4a1 1 0 100 2h.01a1 1 0 100-2H7zm3 0a1 1 0 100 2h3a1 1 0 100-2h-3z" clip-rule="evenodd"/>
                </svg>
                <span>{{ itinerary.activities || 0 }} 个活动</span>
              </div>
            </div>
            <div class="mt-3 flex items-center justify-between">
              <span class="text-xs text-gray-400">{{ formatTime(itinerary.createTime) }}</span>
              <span :class="getStatusClass(itinerary.status)" class="text-xs px-2 py-1 rounded-full">
                {{ getStatusText(itinerary.status) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 创建行程模态框 -->
    <div v-if="showCreateModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl shadow-2xl p-8 max-w-md w-full mx-4">
        <h2 class="text-2xl font-bold mb-6">创建新行程</h2>
        <form @submit.prevent="handleCreate" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">行程标题</label>
            <input
                v-model="createForm.title"
                type="text"
                required
                placeholder="例如：北京三日游"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">目的地</label>
            <input
                v-model="createForm.destination"
                type="text"
                required
                placeholder="例如：北京"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">天数</label>
            <input
                v-model.number="createForm.days"
                type="number"
                min="1"
                required
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>
          <div v-if="error" class="text-red-600 text-sm">{{ error }}</div>
          <div class="flex gap-3">
            <button
                type="button"
                @click="showCreateModal = false"
                class="flex-1 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
            >
              取消
            </button>
            <button
                type="submit"
                :disabled="isCreating"
                class="flex-1 py-2 bg-linear-to-r from-indigo-600 to-blue-600 text-white rounded-lg hover:from-indigo-700 hover:to-blue-700 transition-all disabled:opacity-50 font-semibold"
            >
              {{ isCreating ? '创建中...' : '✨ 创建行程' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { routeApi } from '@/api/route';
import type { RoutePlan } from '@/api/route';

const router = useRouter();
const itineraries = ref<RoutePlan[]>([]);
const isLoading = ref(false);
const error = ref<string | null>(null);
const showCreateModal = ref(false);
const isCreating = ref(false);

const createForm = ref({
  title: '',
  destination: '',
  days: 3,
});

// 加载行程列表
const loadItineraries = async () => {
  try {
    isLoading.value = true;
    error.value = null;
    // TODO: 从用户信息中获取 userId
    const userId = 1;
    const data = await routeApi.getList(userId);
    itineraries.value = Array.isArray(data) ? data : [];
  } catch (err: any) {
    console.error('加载行程列表失败:', err);
    error.value = err.message || '加载行程列表失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
};

// 查看详情
const viewDetail = (id: number) => {
  router.push(`/itineraries/${id}`);
};

// 创建行程
const handleCreate = async () => {
  try {
    isCreating.value = true;
    error.value = null;

    // TODO: 调用创建行程 API
    await new Promise(resolve => setTimeout(resolve, 1000));

    showCreateModal.value = false;
    createForm.value = { title: '', destination: '', days: 3 };
    await loadItineraries();
  } catch (err: any) {
    console.error('创建行程失败:', err);
    error.value = err.message || '创建行程失败，请稍后重试';
  } finally {
    isCreating.value = false;
  }
};

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return '';
  const date = new Date(time);
  return date.toLocaleDateString('zh-CN');
};

// 获取状态样式
const getStatusClass = (status?: string) => {
  const statusMap: Record<string, string> = {
    'planning': 'bg-yellow-100 text-yellow-800',
    'ongoing': 'bg-blue-100 text-blue-800',
    'completed': 'bg-green-100 text-green-800',
    'cancelled': 'bg-red-100 text-red-800',
  };
  return statusMap[status || ''] || 'bg-gray-100 text-gray-800';
};

// 获取状态文本
const getStatusText = (status?: string) => {
  const statusMap: Record<string, string> = {
    'planning': '规划中',
    'ongoing': '进行中',
    'completed': '已完成',
    'cancelled': '已取消',
  };
  return statusMap[status || ''] || '未知';
};

onMounted(() => {
  loadItineraries();
});
</script>
