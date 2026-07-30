<template>
  <div class="max-w-4xl mx-auto px-6 py-12">
    <div class="flex items-center justify-between mb-8">
      <h1 class="text-2xl font-semibold text-stone-900">文件管理</h1>
      <div class="flex items-center gap-3">
        <button @click="showCategoryManager = !showCategoryManager" class="text-sm px-4 py-2 border border-stone-200 rounded-lg hover:bg-stone-50 transition-colors">
          {{ showCategoryManager ? '返回列表' : '分类管理' }}
        </button>
        <label class="text-sm px-4 py-2 bg-stone-900 text-white rounded-lg hover:bg-stone-800 transition-colors cursor-pointer inline-flex items-center gap-1">
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          上传文件
          <input type="file" class="hidden" @change="uploadFile" />
        </label>
      </div>
    </div>

    <!-- 分类管理面板 -->
    <div v-if="showCategoryManager" class="bg-white rounded-xl border border-stone-200 p-6 mb-6">
      <h2 class="text-base font-semibold text-stone-900 mb-4">分类管理</h2>
      <div class="flex gap-3 mb-4">
        <input v-model="newCategoryName" type="text" placeholder="新分类名称" class="flex-1 px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300" />
        <button @click="createCategory" class="px-4 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors">创建分类</button>
      </div>
      <div v-if="categories.length" class="space-y-2">
        <div v-for="cat in categories" :key="cat.id || cat.name" class="flex items-center justify-between p-3 bg-stone-50 rounded-lg">
          <span class="text-sm text-stone-700">{{ cat.name || cat }}</span>
          <button @click="deleteCategory(cat.id || cat.name)" class="px-3 py-1 text-red-500 border border-red-200 rounded text-xs hover:bg-red-50 transition-colors">删除</button>
        </div>
      </div>
      <p v-else class="text-sm text-stone-400 text-center py-4">暂无分类</p>
    </div>

    <LoadingSpinner v-if="loading" />
    <template v-else>
      <!-- 统计卡片 -->
      <div class="grid grid-cols-3 gap-4 mb-6">
        <div class="bg-white rounded-xl border border-stone-200 p-4 text-center">
          <p class="text-2xl font-semibold text-stone-900">{{ stats?.totalFiles || 0 }}</p>
          <p class="text-xs text-stone-500 mt-1">总文件数</p>
        </div>
        <div class="bg-white rounded-xl border border-stone-200 p-4 text-center">
          <p class="text-2xl font-semibold text-stone-900">{{ stats?.totalSize || '0 MB' }}</p>
          <p class="text-xs text-stone-500 mt-1">总大小</p>
        </div>
        <div class="bg-white rounded-xl border border-stone-200 p-4 text-center">
          <p class="text-2xl font-semibold text-stone-900">{{ files.length }}</p>
          <p class="text-xs text-stone-500 mt-1">当前列表</p>
        </div>
      </div>

      <!-- 搜索 -->
      <div class="relative mb-6">
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索文件..."
          class="w-full pl-10 pr-4 py-2.5 bg-white border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
          @input="doSearch"
        />
        <svg class="w-4 h-4 text-stone-400 absolute left-3 top-1/2 -translate-y-1/2" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
        </svg>
      </div>

      <!-- 文件列表 -->
      <div v-if="files.length" class="space-y-3">
        <div
          v-for="item in files"
          :key="item.id"
          class="bg-white rounded-xl border border-stone-200 p-4 flex items-center gap-4 hover:shadow-sm transition-shadow"
        >
          <div class="w-10 h-10 bg-stone-100 rounded-lg flex items-center justify-center text-stone-400 shrink-0">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
            </svg>
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-stone-900 truncate">{{ item.fileName }}</p>
            <p class="text-xs text-stone-400">
              {{ item.size || '--' }}
              <span v-if="item.category" class="ml-2">分类: {{ item.category }}</span>
              <span v-if="item.uploadTime" class="ml-2">{{ new Date(item.uploadTime).toLocaleString() }}</span>
            </p>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <button
              @click="showVersions(item)"
              class="p-2 text-stone-400 hover:text-stone-600 hover:bg-stone-100 rounded-lg transition-colors"
              title="版本管理"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
              </svg>
            </button>
            <a
              v-if="item.url"
              :href="item.url"
              target="_blank"
              class="p-2 text-stone-400 hover:text-stone-600 hover:bg-stone-100 rounded-lg transition-colors"
              title="下载"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3" />
              </svg>
            </a>
            <button
              @click="deleteFile(item)"
              class="p-2 text-stone-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
              title="删除"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
              </svg>
            </button>
          </div>
        </div>
      </div>
      <p v-else class="text-sm text-stone-400 text-center py-16">
        {{ keyword ? '未找到相关文件' : '暂无文件，点击右上角上传' }}
      </p>
    </template>

    <!-- 版本管理弹窗 -->
    <Teleport to="body">
      <div v-if="versionModal.open" class="fixed inset-0 z-50 flex items-start justify-center pt-16 pb-8 px-4" @click.self="versionModal.open = false">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="versionModal.open = false" />
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-full overflow-y-auto">
          <div class="sticky top-0 bg-white border-b border-stone-100 px-6 py-4 flex items-center justify-between rounded-t-2xl z-10">
            <h2 class="text-lg font-semibold text-stone-900">版本历史 - {{ versionModal.fileName }}</h2>
            <button @click="versionModal.open = false" class="p-2 text-stone-400 hover:text-stone-600 hover:bg-stone-100 rounded-lg">
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" /></svg>
            </button>
          </div>
          <div class="p-6">
            <LoadingSpinner v-if="versionModal.loading" />
            <div v-else-if="versionModal.versions.length" class="space-y-3">
              <div v-for="(v, i) in versionModal.versions" :key="i" class="flex items-center justify-between p-3 bg-stone-50 rounded-lg">
                <div>
                  <p class="text-sm font-medium text-stone-700">版本 {{ v.version || v.versionNumber || (i + 1) }}</p>
                  <p class="text-xs text-stone-400">{{ v.createTime || v.uploadTime || '' }}</p>
                </div>
                <div class="flex gap-2">
                  <a v-if="v.url" :href="v.url" target="_blank" class="px-3 py-1 border border-stone-200 rounded text-xs hover:bg-stone-100 transition-colors">下载</a>
                  <button @click="compareWithLatest(v)" class="px-3 py-1 border border-stone-200 rounded text-xs hover:bg-stone-100 transition-colors">对比</button>
                </div>
              </div>
            </div>
            <p v-else class="text-sm text-stone-400 text-center py-4">暂无版本记录</p>

            <!-- 版本对比结果 -->
            <div v-if="compareResult" class="mt-4 p-4 bg-amber-50 border border-amber-200 rounded-lg">
              <h4 class="text-sm font-medium text-amber-800 mb-2">版本对比</h4>
              <div class="text-xs text-amber-700 space-y-1">
                <div v-for="(v, k) in compareResult" :key="k" class="flex gap-2">
                  <span class="text-amber-500">{{ k }}:</span>
                  <span>{{ typeof v === 'object' ? JSON.stringify(v) : v }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fileApi } from '../api/file.api'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import { DEBOUNCE_DELAY } from '../constants'

