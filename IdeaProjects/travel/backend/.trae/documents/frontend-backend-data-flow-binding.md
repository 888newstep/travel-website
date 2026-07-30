# 前后端数据流绑定实施计划

## Context（背景）

用户希望"把前端和后端的相关组件绑定起来，使得数据流可以连通"。

经探索确认，前后端基础设施已就绪：
- **URL 路径匹配**：后端 `application.properties` 配置 `server.servlet.context-path=/api`，前端 `utils/api.ts` 的 baseURL 是 `/api`，vite proxy 转发 `/api` 到 `http://localhost:8080`（无 rewrite）。前端 `/api/users/login` 正确到达后端 `/users/login` 端点。
- **响应格式统一**：后端 `Result` 类有 code/message/data 字段，前端拦截器在 code=200 时解包 data。
- **认证机制**：前端 localStorage token + Authorization Bearer 头，后端 JWT 校验。

**核心问题**：很多按钮/状态变量看似有交互，但 API 路径与后端不匹配（断链），或 handler 未调用 API。经逐行验证（已交叉核对后端 Controller 端点清单），主要问题集中在 4 类：
1. 前端 API 路径与后端端点不匹配（`intelligentRouteApi` 全部 10 个路径、`ai.api.ts` 2 个路径、`collection.api.ts` 第二套 6 个路径、`routeApi.optimizeRoute` 1 个路径）
2. handler 用 `document.querySelector` 取 DOM 而非 Vue 响应式变量
3. 部分 handler 未调用 API（`openComments` 不加载评论、`applyOptimization` 不持久化、`handleOptimizationHistory` 用硬编码）
4. 状态变量无数据源（`satisfactionPrediction` 被强制置 0 误导 UI、`chatHistory` 无持久化、`platformOverview` 后端已删除 Analytics Controller）

**预期成果**：核心数据流（登录、路线、笔记、AI、收藏、评论、分享、统计）端到端 200 响应，无 Console 404。

## 已验证的后端端点（实施依据）

- `AIController` (`/ai`)：`/chat`(35)、`/advanced/chat`(56)、`/assistant/chat`(81)、`/itinerary/generate`(147)、`/multimodal/query`(408) 等
- `RouteController` (`/routes`)：`/smart/recommend-by-preference`(255)、`/smart/optimize`(273)、`/smart/generate-personalized`(364)、`/smart/theme`(239)、`/smart/real-time-adjustment/{routeId}`(336)、`/smart/optimization-suggestions/{routeId}`(289)、`/smart/evaluate/{routeId}`(305)、`/smart/compare`(321)、`/smart/popular`(192)、`/smart/similar/{routeId}`(208)、`/smart/seasonal`(223) 等
- `RouteCollectionController` (`/v1/route-collections`)：`/collect`(24)、`/uncollect`(36)、`/list/{userId}`(48)、`/add`(116)、`/remove`(133)、`/update-note`(145)、`/categories/{userId}`(161)、`/category/{userId}/{category}`(173)、`/batch-remove`(199) 等
- `RouteOptimizationController` (`/route-optimization`)：`/apply`(126)、`/history/{routeId}`(142)
- `UserStatisticsController` (`/v1/user/stats`)：`GET /`(25)、`GET /{userId}`(41)

## 阶段 1：修复前端 API 路径断链（最高优先级）

仅修改 `frontend/src/api/*.api.ts`，不动 App.vue、不动后端。

### 1.1 修复 `frontend/src/api/ai.api.ts`

| 函数 | 当前路径 | 修复后路径 | 后端端点行号 |
|---|---|---|---|
| `advancedChatbot`（第63行） | `/ai/advanced/chatbot` | `/ai/advanced/chat` | AIController.java:56 |
| `smartAssistant`（第56行） | `/ai/assistant/query` | `/ai/assistant/chat` | AIController.java:81 |

### 1.2 修复 `frontend/src/api/collection.api.ts` 第二套 API（6 个路径）

第二套 API（第56-94行）被 App.vue 实际使用但全部断链。统一将 `/route-collection/...` 改为 `/v1/route-collections/...`（后端 RouteCollectionController 第116-199行已提供兼容端点）：

- `addCollection`：`/route-collection/add` → `/v1/route-collections/add`
- `removeCollection`：`/route-collection/remove` → `/v1/route-collections/remove`
- `updateCollectionNote`：`/route-collection/update-note` → `/v1/route-collections/update-note`
- `getCollectionCategories`：`/route-collection/categories/${userId}` → `/v1/route-collections/categories/${userId}`
- `getCollectionsByCategory`：`/route-collection/category/${userId}/${category}` → `/v1/route-collections/category/${userId}/${category}`
- `batchRemoveCollections`：`/route-collection/batch-remove` → `/v1/route-collections/batch-remove`

### 1.3 修复 `frontend/src/api/route.api.ts` 的 `intelligentRouteApi`（10 个路径全部断链）

