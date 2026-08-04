<template>
  <div class="app-container pb-16 pt-4 md:pt-6">
    <section class="surface-card mb-8 overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(255,251,235,0.92)_45%,rgba(240,249,255,0.9))] px-6 py-8 sm:px-8 sm:py-9">
      <div class="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
        <div>
          <div class="mb-4 flex flex-wrap gap-2">
            <span class="chip">旅行内容沉淀</span>
            <span class="chip">关键词检索</span>
            <span class="chip">发布与互动</span>
          </div>
          <h1 class="text-3xl font-semibold tracking-tight text-stone-900 md:text-4xl">把旅行体验写成更容易被发现的内容</h1>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-stone-600 md:text-base">
            游记页现在更突出创作入口、内容信息层级和互动反馈，方便用户在阅读与发布之间快速切换。
          </p>
          <div class="mt-6 flex flex-wrap gap-3">
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">当前结果</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ notes.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">浏览模式</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ keyword ? '搜索结果' : '社区精选' }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">发布状态</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ isLoggedIn ? '可立即创作' : '登录后创作' }}</div>
            </div>
          </div>
        </div>

        <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
          <div class="mb-4 flex items-center justify-between gap-3">
            <div>
              <div class="text-sm font-medium text-stone-500">快速发现</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">先从关键词开始</div>
            </div>
            <button
              v-if="isLoggedIn"
              class="rounded-full bg-stone-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-stone-800"
              @click="showCreate = true"
            >写游记</button>
          </div>
          <div class="relative">
            <input
              v-model="keyword"
              type="text"
              placeholder="搜索标题、作者或内容片段..."
              class="w-full rounded-2xl border border-stone-200 bg-white px-12 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
              @input="doSearch"
            />
            <svg class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
            </svg>
            <button
              v-if="keyword"
              type="button"
              class="absolute right-3 top-1/2 inline-flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full text-stone-400 transition hover:bg-stone-100 hover:text-stone-700"
              @click="keyword = ''; fetchNotes()"
            >
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div class="mt-4 grid gap-2 text-sm text-stone-500 sm:grid-cols-2">
            <div class="rounded-2xl bg-stone-50 px-4 py-3">可搜索旅行主题、作者昵称和内容关键词</div>
            <div class="rounded-2xl bg-stone-50 px-4 py-3">点击卡片可继续查看详情、点赞、收藏、评论和编辑</div>
          </div>
        </div>
      </div>
    </section>

    <div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="section-heading text-[1.75rem]">游记列表</h2>
        <p class="section-subtitle mt-2">优先展示标题、摘要、作者、互动数与发布时间，降低浏览成本。</p>
      </div>
      <div class="flex flex-wrap gap-2 text-xs text-stone-500">
        <span class="chip">共 {{ notes.length }} 篇游记</span>
        <span class="chip">{{ keyword ? `关键词：${keyword}` : '当前浏览全部内容' }}</span>
      </div>
    </div>

    <LoadingSpinner v-if="loading" />
    <template v-else>
      <div v-if="notes.length" class="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
        <article
          v-for="item in notes"
          :key="item.id"
          class="surface-card surface-card-hover group cursor-pointer overflow-hidden rounded-[1.75rem]"
          @click="openDetail(item)"
        >
          <div class="relative h-48 overflow-hidden bg-stone-100">
            <img
              v-if="item.image"
              :src="item.image"
              :alt="item.title"
              class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
              @error="($event.target as HTMLImageElement).style.display='none'"
            />
            <div v-else class="flex h-full items-center justify-center text-stone-300">
              <svg class="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1">
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
              </svg>
            </div>
            <div class="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/35 to-transparent"></div>
            <div class="absolute left-4 top-4 rounded-full bg-white/90 px-3 py-1 text-xs font-medium text-stone-700 shadow-sm">
              {{ item.author || TEXT.ANONYMOUS }}
            </div>
          </div>
          <div class="p-5">
            <h3 class="line-clamp-1 text-lg font-semibold text-stone-900">{{ item.title }}</h3>
            <p class="mt-3 line-clamp-3 text-sm leading-6 text-stone-500">{{ item.excerpt || item.content || TEXT.NO_CONTENT }}</p>
            <div class="mt-4 flex flex-wrap gap-2 text-xs text-stone-500">
              <span class="rounded-full bg-stone-50 px-3 py-1.5">❤️ {{ item.likes || 0 }}</span>
              <span class="rounded-full bg-stone-50 px-3 py-1.5">💬 {{ item.comments || 0 }}</span>
              <span class="rounded-full bg-stone-50 px-3 py-1.5">{{ item.createTime ? new Date(item.createTime).toLocaleDateString() : '最近发布' }}</span>
            </div>
            <div class="mt-5 flex items-center justify-between text-sm">
              <span class="text-stone-400">点击查看全文和互动详情</span>
              <span class="font-medium text-stone-700 transition group-hover:text-stone-900">继续阅读</span>
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
        <h3 class="text-lg font-semibold text-stone-900">{{ keyword ? '没有找到相关游记' : '还没有游记内容' }}</h3>
        <p class="mt-2 text-sm text-stone-500">{{ keyword ? '试试更短的关键词，或者返回全部游记。' : '登录后即可发布第一篇游记，开始沉淀旅行故事。' }}</p>
        <button
          v-if="keyword"
          type="button"
          class="mt-5 inline-flex rounded-full bg-stone-900 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-stone-800"
          @click="keyword = ''; fetchNotes()"
        >重置搜索</button>
      </div>
    </template>
    <!-- 写游记弹窗 -->
    <Teleport to="body">
      <div
        v-if="showCreate"
        class="fixed inset-0 z-50 flex items-center justify-center px-4"
        @click.self="showCreate = false"
      >
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="showCreate = false" />
        <div class="surface-card relative w-full max-w-lg rounded-[1.75rem] border border-white/80 bg-white/95 p-6 shadow-[0_32px_90px_-36px_rgba(15,23,42,0.45)] backdrop-blur">
          <h2 class="text-lg font-semibold text-stone-900 mb-4">写游记</h2>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-stone-700 mb-1">标题</label>
              <input
                v-model="form.title"
                type="text"
                placeholder="给你的游记起个名字"
                class="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-stone-700 mb-1">简述</label>
              <input
                v-model="form.excerpt"
                type="text"
                placeholder="一句话概括游记内容"
                class="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-stone-700 mb-1">内容</label>
              <textarea
                v-model="form.content"
                rows="6"
                placeholder="写下你的旅途故事..."
                class="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 resize-none"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-stone-700 mb-1">图片链接（可选）</label>
              <input
                v-model="form.image"
                type="text"
                placeholder="输入图片 URL"
                class="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
              />
            </div>
          </div>
          <div class="flex items-center justify-end gap-3 mt-6">
            <button
              @click="showCreate = false"
              class="px-4 py-2 text-sm text-stone-600 hover:bg-stone-100 rounded-lg transition-colors"
            >取消</button>
            <button
              @click="submitNote"
              :disabled="!form.title || !form.content"
              class="px-4 py-2 text-sm bg-stone-900 text-white rounded-lg hover:bg-stone-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >发布</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 游记详情弹窗 -->
    <Teleport to="body">
      <div
        v-if="showDetail"
        class="fixed inset-0 z-50 flex items-center justify-center px-4"
        @click.self="showDetail = false"
      >
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="showDetail = false" />
        <div class="surface-card relative max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-[1.75rem] border border-white/80 bg-white/95 p-6 shadow-[0_32px_90px_-36px_rgba(15,23,42,0.45)] backdrop-blur">
          <button
            @click="showDetail = false"
            class="absolute top-4 right-4 w-8 h-8 flex items-center justify-center rounded-full hover:bg-stone-100 text-stone-400 transition-colors"
          >
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
            </svg>
          </button>

          <LoadingSpinner v-if="detailLoading" />
          <template v-else-if="detailNote">
            <!-- 标题 -->
            <h2 class="text-xl font-semibold text-stone-900 mb-2 pr-8">{{ detailNote.title }}</h2>

            <!-- 作者、时间 -->
            <div class="flex items-center gap-4 text-sm text-stone-500 mb-4">
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0" />
                </svg>
                {{ detailNote.author || TEXT.ANONYMOUS }}
              </span>
              <span>{{ detailNote.createTime ? new Date(detailNote.createTime).toLocaleString() : '' }}</span>
            </div>

            <!-- 图片 -->
            <img
              v-if="detailNote.image"
              :src="detailNote.image"
              :alt="detailNote.title"
              class="w-full rounded-xl mb-4 max-h-80 object-cover"
              @error="($event.target as HTMLImageElement).style.display='none'"
            />

            <!-- 内容 -->
            <div class="text-stone-700 leading-relaxed mb-6 whitespace-pre-wrap">{{ detailNote.content }}</div>

            <!-- 操作栏 -->
            <div class="flex items-center gap-4 mb-6 pt-4 border-t border-stone-100">
              <button
                @click="toggleLike"
                class="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-lg border transition-colors"
                :class="detailNote.isLiked ? 'border-red-200 bg-red-50 text-red-500' : 'border-stone-200 text-stone-500 hover:bg-stone-50'"
              >
                <span>{{ detailNote.isLiked ? '❤️' : '🤍' }}</span>
                <span>{{ detailNote.likes || 0 }}</span>
              </button>
              <button
                @click="toggleCollect"
                class="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-lg border transition-colors"
                :class="detailNote.isCollected ? 'border-amber-200 bg-amber-50 text-amber-600' : 'border-stone-200 text-stone-500 hover:bg-stone-50'"
              >
                <span>{{ detailNote.isCollected ? '⭐' : '☆' }}</span>
                <span>{{ detailNote.collections || 0 }}</span>
              </button>

              <template v-if="isOwnNote">
                <button
                  @click="openEdit"
                  class="ml-auto text-sm px-3 py-1.5 text-stone-600 hover:bg-stone-100 rounded-lg transition-colors"
                >编辑</button>
                <button
                  @click="handleDelete"
                  class="text-sm px-3 py-1.5 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                >删除</button>
              </template>
            </div>

            <!-- 评论区域 -->
            <div class="border-t border-stone-100 pt-4">
              <h3 class="font-medium text-stone-900 mb-3">评论</h3>

              <div v-if="comments.length" class="space-y-3 mb-4">
                <div
                  v-for="c in comments"
                  :key="c.id"
                  class="bg-stone-50 rounded-lg p-3"
                >
                  <div class="flex items-center gap-2 mb-1">
                    <span class="text-sm font-medium text-stone-700">{{ c.username || c.author || TEXT.ANONYMOUS }}</span>
                    <span class="text-xs text-stone-400">{{ c.createTime ? new Date(c.createTime).toLocaleString() : '' }}</span>
                  </div>
                  <p class="text-sm text-stone-600">{{ c.content }}</p>
                </div>
              </div>
              <p v-else class="text-sm text-stone-400 mb-4">暂无评论</p>

              <div v-if="isLoggedIn" class="flex gap-2">
                <input
                  v-model="commentText"
                  type="text"
                  placeholder="写下你的评论..."
                  class="flex-1 px-4 py-2 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
                  @keyup.enter="submitComment"
                />
                <button
                  @click="submitComment"
                  :disabled="!commentText.trim()"
                  class="px-4 py-2 text-sm bg-stone-900 text-white rounded-lg hover:bg-stone-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >发送</button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </Teleport>

    <!-- 编辑游记弹窗 -->
    <Teleport to="body">
      <div
        v-if="showEdit"
        class="fixed inset-0 z-50 flex items-center justify-center px-4"
        @click.self="showEdit = false"
      >
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="showEdit = false" />
        <div class="surface-card relative w-full max-w-lg rounded-[1.75rem] border border-white/80 bg-white/95 p-6 shadow-[0_32px_90px_-36px_rgba(15,23,42,0.45)] backdrop-blur">
          <h2 class="text-lg font-semibold text-stone-900 mb-4">编辑游记</h2>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-stone-700 mb-1">标题</label>
              <input
                v-model="editForm.title"
                type="text"
                placeholder="给你的游记起个名字"
                class="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-stone-700 mb-1">简述</label>
              <input
                v-model="editForm.excerpt"
                type="text"
                placeholder="一句话概括游记内容"
                class="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-stone-700 mb-1">内容</label>
              <textarea
                v-model="editForm.content"
                rows="6"
                placeholder="写下你的旅途故事..."
                class="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 resize-none"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-stone-700 mb-1">图片链接（可选）</label>
              <input
                v-model="editForm.image"
                type="text"
                placeholder="输入图片 URL"
                class="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
              />
            </div>
          </div>
          <div class="flex items-center justify-end gap-3 mt-6">
            <button
              @click="showEdit = false"
              class="px-4 py-2 text-sm text-stone-600 hover:bg-stone-100 rounded-lg transition-colors"
            >取消</button>
            <button
              @click="submitEdit"
              :disabled="!editForm.title || !editForm.content"
              class="px-4 py-2 text-sm bg-stone-900 text-white rounded-lg hover:bg-stone-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >保存</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { noteApi } from '../api/note.api'