const loading = ref(true)
const files = ref<any[]>([])
const keyword = ref('')
const stats = ref<any>(null)

async function fetchFiles() {
  loading.value = true
  try {
    files.value = keyword.value
      ? await fileApi.searchFiles(keyword.value) as any[]
      : await fileApi.getFileList() as any[]
    stats.value = await fileApi.getFileStatistics() as any
  } catch { /* ignore */ }
  loading.value = false
}

async function uploadFile(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files?.length) return
  const formData = new FormData()
  formData.append('file', input.files[0])
  try {
    await fileApi.uploadFile(formData)
    input.value = ''
    fetchFiles()
  } catch { /* ignore */ }
}

async function deleteFile(item: any) {
  if (!confirm('确定删除 "' + item.fileName + '" ？')) return
  try {
    await fileApi.deleteFile(item.id)
    files.value = files.value.filter(f => f.id !== item.id)
  } catch { /* ignore */ }
}

let searchTimer: ReturnType<typeof setTimeout>
function doSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(fetchFiles, DEBOUNCE_DELAY)
}

// 分类管理
const showCategoryManager = ref(false)
const newCategoryName = ref('')
const categories = ref<any[]>([])

async function fetchCategories() {
  try {
    categories.value = await fileApi.getCategories() as any[] || []
  } catch { /* ignore */ }
}

async function createCategory() {
  if (!newCategoryName.value.trim()) return
  try {
    await fileApi.createCategory({ tagName: newCategoryName.value.trim() })
    newCategoryName.value = ''
    fetchCategories()
  } catch { /* ignore */ }
}

async function deleteCategory(id: string) {
  try {
    await fileApi.deleteCategory(id)
    fetchCategories()
  } catch { /* ignore */ }
}

// 版本管理
const versionModal = ref({
  open: false,
  loading: false,
  fileId: '',
  fileName: '',
  versions: [] as any[],
})

async function showVersions(item: any) {
  versionModal.value = {
    open: true,
    loading: true,
    fileId: item.id,
    fileName: item.fileName,
    versions: [],
  }
  compareResult.value = null
  try {
    const data = await fileApi.getFileVersions(item.id) as any[]
    versionModal.value.versions = Array.isArray(data) ? data : []
  } catch { /* ignore */ }
  versionModal.value.loading = false
}

// 版本对比
const compareResult = ref<any>(null)

async function compareWithLatest(version: any) {
  compareResult.value = null
  try {
    const result = await fileApi.getFileVersions(versionModal.value.fileId) as any[]
    const versions = Array.isArray(result) ? result : []
    if (versions.length >= 2) {
      // 调用后端对比接口
      const latestVersion = versions[0]
      const compare = await fileApi.compareVersions(version.id || versions[versions.length - 1].id, latestVersion.id) as any
      compareResult.value = compare || { 
        version1: version.version || '旧版本',
        version2: '最新版本',
        diff: '请查看版本详情',
      }
    } else {
      compareResult.value = { message: '只有1个版本，无法对比' }
    }
  } catch (e: any) {
    compareResult.value = { error: '对比失败: ' + (e?.message || '') }
  }
}

onMounted(() => {
  fetchFiles()
  fetchCategories()
})
</script>