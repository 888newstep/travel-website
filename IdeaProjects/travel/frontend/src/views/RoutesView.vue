<template>
  <div class="app-container pb-16 pt-4 md:pt-6">
    <section class="surface-card mb-8 overflow-hidden rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.96),rgba(255,247,237,0.94)_45%,rgba(239,246,255,0.9))] px-6 py-8 sm:px-8 sm:py-9">
      <div class="grid gap-8 xl:grid-cols-[1.15fr_0.85fr] xl:items-center">
        <div>
          <div class="mb-4 flex flex-wrap gap-2">
            <span class="chip">路线浏览与筛选</span>
            <span class="chip">路线对比</span>
            <span class="chip">AI 智能规划</span>
          </div>
          <h1 class="text-3xl font-semibold tracking-tight text-stone-900 md:text-4xl">路线规划更聚焦，筛选与比较更顺手</h1>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-stone-600 md:text-base">
            把全部、热门、智能、季节和主题路线放进统一入口，同时保留对比、分享、收藏和 AI 优化能力。
          </p>
          <div class="mt-6 flex flex-wrap gap-3">
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">当前路线</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ routes.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">已选对比</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ selectedForCompare.length }}</div>
            </div>
            <div class="surface-card rounded-2xl px-4 py-3">
              <div class="text-xs text-stone-500">当前视图</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">{{ routeTabs.find(tab => tab.key === activeRouteTab)?.label || '路线' }}</div>
            </div>
          </div>
        </div>

        <div class="surface-card rounded-[1.75rem] p-5 sm:p-6">
          <div class="mb-5 flex items-center justify-between gap-3">
            <div>
              <div class="text-sm font-medium text-stone-500">快捷操作</div>
              <div class="mt-1 text-xl font-semibold text-stone-900">从浏览直接进入决策</div>
            </div>
            <span class="rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-600">AI Ready</span>
          </div>
          <div class="grid gap-3 sm:grid-cols-2">
            <button
              @click="showCompare = true"
              :disabled="selectedForCompare.length < 2"
              class="rounded-2xl border border-stone-200 bg-white px-4 py-4 text-left text-sm transition hover:border-stone-300 hover:bg-stone-50 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <div class="font-medium text-stone-900">路线对比</div>
              <div class="mt-1 text-stone-500">至少选择 2 条路线后，比较时长、难度和热度</div>
            </button>
            <router-link
              to="/ai-chat"
              class="rounded-2xl bg-stone-900 px-4 py-4 text-left text-sm text-white transition hover:bg-stone-800"
            >
              <div class="font-medium">AI 智能规划</div>
              <div class="mt-1 text-stone-300">快速生成、评估或优化路线思路</div>
            </router-link>
          </div>
          <div class="mt-4 grid gap-2 text-sm text-stone-500 sm:grid-cols-2">
            <div class="rounded-2xl bg-stone-50 px-4 py-3">支持公开 / 私密路线状态识别</div>
            <div class="rounded-2xl bg-stone-50 px-4 py-3">支持季节与主题路线的上下文筛选</div>
          </div>
        </div>
      </div>
    </section>

    <section class="surface-card mb-6 rounded-[1.75rem] p-4 sm:p-5">
      <div class="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
        <div class="min-w-0">
          <h2 class="section-heading text-[1.75rem]">路线筛选</h2>
          <p class="section-subtitle mt-2">切换分类后即时拉取对应路线，保留当前内容密度但提升可读性。</p>
        </div>
        <div class="flex flex-wrap gap-2 text-xs text-stone-500">
          <span class="chip">{{ routes.length }} 条结果</span>
          <span class="chip">{{ selectedForCompare.length }} 条待对比</span>
          <span v-if="activeRouteTab === 'seasonal'" class="chip">季节：{{ seasonOptions.find(s => s.value === seasonFilter)?.label || seasonFilter }}</span>
          <span v-if="activeRouteTab === 'theme'" class="chip">主题：{{ themeOptions.find(t => t.value === themeFilter)?.label || themeFilter }}</span>
        </div>
      </div>

      <div class="mt-5 overflow-x-auto pb-1">
        <div class="inline-flex min-w-full gap-2 rounded-full border border-stone-200/80 bg-stone-50/90 p-2 sm:min-w-0">
          <button
            v-for="tab in routeTabs"
            :key="tab.key"
            class="shrink-0 rounded-full px-4 py-2.5 text-sm font-medium transition-all"
            :class="activeRouteTab === tab.key ? 'bg-stone-900 text-white shadow-sm' : 'text-stone-500 hover:bg-white hover:text-stone-900'"
            @click="switchRouteTab(tab.key)"
          >{{ tab.label }}</button>
        </div>
      </div>

      <div v-if="activeRouteTab === 'seasonal' || activeRouteTab === 'theme'" class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div v-if="activeRouteTab === 'seasonal'" class="flex items-center gap-3">
          <span class="text-sm font-medium text-stone-600">选择季节</span>
          <select v-model="seasonFilter" @change="fetchSeasonalRoutes" class="rounded-2xl border border-stone-200 bg-white px-4 py-2.5 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10">
            <option v-for="s in seasonOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>
        <div v-if="activeRouteTab === 'theme'" class="flex items-center gap-3">
          <span class="text-sm font-medium text-stone-600">选择主题</span>
          <select v-model="themeFilter" @change="fetchThemeRoutes" class="rounded-2xl border border-stone-200 bg-white px-4 py-2.5 text-sm text-stone-700 outline-none transition focus:border-amber-400 focus:ring-4 focus:ring-amber-500/10">
            <option v-for="t in themeOptions" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
        </div>
      </div>
    </section>

    <LoadingSpinner v-if="loading" />

    <template v-else>
      <div v-if="routes.length" class="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <article
          v-for="item in routes"
          :key="item.id"
          class="surface-card surface-card-hover group cursor-pointer overflow-hidden rounded-[1.75rem]"
          @click="openDetail(item)"
        >
          <div class="relative h-48 overflow-hidden bg-stone-100">
            <img
              v-if="item.coverImage"
              :src="item.coverImage"
              :alt="item.title"
              class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
              @error="($event.target as HTMLImageElement).style.display='none'"
            />
            <div v-else class="flex h-full items-center justify-center text-stone-300">
              <svg class="h-14 w-14" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 6.75V15m6-6v8.25m.503 3.498 4.875-2.437c.381-.19.622-.58.622-1.006V4.82c0-.836-.88-1.38-1.628-1.006l-3.869 1.934c-.317.159-.69.159-1.006 0L9.503 3.252a1.125 1.125 0 0 0-1.006 0L3.622 5.689C3.24 5.88 3 6.27 3 6.695V19.18c0 .836.88 1.38 1.628 1.006l3.869-1.934c.317-.159.69-.159 1.006 0l4.994 2.497c.317.158.69.158 1.006 0Z" />
              </svg>
            </div>
            <div class="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/35 to-transparent"></div>
            <div class="absolute left-4 top-4 flex flex-wrap gap-2">
              <span class="rounded-full bg-white/88 px-3 py-1 text-xs font-medium text-stone-700 shadow-sm">{{ item.difficulty || '轻松' }}</span>
              <span class="rounded-full bg-white/88 px-3 py-1 text-xs font-medium text-stone-700 shadow-sm">{{ item.durationDays ? `${item.durationDays} 天` : '灵活行程' }}</span>
            </div>
            <label class="absolute right-4 top-4 z-10" @click.stop>
              <span class="flex items-center gap-2 rounded-full bg-white/88 px-3 py-1.5 text-xs font-medium text-stone-700 shadow-sm">
                <input
                  type="checkbox"
                  :checked="selectedForCompare.includes(item.id)"
                  @change="toggleCompare(item.id)"
                  class="h-4 w-4 rounded border-stone-300 text-stone-900 focus:ring-stone-500"
                />
                对比
              </span>
            </label>
          </div>
          <div class="p-5">
            <div class="flex items-start justify-between gap-3">
              <h3 class="text-lg font-semibold text-stone-900">{{ item.title }}</h3>
              <span
                class="rounded-full px-2.5 py-1 text-xs font-medium"
                :class="item.isPublic ? 'bg-emerald-50 text-emerald-600' : 'bg-stone-100 text-stone-500'"
              >{{ item.isPublic ? '公开' : '私密' }}</span>
            </div>
            <p class="mt-3 line-clamp-2 text-sm leading-6 text-stone-500">{{ item.description || TEXT.NO_DESCRIPTION }}</p>
            <div class="mt-4 flex flex-wrap gap-2 text-xs text-stone-500">
              <span class="rounded-full bg-stone-50 px-3 py-1.5">👁 {{ item.viewCount || 0 }}</span>
              <span class="rounded-full bg-stone-50 px-3 py-1.5">❤️ {{ item.likeCount || 0 }}</span>
              <span class="rounded-full bg-stone-50 px-3 py-1.5">{{ item.createdAt ? new Date(item.createdAt).toLocaleDateString() : '最近更新' }}</span>
            </div>
            <div class="mt-5 flex items-center justify-between text-sm">
              <span class="text-stone-400">点击查看详情、评论、收藏和分享</span>
              <span class="font-medium text-stone-700 transition group-hover:text-stone-900">进入路线</span>
            </div>
          </div>
        </article>
      </div>
      <div v-else class="surface-card rounded-[1.75rem] px-6 py-14 text-center">
        <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-stone-100 text-stone-400">
          <svg class="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 6.75V15m6-6v8.25m.503 3.498 4.875-2.437c.381-.19.622-.58.622-1.006V4.82c0-.836-.88-1.38-1.628-1.006l-3.869 1.934c-.317.159-.69.159-1.006 0L9.503 3.252a1.125 1.125 0 0 0-1.006 0L3.622 5.689C3.24 5.88 3 6.27 3 6.695V19.18c0 .836.88 1.38 1.628 1.006l3.869-1.934c.317-.159.69-.159 1.006 0l4.994 2.497c.317.158.69.158 1.006 0Z" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-stone-900">当前分类暂无路线</h3>
        <p class="mt-2 text-sm text-stone-500">可以切换其他分类，或直接使用 AI 智能规划生成新路线。</p>
        <router-link
          to="/ai-chat"
          class="mt-5 inline-flex rounded-full bg-stone-900 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-stone-800"
        >去试试 AI 规划</router-link>
      </div>
    </template>
    <!-- 路线详情弹窗 -->
    <Teleport to="body">
      <div
        v-if="selectedRoute"
        class="fixed inset-0 z-50 flex items-start justify-center pt-16 pb-8 px-4"
        @click.self="closeDetail"
      >
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeDetail" />
        <div class="surface-card relative max-h-full w-full max-w-2xl overflow-y-auto rounded-[1.75rem] border border-white/80 bg-white/95 shadow-[0_32px_90px_-36px_rgba(15,23,42,0.45)] backdrop-blur">
          <!-- 弹窗头部 -->
          <div class="sticky top-0 z-10 flex items-center justify-between rounded-t-[1.75rem] border-b border-stone-100/80 bg-white/90 px-6 py-4 backdrop-blur">
            <h2 class="text-lg font-semibold text-stone-900 truncate max-w-md">{{ selectedRoute.title }}</h2>
            <div class="flex items-center gap-2">
              <!-- 收藏按钮 -->
              <button
                v-if="isLoggedIn"
                @click="toggleCollect"
                :class="[
                  'p-2 rounded-lg transition-colors',
                  isCollected ? 'text-red-500 bg-red-50 hover:bg-red-100' : 'text-stone-400 hover:text-red-500 hover:bg-stone-100'
                ]"
                :title="isCollected ? '取消收藏' : '收藏'"
              >
                <svg class="w-5 h-5" :fill="isCollected ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" />
                </svg>
              </button>
              <!-- 分享按钮 -->
              <button
                v-if="isLoggedIn"
                @click="shareRoute"
                class="p-2 text-stone-400 hover:text-amber-500 hover:bg-stone-100 rounded-lg transition-colors"
                title="分享"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M7.217 10.907a2.25 2.25 0 1 0 0 2.186m0-2.186c.18.324.283.696.283 1.093s-.103.77-.283 1.093m0-2.186 9.566-5.314m-9.566 7.5 9.566 5.314m0 0a2.25 2.25 0 1 0 3.935 2.186 2.25 2.25 0 0 0-3.935-2.186Zm0-12.814a2.25 2.25 0 1 0 3.933-2.185 2.25 2.25 0 0 0-3.933 2.185Z" />
                </svg>
              </button>
              <!-- 关闭按钮 -->
              <button @click="closeDetail" class="p-2 text-stone-400 hover:text-stone-600 hover:bg-stone-100 rounded-lg transition-colors">
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" /></svg>
              </button>
            </div>
          </div>

          <!-- 弹窗内容 -->
          <div class="p-6 space-y-6">
            <!-- 路线信息 -->
            <div>
              <p class="text-stone-600 text-sm leading-relaxed">{{ selectedRoute.description || TEXT.NO_DESCRIPTION }}</p>
              <div class="flex items-center gap-4 mt-3 text-sm text-stone-400">
                <span>👁 浏览 {{ selectedRoute.viewCount || 0 }}</span>
                <span>❤️ 点赞 {{ selectedRoute.likeCount || 0 }}</span>
                <span v-if="selectedRoute.createdAt">📅 {{ new Date(selectedRoute.createdAt).toLocaleDateString() }}</span>
              </div>
            </div>

            <!-- 操作按钮组 -->
            <div class="flex flex-wrap gap-2">
              <button @click="copyRoute" class="px-3 py-1.5 text-xs border border-stone-200 rounded-lg hover:bg-stone-50 transition-colors">复制路线</button>
              <button @click="toggleVisibility" class="px-3 py-1.5 text-xs border border-stone-200 rounded-lg hover:bg-stone-50 transition-colors">
                {{ selectedRoute.isPublic ? '设为私密' : '设为公开' }}
              </button>
              <button @click="optimizeRoute" class="px-3 py-1.5 text-xs bg-amber-50 text-amber-700 border border-amber-200 rounded-lg hover:bg-amber-100 transition-colors">优化路线</button>
              <button @click="evaluateRoute" class="px-3 py-1.5 text-xs bg-stone-50 text-stone-600 border border-stone-200 rounded-lg hover:bg-stone-100 transition-colors">评估路线</button>
            </div>

            <!-- 优化建议 -->
            <div v-if="optimizeSuggestions" class="p-4 bg-amber-50 border border-amber-200 rounded-xl">
              <p class="text-sm font-medium text-amber-800 mb-2">优化建议</p>
              <div class="text-xs text-amber-700 space-y-1">
                <p v-for="(s, i) in optimizeSuggestions" :key="i">{{ s }}</p>
              </div>
            </div>

            <!-- 评估结果 -->
            <div v-if="evaluateResult" class="p-4 bg-blue-50 border border-blue-200 rounded-xl">
              <p class="text-sm font-medium text-blue-800 mb-2">路线评估</p>
              <div class="text-xs text-blue-700 space-y-1">
                <p v-if="evaluateResult.score != null">综合评分：{{ evaluateResult.score }} / 10</p>
                <p v-if="evaluateResult.summary">{{ evaluateResult.summary }}</p>
              </div>
            </div>

            <!-- 实时调整 -->
            <div class="p-4 bg-stone-50 border border-stone-200 rounded-xl">
              <p class="text-sm font-medium text-stone-800 mb-3">实时调整</p>
              <div class="flex flex-wrap gap-2">
                <button @click="adjustRoute(1)" class="px-3 py-1.5 text-xs border border-red-200 text-red-600 rounded-lg hover:bg-red-50 transition-colors">避开拥堵</button>
                <button @click="adjustRoute(2)" class="px-3 py-1.5 text-xs border border-amber-200 text-amber-600 rounded-lg hover:bg-amber-50 transition-colors">缩短距离</button>
                <button @click="adjustRoute(3)" class="px-3 py-1.5 text-xs border border-blue-200 text-blue-600 rounded-lg hover:bg-blue-50 transition-colors">减少时间</button>
                <button @click="getAlternativeRoutes" class="px-3 py-1.5 text-xs border border-stone-200 text-stone-600 rounded-lg hover:bg-stone-100 transition-colors">备选路线</button>
              </div>
              <div v-if="adjustResult" class="mt-3 p-3 bg-white rounded-lg border border-stone-100 text-xs text-stone-600">
                <p v-if="adjustResult.adjustedTotalDistance">调整后距离: {{ adjustResult.adjustedTotalDistance }} km</p>
                <p v-if="adjustResult.adjustedEstimatedTime">调整后时间: {{ adjustResult.adjustedEstimatedTime }} 分钟</p>
                <p v-if="adjustResult.adjustmentReason" class="mt-1 text-stone-500">{{ adjustResult.adjustmentReason }}</p>
              </div>
              <div v-if="alternativeRoutes.length" class="mt-3 space-y-2">
                <p class="text-xs font-medium text-stone-700">备选路线:</p>
                <div v-for="(alt, i) in alternativeRoutes" :key="i" class="p-2 bg-white rounded border border-stone-100 text-xs text-stone-600">
                  <span class="font-medium">{{ alt.adjustmentReason || '路线 ' + (i + 1) }}</span>
                  <span v-if="alt.adjustedTotalDistance" class="ml-2">距离: {{ alt.adjustedTotalDistance }} km</span>
                  <span v-if="alt.adjustedEstimatedTime" class="ml-2">时间: {{ alt.adjustedEstimatedTime }} min</span>
                </div>
              </div>
            </div>

            <!-- 收藏分类 -->
            <div v-if="isLoggedIn" class="p-4 bg-stone-50 border border-stone-200 rounded-xl">
              <p class="text-sm font-medium text-stone-800 mb-3">收藏管理</p>
              <div class="flex items-center gap-3 mb-2">
                <button @click="toggleCollect" :class="['px-3 py-1.5 text-xs rounded-lg transition-colors', isCollected ? 'bg-red-50 text-red-600 border border-red-200' : 'bg-white text-stone-600 border border-stone-200 hover:bg-stone-50']">
                  {{ isCollected ? '取消收藏' : '收藏路线' }}
                </button>
                <select v-if="isCollected" v-model="collectionCategory" class="px-2 py-1.5 bg-white border border-stone-200 rounded text-xs">
                  <option value="">选择分类</option>
                  <option v-for="cat in collectionCategories" :key="cat" :value="cat">{{ cat }}</option>
                </select>
                <button v-if="isCollected && collectionCategory" @click="updateCollectionCategory" class="px-3 py-1.5 text-xs bg-stone-900 text-white rounded-lg hover:bg-stone-800 transition-colors">保存分类</button>
              </div>
              <div class="flex gap-2">
                <input v-if="isCollected" v-model="collectionNote" placeholder="添加备注..." class="flex-1 px-2 py-1.5 bg-white border border-stone-200 rounded text-xs focus:outline-none focus:ring-1 focus:ring-stone-300" />
                <button v-if="isCollected && collectionNote" @click="updateCollectionNote" class="px-3 py-1.5 text-xs bg-stone-900 text-white rounded-lg hover:bg-stone-800 transition-colors shrink-0">保存备注</button>
              </div>
            </div>

            <!-- 分享码展示 -->
            <div v-if="shareCode" class="p-3 bg-amber-50 border border-amber-200 rounded-xl">
              <p class="text-sm font-medium text-amber-800 mb-1">分享链接已生成</p>
              <div class="flex items-center gap-2">
                <input
                  :value="`${windowOrigin}/share/${shareCode}`"
                  readonly
                  class="flex-1 text-xs bg-white border border-amber-200 rounded-lg px-3 py-2 text-stone-600"
                  @click="copyShareLink"
                />
                <button @click="copyShareLink" class="px-3 py-2 bg-amber-500 text-white text-xs rounded-lg hover:bg-amber-600 transition-colors shrink-0">复制</button>
              </div>
            </div>

            <!-- 评论区 -->
            <div>
              <h3 class="text-base font-semibold text-stone-900 mb-4">评论 ({{ comments.length }})</h3>

              <!-- 添加评论 -->
              <div v-if="isLoggedIn" class="flex gap-3 mb-4">
                <input
                  v-model="newComment"
                  type="text"
                  placeholder="写下你的评论..."
                  class="flex-1 px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 transition-all"
                  @keydown.enter="addComment"
                />
                <button
                  @click="addComment"
                  :disabled="!newComment.trim()"
                  class="px-5 py-2.5 bg-amber-500 text-white text-sm rounded-xl font-medium hover:bg-amber-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >发送</button>
              </div>
              <p v-else class="text-xs text-stone-400 mb-4">
                <router-link to="/login" class="text-amber-600 hover:underline">登录</router-link> 后即可评论
              </p>

              <!-- 评论列表 -->
              <div v-if="comments.length" class="space-y-3">
                <div
                  v-for="c in comments"
                  :key="c.id"
                  class="p-3 bg-stone-50 rounded-xl"
                >
                  <div class="flex items-center justify-between mb-1">
                    <span class="text-sm font-medium text-stone-700">用户 #{{ c.userId }}</span>
                    <span class="text-xs text-stone-400">{{ c.createTime ? new Date(c.createTime).toLocaleDateString() : '' }}</span>
                  </div>
                  <p class="text-sm text-stone-600">{{ c.content }}</p>
                  <div v-if="c.rating" class="mt-1 text-xs text-amber-500">评分：{{ '★'.repeat(c.rating) }}{{ '☆'.repeat(MAX_RATING - c.rating) }}</div>
                </div>
              </div>
              <p v-else class="text-sm text-stone-400 text-center py-6">暂无评论，来发表第一条吧</p>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 路线对比弹窗 -->
    <Teleport to="body">
      <div
        v-if="showCompare"
        class="fixed inset-0 z-50 flex items-start justify-center pt-16 pb-8 px-4"
        @click.self="showCompare = false"
      >
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="showCompare = false" />
        <div class="surface-card relative max-h-full w-full max-w-4xl overflow-y-auto rounded-[1.75rem] border border-white/80 bg-white/95 shadow-[0_32px_90px_-36px_rgba(15,23,42,0.45)] backdrop-blur">
          <div class="sticky top-0 z-10 flex items-center justify-between rounded-t-[1.75rem] border-b border-stone-100/80 bg-white/90 px-6 py-4 backdrop-blur">
            <h2 class="text-lg font-semibold text-stone-900">路线对比</h2>
            <button @click="showCompare = false" class="p-2 text-stone-400 hover:text-stone-600 hover:bg-stone-100 rounded-lg transition-colors">
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" /></svg>
            </button>
          </div>
          <div class="p-6">
            <LoadingSpinner v-if="compareLoading" />
            <template v-else-if="compareResults.length">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div v-for="(r, i) in compareResults" :key="i" class="p-4 bg-stone-50 rounded-xl border border-stone-200">
                  <h3 class="font-medium text-stone-900 mb-2">{{ r.title || '路线 ' + (i + 1) }}</h3>
                  <div class="text-xs text-stone-500 space-y-1">
                    <p v-if="r.durationDays">天数：{{ r.durationDays }} 天</p>
                    <p v-if="r.difficulty">难度：{{ r.difficulty }}</p>
                    <p v-if="r.viewCount != null">浏览：{{ r.viewCount }}</p>
                    <p v-if="r.likeCount != null">点赞：{{ r.likeCount }}</p>
                    <p v-if="r.description" class="line-clamp-2">{{ r.description }}</p>
                  </div>
                </div>
              </div>
              <div v-if="compareSummary" class="mt-4 p-4 bg-amber-50 border border-amber-200 rounded-xl">
                <p class="text-sm font-medium text-amber-800 mb-1">对比分析</p>
                <p class="text-xs text-amber-700">{{ compareSummary }}</p>
              </div>
            </template>
            <p v-else class="text-sm text-stone-400 text-center py-8">请选择至少 2 条路线进行对比</p>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 分享成功提示 -->
    <div
      v-if="copySuccess"
      class="fixed bottom-8 left-1/2 -translate-x-1/2 z-50 px-4 py-2 bg-stone-900 text-white text-sm rounded-lg shadow-lg"
    >链接已复制到剪贴板</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { routeCrudApi, intelligentRouteApi } from '../api/route.api'
