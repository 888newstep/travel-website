NEW_FILE_CODE
<template>
  <div class="space-y-8">
    <!-- 顶部搜索和筛选 -->
    <section class="bg-white rounded-xl shadow-md p-6">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="flex-1">
          <input
              v-model="searchKeyword"
              @keyup.enter="handleSearch"
              type="text"
              placeholder="搜索景点名称..."
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <select
            v-model="selectedCity"
            @change="loadAttractions"
            class="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
        >
          <option value="">全部城市</option>
          <option v-for="city in cities" :key="city.id" :value="city.id">
            {{ city.name }}
          </option>
        </select>
        <button
            @click="loadAttractions"
            class="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >
          🔍 搜索
        </button>
      </div>
    </section>

    <!-- 加载状态 -->
    <div v-if="isLoading && attractions.length === 0" class="flex justify-center py-16">
      <div class="text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
        <p class="mt-4 text-gray-600">加载景点中...</p>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-else-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4 text-red-600">
      {{ error }}
      <button @click="loadAttractions" class="ml-4 text-indigo-600 hover:text-indigo-700">重试</button>
    </div>

    <!-- 景点网格 -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div
          v-for="attraction in attractions"
          :key="attraction.id"
          class="bg-white rounded-xl shadow-md overflow-hidden hover:shadow-xl transition-all cursor-pointer group"
          @click="viewDetail(attraction.id)"
      >
        <!-- 景点图片 -->
        <div class="relative h-56 overflow-hidden">
          <img
              :src="getFirstImage(attraction.images) || 'https://via.placeholder.com/400x300?text=景点'"
              :alt="attraction.name"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
          <div class="absolute top-2 right-2 px-3 py-1 bg-white bg-opacity-90 rounded-full text-sm font-semibold">
            ⭐ {{ attraction.rating }}
          </div>
        </div>

        <!-- 景点信息 -->
        <div class="p-5">
          <h3 class="text-xl font-bold text-gray-800 mb-2">{{ attraction.name }}</h3>

          <p class="text-gray-600 text-sm mb-3 line-clamp-2">{{ attraction.description }}</p>

          <!-- 地址 -->
          <div class="flex items-start space-x-2 text-sm text-gray-600 mb-3">
            <span>📍</span>
            <span class="line-clamp-1">{{ attraction.address || '地址待定' }}</span>
          </div>

          <!-- 开放时间和门票 -->
          <div class="space-y-2 text-sm">
            <div class="flex items-center justify-between">
              <span class="text-gray-600">🕐 开放时间：</span>
              <span class="text-gray-800">{{ attraction.openingHours || '全天' }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-gray-600">🎫 门票：</span>
              <span class="text-indigo-600 font-semibold">
                {{ attraction.ticketPrice > 0 ? `¥${attraction.ticketPrice}` : '免费' }}
              </span>
            </div>
          </div>

          <!-- 浏览量 -->
          <div class="mt-4 pt-3 border-t border-gray-100 flex items-center justify-between text-sm text-gray-500">
            <span>👁️ {{ attraction.viewCount || 0 }} 次浏览</span>
            <button class="text-indigo-600 hover:text-indigo-700 font-medium">
              查看详情 →
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!isLoading && attractions.length === 0" class="text-center py-16">
      <div class="text-6xl mb-4">🏞️</div>
      <p class="text-gray-500 text-lg">暂无景点数据</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

interface Attraction {
  id: number;
  name: string;
  cityId: number;
  address?: string;
  description: string;
  ticketPrice: number;
  openingHours?: string;
  latitude?: number;
  longitude?: number;
  images?: string; // JSON字符串
  rating: number;
  viewCount: number;
  createTime?: string;
  updateTime?: string;
}

interface City {
  id: number;
  name: string;
  country?: string;
  province?: string;
}

const router = useRouter();

// 状态管理
const attractions = ref<Attraction[]>([]);
const cities = ref<City[]>([]);
const isLoading = ref(false);
const error = ref<string | null>(null);
const searchKeyword = ref('');
const selectedCity = ref('');

// 获取第一张图片
const getFirstImage = (imagesJson?: string) => {
  if (!imagesJson) return '';
  try {
    const images = JSON.parse(imagesJson);
    return Array.isArray(images) && images.length > 0 ? images[0] : '';
  } catch {
    return '';
  }
};

// 加载城市列表
const loadCities = async () => {
  try {
    const response = await axios.get('/api/cities');
    cities.value = response.data.data || response.data || [];
  } catch (err) {
    console.error('加载城市列表失败:', err);
  }
};

// 加载景点列表
const loadAttractions = async () => {
  try {
    isLoading.value = true;
    error.value = null;

    let url = '/api/attractions';
    const params: any = {};

    if (selectedCity.value) {
      url = `/api/attractions/city/${selectedCity.value}`;
    } else if (searchKeyword.value) {
      url = '/api/attractions/search';
      params.keyword = searchKeyword.value;
    }

    const response = await axios.get(url, { params });
    attractions.value = response.data.data || response.data || [];
  } catch (err: any) {
    error.value = err.message || '加载景点失败，请检查后端服务';
    console.error('加载景点失败:', err);
  } finally {
    isLoading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  loadAttractions();
};

// 查看详情
const viewDetail = (id: number) => {
  router.push(`/attractions/${id}`);
};

// 初始化
onMounted(() => {
  loadCities();
  loadAttractions();
});
</script>
