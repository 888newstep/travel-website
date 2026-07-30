<template>
  <div class="max-w-4xl mx-auto px-6 py-12">
    <h1 class="text-2xl font-semibold text-stone-900 mb-8">分享管理</h1>

    <!-- 生成分享码 -->
    <div class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-sm font-medium text-stone-700 mb-4">生成分享码</h2>
      <div class="flex flex-wrap gap-3">
        <input v-model="genForm.itemId" type="text" placeholder="Item ID" class="px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm" />
        <input v-model="genForm.itemType" type="text" placeholder="Item Type" class="px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm" />
        <button @click="generateShare" class="px-4 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors">生成</button>
      </div>
      <p v-if="genResult" class="mt-3 text-sm text-stone-600">分享码：{{ genResult.code || genResult }}</p>
    </div>

    <!-- 验证分享码 -->
    <div class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-sm font-medium text-stone-700 mb-4">验证分享码</h2>
      <div class="flex flex-wrap gap-3">
        <input v-model="validateCode" type="text" placeholder="输入分享码" class="px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm" />
        <button @click="validateShare" class="px-4 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors">验证</button>
      </div>
      <p v-if="validateResult" class="mt-3 text-sm" :class="validateResult.valid ? 'text-green-600' : 'text-red-500'">{{ validateResult.valid ? '有效' : '无效' }}</p>
    </div>

    <!-- 我的分享列表 -->
    <div class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-sm font-medium text-stone-700 mb-4">我的分享列表</h2>
      <div class="flex flex-wrap gap-3 mb-4">
        <input v-model="listUserId" type="text" placeholder="用户 ID" class="px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm" />
        <button @click="fetchUserShares" class="px-4 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors">查询</button>
      </div>
      <div v-if="shareList.length" class="space-y-3">
        <div
          v-for="item in shareList"
          :key="item.id"
          class="flex items-center justify-between border border-stone-100 rounded-lg p-3"
        >
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-stone-900">{{ item.code || item.shareCode }}</p>
            <p class="text-xs text-stone-400">ID: {{ item.id }} | 类型: {{ item.itemType || '--' }}</p>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <button @click="cancelShare(item.id)" class="px-3 py-1 border border-stone-200 rounded-lg text-xs text-stone-500 hover:text-red-500 hover:border-red-200 transition-colors">取消</button>
          </div>
        </div>
      </div>
      <p v-else-if="listUserId && !shareList.length" class="text-sm text-stone-400">暂无分享记录</p>
    </div>

    <!-- 热门分享 -->
    <div class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-sm font-medium text-stone-700">热门分享</h2>
        <button @click="fetchPopular" class="px-4 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors">获取热门</button>
      </div>
      <div v-if="popularList.length" class="space-y-3">
        <div
          v-for="item in popularList"
          :key="item.id"
          class="flex items-center justify-between border border-stone-100 rounded-lg p-3"
        >
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-stone-900">{{ item.code || item.shareCode }}</p>
            <p class="text-xs text-stone-400">访问: {{ item.visitCount || 0 }} | ID: {{ item.id }}</p>
          </div>
        </div>
      </div>
      <p v-else class="text-sm text-stone-400">点击按钮获取热门分享</p>
    </div>

    <!-- 分享统计 -->
    <div class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-sm font-medium text-stone-700 mb-4">分享统计</h2>
      <div class="flex flex-wrap gap-3">
        <input v-model="statsId" type="text" placeholder="分享 ID" class="px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm" />
        <button @click="fetchStats" class="px-4 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors">查看统计</button>
      </div>
      <div v-if="statsResult" class="mt-4 p-4 bg-stone-50 rounded-lg">
        <pre class="text-xs text-stone-600 whitespace-pre-wrap">{{ JSON.stringify(statsResult, null, 2) }}</pre>
      </div>
    </div>

    <!-- 批量取消分享 -->
    <div class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-sm font-medium text-stone-700 mb-4">批量取消分享</h2>
      <div class="flex flex-wrap gap-3">
        <input v-model="batchIds" type="text" placeholder="ID 列表（逗号分隔）" class="px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm flex-1" />
        <button @click="batchCancel" class="px-4 py-2 border border-stone-200 rounded-lg text-sm text-red-500 hover:bg-red-50 transition-colors">批量取消</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { shareApi } from '../api/share.api'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, DEFAULT_LIMIT } from '../constants'

const genForm = ref({ itemId: '', itemType: '' })
const genResult = ref<any>(null)

const validateCode = ref('')
const validateResult = ref<any>(null)

const listUserId = ref('')
const shareList = ref<any[]>([])

const popularList = ref<any[]>([])

const statsId = ref('')
const statsResult = ref<any>(null)

const batchIds = ref('')

function generateShare() {
  shareApi.generateShareCode(Number(genForm.value.itemId) || 0, genForm.value.itemType || 'route')
    .then((res: any) => { genResult.value = res })
    .catch(() => {})
}

function validateShare() {
  shareApi.validateShareCode(validateCode.value)
    .then((res: any) => { validateResult.value = res })
    .catch(() => {})
}

function fetchUserShares() {
  shareApi.getUserShares(Number(listUserId.value) || 0, DEFAULT_PAGE, DEFAULT_PAGE_SIZE)
    .then((res: any) => { shareList.value = Array.isArray(res) ? res : (res.records || []) })
    .catch(() => {})
}

function fetchPopular() {
  shareApi.getPopularShares(DEFAULT_LIMIT)
    .then((res: any) => { popularList.value = Array.isArray(res) ? res : (res.records || []) })
    .catch(() => {})
}

function fetchStats() {
  shareApi.getShareStatistics(Number(statsId.value) || 0)
    .then((res: any) => { statsResult.value = res })
    .catch(() => {})
}

function cancelShare(id: string) {
  shareApi.cancelShare(Number(id))
    .then(() => { shareList.value = shareList.value.filter(item => item.id !== id) })
    .catch(() => {})
}

function batchCancel() {
  const ids = batchIds.value.split(',').map(s => Number(s.trim())).filter(n => n > 0)
  if (!ids.length) return
  shareApi.batchCancelShares(ids)
    .then(() => { batchIds.value = ''; alert('批量取消成功') })
    .catch(() => {})
}
</script>