import { commentApi } from '../api/comment.api'
import { collectionApi } from '../api/collection.api'
import { shareApi } from '../api/share.api'
import { userApi } from '../api/user.api'
import { dictionaryApi } from '../api/dictionary.api'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import { DEFAULT_CITY_ID, DEFAULT_DAYS, DEFAULT_PAGE_SIZE, MAX_RATING, TEXT } from '../constants'

const loading = ref(true)
const routes = ref<any[]>([])
const isLoggedIn = ref(!!localStorage.getItem('token'))
const currentUserId = ref<number | null>(null)

// Tab 状态 (默认值，onMounted时异步更新为后端配置)
const DEFAULT_ROUTE_TABS = [
  { key: 'all', label: '全部路线' },
  { key: 'popular', label: '热门路线' },
  { key: 'smart', label: '智能推荐' },
  { key: 'seasonal', label: '季节推荐' },
  { key: 'theme', label: '主题路线' },
]
const routeTabs = ref<{ key: string; label: string }[]>(DEFAULT_ROUTE_TABS)
const activeRouteTab = ref('all')
const seasonFilter = ref('spring')
const themeFilter = ref('自然风光')
const seasonOptions = ref<{ value: string; label: string }[]>([
  { value: 'spring', label: '春季' },
  { value: 'summer', label: '夏季' },
  { value: 'autumn', label: '秋季' },
  { value: 'winter', label: '冬季' },
])
const themeOptions = ref<{ value: string; label: string }[]>([
  { value: '自然风光', label: '自然风光' },
  { value: '历史文化', label: '历史文化' },
  { value: '美食之旅', label: '美食之旅' },
  { value: '亲子游', label: '亲子游' },
  { value: '摄影', label: '摄影' },
])

