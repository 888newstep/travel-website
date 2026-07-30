<template>
  <div class="max-w-6xl mx-auto px-6 py-12">
    <div class="flex items-center justify-between mb-8">
      <h1 class="text-2xl font-semibold text-stone-900">游记</h1>
      <div class="flex items-center gap-3">
        <div class="relative">
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索游记..."
            class="w-56 pl-10 pr-4 py-2 bg-white border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            @input="doSearch"
          />
          <svg class="w-4 h-4 text-stone-400 absolute left-3 top-1/2 -translate-y-1/2" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
          </svg>
        </div>
        <button
          v-if="isLoggedIn"
          class="text-sm px-4 py-2 bg-stone-900 text-white rounded-lg hover:bg-stone-800 transition-colors"
          @click="showCreate = true"
        >写游记</button>
      </div>
    </div>

    <LoadingSpinner v-if="loading" />
    <template v-else>
      <div v-if="notes.length" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          v-for="item in notes"
          :key="item.id"
          class="bg-white rounded-xl border border-stone-200 overflow-hidden hover:shadow-md transition-shadow group cursor-pointer"
          @click="openDetail(item)"
        >
          <div class="h-40 bg-stone-100 overflow-hidden">
            <img
              v-if="item.image"
              :src="item.image"
              :alt="item.title"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
              @error="($event.target as HTMLImageElement).style.display='none'"
            />
            <div v-else class="h-full flex items-center justify-center text-stone-300">
              <svg class="w-12 h-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1">
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
              </svg>
            </div>
          </div>
          <div class="p-4">
            <h3 class="font-medium text-stone-900 mb-1 truncate">{{ item.title }}</h3>
            <p class="text-sm text-stone-500 line-clamp-2 mb-3">{{ item.excerpt || item.content || TEXT.NO_CONTENT }}</p>
            <div class="flex items-center gap-3 text-xs text-stone-400">
              <span class="flex items-center gap-1">
                <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0" />
                </svg>
                {{ item.author || TEXT.ANONYMOUS }}
              </span>
              <span>❤️ {{ item.likes || 0 }}</span>
              <span>💬 {{ item.comments || 0 }}</span>
              <span class="ml-auto">{{ item.createTime ? new Date(item.createTime).toLocaleDateString() : '' }}</span>
            </div>
          </div>
        </div>
      </div>
      <p v-else class="text-sm text-stone-400 text-center py-16">
        {{ keyword ? '未找到相关游记' : '暂无游记，登录后可以发布第一篇游记' }}
      </p>
    </template>

    <!-- 写游记弹窗 -->
    <Teleport to="body">
      <div
        v-if="showCreate"
        class="fixed inset-0 z-50 flex items-center justify-center px-4"
        @click.self="showCreate = false"
      >
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="showCreate = false" />
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg p-6">
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
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6">
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
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg p-6">
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