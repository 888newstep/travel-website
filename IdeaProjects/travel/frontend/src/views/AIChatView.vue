<template>
  <div class="max-w-4xl mx-auto px-6 py-12">
    <h1 class="text-2xl font-semibold text-stone-900 mb-8">AI 旅行助手</h1>

    <!-- Tab 导航 -->
    <div class="flex gap-1 mb-6 bg-stone-100 rounded-lg p-1">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="flex-1 px-4 py-2 rounded-md text-sm font-medium transition-colors"
        :class="activeTab === tab.key ? 'bg-white text-stone-900 shadow-sm' : 'text-stone-500 hover:text-stone-700'"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- ==================== Tab 1: AI 对话 ==================== -->
    <div v-if="activeTab === 'chat'" class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <!-- Messages -->
      <div class="h-125 overflow-y-auto p-6 space-y-4 scrollbar-hide" ref="chatBox">
        <div v-if="!messages.length" class="text-center py-12">
          <div class="w-16 h-16 bg-stone-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <svg class="w-8 h-8 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 0 0-3.09 3.09ZM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 0 0-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 0 0 2.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 0 0 2.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 0 0-2.455 2.456ZM16.894 20.567L16.5 21.75l-.394-1.183a2.25 2.25 0 0 0-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 0 0 1.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 0 0 1.423 1.423l1.183.394-1.183.394a2.25 2.25 0 0 0-1.423 1.423Z" />
            </svg>
          </div>
          <p class="text-sm text-stone-500">你好！我是你的 AI 旅行助手。<br/>可以问我景点推荐、路线规划或旅行建议。</p>
        </div>

        <div
          v-for="(msg, i) in messages"
          :key="i"
          class="flex gap-3"
          :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
        >
          <div
            class="max-w-[80%] px-4 py-2.5 rounded-xl text-sm leading-relaxed"
            :class="msg.role === 'user' ? 'bg-stone-900 text-white rounded-br-sm' : 'bg-stone-100 text-stone-700 rounded-bl-sm'"
          >
            {{ msg.content }}
          </div>
        </div>

        <div v-if="loading" class="flex gap-3">
          <div class="bg-stone-100 rounded-xl rounded-bl-sm px-4 py-2.5">
            <LoadingDots />
          </div>
        </div>
      </div>

      <!-- Input -->
      <div class="border-t border-stone-200 p-4">
        <form class="flex gap-3" @submit.prevent="sendMessage">
          <input
            v-model="input"
            type="text"
            placeholder="输入你的问题..."
            class="flex-1 px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            :disabled="loading"
          />
          <button
            type="submit"
            class="px-5 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors disabled:opacity-50"
            :disabled="loading || !input.trim()"
          >
            发送
          </button>
        </form>
      </div>
    </div>

    <!-- ==================== Tab 2: AI 旅行推荐 ==================== -->
    <div v-if="activeTab === 'recommend'" class="bg-white rounded-xl border border-stone-200 p-6">
      <form class="space-y-4" @submit.prevent="getRecommendation">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-1">目的地</label>
            <input
              v-model="recommendForm.location"
              type="text"
              placeholder="例如：云南、三亚..."
              class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-1">预算（元）</label>
            <input
              v-model.number="recommendForm.budget"
              type="number"
              placeholder="例如：5000"
              class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-1">旅行天数</label>
            <input
              v-model.number="recommendForm.duration"
              type="number"
              placeholder="例如：5"
              class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-1">偏好</label>
            <input
              v-model="recommendForm.preferences"
              type="text"
              placeholder="例如：自然风光、美食..."
              class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            />
          </div>
        </div>
        <button
          type="submit"
          class="px-5 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors disabled:opacity-50"
          :disabled="recommendLoading"
        >
          {{ recommendLoading ? '推荐中...' : '获取推荐' }}
        </button>
      </form>

      <!-- Loading -->
      <div v-if="recommendLoading" class="mt-6 flex justify-center">
        <LoadingDots />
      </div>

      <!-- Error -->
      <div v-if="recommendError" class="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">
        {{ recommendError }}
      </div>

      <!-- Results as cards -->
      <div v-if="recommendResults.length" class="mt-6 space-y-4">
        <h3 class="text-base font-medium text-stone-800">推荐结果</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div
            v-for="(item, idx) in recommendResults"
            :key="idx"
            class="p-4 bg-stone-50 border border-stone-200 rounded-xl hover:shadow-sm transition-shadow"
          >
            <div class="text-sm font-medium text-stone-900 mb-1">{{ item.name || item.title || '推荐 ' + (+idx + 1) }}</div>
            <div class="text-xs text-stone-500">{{ item.description || item.detail || '' }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== Tab 3: AI 行程生成 ==================== -->
    <div v-if="activeTab === 'itinerary'" class="bg-white rounded-xl border border-stone-200 p-6">
      <form class="space-y-4" @submit.prevent="generateItinerary">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-1">目的地</label>
            <input
              v-model="itineraryForm.destination"
              type="text"
              placeholder="例如：北京"
              class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-1">天数</label>
            <input
              v-model.number="itineraryForm.days"
              type="number"
              placeholder="例如：3"
              class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-1">预算（元）</label>
            <input
              v-model.number="itineraryForm.budget"
              type="number"
              placeholder="例如：3000"
              class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            />
          </div>
        </div>
        <button
          type="submit"
          class="px-5 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors disabled:opacity-50"
          :disabled="itineraryLoading"
        >
          {{ itineraryLoading ? '生成中...' : '生成行程' }}
        </button>
      </form>

      <!-- Loading -->
      <div v-if="itineraryLoading" class="mt-6 flex justify-center">
        <LoadingDots />
      </div>

      <!-- Error -->
      <div v-if="itineraryError" class="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">
        {{ itineraryError }}
      </div>

      <!-- Result -->
      <div v-if="itineraryResult" class="mt-6">
        <h3 class="text-base font-medium text-stone-800 mb-3">生成行程</h3>
        <div class="p-4 bg-stone-50 border border-stone-200 rounded-xl">
          <div v-if="itineraryResult.days" class="space-y-4">
            <div v-for="(day, idx) in itineraryResult.days" :key="idx" class="p-3 bg-white rounded-lg border border-stone-100">
              <div class="text-sm font-medium text-stone-900 mb-2">第 {{ day.day || (+idx + 1) }} 天</div>
              <div class="text-xs text-stone-600 space-y-1">
                <div v-if="day.morning" class="flex gap-2">
                  <span class="text-stone-400">上午</span>
                  <span>{{ day.morning }}</span>
                </div>
                <div v-if="day.afternoon" class="flex gap-2">
                  <span class="text-stone-400">下午</span>
                  <span>{{ day.afternoon }}</span>
                </div>
                <div v-if="day.evening" class="flex gap-2">
                  <span class="text-stone-400">晚上</span>
                  <span>{{ day.evening }}</span>
                </div>
                <div v-if="day.activities" class="flex gap-2">
                  <span class="text-stone-400">活动</span>
                  <span>{{ day.activities }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="text-sm text-stone-600 whitespace-pre-wrap">{{ itineraryResult.response || JSON.stringify(itineraryResult, null, 2) }}</div>
        </div>
      </div>
    </div>

    <!-- ==================== Tab 4: 图像分析 ==================== -->
    <div v-if="activeTab === 'image'" class="bg-white rounded-xl border border-stone-200 p-6">
      <form class="space-y-4" @submit.prevent="analyzeImage">
        <div>
          <label class="block text-sm font-medium text-stone-700 mb-1">图片 URL</label>
          <input
            v-model="imageForm.url"
            type="text"
            placeholder="输入图片链接..."
            class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
          />
        </div>
        <div class="flex items-center gap-4">
          <select v-model="imageForm.type" class="px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm">
            <option v-for="t in imageTypeOptions" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
          <button
            type="submit"
            class="px-5 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors"
            :disabled="imageLoading"
          >{{ imageLoading ? '分析中...' : '开始分析' }}</button>
        </div>
      </form>
      <div v-if="imageResult" class="mt-4 p-4 bg-stone-50 rounded-lg border border-stone-200">
        <h3 class="text-sm font-medium text-stone-800 mb-2">分析结果</h3>
        <pre class="text-xs text-stone-600 whitespace-pre-wrap">{{ JSON.stringify(imageResult, null, 2) }}</pre>
      </div>
      <div v-if="imageError" class="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">{{ imageError }}</div>
    </div>

    <!-- ==================== Tab 5: 多模态 ==================== -->
    <div v-if="activeTab === 'multimodal'" class="bg-white rounded-xl border border-stone-200 p-6">
      <form class="space-y-4" @submit.prevent="multimodalQuery">
        <div>
          <label class="block text-sm font-medium text-stone-700 mb-1">文字描述</label>
          <textarea
            v-model="multimodalForm.text"
            rows="3"
            placeholder="描述你想查询的内容..."
            class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300 resize-none"
          ></textarea>
        </div>
        <div>
          <label class="block text-sm font-medium text-stone-700 mb-1">图片 URL（可选）</label>
          <input
            v-model="multimodalForm.image"
            type="text"
            placeholder="输入图片链接..."
            class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
          />
        </div>
        <button
          type="submit"
          class="px-5 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors"
          :disabled="multimodalLoading"
        >{{ multimodalLoading ? '查询中...' : '多模态查询' }}</button>
      </form>
      <div v-if="multimodalResult" class="mt-4 p-4 bg-stone-50 rounded-lg border border-stone-200">
        <p class="text-sm text-stone-700 whitespace-pre-wrap">{{ multimodalResult }}</p>
      </div>
    </div>

    <!-- ==================== Tab 6: 预算助手 ==================== -->
    <div v-if="activeTab === 'budget'" class="bg-white rounded-xl border border-stone-200 p-6">
      <form class="space-y-4" @submit.prevent="getBudgetAdvice">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-1">目的地</label>
            <input v-model="budgetForm.destination" type="text" placeholder="例如：三亚" class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300" />
          </div>
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-1">天数</label>
            <input v-model.number="budgetForm.days" type="number" placeholder="例如：5" class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300" />
          </div>
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-1">预算（元）</label>
            <input v-model.number="budgetForm.budget" type="number" placeholder="例如：5000" class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300" />
          </div>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-1">人数</label>
            <input v-model.number="budgetForm.people" type="number" placeholder="例如：2" class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300" />
          </div>
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-1">风格偏好</label>
            <input v-model="budgetForm.style" type="text" placeholder="例如：经济/舒适/奢华" class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300" />
          </div>
        </div>
        <button
          type="submit"
          class="px-5 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors"
          :disabled="budgetLoading"
        >{{ budgetLoading ? '规划中...' : '预算规划' }}</button>
      </form>
      <div v-if="budgetError" class="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">{{ budgetError }}</div>
      <div v-if="budgetResult" class="mt-4 p-4 bg-stone-50 rounded-lg border border-stone-200">
        <h3 class="text-sm font-medium text-stone-800 mb-2">预算规划结果</h3>
        <div class="text-xs text-stone-600 whitespace-pre-wrap">{{ budgetResult }}</div>
      </div>
    </div>

    <!-- ==================== Tab 7: AI 智能助手 ==================== -->
    <div v-if="activeTab === 'assistant'" class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <!-- Messages -->
      <div class="h-125 overflow-y-auto p-6 space-y-4 scrollbar-hide" ref="assistantBox">
        <div v-if="!assistantMessages.length" class="text-center py-12">
          <div class="w-16 h-16 bg-stone-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <svg class="w-8 h-8 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 0 0-3.09 3.09ZM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 0 0-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 0 0 2.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 0 0 2.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 0 0-2.455 2.456ZM16.894 20.567L16.5 21.75l-.394-1.183a2.25 2.25 0 0 0-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 0 0 1.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 0 0 1.423 1.423l1.183.394-1.183.394a2.25 2.25 0 0 0-1.423 1.423Z" />
            </svg>
          </div>
          <p class="text-sm text-stone-500">我是你的 AI 智能助手。<br/>可以问我任何旅行相关的问题、攻略或建议。</p>
        </div>

        <div
          v-for="(msg, i) in assistantMessages"
          :key="i"
          class="flex gap-3"
          :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
        >
          <div
            class="max-w-[80%] px-4 py-2.5 rounded-xl text-sm leading-relaxed"
            :class="msg.role === 'user' ? 'bg-stone-900 text-white rounded-br-sm' : 'bg-stone-100 text-stone-700 rounded-bl-sm'"
          >
            {{ msg.content }}
          </div>
        </div>

        <div v-if="assistantLoading" class="flex gap-3">
          <div class="bg-stone-100 rounded-xl rounded-bl-sm px-4 py-2.5">
            <LoadingDots />
          </div>
        </div>
      </div>

      <!-- Input -->
      <div class="border-t border-stone-200 p-4">
        <form class="flex gap-3" @submit.prevent="sendAssistantQuery">
          <input
            v-model="assistantInput"
            type="text"
            placeholder="输入你的问题..."
            class="flex-1 px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300"
            :disabled="assistantLoading"
          />
          <button
            type="submit"
            class="px-5 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors disabled:opacity-50"
            :disabled="assistantLoading || !assistantInput.trim()"
          >
            发送
          </button>
        </form>
      </div>
    </div>
  <!-- ==================== Tab 8: 智能规划 ==================== -->
    <div v-if="activeTab === 'plan'" class="bg-white rounded-xl border border-stone-200 p-6">
      <h3 class="text-sm font-medium text-stone-700 mb-4">AI 智能路线规划</h3>
      <form class="space-y-4" @submit.prevent="planRoute">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-xs text-stone-500 mb-1">偏好标签（逗号分隔）</label>
            <input v-model="planForm.preferences" type="text" placeholder="自然风光, 美食, 文化" class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300" />
          </div>
          <div>
            <label class="block text-xs text-stone-500 mb-1">约束条件</label>
            <input v-model="planForm.constraints" type="text" placeholder="预算5000, 天数3" class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300" />
          </div>
        </div>
        <button type="submit" class="px-5 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors" :disabled="planLoading">
          {{ planLoading ? '规划中...' : '开始规划' }}
        </button>
      </form>
      <div v-if="planError" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">{{ planError }}</div>
      <div v-if="planResult" class="mt-4 p-4 bg-stone-50 rounded-lg border border-stone-200">
        <h4 class="text-sm font-medium text-stone-800 mb-2">规划结果</h4>
        <div class="text-xs text-stone-600 space-y-1">
          <div v-for="(v, k) in planResult" :key="k" class="flex gap-2">
            <span class="text-stone-400">{{ k }}:</span>
            <span>{{ typeof v === 'object' ? JSON.stringify(v) : v }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== Tab 9: 安全建议 ==================== -->
    <div v-if="activeTab === 'safety'" class="bg-white rounded-xl border border-stone-200 p-6">
      <h3 class="text-sm font-medium text-stone-700 mb-4">旅游安全建议</h3>
      <div class="flex gap-3">
        <input v-model="safetyCityId" type="number" placeholder="城市 ID" class="flex-1 px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300" />
        <button @click="getSafetyAdvice" class="px-5 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors" :disabled="safetyLoading">
          {{ safetyLoading ? '查询中...' : '获取安全建议' }}
        </button>
      </div>
      <div v-if="safetyError" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">{{ safetyError }}</div>
      <div v-if="safetyResult" class="mt-4 p-4 bg-stone-50 rounded-lg border border-stone-200">
        <h4 class="text-sm font-medium text-stone-800 mb-2">安全建议</h4>
        <div v-if="safetyResult.generalAdvice" class="mb-3">
          <p class="text-xs font-medium text-stone-600 mb-1">通用建议</p>
          <ul class="text-xs text-stone-500 space-y-0.5 list-disc list-inside">
            <li v-for="(a, i) in safetyResult.generalAdvice" :key="'g'+i">{{ a }}</li>
          </ul>
        </div>
        <div v-if="safetyResult.travelAdvice" class="mb-3">
          <p class="text-xs font-medium text-stone-600 mb-1">出行建议</p>
          <ul class="text-xs text-stone-500 space-y-0.5 list-disc list-inside">
            <li v-for="(a, i) in safetyResult.travelAdvice" :key="'t'+i">{{ a }}</li>
          </ul>
        </div>
        <div v-if="safetyResult.areaAdvice">
          <p class="text-xs font-medium text-stone-600 mb-1">区域建议</p>
          <div v-for="(v, k) in safetyResult.areaAdvice" :key="k" class="mb-1">
            <span class="text-xs text-stone-700 font-medium">{{ k }}：</span>
            <span class="text-xs text-stone-500">{{ Array.isArray(v) ? v.join(', ') : v }}</span>
          </div>
        </div>
        <div v-if="!safetyResult.generalAdvice && !safetyResult.travelAdvice && !safetyResult.areaAdvice" class="text-xs text-stone-600 whitespace-pre-wrap">{{ JSON.stringify(safetyResult, null, 2) }}</div>
      </div>
    </div>

    <!-- ==================== Tab 10: 语音助手 ==================== -->
    <div v-if="activeTab === 'voice'" class="bg-white rounded-xl border border-stone-200 p-6">
      <h3 class="text-sm font-medium text-stone-700 mb-4">AI 语音助手</h3>
      <p class="text-xs text-stone-400 mb-4">输入文字，AI 将模拟语音交互体验。支持语音输入的文字转写。</p>
      <form class="space-y-4" @submit.prevent="processVoice">
        <div>
          <label class="block text-xs text-stone-500 mb-1">语音转写文本</label>
          <textarea v-model="voiceText" rows="3" placeholder="输入您想说的话..." class="w-full px-4 py-2 bg-stone-50 border border-stone-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-stone-300 resize-none"></textarea>
        </div>
        <button type="submit" class="px-5 py-2 bg-stone-900 text-white rounded-lg text-sm hover:bg-stone-800 transition-colors" :disabled="voiceLoading">
          {{ voiceLoading ? '处理中...' : '提交语音请求' }}
        </button>
      </form>
      <div v-if="voiceError" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">{{ voiceError }}</div>
      <div v-if="voiceResult" class="mt-4 p-4 bg-stone-50 rounded-lg border border-stone-200">
        <h4 class="text-sm font-medium text-stone-800 mb-2">语音处理结果</h4>
        <div class="text-xs text-stone-600 whitespace-pre-wrap">{{ voiceResult }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { aiApi } from '../api/ai.api'
import { dictionaryApi } from '../api/dictionary.api'
import LoadingDots from '../components/common/LoadingDots.vue'

// ==================== Tab 状态 ====================
const DEFAULT_TABS = [
  { key: 'chat', label: 'AI 对话' },
  { key: 'recommend', label: 'AI 旅行推荐' },
  { key: 'itinerary', label: 'AI 行程生成' },
  { key: 'image', label: '图像分析' },
  { key: 'multimodal', label: '多模态' },
  { key: 'budget', label: '预算助手' },
  { key: 'assistant', label: 'AI 智能助手' },
  { key: 'plan', label: '智能规划' },
  { key: 'safety', label: '安全建议' },
  { key: 'voice', label: '语音助手' },
]
const tabs = ref<{ key: string; label: string }[]>(DEFAULT_TABS)

// 启动时获取最新Tab配置
dictionaryApi.getByType('ai_tabs').then((res: any) => {
  if (Array.isArray(res) && res.length) {
    tabs.value = res.map((item: any) => ({ key: item.key, label: item.label }))
  }
}).catch(() => {})
const activeTab = ref('chat')

// ==================== Tab 1: AI 对话 ====================
const input = ref('')
const loading = ref(false)
const messages = ref<{ role: string; content: string }[]>([])
const chatBox = ref<HTMLElement | null>(null)

async function scrollToBottom() {
  await nextTick()
  if (chatBox.value) {
    chatBox.value.scrollTop = chatBox.value.scrollHeight
  }
}

function getAIErrorMessage(e: any): string {
  const msg = e?.message || ''
  if (msg.includes('AI服务暂未启用') || msg.includes('AI服务') || msg.includes('不可用')) {
    return 'AI 服务暂未启用，请确认后端已配置通义千问 API Key 并已启动服务。'
  }
  if (msg.includes('timeout') || msg.includes('超时')) {
    return 'AI 响应超时，通义千问模型生成可能需要较长时间，请稍后重试。'
  }
  if (msg.includes('401') || msg.includes('未授权')) {
    return '登录后可使用更多 AI 功能（路线规划、行程生成等）。当前基础对话仍可使用。'
  }
  return '网络错误，请稍后重试。' + (msg ? ' (' + msg + ')' : '')
}

async function sendMessage() {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const res = await aiApi.chat({ message: text })
    messages.value.push({ role: 'assistant', content: (res as any).response || '抱歉，暂时无法回答。' })
  } catch (e: any) {
    messages.value.push({ role: 'assistant', content: getAIErrorMessage(e) })
  }
  loading.value = false
  await scrollToBottom()
}

// ==================== Tab 2: AI 旅行推荐 ====================
const recommendForm = ref({
  location: '',
  budget: null as number | null,
  duration: null as number | null,
  preferences: '',
})
const recommendLoading = ref(false)
const recommendError = ref('')
const recommendResults = ref<any[]>([])

function getRecommendation() {
  recommendError.value = ''
  recommendResults.value = []
  recommendLoading.value = true

  const data: any = {}
  if (recommendForm.value.location) data.location = recommendForm.value.location
  if (recommendForm.value.budget) data.budget = recommendForm.value.budget
  if (recommendForm.value.duration) data.duration = recommendForm.value.duration
  if (recommendForm.value.preferences) {
    data.preferences = recommendForm.value.preferences
      .split(/[,，]/)
      .map((s: string) => s.trim())
      .filter((s: string) => s)
  }

  aiApi.getTravelRecommendation(data)
    .then((res: any) => {
      const result = res.data || res
      if (Array.isArray(result)) {
        recommendResults.value = result
      } else if (result.recommendations && Array.isArray(result.recommendations)) {
        recommendResults.value = result.recommendations
      } else if (result.data && Array.isArray(result.data)) {
        recommendResults.value = result.data
      } else {
        recommendResults.value = [result]
      }
    })
    .catch((e: any) => {
      recommendError.value = '获取推荐失败，请稍后重试。' + (e?.message ? ' (' + e.message + ')' : '')
    })
    .finally(() => {
      recommendLoading.value = false
    })
}

// ==================== Tab 3: AI 行程生成 ====================
const itineraryForm = ref({
  destination: '',
  days: null as number | null,
  budget: null as number | null,
})
const itineraryLoading = ref(false)
const itineraryError = ref('')
const itineraryResult = ref<any>(null)

function generateItinerary() {
  itineraryError.value = ''
  itineraryResult.value = null
  itineraryLoading.value = true

  const data: any = {
    destination: itineraryForm.value.destination || '未指定',
    days: itineraryForm.value.days || 1,
  }
  if (itineraryForm.value.budget) data.budget = itineraryForm.value.budget

  aiApi.generateItinerary(data)
    .then((res: any) => {
      itineraryResult.value = res.data || res
    })
    .catch((e: any) => {
      itineraryError.value = '生成行程失败，请稍后重试。' + (e?.message ? ' (' + e.message + ')' : '')
    })
    .finally(() => {
      itineraryLoading.value = false
    })
}

// ==================== Tab 4: 图像分析 ====================
const imageForm = ref({ url: '', type: 'scene' })
const imageLoading = ref(false)
const imageResult = ref<any>(null)
const imageError = ref('')
const imageTypeOptions = ref<{ value: string; label: string }[]>([
  { value: 'scene', label: '场景识别' },
  { value: 'dish', label: '菜品识别' },
  { value: 'ocr', label: '文字识别' },
])

// 启动时获取最新的图像分析类型
aiApi.getImageAnalysisTypes().then((res: any) => {
  if (Array.isArray(res) && res.length) imageTypeOptions.value = res
}).catch(() => {})

function analyzeImage() {
  if (!imageForm.value.url) return
  imageError.value = ''
  imageResult.value = null
  imageLoading.value = true
  aiApi.analyzeImage({ imageUrl: imageForm.value.url, analysisType: imageForm.value.type })
    .then((res: any) => { imageResult.value = res.data || res })
    .catch((e: any) => { imageError.value = '分析失败: ' + (e?.message || '') })
    .finally(() => { imageLoading.value = false })
}

// ==================== Tab 5: 多模态 ====================
const multimodalForm = ref({ text: '', image: '' })
const multimodalLoading = ref(false)
const multimodalResult = ref('')

function multimodalQuery() {
  if (!multimodalForm.value.text) return
  multimodalResult.value = ''
  multimodalLoading.value = true
  const data: any = { text: multimodalForm.value.text }
  if (multimodalForm.value.image) data.image = multimodalForm.value.image
  aiApi.multimodalQuery(data)
    .then((res: any) => { multimodalResult.value = res.response || res.data?.response || JSON.stringify(res) })
    .catch((e: any) => { multimodalResult.value = '查询失败: ' + (e?.message || '') })
    .finally(() => { multimodalLoading.value = false })
}

// ==================== Tab 6: 预算助手 ====================
const budgetForm = ref({ destination: '', days: null as number | null, budget: null as number | null, people: null as number | null, style: '' })
const budgetLoading = ref(false)
const budgetResult = ref('')
const budgetError = ref('')

function getBudgetAdvice() {
  budgetError.value = ''
  budgetResult.value = ''
  budgetLoading.value = true
  const data: any = {}
  if (budgetForm.value.destination) data.destination = budgetForm.value.destination
  if (budgetForm.value.days) data.days = budgetForm.value.days
  if (budgetForm.value.budget) data.budget = budgetForm.value.budget
  if (budgetForm.value.people) data.people = budgetForm.value.people
  if (budgetForm.value.style) data.style = budgetForm.value.style
  aiApi.getBudgetEstimation(data)
    .then((res: any) => { budgetResult.value = res.response || res.data?.response || JSON.stringify(res) })
    .catch((e: any) => { budgetError.value = '规划失败: ' + (e?.message || '') })
    .finally(() => { budgetLoading.value = false })
}

// ==================== Tab 7: AI 智能助手 ====================
const assistantInput = ref('')
const assistantLoading = ref(false)
const assistantMessages = ref<{ role: string; content: string }[]>([])
const assistantBox = ref<HTMLElement | null>(null)

async function scrollAssistantToBottom() {
  await nextTick()
  if (assistantBox.value) {
    assistantBox.value.scrollTop = assistantBox.value.scrollHeight
  }
}

function sendAssistantQuery() {
  const query = assistantInput.value.trim()
  if (!query || assistantLoading.value) return

  assistantMessages.value.push({ role: 'user', content: query })
  assistantInput.value = ''
  assistantLoading.value = true
  scrollAssistantToBottom()

  aiApi.smartAssistant(query)
    .then((res: any) => {
      assistantMessages.value.push({
        role: 'assistant',
        content: (res as any).response || (res as any).data?.response || '抱歉，暂时无法回答。',
      })
    })
    .catch((e: any) => {
      assistantMessages.value.push({ role: 'assistant', content: getAIErrorMessage(e) })
    })
    .finally(() => {
      assistantLoading.value = false
      scrollAssistantToBottom()
    })
}

// ==================== Tab 8: 智能规划 ====================
const planForm = ref({ preferences: '', constraints: '' })
const planLoading = ref(false)
const planResult = ref<any>(null)
const planError = ref('')

function planRoute() {
  planError.value = ''
  planResult.value = null
  planLoading.value = true
  const data: any = {
    preferences: {},
    constraints: {},
  }
  if (planForm.value.preferences) {
    planForm.value.preferences.split(',').forEach((s: string) => {
      const parts = s.trim().split(':')
      if (parts.length >= 2) data.preferences[parts[0].trim()] = parts[1].trim()
      else data.preferences[parts[0].trim()] = true
    })
  }
  if (planForm.value.constraints) {
    planForm.value.constraints.split(',').forEach((s: string) => {
      const parts = s.trim().split(':')
      if (parts.length >= 2) data.constraints[parts[0].trim()] = parts[1].trim()
      else data.constraints[parts[0].trim()] = true
    })
  }
  aiApi.planSmartRoute(data)
    .then((res: any) => { planResult.value = res.data || res })
    .catch((e: any) => { planError.value = '规划失败: ' + (e?.message || '') })
    .finally(() => { planLoading.value = false })
}

// ==================== Tab 9: 安全建议 ====================
const safetyCityId = ref('')
const safetyLoading = ref(false)
const safetyResult = ref<any>(null)
const safetyError = ref('')

function getSafetyAdvice() {
  const cityId = safetyCityId.value.trim()
  if (!cityId) return
  safetyError.value = ''
  safetyResult.value = null
  safetyLoading.value = true
  aiApi.getSafetyAdvice(Number(cityId))
    .then((res: any) => { safetyResult.value = res.data || res })
    .catch((e: any) => { safetyError.value = '获取失败: ' + (e?.message || '') })
    .finally(() => { safetyLoading.value = false })
}

// ==================== Tab 10: 语音助手 ====================
const voiceText = ref('')
const voiceLoading = ref(false)
const voiceResult = ref('')
const voiceError = ref('')

function processVoice() {
  if (!voiceText.value.trim()) return
  voiceError.value = ''
  voiceResult.value = ''
  voiceLoading.value = true
  aiApi.processVoice({ audioData: null, text: voiceText.value.trim() })
    .then((res: any) => { voiceResult.value = res.response || res.data?.response || JSON.stringify(res) })
    .catch((e: any) => { voiceError.value = '处理失败: ' + (e?.message || '') })
    .finally(() => { voiceLoading.value = false })
}
</script>