// 路线对比
const selectedForCompare = ref<number[]>([])
const showCompare = ref(false)
const compareLoading = ref(false)
const compareResults = ref<any[]>([])
const compareSummary = ref('')

// 详情弹窗
const selectedRoute = ref<any>(null)
const comments = ref<any[]>([])
const newComment = ref('')
const isCollected = ref(false)
const shareCode = ref('')
const copySuccess = ref(false)
const windowOrigin = window.location.origin
const optimizeSuggestions = ref<string[] | null>(null)
const evaluateResult = ref<any>(null)

// ==================== Tab 切换 ====================
function switchRouteTab(key: string) {
  activeRouteTab.value = key
  loading.value = true
  switch (key) {
    case 'all': fetchAllRoutes(); break
    case 'popular': fetchPopularRoutes(); break
    case 'smart': fetchSmartRoutes(); break
    case 'seasonal': fetchSeasonalRoutes(); break
    case 'theme': fetchThemeRoutes(); break
  }
}

async function fetchAllRoutes() {
  try {
    routes.value = await routeCrudApi.getRoutesByCity(DEFAULT_CITY_ID) as any[]
  } catch { /* ignore */ }
  loading.value = false
}

async function fetchPopularRoutes() {
  try {
    routes.value = await intelligentRouteApi.getSmartRouteList({ type: 'popular', cityId: DEFAULT_CITY_ID, days: DEFAULT_DAYS, limit: DEFAULT_PAGE_SIZE }) as any[]
  } catch { /* ignore */ }
  loading.value = false
}

