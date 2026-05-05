<template>
  <div class="space-y-8">
    <section v-if="currentUser" class="bg-white rounded-xl shadow-md p-8">
      <h2 class="text-2xl font-bold text-gray-800 mb-4">个人信息</h2>
      <div class="space-y-4">
        <div>
          <span class="font-semibold">用户名：</span>
          <span>{{ currentUser.username }}</span>
        </div>
        <div>
          <span class="font-semibold">手机号：</span>
          <span>{{ currentUser.phone }}</span>
        </div>
        <div v-if="currentUser.stats">
          <span class="font-semibold">统计信息：</span>
          <div class="mt-2 space-x-4">
            <span>笔记: {{ currentUser.stats.notes }}</span>
            <span>收藏: {{ currentUser.stats.collections }}</span>
            <span>分享: {{ currentUser.stats.shares }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { userApi } from '@/api/user';

interface UserProfile {
  id: number;
  username: string;
  phone: string;
  avatar: string;
  role: 'admin' | 'user';
  stats?: {
    notes: number;
    collections: number;
    shares: number;
  };
}

const currentUser = ref<UserProfile | null>(null);

onMounted(async () => {
  try {
    const user = await userApi.getCurrentUser();
    currentUser.value = {
      ...user,
      stats: user.stats || { notes: 0, collections: 0, shares: 0 }
    };
  } catch (err) {
    console.error('获取用户信息失败:', err);
  }
});
</script>
