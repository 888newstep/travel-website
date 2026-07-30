# 前端 Mock 数据替换与事件绑定修复计划

## Context（背景）

用户要求"把前端的组件上的响应事件与后端相绑定"，并特别强调"注意前端使用的 mock 数据——如果后端已经实现具体的代码和数据库能力，请直接更换为后端的代码接口"。

经过对 `frontend/src/App.vue`（约 3600 行）的逐项排查，以及对后端 Controller 端点清单的交叉核对，确认了以下现状：

**已经正确接线的数据流（无需改动，仅列示确认）：**
- `travelNotes`：onMounted 调用 `noteApi.getNotes(0, 10)` 加载真实笔记（L1588）
- `resourceFiles` / `fileCategories`：onMounted 调用 `fileApi.getFileList()` / `fileCategoryApi.getCategoryList()`（L1531-1554）
- `PLANNED_ITINERARIES`：onMounted 调用 `routeCrudApi.getMyRoutes(userId)`（L1561-1576）
- `userCollections` / `notifications`：onMounted 调用 `loadUserCollections()` / `loadNotifications()`
- `optimizationSuggestions` / `optimizationHistory`：已绑定 `intelligentRouteApi.getOptimizationSuggestions` / `routeApi.getOptimizationHistory`（上一轮任务完成）

**真正需要替换的 Mock 数据 / 需要修复的事件绑定（本计划目标）：**

| # | 位置 | 当前状态 | 后端能力 | 处理方式 |
|---|---|---|---|---|
| 1 | `MOCK_SPOTS`（L686-702）+ `filteredSpots` computed（L704-709）+ `handleSelectDestination`（L1460） | 3 城市硬编码景点，地点搜索预览完全静态 | `AttractionController.searchAttractions(keyword)` `GET /attractions/search` | 改为调用 `attractionApi.searchAttractions` |
| 2 | `REALTIME_ALERTS`（L1012-1017） | 4 条硬编码实时预警（天气/人流/交通） | `realtimeApi.getActiveWarns()` `GET /realtime-status/warns` | onMounted 加载真实预警 |
| 3 | `travelNotes` 初始值（L711-741） | 2 条硬编码笔记作为初始值 | 已有 `noteApi.getNotes` 加载 | 清空为 `[]`，避免 API 失败时显示陈旧假数据 |
| 4 | 笔记收藏按钮事件（模板 L2336） | `@click="toggleCollection(note)"` 错误调用**路线**收藏 API（`collectionApi.addCollection({routeId})`） | 笔记收藏应用 `noteApi.collectNote` / `uncollectNote`（已有 `collectNote` 函数 L764） | 改为 `@click="collectNote(note)"` |

**评估后决定保留的 Mock 数据（后端无对应能力，或属于 UI 配置）：**
- `RECOMMENDATIONS`（L576）/ `DESTINATIONS`（L606）：编辑精选展示内容，后端无"精选目的地"端点（`getRecommendations` 需 cityId，不适合无城市上下文的首页展示）
- `collaborators`（L399）/ `tripTasks`（L405）：后端无协作/任务管理功能
- `SYSTEM_STATS`（L550）/ `SYSTEM_CONFIG`（L543）：系统监控与配置，后端无对应端点
- `platformOverview`（L417）：Analytics Controller 已删除，上一轮已加"演示用途"提示
- `TRAVEL_STYLES`（L557）：旅行风格选项，属于 UI 静态配置

## 实施步骤

### 步骤 1：替换 MOCK_SPOTS 为真实景点搜索

**文件**：`frontend/src/App.vue`

1.1 删除 `MOCK_SPOTS` 常量（L686-702）。

1.2 新增响应式变量 `searchedSpots`，并将 `filteredSpots` computed（L704-709）改为：
```ts
const searchedSpots = ref<any[]>([]);
let spotSearchTimer: any = null;

const filteredSpots = computed(() => searchedSpots.value);
```

1.3 改造地点搜索输入：监听 `placeSearchQuery` 变化，防抖 300ms 后调用 `attractionApi.searchAttractions(keyword)` 填充 `searchedSpots`。需在 `placeSearchQuery` 的 `watch` 中实现（若已有 input 事件 handler 则改造之，否则新增 watch）。每项 map 为 `{ name: a.name, image: a.images?.[0] || a.coverImage || <默认图>, description: a.description }`。