async function fetchSmartRoutes() {
  try {
    const user = currentUserId.value
    if (user) {
      routes.value = await intelligentRouteApi.recommendByPreference({}, { userId: user, cityId: DEFAULT_CITY_ID, days: DEFAULT_DAYS }) as any[]
    } else {
      routes.value = await intelligentRouteApi.getSmartRouteList({ type: 'popular', cityId: DEFAULT_CITY_ID, days: DEFAULT_DAYS, limit: DEFAULT_PAGE_SIZE }) as any[]
    }
  } catch { /* ignore */ }
  loading.value = false
}

async function fetchSeasonalRoutes() {
  try {
    routes.value = await intelligentRouteApi.getSmartRouteList({ type: 'seasonal', cityId: DEFAULT_CITY_ID, days: DEFAULT_DAYS, season: seasonFilter.value }) as any[]
  } catch { /* ignore */ }
  loading.value = false
}

async function fetchThemeRoutes() {
  try {
    routes.value = await intelligentRouteApi.getSmartRouteList({ type: 'theme', cityId: DEFAULT_CITY_ID, days: DEFAULT_DAYS, theme: themeFilter.value }) as any[]
  } catch { /* ignore */ }
  loading.value = false
}

// ==================== 路线对比 ====================
function toggleCompare(id: number) {
  const idx = selectedForCompare.value.indexOf(id)
  if (idx >= 0) {
    selectedForCompare.value.splice(idx, 1)
  } else {
    selectedForCompare.value.push(id)
  }
}

