<template>
  <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-stone-900">意见反馈</h1>
      <p class="mt-2 text-stone-500">我们非常重视您的反馈，感谢您的支持</p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-5 gap-8">
      <!-- 提交反馈表单 -->
      <div class="lg:col-span-3">
        <div class="bg-white rounded-2xl border border-stone-200 p-6">
          <h2 class="text-lg font-semibold text-stone-900 mb-6">提交反馈</h2>

          <div class="space-y-5">
            <!-- 反馈类型 -->
            <div>
              <label class="block text-sm font-medium text-stone-700 mb-2">反馈类型</label>
              <div class="grid grid-cols-2 sm:grid-cols-4 gap-2">
                <button
                  v-for="t in feedbackTypes"
                  :key="t.value"
                  @click="form.type = t.value"
                  :class="[
                    'px-4 py-2.5 rounded-xl text-sm font-medium border transition-all',
                    form.type === t.value
                      ? 'bg-amber-50 border-amber-300 text-amber-700'
                      : 'bg-white border-stone-200 text-stone-600 hover:border-stone-300'
                  ]"
                >
                  {{ t.label }}
                </button>
              </div>
            </div>

            <!-- 反馈内容 -->
            <div>
              <label class="block text-sm font-medium text-stone-700 mb-2">反馈内容</label>
              <textarea
                v-model="form.content"
                rows="5"
                placeholder="请详细描述您的问题或建议..."
                class="w-full px-4 py-3 bg-white border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 transition-all resize-none"
              ></textarea>
              <p class="mt-1 text-xs text-stone-400 text-right">{{ form.content.length }}/{{ FEEDBACK_MAX_LENGTH }}</p>
            </div>

            <!-- 联系方式 -->
            <div>
              <label class="block text-sm font-medium text-stone-700 mb-2">联系方式 <span class="text-stone-400 font-normal">（选填）</span></label>
              <input
                v-model="form.contactInfo"
                type="text"
                placeholder="手机号、邮箱或微信号"
                class="w-full px-4 py-2.5 bg-white border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 transition-all"
              />
            </div>

            <!-- 提交按钮 -->
            <button
              @click="submitFeedback"
              :disabled="submitting || !form.type || !form.content.trim()"
              class="w-full py-3 bg-amber-500 text-white rounded-xl font-medium hover:bg-amber-600 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
            >
              {{ submitting ? '提交中...' : '提交反馈' }}
            </button>

            <!-- 提交成功提示 -->
            <div
              v-if="submitted"
              class="flex items-center gap-2 p-3 bg-green-50 border border-green-200 rounded-xl text-sm text-green-700"
            >
              <svg class="w-5 h-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
              </svg>
              感谢您的反馈！我们会尽快处理。
            </div>
          </div>
        </div>
      </div>

      <!-- 历史反馈 -->
      <div class="lg:col-span-2">
        <div class="bg-white rounded-2xl border border-stone-200 p-6">
          <h2 class="text-lg font-semibold text-stone-900 mb-6">历史反馈</h2>

          <div v-if="historyLoading" class="flex justify-center py-10">
            <LoadingSpinner />
          </div>

          <div v-else-if="history.length === 0" class="text-center py-10">
            <svg class="w-12 h-12 mx-auto text-stone-300 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m5.231 13.481L15 17.25m-4.5-15H5.625c-.621 0-1.125.504-1.125 1.125v16.5c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Zm3.75 11.625a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" />
            </svg>
            <p class="text-stone-400">暂无历史反馈</p>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="item in history"
              :key="item.id"
              class="p-3 rounded-xl border border-stone-100 hover:border-stone-200 transition-colors"
            >
              <div class="flex items-center justify-between mb-1.5">
                <span class="px-2 py-0.5 bg-stone-100 text-stone-600 rounded text-xs font-medium">{{ typeLabel(item.type) }}</span>
                <span :class="[
                  'text-xs font-medium',
                  item.status === 'processed' ? 'text-green-600' : 'text-amber-600'
                ]">
                  {{ item.status === 'processed' ? '已处理' : '待处理' }}
                </span>
              </div>
              <p class="text-sm text-stone-700 line-clamp-2">{{ item.content }}</p>
              <p v-if="item.replyContent" class="mt-1.5 text-xs text-stone-400 pl-2 border-l-2 border-stone-200">
                回复：{{ item.replyContent }}
              </p>
              <p class="mt-1 text-xs text-stone-400">{{ item.createTime }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { feedbackApi } from '../api/notification-feedback.api'
import { userApi } from '../api/user.api'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import { FEEDBACK_MAX_LENGTH } from '../constants'

const DEFAULT_FEEDBACK_TYPES = [
  { value: 'suggestion', label: '建议' },
  { value: 'bug', label: '问题反馈' },
  { value: 'complaint', label: '投诉' },
  { value: 'other', label: '其他' },
]

const feedbackTypes = ref<{ value: string; label: string }[]>([])

const form = ref({ type: '', content: '', contactInfo: '' })
const submitting = ref(false)
const submitted = ref(false)

const history = ref<any[]>([])
const historyLoading = ref(true)
const currentUserId = ref<number | null>(null)

function typeLabel(type: string) {
  return feedbackTypes.find(t => t.value === type)?.label || type
}

async function submitFeedback() {
  if (!form.value.type || !form.value.content.trim() || submitting.value) return
  submitting.value = true
  submitted.value = false
  try {
    await feedbackApi.submitFeedback({
      type: form.value.type,
      content: form.value.content.trim(),
      contactInfo: form.value.contactInfo || undefined,
    })
    submitted.value = true
    form.value = { type: '', content: '', contactInfo: '' }
    // 刷新历史
    if (currentUserId.value) {
      history.value = await feedbackApi.getFeedbackList(currentUserId.value) as any[]
    }
  } catch { /* ignore */ }
  submitting.value = false
}

onMounted(async () => {
  // 获取反馈类型
  try {
    feedbackTypes.value = await feedbackApi.getFeedbackTypes() as any[]
  } catch {
    feedbackTypes.value = DEFAULT_FEEDBACK_TYPES
  }
  // 获取用户和历史
  try {
    const user = await userApi.getCurrentUser() as any
    currentUserId.value = user.id
    history.value = await feedbackApi.getFeedbackList(user.id) as any[]
  } catch { /* ignore */ }
  historyLoading.value = false
})
</script>