import { userApi } from '../api/user.api'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, DEBOUNCE_DELAY, TEXT } from '../constants'

const loading = ref(true)
const notes = ref<any[]>([])
const showCreate = ref(false)
const keyword = ref('')
const isLoggedIn = ref(!!localStorage.getItem('token'))
const form = ref({ title: '', excerpt: '', content: '', image: '' })

// 详情弹窗
const showDetail = ref(false)
const detailNote = ref<any>(null)
const detailLoading = ref(false)

// 编辑弹窗
const showEdit = ref(false)
const editForm = ref({ title: '', excerpt: '', content: '', image: '' })

// 评论
const comments = ref<any[]>([])
const commentText = ref('')

// 当前用户
const currentUser = ref<any>(null)

const isOwnNote = computed(() => {
  if (!currentUser.value || !detailNote.value) return false
  return currentUser.value.username === detailNote.value.author
    || currentUser.value.id === detailNote.value.userId
})

async function fetchNotes() {
  loading.value = true
  try {
    notes.value = keyword.value
      ? await noteApi.searchNotes(keyword.value, DEFAULT_PAGE, DEFAULT_PAGE_SIZE) as any[]
      : await noteApi.getLatestNotes(DEFAULT_PAGE_SIZE) as any[]
  } catch { /* ignore */ }
  loading.value = false
}