watch(showCompare, async (val) => {
  if (val && selectedForCompare.value.length >= 2) {
    compareLoading.value = true
    compareResults.value = []
    compareSummary.value = ''
    try {
      const result = await intelligentRouteApi.compareRoutes(selectedForCompare.value) as any
      if (Array.isArray(result)) {
        compareResults.value = result
      } else if (result?.routes) {
        compareResults.value = result.routes
        compareSummary.value = result.summary || result.analysis || ''
      } else {
        compareResults.value = [result]
      }
    } catch { /* ignore */ }
    compareLoading.value = false
  }
})

// ==================== 详情弹窗 ====================
function openDetail(route: any) {
  selectedRoute.value = route
  comments.value = []
  newComment.value = ''
  shareCode.value = ''
  optimizeSuggestions.value = null
  evaluateResult.value = null
  fetchComments(route.id)
  if (isLoggedIn.value && currentUserId.value) {
    checkCollected(route.id)
  }
}

function closeDetail() {
  selectedRoute.value = null
}

// 评论
async function fetchComments(routeId: number) {
  try {
    comments.value = await commentApi.getRouteComments(routeId) as any[]
  } catch { /* ignore */ }
}

async function addComment() {
  if (!newComment.value.trim() || !selectedRoute.value || !currentUserId.value) return
  try {
    await commentApi.createComment({
      routeId: selectedRoute.value.id,
      userId: currentUserId.value,
      content: newComment.value.trim(),
    })
    newComment.value = ''
    fetchComments(selectedRoute.value.id)
  } catch { /* ignore */ }
}