1.4 `handleSelectDestination`（L1460）无需改动——它从 `filteredSpots` 取项，数据源切换后自动生效。

### 步骤 2：加载真实实时预警

**文件**：`frontend/src/App.vue`

2.1 `REALTIME_ALERTS`（L1012）保留初始值但清空为空数组 `ref([])`（避免硬编码假预警误导）。

2.2 在 onMounted 中（token 校验之后，与 notes 加载同级）追加加载逻辑：
```ts
try {
  const { realtimeApi } = await import('./api/realtime.api');
  const warns: any = await realtimeApi.getActiveWarns();
  if (Array.isArray(warns) && warns.length > 0) {
    REALTIME_ALERTS.value = warns.map((w: any) => ({
      id: w.id,
      type: w.type || 'status',
      title: w.title || w.warnTitle || '实时预警',
      description: w.description || w.content || '',
      severity: w.severity || 'medium',
      category: w.category || w.type || 'system',
    }));
  }
} catch (e) {
  console.warn('加载实时预警失败:', e);
}
```

2.3 `syncAllData`（L1019）的本地 unshift 假数据逻辑保留（同步成功提示），不冲突。

### 步骤 3：清空 travelNotes 初始硬编码

**文件**：`frontend/src/App.vue`

3.1 将 `travelNotes` 的初始值（L711-741）从 2 条硬编码笔记改为空数组：
```ts
const travelNotes = ref<any[]>([]);
```

**理由**：onMounted 已调用 `noteApi.getNotes` 加载真实数据；保留硬编码会在 API 慢或失败时显示陈旧假数据，且 `hotNotes` computed 会基于假数据排序。空数组 + API 加载是更诚实的默认。

### 步骤 4：修复笔记收藏事件绑定

**文件**：`frontend/src/App.vue`

4.1 模板 L2336 附近，笔记卡片的收藏按钮：
```html
<button @click="toggleCollection(note)" ...>
```
改为：
```html
<button @click="collectNote(note)" ...>
```

**理由**：`toggleCollection`（L1136）调用 `collectionApi.addCollection({routeId: item.id})`——这是**路线收藏** API，把笔记 id 当 routeId 传会导致后端写入错误数据。`collectNote`（L764）正确调用 `noteApi.collectNote(note.id)`，对应后端 `TravelNoteController` 的 `POST /travel-notes/{noteId}/collect`。

## 关键文件清单

- `frontend/src/App.vue`（全部 4 个步骤均涉及此文件）
- `frontend/src/api/attraction.api.ts`（步骤 1 引用，已存在 `searchAttractions` 方法，无需修改）

## 风险点

1. **景点搜索结果字段映射**：`Attraction` 实体的 `images` 字段可能是 JSON 数组或逗号分隔字符串。步骤 1.3 的 map 需做兼容处理（`Array.isArray(a.images) ? a.images[0] : a.images?.split(',')[0]`）。
2. **getActiveWarns 返回结构未知**：步骤 2.2 的 map 对多个字段名做 fallback（title/warnTitle、description/content），降低字段不匹配风险。
3. **空 travelNotes 的 UI 兜底**：清空初始值后，若 API 返回空，笔记列表区域会显示空。需确认模板有空状态提示（若有 `v-if="travelNotes.length === 0"` 的空状态则无需改；若无，仅显示空列表不影响功能）。
4. **地点搜索防抖**：步骤 1.3 用 `watch` + `setTimeout` 实现防抖，避免每次按键都调 API。

## 验证步骤

1. `npm run lint`（vue-tsc --noEmit）类型检查通过
2. 启动前后端，浏览器 DevTools Network 验证：
   - 地点搜索框输入"京都" → `GET /api/attractions/search?keyword=京都` 200，下拉显示真实景点
   - 页面加载 → `GET /api/realtime-status/warns` 200，预警栏显示真实预警（或为空）
   - 页面加载 → `GET /api/travel-notes/list` 200，笔记列表显示真实笔记
   - 点击笔记收藏星标 → `POST /api/travel-notes/{noteId}/collect` 200（而非 `/v1/route-collections/collect`）
3. 确认无 Console 404 / 类型错误
