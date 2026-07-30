<template>
  <div class="min-h-screen flex items-center justify-center px-6 py-16">
    <div class="w-full max-w-sm">
      <div class="text-center mb-8">
        <router-link to="/" class="inline-flex items-center gap-2 text-lg font-semibold text-stone-900 mb-2">
          <span class="w-8 h-8 bg-stone-900 rounded-lg flex items-center justify-center text-white text-sm font-bold">T</span>
          智慧旅游
        </router-link>
        <p class="text-sm text-stone-500">{{ isLogin ? '登录你的账号' : '创建你的账号' }}</p>
      </div>

      <div class="bg-white rounded-xl border border-stone-200 p-6">
        <div class="flex mb-6 border-b border-stone-200">
          <button
            class="flex-1 pb-3 text-sm font-medium transition-colors"
            :class="isLogin ? 'text-stone-900 border-b-2 border-stone-900' : 'text-stone-400'"
            @click="isLogin = true"
          >登录</button>
          <button
            class="flex-1 pb-3 text-sm font-medium transition-colors"
            :class="!isLogin ? 'text-stone-900 border-b-2 border-stone-900' : 'text-stone-400'"
            @click="isLogin = false"
          >注册</button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-4">
          <div v-if="!isLogin">
            <label class="block text-xs text-stone-500 mb-1">用户名</label>
            <input
              v-model="form.username"
              type="text"
              required
              class="w-full px-3 py-2 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            />
          </div>
          <div>
            <label class="block text-xs text-stone-500 mb-1">{{ isLogin ? '用户名 / 手机号' : '手机号' }}</label>
            <input
              v-model="form.phone"
              :type="isLogin ? 'text' : 'tel'"
              required
              class="w-full px-3 py-2 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            />
          </div>
          <div>
            <label class="block text-xs text-stone-500 mb-1">密码</label>
            <input
              v-model="form.password"
              type="password"
              required
              class="w-full px-3 py-2 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            />
          </div>
          <div v-if="!isLogin" class="flex gap-2">
            <input
              v-model="form.captcha"
              placeholder="验证码"
              class="flex-1 px-3 py-2 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            />
            <button type="button" class="px-3 py-2 text-xs text-stone-500 border border-stone-200 rounded-lg hover:bg-stone-50" @click="sendCaptcha">
              获取验证码
            </button>
          </div>
          <button
            type="submit"
            class="w-full py-2.5 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors"
            :disabled="submitting"
          >
            {{ submitting ? '处理中...' : (isLogin ? '登录' : '注册') }}
          </button>
        </form>

        <p v-if="error" class="mt-4 text-xs text-red-500 text-center">{{ error }}</p>
      </div>
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
      // Token is stored by api.ts interceptor
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