// 收藏
async function checkCollected(routeId: number) {
  if (!currentUserId.value) return
  try {
    isCollected.value = await collectionApi.checkCollected(currentUserId.value, routeId) as any
  } catch { /* ignore */ }
}

async function toggleCollect() {
  if (!selectedRoute.value || !currentUserId.value) return
  try {
    const result = await collectionApi.toggleCollection(selectedRoute.value.id, currentUserId.value) as any
    isCollected.value = result?.collected ?? !isCollected.value
  } catch { /* ignore */ }
}

// 分享
async function shareRoute() {
  if (!selectedRoute.value) return
  try {
    const result = await shareApi.generateShareCode(selectedRoute.value.id, 'route') as any
    shareCode.value = result?.shareCode || ''
  } catch { /* ignore */ }
}

function copyShareLink() {
  const link = `${windowOrigin}/share/${shareCode.value}`
  navigator.clipboard.writeText(link).then(() => {
    copySuccess.value = true
    setTimeout(() => { copySuccess.value = false }, 2000)
  }).catch(() => {})
}

// 复制路线
async function copyRoute() {
  if (!selectedRoute.value || !currentUserId.value) return
  try {
    await routeCrudApi.copyRoute(selectedRoute.value.id, currentUserId.value)
    closeDetail()
    fetchAllRoutes()
  } catch { /* ignore */ }
}

