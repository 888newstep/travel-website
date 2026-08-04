<template>
  <div class="app-container pb-16 pt-4 md:pt-6">
    <section class="surface-card mb-8 overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(255,251,235,0.92)_45%,rgba(240,249,255,0.9))] px-6 py-8 sm:px-8 sm:py-9">
      <div class="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
        <div>
          <div class="mb-4 flex flex-wrap gap-2">
            <span class="chip">问题与建议收集</span>
            <span class="chip">历史反馈回看</span>
            <span class="chip">处理状态跟踪</span>
          </div>
          <h1 class="text-3xl font-semibold tracking-tight text-stone-900 md:text-4xl">把反馈页整理成更友好的沟通入口</h1>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-stone-600 md:text-base">
            统一反馈提交、状态查看和历史回看区块，让用户更容易表达问题，也更容易理解处理进度。
          </p>
          <div class="mt-6 grid gap-3 sm:grid-cols-3">
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">反馈类型</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ feedbackTypes.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">历史记录</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ history.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">当前输入</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ form.content.length }}</div>
            </div>
          </div>
        </div>

        <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
          <div class="mb-4">
            <div class="text-sm font-medium text-stone-500">填写建议</div>
            <div class="mt-1 text-xl font-semibold text-stone-900">尽量描述场景、问题和期望结果</div>
          </div>
          <div class="grid gap-2 text-sm text-stone-500 sm:grid-cols-2">
            <div class="rounded-2xl bg-stone-50 px-4 py-3">选择反馈类型，方便平台更快分流处理</div>
            <div class="rounded-2xl bg-stone-50 px-4 py-3">可以补充联系方式，便于后续沟通与回访</div>
          </div>
        </div>
      </div>
    </section>

    <div class="grid grid-cols-1 gap-8 lg:grid-cols-5">
      <section class="lg:col-span-3 surface-card rounded-[1.75rem] p-6">
        <h2 class="mb-6 text-lg font-semibold text-stone-900">提交反馈</h2>
        <div class="space-y-5">
          <div>
            <label class="mb-2 block text-sm font-medium text-stone-700">反馈类型</label>
            <div class="grid grid-cols-2 gap-2 sm:grid-cols-4">
              <button
                v-for="t in feedbackTypes"
                :key="t.value"
                @click="form.type = t.value"
                :class="[
                  'rounded-xl border px-4 py-2.5 text-sm font-medium transition-all',
                  form.type === t.value
                    ? 'border-amber-300 bg-amber-50 text-amber-700'
                    : 'border-stone-200 bg-white text-stone-600 hover:border-stone-300'
                ]"
              >
                {{ t.label }}
              </button>
            </div>
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-stone-700">反馈内容</label>
            <textarea
              v-model="form.content"
              rows="6"
              placeholder="请详细描述你的问题或建议..."
              class="w-full resize-none rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
            ></textarea>
            <p class="mt-1 text-right text-xs text-stone-400">{{ form.content.length }}/{{ FEEDBACK_MAX_LENGTH }}</p>
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-stone-700">联系方式 <span class="font-normal text-stone-400">（选填）</span></label>
            <input
              v-model="form.contactInfo"
              type="text"
              placeholder="手机号、邮箱或微信号"
              class="w-full rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
            />
          </div>

          <button
            @click="submitFeedback"
            :disabled="submitting || !form.type || !form.content.trim()"
            class="w-full rounded-2xl bg-amber-500 py-3 font-medium text-white transition-all hover:bg-amber-600 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {{ submitting ? '提交中...' : '提交反馈' }}
          </button>

          <div
            v-if="submitted"
            class="flex items-center gap-2 rounded-2xl border border-green-200 bg-green-50 p-3 text-sm text-green-700"
          >
            <svg class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
            </svg>
            感谢你的反馈！我们会尽快处理。
          </div>
        </div>
      </section>

      <section class="lg:col-span-2 surface-card rounded-[1.75rem] p-6">
        <h2 class="mb-6 text-lg font-semibold text-stone-900">历史反馈</h2>

        <div v-if="historyLoading" class="flex justify-center py-10">
          <LoadingSpinner />
        </div>

        <div v-else-if="history.length === 0" class="py-10 text-center">
          <svg class="mx-auto mb-3 h-12 w-12 text-stone-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m5.231 13.481L15 17.25m-4.5-15H5.625c-.621 0-1.125.504-1.125 1.125v16.5c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Zm3.75 11.625a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" />
          </svg>
          <p class="text-stone-400">暂无历史反馈</p>
        </div>

        <div v-else class="space-y-3">
          <article
            v-for="item in history"
            :key="item.id"
            class="rounded-2xl border border-stone-100 p-4 transition-colors hover:border-stone-200"
          >
            <div class="mb-2 flex items-center justify-between gap-3">
              <span class="rounded-full bg-stone-100 px-2.5 py-1 text-xs font-medium text-stone-600">{{ typeLabel(item.type) }}</span>
              <span :class="['text-xs font-medium', item.status === 'processed' ? 'text-green-600' : 'text-amber-600']">
                {{ item.status === 'processed' ? '已处理' : '待处理' }}
              </span>
            </div>
            <p class="line-clamp-2 text-sm text-stone-700">{{ item.content }}</p>
            <p v-if="item.replyContent" class="mt-2 border-l-2 border-stone-200 pl-2 text-xs text-stone-400">回复：{{ item.replyContent }}</p>
            <p class="mt-2 text-xs text-stone-400">{{ item.createTime }}</p>
          </article>
        </div>
      </section>
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
  return feedbackTypes.value.find((t) => t.value === type)?.label || type
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
    if (currentUserId.value) {
      history.value = await feedbackApi.getFeedbackList(currentUserId.value) as any[]
    }
  } catch { /* ignore */ }
  submitting.value = false
}

onMounted(async () => {
  try {
    feedbackTypes.value = await feedbackApi.getFeedbackTypes() as any[]
  } catch {
    feedbackTypes.value = DEFAULT_FEEDBACK_TYPES
  }
  try {
    const user = await userApi.getCurrentUser() as any
    currentUserId.value = user.id
    history.value = await feedbackApi.getFeedbackList(user.id) as any[]
  } catch { /* ignore */ }
  historyLoading.value = false
})
</script>