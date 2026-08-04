<template>
  <div class="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(245,158,11,0.14),_transparent_26%),linear-gradient(180deg,#fafaf9_0%,#f5f5f4_100%)] px-4 py-10 sm:px-6 lg:px-8">
    <div class="mx-auto grid min-h-[calc(100vh-5rem)] w-full max-w-6xl gap-8 lg:grid-cols-[1.05fr_0.95fr] lg:items-center">
      <section class="surface-card overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(255,251,235,0.94)_45%,rgba(239,246,255,0.9))] px-6 py-8 sm:px-8 sm:py-10">
        <router-link to="/" class="inline-flex items-center gap-3 text-stone-900">
          <span class="flex h-10 w-10 items-center justify-center rounded-2xl bg-stone-900 text-sm font-bold text-white shadow-lg shadow-stone-900/15">T</span>
          <span class="text-lg font-semibold">智慧旅游</span>
        </router-link>

        <div class="mt-6 flex flex-wrap gap-2">
          <span class="chip">旅行内容与路线规划</span>
          <span class="chip">AI 助手与实时状态</span>
          <span class="chip">一站式旅游平台</span>
        </div>

        <h1 class="mt-6 max-w-2xl text-4xl font-semibold tracking-tight text-stone-900 md:text-5xl">
          {{ isLogin ? '登录后继续你的旅行计划' : '注册后开始沉淀你的旅行资产' }}
        </h1>
        <p class="mt-4 max-w-2xl text-sm leading-7 text-stone-600 md:text-base">
          统一管理路线、游记、收藏、通知和 AI 助手入口，让从灵感到出发的整段体验保持连续。
        </p>

        <div class="mt-8 grid gap-3 sm:grid-cols-3">
          <div class="surface-card rounded-2xl px-4 py-4">
            <div class="text-xs text-stone-500">旅行路线</div>
            <div class="mt-2 text-xl font-semibold text-stone-900">智能规划</div>
            <div class="mt-1 text-xs text-stone-400">生成、优化并分享路线</div>
          </div>
          <div class="surface-card rounded-2xl px-4 py-4">
            <div class="text-xs text-stone-500">旅行内容</div>
            <div class="mt-2 text-xl font-semibold text-stone-900">游记沉淀</div>
            <div class="mt-1 text-xs text-stone-400">记录经验并积累互动</div>
          </div>
          <div class="surface-card rounded-2xl px-4 py-4">
            <div class="text-xs text-stone-500">旅行服务</div>
            <div class="mt-2 text-xl font-semibold text-stone-900">实时协同</div>
            <div class="mt-1 text-xs text-stone-400">查看状态、通知与文件</div>
          </div>
        </div>
      </section>

      <section class="surface-card rounded-[2rem] p-6 sm:p-8">
        <div class="mb-6 text-center lg:text-left">
          <div class="text-sm font-medium text-stone-500">账户入口</div>
          <h2 class="mt-2 text-2xl font-semibold text-stone-900">{{ isLogin ? '欢迎回来' : '创建你的账号' }}</h2>
          <p class="mt-2 text-sm text-stone-500">{{ isLogin ? '登录后继续查看路线、游记与通知。' : '注册后即可使用平台的完整旅游功能。' }}</p>
        </div>

        <div class="mb-6 rounded-full border border-stone-200/80 bg-stone-50/90 p-1.5">
          <div class="grid grid-cols-2 gap-1">
            <button
              class="rounded-full px-4 py-2.5 text-sm font-medium transition-all"
              :class="isLogin ? 'bg-stone-900 text-white shadow-sm' : 'text-stone-400'"
              @click="isLogin = true"
            >登录</button>
            <button
              class="rounded-full px-4 py-2.5 text-sm font-medium transition-all"
              :class="!isLogin ? 'bg-stone-900 text-white shadow-sm' : 'text-stone-400'"
              @click="isLogin = false"
            >注册</button>
          </div>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-4">
          <div v-if="!isLogin">
            <label class="mb-1.5 block text-xs font-medium text-stone-500">用户名</label>
            <input
              v-model="form.username"
              type="text"
              required
              placeholder="输入用户名"
              class="w-full rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
            />
          </div>
          <div>
            <label class="mb-1.5 block text-xs font-medium text-stone-500">{{ isLogin ? '用户名 / 手机号' : '手机号' }}</label>
            <input
              v-model="form.phone"
              :type="isLogin ? 'text' : 'tel'"
              required
              :placeholder="isLogin ? '输入用户名或手机号' : '输入手机号'"
              class="w-full rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
            />
          </div>
          <div>
            <label class="mb-1.5 block text-xs font-medium text-stone-500">密码</label>
            <input
              v-model="form.password"
              type="password"
              required
              placeholder="输入密码"
              class="w-full rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
            />
          </div>
          <div v-if="!isLogin" class="flex flex-col gap-3 sm:flex-row">
            <input
              v-model="form.captcha"
              placeholder="输入验证码"
              class="flex-1 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10"
            />
            <button type="button" class="rounded-2xl border border-stone-200 px-4 py-3 text-sm font-medium text-stone-600 transition hover:bg-stone-50" @click="sendCaptcha">
              获取验证码
            </button>
          </div>
          <button
            type="submit"
            class="w-full rounded-2xl bg-stone-900 py-3 text-sm font-medium text-white transition hover:bg-stone-800 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="submitting"
          >
            {{ submitting ? '处理中...' : (isLogin ? '登录并继续' : '注册并开始') }}
          </button>
        </form>

        <p v-if="error" class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-center text-xs text-red-500">{{ error }}</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '../api/user.api'

const router = useRouter()
const isLogin = ref(true)
const submitting = ref(false)
const error = ref('')

const form = reactive({
  username: '',
  phone: '',
  password: '',
  captcha: '',
})

async function handleSubmit() {
  error.value = ''
  submitting.value = true
  try {
    if (isLogin.value) {
      const res = await userApi.login({ username: form.phone, password: form.password })
      localStorage.setItem('token', (res as any).token || '')
      router.push('/')
    } else {
      await userApi.register({
        username: form.username,
        phone: form.phone,
        password: form.password,
        captcha: form.captcha,
        agreement: true,
      })
      isLogin.value = true
      error.value = '注册成功，请登录'
    }
  } catch (e: any) {
    const msg = e.message || ''
    if (msg.includes('用户不存在') || msg.includes('USER_NOT_EXIST')) {
      error.value = '用户不存在，请先注册'
    } else if (msg.includes('账号或密码错误') || msg.includes('PASSWORD_ERROR')) {
      error.value = '账号或密码错误'
    } else if (msg.includes('404') || msg.includes('not found')) {
      error.value = '后端服务未启动，请先启动后端'
    } else if (msg.includes('Network Error') || msg.includes('timeout')) {
      error.value = '网络错误，请确认后端服务已启动（端口8082）'
    } else {
      error.value = msg || '操作失败'
    }
  }
  submitting.value = false
}

async function sendCaptcha() {
  if (!form.phone) return
  try {
    await userApi.sendCaptcha(form.phone)
    error.value = '验证码已发送'
  } catch (e: any) {
    error.value = e.message || '发送失败'
  }
}
</script>