async function submitNote() {
  if (!form.value.title || !form.value.content) return
  try {
    const user = await userApi.getCurrentUser() as any
    await noteApi.createNote({
      travelNote: {
        ...form.value,
        author: user?.username || '用户',
      },
    })
    showCreate.value = false
    form.value = { title: '', excerpt: '', content: '', image: '' }
    fetchNotes()
  } catch { /* ignore */ }
}

let searchTimer: ReturnType<typeof setTimeout>
function doSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(fetchNotes, DEBOUNCE_DELAY)
}

// 打开详情
async function openDetail(note: any) {
  showDetail.value = true
  detailNote.value = note
  detailLoading.value = true
  try {
    const detail = await noteApi.getNoteById(note.id) as any
    detailNote.value = detail
  } catch { /* ignore */ }
  detailLoading.value = false
  fetchComments(note.id)
}

// 点赞
async function toggleLike() {
  if (!detailNote.value || !currentUser.value) return
  try {
    const res = await noteApi.toggleLikeNote(detailNote.value.id, currentUser.value.id) as any
    detailNote.value.isLiked = res.liked
    detailNote.value.likes = res.likeCount
  } catch { /* ignore */ }
}

// 收藏
async function toggleCollect() {
  if (!detailNote.value) return
  try {
    const res = await noteApi.toggleCollectNote(detailNote.value.id) as any
    detailNote.value.isCollected = res.collected
    if (detailNote.value.collections !== undefined) {
      detailNote.value.collections += res.collected ? 1 : -1
    }
  } catch { /* ignore */ }
}

