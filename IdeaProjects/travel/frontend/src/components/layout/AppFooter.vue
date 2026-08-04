<template>
  <footer class="relative z-10 mt-8 border-t border-stone-200/70 bg-white/70 backdrop-blur-sm">
    <div class="app-container py-10">
      <div class="grid gap-8 lg:grid-cols-[1.4fr_1fr_1fr]">
        <div>
          <div class="mb-4 flex items-center gap-3 text-stone-900">
            <span class="flex h-10 w-10 items-center justify-center rounded-2xl bg-stone-900 text-sm font-bold text-white">T</span>
            <div>
              <div class="text-base font-semibold">智慧旅游</div>
              <div class="text-sm text-stone-500">让路线规划、内容记录与实时出行建议更简单</div>
            </div>
          </div>
          <p class="max-w-md text-sm leading-6 text-stone-500">
            聚合景点推荐、路线规划、AI 助手、游记与实时信息，帮助用户从灵感到出发完成整段旅程。
          </p>
        </div>

        <div>
          <h3 class="mb-3 text-sm font-semibold uppercase tracking-[0.18em] text-stone-400">快速入口</h3>
          <div class="grid gap-2 text-sm text-stone-600">
            <router-link to="/attractions" class="transition hover:text-stone-900">景点探索</router-link>
            <router-link to="/routes" class="transition hover:text-stone-900">路线浏览</router-link>
            <router-link to="/notes" class="transition hover:text-stone-900">游记社区</router-link>
            <router-link to="/ai-chat" class="transition hover:text-stone-900">AI 助手</router-link>
          </div>
        </div>

        <div>
          <h3 class="mb-3 text-sm font-semibold uppercase tracking-[0.18em] text-stone-400">平台说明</h3>
          <div class="grid gap-2 text-sm text-stone-600">
            <router-link to="/feedback" class="transition hover:text-stone-900">意见反馈</router-link>
            <router-link to="/realtime" class="transition hover:text-stone-900">实时状态</router-link>
            <router-link to="/share" class="transition hover:text-stone-900">路线分享</router-link>
            <router-link to="/files" class="transition hover:text-stone-900">文件中心</router-link>
          </div>
        </div>
      </div>

      <div class="mt-8 flex flex-col gap-3 border-t border-stone-200/70 pt-6 text-sm text-stone-400 md:flex-row md:items-center md:justify-between">
        <p>© {{ currentYear }} 智慧旅游平台 · Travel smarter, plan calmer.</p>
        <p>界面已优化为更好的导航密度、移动端可用性与首页信息层次。</p>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
const currentYear = new Date().getFullYear()
</script>