后端 RouteController 把智能路线功能放在 `/routes/smart/...` 下（RouteController.java 第183-387行），前端却用 `/intelligent-route/...`。所有 10 个函数路径替换：

| 函数 | 当前路径 | 修复后路径 |
|---|---|---|
| `recommendByPreference` | `/intelligent-route/recommend-by-preference` | `/routes/smart/recommend-by-preference` |
| `compareRoutes` | `/intelligent-route/compare` | `/routes/smart/compare` |
| `getRealTimeAdjustment` | `/intelligent-route/real-time-adjustment/${routeId}` | `/routes/smart/real-time-adjustment/${routeId}` |
| `evaluateRouteQuality` | `/intelligent-route/evaluate/${routeId}` | `/routes/smart/evaluate/${routeId}` |
| `generatePersonalizedRoute` | `/intelligent-route/generate-personalized` | `/routes/smart/generate-personalized` |
| `getPopularRoutes` | `/intelligent-route/popular` | `/routes/smart/popular` |
| `getSimilarRoutes` | `/intelligent-route/similar/${routeId}` | `/routes/smart/similar/${routeId}` |
| `getSeasonalRoutes` | `/intelligent-route/seasonal` | `/routes/smart/seasonal` |
| `getThemeRoutes` | `/intelligent-route/theme` | `/routes/smart/theme` |
| `getOptimizationSuggestions` | `/intelligent-route/optimization-suggestions/${routeId}` | `/routes/smart/optimization-suggestions/${routeId}` |

### 1.4 修复 `frontend/src/api/route.api.ts` 的 `routeApi.optimizeRoute`

仅此函数被 App.vue 调用（第857行）。后端 `RouteController.java:273` 签名为 `@PostMapping("/smart/optimize") public Result<String> optimizeRoute(@RequestParam Integer routeId)`。

```ts
optimizeRoute(routeId: number, optimizationType: string) {
    return apiClient.post<RouteOptimization>('/routes/smart/optimize', null, {
        params: { routeId },
    });
}
```

`optimizationType` 参数后端不接受，保留函数签名以兼容调用。

### 阶段 1 验证

启动后端 + 前端，浏览器 DevTools Network：
- 登录 → `GET /api/v1/route-collections/list/{userId}` 与 `/categories/{userId}` 200
- AI 助手发消息（翻译模式）→ `POST /api/ai/assistant/chat` 200
- 点击"生成个性化路线" → `POST /api/routes/smart/generate-personalized` 200
- 点击"生成攻略" → `GET /api/routes/smart/theme?theme=...` 200

## 阶段 2：绑定未连线的 UI 行为到已有 API

修改 `frontend/src/App.vue` 与 `frontend/src/api/route.api.ts`。

### 2.1 `openComments` 加载已有评论

App.vue 第875-878行附近，改为 async，调用 `commentApi.getRouteComments(note.id, 1, 20)` 填充 `note.commentList`，map 字段为 `{id, user: c.username||'用户'+c.userId, text: c.content, likes: c.likeCount||0, replies: []}`。

### 2.2 `handleGenerateGuide` 与 `handleEstimateBudget` 移除 DOM 查询

App.vue 第1306-1320行、第1322-1338行，用已有响应式变量 `searchQuery.value`、`tripDuration.value` 替代 `document.querySelector('input[placeholder="..."]')`。`handleGenerateGuide` 调用 `intelligentRouteApi.getThemeRoutes('文化', 1, days)`；`handleEstimateBudget` 调用 `aiApi.getTravelRecommendation({location, budget, duration})`。

### 2.3 新增 `routeApi.applyOptimization` 与 `routeApi.getOptimizationHistory`

在 `frontend/src/api/route.api.ts` 的 `routeApi` 对象内追加（指向后端 RouteOptimizationController 已存在的端点）：

```ts
applyOptimization(data: { routeId: number; suggestion?: Record<string, any> }) {
    return apiClient.post<boolean>('/route-optimization/apply', data);
},
getOptimizationHistory(routeId: number) {
    return apiClient.get<Record<string, any>[]>(`/route-optimization/history/${routeId}`);
},
```

### 2.4 `applyOptimization` 与 `handleOptimizationHistory` 调用新 API

App.vue 第853-863行 `applyOptimization`：在本地 push 历史后追加调用 `routeApi.applyOptimization({routeId: activeRouteForAdjustment.value.id, suggestion: {type, title, impact}})` 持久化。

App.vue 第1445-1447行 `handleOptimizationHistory`：改为 async，调用 `routeApi.getOptimizationHistory(routeId)`（routeId 取 `activeRouteForAdjustment.value?.id || selectedRecommendation.value?.id`），将返回 map 到 `optimizationHistory.value`。空则保留硬编码兜底。

### 2.5 `openAnalytics` 加载当前用户真实统计

App.vue 第845-847行，改为 async，调用 `userStatsApi.getCurrentUserStats()` 更新 `currentUser.value.stats = {notes, collections, shares}`。失败则保留硬编码。

