<template>
  <div class="app-container pb-16 pt-4 md:pt-6">
    <section class="surface-card mb-8 overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(255,251,235,0.92)_45%,rgba(240,249,255,0.9))] px-6 py-8 sm:px-8 sm:py-9">
      <div class="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
        <div>
          <div class="mb-4 flex flex-wrap gap-2">
            <span class="chip">分享码生成</span>
            <span class="chip">校验与统计</span>
            <span class="chip">热门与批量管理</span>
          </div>
          <h1 class="text-3xl font-semibold tracking-tight text-stone-900 md:text-4xl">把分享管理整理成更清晰的运营工作台</h1>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-stone-600 md:text-base">
            统一分享码生成、校验、列表、热门、统计和批量取消的展示方式，减少工具页的理解负担。
          </p>
          <div class="mt-6 grid gap-3 sm:grid-cols-3">
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">我的分享</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ shareList.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">热门分享</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ popularList.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-4">
              <div class="text-xs text-stone-500">批量取消</div>
              <div class="mt-2 text-2xl font-semibold text-stone-900">{{ batchIds ? batchIds.split(',').filter(Boolean).length : 0 }}</div>
            </div>
          </div>
        </div>

        <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
          <div class="mb-4">
            <div class="text-sm font-medium text-stone-500">使用说明</div>
            <div class="mt-1 text-xl font-semibold text-stone-900">先生成，再校验，再查看统计</div>
          </div>
          <div class="grid gap-2 text-sm text-stone-500 sm:grid-cols-2">
            <div class="rounded-2xl bg-stone-50 px-4 py-3">生成分享码后可继续进入列表与热门榜单</div>
            <div class="rounded-2xl bg-stone-50 px-4 py-3">支持按用户查询分享记录并进行批量取消</div>
          </div>
        </div>
      </div>
    </section>

    <div class="grid gap-6 xl:grid-cols-2">
      <section class="surface-card rounded-[1.75rem] p-6">
        <h2 class="mb-4 text-lg font-semibold text-stone-900">生成分享码</h2>
        <div class="grid gap-3 sm:grid-cols-[1fr_1fr_auto]">
          <input v-model="genForm.itemId" type="text" placeholder="内容 ID" class="rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10" />
          <input v-model="genForm.itemType" type="text" placeholder="内容类型（默认 route）" class="rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10" />
          <button @click="generateShare" class="rounded-2xl bg-stone-900 px-5 py-3 text-sm font-medium text-white transition hover:bg-stone-800">生成</button>
        </div>
        <div v-if="genResult" class="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">
          <pre class="whitespace-pre-wrap text-xs">{{ JSON.stringify(genResult, null, 2) }}</pre>
        </div>
      </section>

      <section class="surface-card rounded-[1.75rem] p-6">
        <h2 class="mb-4 text-lg font-semibold text-stone-900">校验分享码</h2>
        <div class="flex flex-col gap-3 sm:flex-row">
          <input v-model="validateCode" type="text" placeholder="输入分享码" class="flex-1 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10" />
          <button @click="validateShare" class="rounded-2xl bg-stone-900 px-5 py-3 text-sm font-medium text-white transition hover:bg-stone-800">校验</button>
        </div>
        <div v-if="validateResult" class="mt-4 rounded-2xl border border-stone-200 bg-stone-50 p-4">
          <pre class="whitespace-pre-wrap text-xs text-stone-600">{{ JSON.stringify(validateResult, null, 2) }}</pre>
        </div>
      </section>

      <section class="surface-card rounded-[1.75rem] p-6 xl:col-span-2">
        <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 class="text-lg font-semibold text-stone-900">我的分享记录</h2>
            <p class="mt-1 text-sm text-stone-500">按用户查询分享记录，并可逐条取消。</p>
          </div>
          <div class="flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
            <input v-model="listUserId" type="text" placeholder="用户 ID" class="rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10" />
            <button @click="fetchUserShares" class="rounded-2xl bg-stone-900 px-5 py-3 text-sm font-medium text-white transition hover:bg-stone-800">查询</button>
          </div>
        </div>
        <div v-if="shareList.length" class="space-y-3">
          <div
            v-for="item in shareList"
            :key="item.id"
            class="flex items-center justify-between gap-3 rounded-2xl bg-stone-50 px-4 py-4"
          >
            <div class="min-w-0 flex-1">
              <p class="text-sm font-medium text-stone-900">{{ item.code || item.shareCode }}</p>
              <p class="mt-1 text-xs text-stone-400">ID: {{ item.id }} | 类型: {{ item.itemType || '--' }}</p>
            </div>
            <button @click="cancelShare(item.id)" class="rounded-full border border-stone-200 px-3 py-1.5 text-xs font-medium text-stone-500 transition hover:border-red-200 hover:bg-red-50 hover:text-red-500">取消</button>
          </div>
        </div>
        <p v-else-if="listUserId && !shareList.length" class="text-sm text-stone-400">暂无分享记录</p>
      </section>

      <section class="surface-card rounded-[1.75rem] p-6">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-semibold text-stone-900">热门分享</h2>
            <p class="mt-1 text-sm text-stone-500">查看当前访问量较高的分享码。</p>
          </div>
          <button @click="fetchPopular" class="rounded-2xl bg-stone-900 px-5 py-3 text-sm font-medium text-white transition hover:bg-stone-800">获取热门</button>
        </div>
        <div v-if="popularList.length" class="space-y-3">
          <div
            v-for="item in popularList"
            :key="item.id"
            class="rounded-2xl bg-stone-50 px-4 py-4"
          >
            <p class="text-sm font-medium text-stone-900">{{ item.code || item.shareCode }}</p>
            <p class="mt-1 text-xs text-stone-400">访问: {{ item.visitCount || 0 }} | ID: {{ item.id }}</p>
          </div>
        </div>
        <p v-else class="text-sm text-stone-400">点击按钮获取热门分享</p>
      </section>

      <section class="surface-card rounded-[1.75rem] p-6">
        <h2 class="mb-4 text-lg font-semibold text-stone-900">分享统计</h2>
        <div class="flex flex-col gap-3 sm:flex-row">
          <input v-model="statsId" type="text" placeholder="分享 ID" class="flex-1 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10" />
          <button @click="fetchStats" class="rounded-2xl bg-stone-900 px-5 py-3 text-sm font-medium text-white transition hover:bg-stone-800">查看统计</button>
        </div>
        <div v-if="statsResult" class="mt-4 rounded-2xl bg-stone-50 p-4">
          <pre class="whitespace-pre-wrap text-xs text-stone-600">{{ JSON.stringify(statsResult, null, 2) }}</pre>
        </div>
      </section>

      <section class="surface-card rounded-[1.75rem] p-6 xl:col-span-2">
        <h2 class="mb-4 text-lg font-semibold text-stone-900">批量取消分享</h2>
        <div class="flex flex-col gap-3 sm:flex-row">
          <input v-model="batchIds" type="text" placeholder="ID 列表（逗号分隔）" class="flex-1 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10" />
          <button @click="batchCancel" class="rounded-2xl border border-red-200 px-5 py-3 text-sm font-medium text-red-500 transition hover:bg-red-50">批量取消</button>
        </div>
      </section>
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