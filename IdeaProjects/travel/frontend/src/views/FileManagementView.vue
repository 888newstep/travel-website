<template>
  <div class="app-container pb-16 pt-4 md:pt-6">
    <section class="surface-card mb-8 overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(239,246,255,0.92)_48%,rgba(255,251,235,0.88))] px-6 py-8 sm:px-8 sm:py-9">
      <div class="grid gap-8 xl:grid-cols-[1.1fr_0.9fr] xl:items-center">
        <div>
          <div class="mb-4 flex flex-wrap gap-2">
            <span class="chip">文件上传与检索</span>
            <span class="chip">分类管理</span>
            <span class="chip">版本历史与对比</span>
          </div>
          <h1 class="text-3xl font-semibold tracking-tight text-stone-900 md:text-4xl">把资源文件管理页整理成更清晰的工具工作台</h1>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-stone-600 md:text-base">
            统一上传、搜索、分类、版本和统计信息的展示方式，让文件管理从“功能可用”提升到“查找顺手”。
          </p>
          <div class="mt-6 grid gap-3 sm:grid-cols-3">
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">总文件数</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ stats?.totalFiles || 0 }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">总大小</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ stats?.totalSize || '0 MB' }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">当前列表</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ files.length }}</div>
            </div>
          </div>
        </div>

        <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
          <div class="mb-4 flex items-center justify-between gap-3">
            <div>
              <div class="text-sm font-medium text-stone-500">快捷操作</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">上传、检索与分类切换</div>
            </div>
            <button
              @click="showCategoryManager = !showCategoryManager"
              class="rounded-full border border-stone-200 bg-white px-4 py-2 text-sm font-medium text-stone-700 transition hover:bg-stone-50"
            >{{ showCategoryManager ? '返回列表' : '分类管理' }}</button>
          </div>
          <div class="flex flex-col gap-3 sm:flex-row">
            <div class="relative flex-1">
              <input
                v-model="keyword"
                type="text"
                placeholder="搜索文件名、分类或标签..."
                class="w-full rounded-2xl border border-stone-200 bg-white px-12 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
                @input="doSearch"
              />
              <svg class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
              </svg>
            </div>
            <label class="inline-flex cursor-pointer items-center justify-center gap-2 rounded-2xl bg-stone-900 px-5 py-3 text-sm font-medium text-white transition hover:bg-stone-800">
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
              上传文件
              <input type="file" class="hidden" @change="uploadFile" />
            </label>
          </div>
          <div class="mt-4 grid gap-2 text-sm text-stone-500 sm:grid-cols-2">
            <div class="rounded-2xl bg-stone-50 px-4 py-3">支持文件搜索、分类创建与删除、版本对比</div>
            <div class="rounded-2xl bg-stone-50 px-4 py-3">每项文件都可继续查看历史版本、下载和删除</div>
          </div>
        </div>
      </div>
    </section>

    <section v-if="showCategoryManager" class="surface-card mb-6 rounded-[1.75rem] p-6">
      <div class="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 class="text-lg font-semibold text-stone-900">分类管理</h2>
          <p class="mt-1 text-sm text-stone-500">创建或删除文件分类，帮助整理资源结构。</p>
        </div>
        <span class="chip">{{ categories.length }} 个分类</span>
      </div>
      <div class="mb-4 flex flex-col gap-3 sm:flex-row">
        <input
          v-model="newCategoryName"
          type="text"
          placeholder="新分类名称"
          class="flex-1 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
        />
        <button @click="createCategory" class="rounded-2xl bg-stone-900 px-5 py-3 text-sm font-medium text-white transition hover:bg-stone-800">创建分类</button>
      </div>
      <div v-if="categories.length" class="space-y-3">
        <div v-for="cat in categories" :key="cat.id || cat.name" class="flex items-center justify-between gap-3 rounded-2xl bg-stone-50 px-4 py-4">
          <span class="text-sm font-medium text-stone-700">{{ cat.name || cat }}</span>
          <button @click="deleteCategory(cat.id || cat.name)" class="rounded-full border border-red-200 px-3 py-1.5 text-xs font-medium text-red-500 transition hover:bg-red-50">删除</button>
        </div>
      </div>
      <p v-else class="py-8 text-center text-sm text-stone-400">暂无分类</p>
    </section>

    <LoadingSpinner v-if="loading" />
    <template v-else>
      <section class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 class="section-heading text-[1.75rem]">文件列表</h2>
          <p class="section-subtitle mt-2">用更紧凑的信息结构展示文件名、大小、分类、上传时间和操作入口。</p>
        </div>
        <div class="flex flex-wrap gap-2 text-xs text-stone-500">
          <span class="chip">{{ files.length }} 个结果</span>
          <span class="chip">{{ keyword ? `关键词：${keyword}` : '当前展示全部文件' }}</span>
        </div>
      </section>

      <div v-if="files.length" class="space-y-4">
        <article
          v-for="item in files"
          :key="item.id"
          class="surface-card rounded-[1.5rem] px-5 py-4"
        >
          <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div class="flex min-w-0 items-start gap-4">
              <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-stone-100 text-stone-400">
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
                </svg>
              </div>
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-semibold text-stone-900">{{ item.fileName }}</p>
                <div class="mt-2 flex flex-wrap gap-2 text-xs text-stone-500">
                  <span class="rounded-full bg-stone-50 px-3 py-1">{{ item.size || '--' }}</span>
                  <span v-if="item.category" class="rounded-full bg-stone-50 px-3 py-1">分类：{{ item.category }}</span>
                  <span v-if="item.uploadTime" class="rounded-full bg-stone-50 px-3 py-1">{{ new Date(item.uploadTime).toLocaleString() }}</span>
                </div>
              </div>
            </div>
            <div class="flex items-center gap-2 self-end lg:self-auto">
              <button
                @click="showVersions(item)"
                class="rounded-full p-2 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600"
                title="版本管理"
              >
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                </svg>
              </button>
              <a
                v-if="item.url"
                :href="item.url"
                target="_blank"
                class="rounded-full p-2 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600"
                title="下载"
              >
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3" />
                </svg>
              </a>
              <button
                @click="deleteFile(item)"
                class="rounded-full p-2 text-stone-400 transition hover:bg-red-50 hover:text-red-500"
                title="删除"
              >
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
                </svg>
              </button>
            </div>
          </div>
        </article>
      </div>
      <div v-else class="surface-card rounded-[1.75rem] px-6 py-14 text-center">
        <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-stone-100 text-stone-400">
          <svg class="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
            <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-stone-900">{{ keyword ? '未找到相关文件' : '当前还没有文件' }}</h3>
        <p class="mt-2 text-sm text-stone-500">{{ keyword ? '试试更宽泛的关键词，或返回全部文件。' : '点击右上角上传文件，开始沉淀资源。' }}</p>
      </div>
    </template>

    <Teleport to="body">
      <div v-if="versionModal.open" class="fixed inset-0 z-50 flex items-start justify-center px-4 pb-8 pt-16" @click.self="versionModal.open = false">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="versionModal.open = false" />
        <div class="surface-card relative max-h-full w-full max-w-lg overflow-y-auto rounded-[1.75rem] border border-white/80 bg-white/95 shadow-[0_32px_90px_-36px_rgba(15,23,42,0.45)] backdrop-blur">
          <div class="sticky top-0 z-10 flex items-center justify-between rounded-t-[1.75rem] border-b border-stone-100/80 bg-white/90 px-6 py-4 backdrop-blur">
            <h2 class="text-lg font-semibold text-stone-900">版本历史 - {{ versionModal.fileName }}</h2>
            <button @click="versionModal.open = false" class="rounded-lg p-2 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600">
              <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" /></svg>
            </button>
          </div>
          <div class="p-6">
            <LoadingSpinner v-if="versionModal.loading" />
            <div v-else-if="versionModal.versions.length" class="space-y-3">
              <div v-for="(v, i) in versionModal.versions" :key="i" class="rounded-2xl border border-white/80 bg-stone-50/80 p-4">
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <p class="text-sm font-medium text-stone-700">版本 {{ v.version || v.versionNumber || (i + 1) }}</p>
                    <p class="mt-1 text-xs text-stone-400">{{ v.createTime || v.uploadTime || '' }}</p>
                  </div>
                  <div class="flex gap-2">
                    <a v-if="v.url" :href="v.url" target="_blank" class="rounded-full border border-stone-200 px-3 py-1.5 text-xs transition hover:bg-white">下载</a>
                    <button @click="compareWithLatest(v)" class="rounded-full border border-stone-200 px-3 py-1.5 text-xs transition hover:bg-white">对比</button>
                  </div>
                </div>
              </div>
            </div>
            <p v-else class="py-4 text-center text-sm text-stone-400">暂无版本记录</p>

            <div v-if="compareResult" class="mt-4 rounded-2xl border border-amber-200 bg-amber-50 p-4">
              <h4 class="mb-2 text-sm font-medium text-amber-800">版本对比</h4>
              <div class="space-y-1 text-xs text-amber-700">
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

const compareResult = ref<any>(null)

async function compareWithLatest(version: any) {
  compareResult.value = null
  try {
    const result = await fileApi.getFileVersions(versionModal.value.fileId) as any[]
    const versions = Array.isArray(result) ? result : []
    if (versions.length >= 2) {
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