### 阶段 2 验证

- 点击笔记"评论" → `GET /api/route-comments/route/{id}` 200，弹窗显示服务端评论
- 在路线调整弹窗中点击"应用优化" → `POST /api/route-optimization/apply` 200
- 点击"优化历史" → `GET /api/route-optimization/history/{routeId}` 200
- 点击导航栏"分析" → `GET /api/v1/user/stats` 200，统计数字刷新

## 阶段 3：处理无数据源的状态变量

### 3.1 `satisfactionPrediction` 不再强制置 0

App.vue 第1224行，删除 `satisfactionPrediction.value = 0;` 或改为 `= null`。后端 `/ai/itinerary/generate` 不返回 satisfaction 字段，强制设 0 会让 UI 显示误导性的"0%"。改为 null 后 UI 自然隐藏（v-if 条件为 false）。

### 3.2 `chatHistory` 加 localStorage 持久化

后端无 chat history 端点（任务要求不新建后端 API），用 localStorage 持久化最近 50 条：
- App.vue `sendAIMessage`（第1058行附近）catch 块之后追加 `localStorage.setItem('chatHistory', JSON.stringify(chatHistory.value.slice(-50)))`，外层 try/catch 吞 QuotaExceededError
- App.vue `onMounted`（第1449行附近）首行追加 `const saved = localStorage.getItem('chatHistory'); if (saved) chatHistory.value = JSON.parse(saved)`，外层 try/catch 吞解析错误

### 3.3 `platformOverview` 保持静态 + 加演示提示

后端 TravelAnalyticsController 已删除，无平台级聚合端点。`RouteStatisticsService` 只有 per-route/per-user 统计，无法生成 totalUsers/activeRoutes 等。在 Analytics Modal 标题区（App.vue 第3480行附近）追加 `<p class="text-[10px] text-emerald-100/80 mt-1">* 平台级数据为演示用途，个人统计已实时加载</p>`。

### 阶段 3 验证

- 点击"智能生成行程" → 满意度预测 UI 块不出现（不再显示"0%"）
- 与 AI 聊几轮后刷新页面 → 历史对话保留
- 打开 Analytics Modal → 顶部出现"演示用途"提示

## 阶段 4：边缘情况

### 4.1 修复 401 跳转目标

`frontend/src/utils/api.ts` 第41行：`window.location.href = '/login'` → `window.location.href = '/'`。项目无 `/login` 路由（SPA 登录靠 `showAuthModal`），改为 `/` 更干净。

## 关键文件清单（按修改频次排序）

- `frontend/src/api/route.api.ts`（阶段1：修复 intelligentRouteApi 10 路径 + routeApi.optimizeRoute；阶段2：追加 applyOptimization/getOptimizationHistory）
- `frontend/src/api/collection.api.ts`（阶段1：修复第二套 6 路径）
- `frontend/src/api/ai.api.ts`（阶段1：修复 advancedChatbot/smartAssistant 2 路径）
- `frontend/src/App.vue`（阶段2：openComments/handleGenerateGuide/handleEstimateBudget/applyOptimization/handleOptimizationHistory/openAnalytics；阶段3：satisfactionPrediction/chatHistory/platformOverview）
- `frontend/src/utils/api.ts`（阶段4：401 跳转）

## 风险点

1. **字段映射不一致**：`commentApi.getRouteComments` 返回的 RouteComment 字段（userId/content/likeCount）与 App.vue 内部 commentList 项结构（user/text/likes）不同。步骤 2.1 已显式 map 字段。
2. **空数据兜底**：`handleOptimizationHistory` 中 `activeRouteForAdjustment.value?.id` 可能为空。fallback 到 `selectedRecommendation.value?.id`，仍为空则保留硬编码假数据。
3. **localStorage 上限**：约 5MB，50 条聊天记录含长 AI 回复可能逼近上限。步骤 3.2 用 `slice(-50)` + try/catch 吞 QuotaExceededError。
4. **routeApi.optimizeRoute 忽略 optimizationType**：后端无此参数支持，行为一致即可。
5. **不删除 collectionApi 第二套**：避免引入风险，保持调用兼容；后续可单独清理。

## 验证清单

- [ ] 登录/注册/登出/改密 全链路 200
- [ ] 收藏/取消收藏/分类加载 200（阶段1）
- [ ] 路线 CRUD（创建/复制/删除/可见性/我的路线）200
- [ ] AI 对话/翻译/安全建议 200（阶段1）
- [ ] 智能路线生成/主题路线/个性化路线 200（阶段1）
- [ ] 笔记发布/列表/删除 200
- [ ] 评论加载/发布 200（阶段2）
- [ ] 分享码生成/验证 200
- [ ] 优化建议应用 + 历史记录 200（阶段2）
- [ ] 用户统计 200（阶段2）
- [ ] 无 Console 404 错误