// 切换可见性
async function toggleVisibility() {
  if (!selectedRoute.value || !currentUserId.value) return
  try {
    const newVisibility = !selectedRoute.value.isPublic
    await routeCrudApi.setRouteVisibility(selectedRoute.value.id, currentUserId.value, newVisibility)
    selectedRoute.value.isPublic = newVisibility
    // 更新列表中的状态
    const idx = routes.value.findIndex(r => r.id === selectedRoute.value.id)
    if (idx >= 0) routes.value[idx].isPublic = newVisibility
  } catch { /* ignore */ }
}

// 优化路线
async function optimizeRoute() {
  if (!selectedRoute.value) return
  try {
    const result = await intelligentRouteApi.optimizeRoute(selectedRoute.value.id) as any
    if (result) {
      optimizeSuggestions.value = typeof result === 'string' ? [result] : (Array.isArray(result) ? result : [JSON.stringify(result)])
    }
  } catch { /* ignore */ }
}

// 评估路线
async function evaluateRoute() {
  if (!selectedRoute.value) return
  try {
    const result = await intelligentRouteApi.evaluateRouteQuality(selectedRoute.value.id, {}) as any
    evaluateResult.value = result
  } catch { /* ignore */ }
}

// 实时调整
const adjustResult = ref<any>(null)
const alternativeRoutes = ref<any[]>([])

