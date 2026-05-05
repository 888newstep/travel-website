NEW_FILE_CODE
<template>
  <div class="space-y-8">
    <!-- 智能规划表单 -->
    <section class="bg-white rounded-xl shadow-md p-8">
      <h2 class="text-2xl font-bold text-gray-800 mb-6">🤖 AI智能路线规划</h2>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <!-- 左侧：规划表单 -->
        <div class="space-y-6">
          <div class="bg-gradient-to-br from-indigo-50 to-blue-50 rounded-lg p-6">
            <h3 class="text-lg font-semibold text-gray-800 mb-4">个性化偏好设置</h3>

            <form @submit.prevent="generateRoute" class="space-y-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">目的地城市ID <span class="text-red-500">*</span></label>
                <input
                    v-model.number="planForm.cityId"
                    type="number"
                    required
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    placeholder="例如：7（杭州）"
                />
                <p class="text-xs text-gray-500 mt-1">1-北京, 2-上海, 5-成都, 6-西安, 7-杭州, 8-重庆</p>
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">游玩天数</label>
                <input
                    v-model.number="planForm.days"
                    type="number"
                    min="1"
                    max="15"
                    required
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">兴趣偏好（多选）</label>
                <div class="grid grid-cols-2 gap-2">
                  <label v-for="pref in availablePreferences" :key="pref.value" class="flex items-center space-x-2">
                    <input
                        type="checkbox"
                        :value="pref.value"
                        v-model="planForm.preferences"
                        class="rounded text-indigo-600 focus:ring-indigo-500"
                    />
                    <span class="text-sm text-gray-700">{{ pref.label }}</span>
                  </label>
                </div>
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">预算范围（元）</label>
                <select
                    v-model="planForm.budget"
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="">不限</option>
                  <option value="1000">1000以下</option>
                  <option value="3000">1000-3000</option>
                  <option value="5000">3000-5000</option>
                  <option value="10000">5000-10000</option>
                  <option value="10001">10000以上</option>
                </select>
              </div>

              <button
                  type="submit"
                  :disabled="isGenerating"
                  class="w-full py-3 bg-gradient-to-r from-indigo-600 to-blue-600 text-white rounded-lg hover:from-indigo-700 hover:to-blue-700 transition-all disabled:opacity-50 font-semibold shadow-md"
              >
                {{ isGenerating ? '🤖 AI规划中...' : '✨ 生成智能路线' }}
              </button>
            </form>
          </div>
        </div>

        <!-- 右侧：规划结果 -->
        <div class="space-y-4">
          <h3 class="text-lg font-semibold text-gray-800">推荐路线</h3>

          <!-- 加载状态 -->
          <div v-if="isGenerating" class="flex justify-center py-12">
            <div class="text-center">
              <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto mb-4"></div>
              <p class="text-gray-600">AI正在分析你的偏好...</p>
              <p class="text-sm text-gray-500 mt-2">结合景点评分、距离、开放时间等多维度数据</p>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else-if="generatedRoutes.length === 0" class="bg-gray-50 rounded-lg p-8 text-center">
            <div class="text-6xl mb-4">🗺️</div>
            <p class="text-gray-500">填写左侧信息，AI将为你生成个性化路线</p>
          </div>

          <!-- 路线列表 -->
          <div v-else class="space-y-4">
            <div
                v-for="(route, index) in generatedRoutes"
                :key="index"
                class="bg-white border-2 border-indigo-100 rounded-lg p-5 hover:border-indigo-300 transition-all cursor-pointer shadow-sm"
                @click="selectRoute(route)"
            >
              <div class="flex justify-between items-start mb-3">
                <h4 class="font-semibold text-gray-800 text-lg">{{ route.title }}</h4>
                <span class="px-3 py-1 bg-indigo-100 text-indigo-700 text-xs rounded-full font-medium">
                  推荐方案 {{ index + 1 }}
                </span>
              </div>

              <p class="text-sm text-gray-600 mb-4">{{ route.description }}</p>

              <div class="grid grid-cols-3 gap-3 text-sm">
                <div class="bg-gray-50 rounded p-2 text-center">
                  <div class="text-gray-500">天数</div>
                  <div class="font-semibold text-gray-800">{{ route.days }}天</div>
                </div>
                <div class="bg-gray-50 rounded p-2 text-center">
                  <div class="text-gray-500">预算</div>
                  <div class="font-semibold text-indigo-600">¥{{ route.budget }}</div>
                </div>
                <div class="bg-gray-50 rounded p-2 text-center">
                  <div class="text-gray-500">评分</div>
                  <div class="font-semibold text-yellow-600">⭐ {{ route.rating }}</div>
                </div>
              </div>

              <div v-if="route.attractions && route.attractions.length > 0" class="mt-4 pt-4 border-t border-gray-100">
                <p class="text-xs text-gray-500 mb-2">包含景点：</p>
                <div class="flex flex-wrap gap-2">
                  <span
                      v-for="(attr, idx) in route.attractions.slice(0, 5)"
                      :key="idx"
                      class="px-2 py-1 bg-indigo-50 text-indigo-600 text-xs rounded"
                  >
                    {{ attr }}
                  </span>
                  <span v-if="route.attractions.length > 5" class="text-xs text-gray-500">
                    +{{ route.attractions.length - 5 }}个
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 热门路线推荐 -->
    <section class="bg-white rounded-xl shadow-md p-8">
      <h3 class="text-xl font-bold text-gray-800 mb-6">🔥 热门路线推荐</h3>

      <div v-if="popularLoading" class="flex justify-center py-8">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div
            v-for="route in popularRoutes"
            :key="route.id"
            class="bg-gradient-to-br from-purple-50 to-pink-50 rounded-lg p-6 hover:shadow-lg transition-all cursor-pointer"
            @click="viewPopularRoute(route)"
        >
          <h4 class="font-semibold text-gray-800 mb-2">{{ route.title }}</h4>
          <p class="text-sm text-gray-600 mb-3 line-clamp-2">{{ route.description }}</p>
          <div class="flex justify-between text-sm">
            <span class="text-purple-600 font-medium">{{ route.days }}天</span>
            <span class="text-gray-500">👁️ {{ route.viewCount }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

interface GeneratedRoute {
  title: string;
  description: string;
  days: number;
  budget: number;
  rating: number;
  attractions?: string[];
}

interface PopularRoute {
  id: number;
  title: string;
  description: string;
  days: number;
  viewCount: number;
  cityId: number;
}

const router = useRouter();

// 状态管理
const isGenerating = ref(false);
const generatedRoutes = ref<GeneratedRoute[]>([]);
const popularRoutes = ref<PopularRoute[]>([]);
const popularLoading = ref(false);

// 规划表单
const planForm = ref({
  cityId: undefined as number | undefined,
  days: 3,
  preferences: [] as string[],
  budget: '',
});

// 可用偏好
const availablePreferences = [
  { value: '自然风光', label: '🏞️ 自然风光' },
  { value: '历史文化', label: '🏛️ 历史文化' },
  { value: '美食探索', label: '🍜 美食探索' },
  { value: '购物休闲', label: '🛍️ 购物休闲' },
  { value: '户外运动', label: '🚴 户外运动' },
  { value: '摄影打卡', label: '📸 摄影打卡' },
  { value: '亲子游', label: '👨‍👩‍👧 亲子游' },
  { value: '浪漫情侣', label: '💑 浪漫情侣' },
];

// 当前用户ID
const currentUserId = computed(() => {
  const userStr = localStorage.getItem('user');
  if (userStr) {
    try {
      const user = JSON.parse(userStr);
      return user.id;
    } catch {
      return null;
    }
  }
  return null;
});

// 生成智能路线
const generateRoute = async () => {
  if (!planForm.value.cityId) {
    alert('请选择目的地城市');
    return;
  }

  try {
    isGenerating.value = true;
    generatedRoutes.value = [];

    // 调用后端接口 /api/route-plan/recommend
    const response = await axios.post('/api/route-plan/recommend', {
      userId: currentUserId.value || 1,
      cityId: planForm.value.cityId,
      days: planForm.value.days,
      preferences: planForm.value.preferences.reduce((acc, pref) => {
        acc[pref] = true;
        return acc;
      }, {} as Record<string, boolean>),
    });

    const data = response.data.data || response.data;

    if (Array.isArray(data) && data.length > 0) {
      // 转换后端数据格式
      generatedRoutes.value = data.map((item: any) => ({
        title: item.title || `精选${planForm.value.days}日游`,
        description: item.description || '精心规划的优质路线',
        days: item.days || planForm.value.days,
        budget: item.budget || 3000,
        rating: item.rating || 4.5,
        attractions: item.attractions || [],
      }));
    } else {
      // 如果后端返回空，使用默认推荐
      generatedRoutes.value = getDefaultRoutes();
    }
  } catch (err: any) {
    console.error('生成路线失败:', err);
    // 失败时使用默认路线
    generatedRoutes.value = getDefaultRoutes();
    alert('⚠️ AI服务暂时不可用，已为您展示默认推荐路线');
  } finally {
    isGenerating.value = false;
  }
};

// 获取默认路线（备用方案）
const getDefaultRoutes = (): GeneratedRoute[] => {
  return [
    {
      title: '经典必游路线',
      description: '涵盖城市最著名的景点，适合首次到访',
      days: planForm.value.days,
      budget: 3000,
      rating: 4.8,
      attractions: ['著名景点A', '著名景点B', '著名景点C'],
    },
    {
      title: '深度探索路线',
      description: '避开游客大军，探索本地人推荐的隐藏景点',
      days: planForm.value.days,
      budget: 2500,
      rating: 4.6,
      attractions: ['小众景点A', '特色街区B', '本地美食C'],
    },
    {
      title: '轻松休闲路线',
      description: '慢节奏旅行，充分休息与享受当地生活',
      days: planForm.value.days,
      budget: 3500,
      rating: 4.7,
      attractions: ['休闲公园A', '咖啡厅B', 'SPA中心C'],
    },
  ];
};

// 选择路线
const selectRoute = (route: GeneratedRoute) => {
  const confirmCreate = confirm(`是否创建行程"${route.title}"？`);
  if (confirmCreate) {
    router.push('/itineraries');
  }
};

// 加载热门路线
const loadPopularRoutes = async () => {
  try {
    popularLoading.value = true;

    // 调用后端接口 /api/route-plan/popular
    const response = await axios.get('/api/route-plan/popular', {
      params: {
        cityId: 7, // 默认杭州
        days: 3,
        limit: 3,
      },
    });

    const data = response.data.data || response.data;
    popularRoutes.value = Array.isArray(data) ? data : [];
  } catch (err: any) {
    console.error('加载热门路线失败:', err);
    // 使用默认数据
    popularRoutes.value = [
      {
        id: 1,
        title: '杭州经典三日游',
        description: '西湖、灵隐寺、宋城，体验江南韵味',
        days: 3,
        viewCount: 1234,
        cityId: 7,
      },
      {
        id: 2,
        title: '成都美食文化之旅',
        description: '宽窄巷子、大熊猫基地、火锅美食',
        days: 4,
        viewCount: 987,
        cityId: 5,
      },
      {
        id: 3,
        title: '云南大理丽江五日游',
        description: '苍山洱海、古城风情、玉龙雪山',
        days: 5,
        viewCount: 2156,
        cityId: 9,
      },
    ];
  } finally {
    popularLoading.value = false;
  }
};

// 查看热门路线详情
const viewPopularRoute = (route: PopularRoute) => {
  router.push(`/itineraries/${route.id}`);
};

// 初始化
onMounted(() => {
  loadPopularRoutes();
});
</script>
