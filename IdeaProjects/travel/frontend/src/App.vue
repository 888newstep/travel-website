<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { userApi } from './api/user';
import { systemApi } from './api/system';

const router = useRouter();
const isLoading = ref(false);
const error = ref<string | null>(null);
const showAuthModal = ref(false);
const loginForm = ref({ username: '', password: '' });
const currentUser = ref<any>(null);

const handleLogin = async () => {
  try {
    isLoading.value = true;
    error.value = null;

    const response = await userApi.login({
      username: loginForm.value.username,
      password: loginForm.value.password,
    });

    localStorage.setItem('token', response.token);

    const user = await userApi.getCurrentUser();
    currentUser.value = {
      ...user,
      stats: user.stats || { notes: 0, collections: 0, shares: 0 }
    };

    showAuthModal.value = false;
    loginForm.value = { username: '', password: '' };

    // 登录后跳转到个人资料页
    router.push('/profile');
  } catch (err: any) {
    error.value = err.message || '登录失败，请检查用户名和密码';
    console.error('登录失败:', err);
  } finally {
    isLoading.value = false;
  }
};

const handleLogout = async () => {
  try {
    await userApi.logout();
  } catch (err) {
    console.error('登出失败:', err);
  } finally {
    localStorage.removeItem('token');
    currentUser.value = null;
    router.push('/'); // 退出后回到首页
  }
};

onMounted(async () => {
  try {
    const token = localStorage.getItem('token');
    if (token) {
      const user = await userApi.getCurrentUser();
      currentUser.value = {
        ...user,
        stats: user.stats || { notes: 0, collections: 0, shares: 0 }
      };
    }

    const health = await systemApi.healthCheck();
    console.log('系统健康状态:', health);
  } catch (err) {
    console.error('初始化数据加载失败:', err);
  }
});
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
    <header class="bg-white shadow-sm">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex justify-between items-center">
        <h1 class="text-2xl font-bold text-indigo-600">智慧旅游平台</h1>
        <nav class="flex space-x-4">
          <router-link
              to="/"
              class="px-4 py-2 rounded-lg transition-colors"
              active-class="bg-indigo-600 text-white"
              inactive-class="text-gray-600 hover:bg-gray-100"
          >
            首页
          </router-link>

          <router-link
              v-if="currentUser"
              to="/profile"
              class="px-4 py-2 rounded-lg transition-colors"
              active-class="bg-indigo-600 text-white"
              inactive-class="text-gray-600 hover:bg-gray-100"
          >
            {{ currentUser.username }}
          </router-link>

          <router-link
              to="/attractions"
              class="px-4 py-2 rounded-lg transition-colors"
              active-class="bg-indigo-600 text-white"
              inactive-class="text-gray-600 hover:bg-gray-100"
          >
            景点
          </router-link>

          <router-link
              to="/itineraries"
              class="px-4 py-2 rounded-lg transition-colors"
              active-class="bg-indigo-600 text-white"
              inactive-class="text-gray-600 hover:bg-gray-100"
          >
            行程
          </router-link>

          <router-link
              to="/community"
              class="px-4 py-2 rounded-lg transition-colors"
              active-class="bg-indigo-600 text-white"
              inactive-class="text-gray-600 hover:bg-gray-100"
          >
            社区
          </router-link>

          <button
              v-if="!currentUser"
              @click="showAuthModal = true"
              class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
          >
            登录
          </button>
          <button
              v-if="currentUser"
              @click="handleLogout"
              class="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
          >
            退出
          </button>
        </nav>
      </div>
    </header>

    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div v-if="error" class="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-600">
        {{ error }}
      </div>

      <div v-if="isLoading" class="flex justify-center items-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>

      <!-- 路由视图出口 -->
      <router-view v-else />
    </main>

    <!-- 登录模态框保持不变 -->
    <div v-if="showAuthModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl shadow-2xl p-8 max-w-md w-full mx-4">
        <h2 class="text-2xl font-bold mb-6 text-center">用户登录</h2>
        <form @submit.prevent="handleLogin" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">用户名</label>
            <input
                v-model="loginForm.username"
                type="text"
                required
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">密码</label>
            <input
                v-model="loginForm.password"
                type="password"
                required
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>
          <div v-if="error" class="text-red-600 text-sm">{{ error }}</div>
          <button
              type="submit"
              :disabled="isLoading"
              class="w-full py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50"
          >
            {{ isLoading ? '登录中...' : '登录' }}
          </button>
          <button
              type="button"
              @click="showAuthModal = false"
              class="w-full py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
          >
            取消
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