async function adjustRoute(type: number) {
  if (!selectedRoute.value || !currentUserId.value) return
  adjustResult.value = null
  alternativeRoutes.value = []
  const adjustmentTypeMap: Record<number, string> = { 1: 'avoid_crowd', 2: 'shorten_distance', 3: 'reduce_time' } as const
  try {
    const result = await intelligentRouteApi.getRealTimeAdjustment(selectedRoute.value.id, {
      realTimeFactors: { type: adjustmentTypeMap[type] || 'comprehensive', userId: currentUserId.value },
    }) as any
    adjustResult.value = result
  } catch { /* ignore */ }
}

async function getAlternativeRoutes() {
  if (!selectedRoute.value || !currentUserId.value) return
  try {
    const result = await intelligentRouteApi.getSimilarRoutes(selectedRoute.value.id, 5) as any
    alternativeRoutes.value = Array.isArray(result) ? result : []
  } catch { /* ignore */ }
}

// 收藏分类
const collectionCategory = ref('')
const collectionNote = ref('')
const collectionCategories = ref<string[]>([])

async function fetchCollectionCategories() {
  if (!currentUserId.value) return
  try {
    const cats = await collectionApi.getCollectionCategories(currentUserId.value) as string[]
    collectionCategories.value = cats.length ? cats : ['自然风光', '历史文化', '美食之旅', '亲子游', '摄影', '探险', '休闲度假']
  } catch {
    collectionCategories.value = ['自然风光', '历史文化', '美食之旅', '亲子游', '摄影', '探险', '休闲度假']
  }
}

async function updateCollectionCategory() {
  if (!selectedRoute.value || !currentUserId.value || !collectionCategory.value) return
  try {
    await collectionApi.updateCollectionNotes(selectedRoute.value.id, currentUserId.value, collectionCategory.value)
    collectionCategory.value = ''
  } catch { /* ignore */ }
}

async function updateCollectionNote() {
  if (!selectedRoute.value || !currentUserId.value || !collectionNote.value) return
  try {
    await collectionApi.updateCollectionNotes(selectedRoute.value.id, currentUserId.value, collectionNote.value)
    collectionNote.value = ''
  } catch { /* ignore */ }
}

async function fetchFilterOptions() {
  try {
    const [themes, seasons, tabsData] = await Promise.all([
      intelligentRouteApi.getRouteThemes() as any,
      intelligentRouteApi.getRouteSeasons() as any,
      dictionaryApi.getByType('route_tabs') as any,
    ])
    if (Array.isArray(themes) && themes.length) themeOptions.value = themes
    if (Array.isArray(seasons) && seasons.length) seasonOptions.value = seasons
    if (Array.isArray(tabsData) && tabsData.length) {
      routeTabs.value = tabsData.map((item: any) => ({ key: item.key, label: item.label }))
    }
  } catch { /* ignore */ }
}

onMounted(async () => {
  fetchFilterOptions()
  await fetchAllRoutes()

  // 获取当前用户
  if (isLoggedIn.value) {
    try {
      const user = await userApi.getCurrentUser() as any
      currentUserId.value = user.id
      fetchCollectionCategories()
    } catch { /* ignore */ }
  }
})
</script>