// 删除
async function handleDelete() {
  if (!detailNote.value || !currentUser.value) return
  try {
    await noteApi.deleteNote(detailNote.value.id, currentUser.value.id)
    showDetail.value = false
    detailNote.value = null
    fetchNotes()
  } catch { /* ignore */ }
}

// 打开编辑
function openEdit() {
  if (!detailNote.value) return
  editForm.value = {
    title: detailNote.value.title || '',
    excerpt: detailNote.value.excerpt || '',
    content: detailNote.value.content || '',
    image: detailNote.value.image || '',
  }
  showDetail.value = false
  showEdit.value = true
}

// 提交编辑
async function submitEdit() {
  if (!editForm.value.title || !editForm.value.content || !detailNote.value) return
  try {
    await noteApi.updateNote(detailNote.value.id, {
      travelNote: editForm.value,
    })
    showEdit.value = false
    fetchNotes()
  } catch { /* ignore */ }
}

// 获取评论
async function fetchComments(noteId: number) {
  try {
    comments.value = await noteApi.getComments(noteId) as any[]
  } catch { /* ignore */ }
}

// 提交评论
async function submitComment() {
  if (!commentText.value.trim() || !detailNote.value) return
  try {
    await noteApi.addComment(detailNote.value.id, commentText.value.trim())
    commentText.value = ''
    fetchComments(detailNote.value.id)
  } catch { /* ignore */ }
}

async function fetchCurrentUser() {
  if (!isLoggedIn.value) return
  try {
    currentUser.value = await userApi.getCurrentUser() as any
  } catch { /* ignore */ }
}

onMounted(() => {
  fetchNotes()
  fetchCurrentUser()
})
</script>