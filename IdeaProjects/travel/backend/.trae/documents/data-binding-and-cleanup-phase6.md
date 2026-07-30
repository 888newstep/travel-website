# 前后端数据流绑定 - 第六轮：Mock 数据全面替换 + 冗余代码清理

## Context

经过前五轮工作，前后端核心数据流（登录、路线CRUD、收藏、评论、AI聊天、文件管理）已连通。本轮聚焦：**全面替换 App.vue 中剩余的硬编码 Mock 数据为后端 API 调用**，并创建缺失的前端 API 文件。

## 当前状态分析

### 已绑定到后端 API 的功能 ✓
- 用户登录/注册/登出 → `userApi`
- 路线CRUD → `routeCrudApi`
- 收藏管理 → `collectionApi`
- 评论管理 → `commentApi`
- 笔记管理 → `noteApi`
- 分享 → `shareApi`
- AI聊天 → `aiApi`
- 文件管理 → `fileApi`（onMounted 中加载）
- 景点搜索 → `attractionApi.searchAttractions()`（watch 中）
- 实时预警 → `realtimeApi.getActiveWarns()`（onMounted 中）
- 优化历史 → `routeApi.getOptimizationHistory()`

### 仍需替换的 Mock 数据（按优先级排序）

| # | 变量 | 位置 | 当前状态 | 替换方案 |
|---|------|------|----------|----------|
| 1 | `RECOMMENDATIONS` | App.vue L573-601 | 3条硬编码（**非响应式 const**） | 改为 `ref([])`，onMounted 调 `attractionApi.getRecommendations()` |
| 2 | `DESTINATIONS` | App.vue L603-636 | 4条硬编码（**非响应式 const**） | 改为 `ref([])`，onMounted 调 `attractionApi.getAttractions()` |
| 3 | `optimizationSuggestions` | App.vue L411-414 | 3条硬编码 | 已有 `ref([])` 声明，需在 `openCollaboration` 或 `openAnalytics` 中加载真实数据，调用 `routeApi.getOptimizationSuggestions()` |
| 4 | `collaborators` | App.vue L399-403 | 3条硬编码 | 已有 `ref([])` 声明，需创建 `trip-collaboration.api.ts`，在 `openCollaboration` 中加载 |
| 5 | `tripTasks` | App.vue L405-409 | 3条硬编码 | 已有 `ref([])` 声明，需在 `openCollaboration` 中加载 |
| 6 | `platformOverview` | App.vue L417-422 | 5个硬编码字段 | 已有 `ref()` 声明，改为在 `openAnalytics` 中加载用户统计 |
| 7 | `ATTRACTION_REALTIME_DATA` | App.vue L496-509 | 3条硬编码初始数据 | 改为空对象 `{}`，按需调用 `loadRealtimeStatus` |
| 8 | `resourceFiles` | App.vue L447-451 | 3条硬编码初始数据 | 改为空数组 `[]`，onMounted 中已从 API 加载 |
| 9 | `fileCategories` | App.vue L453-458 | 4条硬编码初始数据 | 改为空数组 `[]`，onMounted 中已从 API 加载 |
| 10 | `SYSTEM_STATS` | App.vue L547-552 | 硬编码系统统计 | 保留（后端无对应端点，属于前端展示配置） |
| 11 | `SYSTEM_CONFIG` | App.vue L540-545 | 静态配置 | 保留（属于前端配置常量） |
| 12 | `TRAVEL_STYLES` | App.vue L554-556 | 静态列表 | 保留（属于前端常量） |

### 需要新建的前端 API 文件

- **`trip-collaboration.api.ts`** - 对应后端 `TripCollaborationController`（`/trip-collaboration`），提供协作者、任务管理接口

---

## 实施步骤

### 步骤1：将 RECOMMENDATIONS 和 DESTINATIONS 改为响应式 ref 并绑定 API

**文件：** `frontend/src/App.vue`

**问题：** `RECOMMENDATIONS` 和 `DESTINATIONS` 是 `const` 常量数组，无法响应式更新。

**修改：**
```typescript
// 替换 L573-636 的硬编码数组为：
const RECOMMENDATIONS = ref<any[]>([]);
const DESTINATIONS = ref<any[]>([]);
```

在 `onMounted` 末尾添加数据加载逻辑：
```typescript
// 加载景点推荐数据
try {
  const { attractionApi } = await import('./api/attraction.api');
  const result: any = await attractionApi.getAttractions();
  const list = Array.isArray(result) ? result : (result?.records || []);
  if (list.length > 0) {
    RECOMMENDATIONS.value = list.slice(0, 6).map((a: any) => ({
      id: a.id,
      title: a.name,
      location: a.address || a.cityName || '未知地点',
      image: (Array.isArray(a.images) ? a.images[0] : a.coverImage) || 'https://picsum.photos/seed/rec/800/600',
      tags: a.tags || (a.category ? [a.category] : ['推荐']),
      description: a.description || '',
      highlights: a.highlights || ['深度旅行体验', '当地人文风情'],
    }));
    DESTINATIONS.value = list.slice(0, 8).map((a: any) => ({
      id: a.id,
      name: a.name,
      image: (Array.isArray(a.images) ? a.images[0] : a.coverImage) || 'https://picsum.photos/seed/dest/800/600',
      rating: a.rating || 4.5,
      price: a.price ? `¥${a.price.toLocaleString()}` : '待定',
      category: a.category || (a.tags && a.tags.length > 0 ? a.tags[0] : '推荐'),
    }));
  }
} catch (e) {
  console.warn('加载景点推荐失败:', e);
}
```

模板中已有的 `v-for="item in RECOMMENDATIONS"` 和 `v-for="dest in DESTINATIONS"` 不需要修改。

### 步骤2：创建 trip-collaboration.api.ts

**新建文件：** `frontend/src/api/trip-collaboration.api.ts`

**对应后端：** `TripCollaborationController`（`/trip-collaboration`）

```typescript
import apiClient from '../utils/api';

export const tripCollaborationApi = {
  getCollaborators(tripId: number) {
    return apiClient.get(`/trip-collaboration/${tripId}/collaborators`);
  },
  getTasks(tripId: number, userId: number) {
    return apiClient.get(`/trip-collaboration/${tripId}/tasks`, {
      params: { userId },
    });
  },
  completeTask(tripId: number, userId: number, taskId: number) {
    return apiClient.post(`/trip-collaboration/${tripId}/task/${taskId}/complete`, null, {
      params: { userId },
    });
  },
  assignTask(tripId: number, assignerId: number, assigneeId: number, taskDescription: string) {
    return apiClient.post(`/trip-collaboration/${tripId}/task/assign`, { assigneeId, taskDescription }, {
      params: { assignerId },
    });
  },
  addComment(tripId: number, userId: number, content: string, targetType?: string, targetId?: number) {
    return apiClient.post(`/trip-collaboration/${tripId}/comment`, { content, targetType, targetId }, {
      params: { userId },
    });
  },
  getComments(tripId: number, targetType: string, targetId: number) {
    return apiClient.get(`/trip-collaboration/${tripId}/comments`, {
      params: { targetType, targetId },
    });
  },
};
```

### 步骤3：替换 collaborators 和 tripTasks Mock 数据

**文件：** `frontend/src/App.vue`

**修改 `openCollaboration` 函数（L773附近）：**

```typescript
const openCollaboration = async (itinerary: any) => {
  activeTripForCollaboration.value = itinerary;
  showCollaborationModal.value = true;
  
  const tripId = itinerary.id;
  const userId = currentUser.value?.id;
  if (!tripId || !userId) return;
  
  try {
    const { tripCollaborationApi } = await import('./api/trip-collaboration.api');
    const [collaboratorList, taskList]: any[] = await Promise.all([
      tripCollaborationApi.getCollaborators(tripId),
      tripCollaborationApi.getTasks(tripId, userId),
    ]);
    
    if (Array.isArray(collaboratorList)) {
      collaborators.value = collaboratorList.map((c: any) => ({
        id: c.userId || c.id,
        name: c.username || c.name || '未知用户',
        avatar: c.avatar || `https://picsum.photos/seed/user${c.userId || c.id}/100/100`,
        role: c.role || 'viewer',
        isOnline: c.isOnline || false,
      }));
    }
    
    if (Array.isArray(taskList)) {
      tripTasks.value = taskList.map((t: any) => ({
        id: t.id,
        description: t.description || t.taskDescription || '',
        assigneeId: t.assigneeId || t.userId,
        isCompleted: t.isCompleted || t.completed || false,
      }));
    }
  } catch (e) {
    console.warn('加载协作数据失败:', e);
  }
};
```

### 步骤4：替换 optimizationSuggestions Mock 数据

**文件：** `frontend/src/App.vue`

**修改 `openAnalytics` 函数（L778附近）：**

在 `openAnalytics` 中加载真实优化建议：
```typescript
const openAnalytics = async () => {
  showAnalyticsModal.value = true;
  const routeId = activeRouteForAdjustment.value?.id || selectedRecommendation.value?.id;
  if (!routeId) return;
  
  try {
    const { routeApi } = await import('./api/route.api');
    const suggestions: any = await routeApi.getOptimizationSuggestions(routeId);
    // routeApi.getOptimizationSuggestions 调用 GET /route-optimization/suggestions/{routeId}
    const list = Array.isArray(suggestions) ? suggestions : (suggestions?.records || []);
    if (list.length > 0) {
      optimizationSuggestions.value = list.map((s: any, idx: number) => ({
        id: s.id ?? idx + 1,
        type: s.type || s.optimizationType || 'comprehensive',
        title: s.title || s.optimizationType || '优化建议',
        description: s.description || s.suggestion || '',
        impact: s.impact || s.expectedImprovement || '',
      }));
    }
  } catch (e) {
    console.warn('加载优化建议失败:', e);
  }
};
```

### 步骤5：替换 platformOverview Mock 数据

**文件：** `frontend/src/App.vue`

**修改 `openAnalytics` 函数，追加平台统计加载：**
```typescript
// 在 openAnalytics 中追加
try {
  const { userStatsApi } = await import('./api/user-stats.api');
  const stats: any = await userStatsApi.getUserStats();
  if (stats) {
    platformOverview.value = {
      totalUsers: stats.totalUsers || platformOverview.value.totalUsers,
      activeRoutes: stats.activeRoutes || platformOverview.value.activeRoutes,
      completedTrips: stats.completedTrips || platformOverview.value.completedTrips,
      avgSatisfaction: stats.avgSatisfaction || platformOverview.value.avgSatisfaction,
    };
  }
} catch (e) {
  console.warn('加载平台统计失败:', e);
}
```

### 步骤6：清理 Mock 初始值

**文件：** `frontend/src/App.vue`

1. `ATTRACTION_REALTIME_DATA`（L496-509）：保留 `ref<Record<...>>({})` 声明，删除硬编码的 3 条数据
2. `resourceFiles`（L447-451）：改为 `ref<ResourceFile[]>([])`，删除硬编码的 3 条数据
3. `fileCategories`（L453-458）：改为 `ref<FileTag[]>([])`，删除硬编码的 4 条数据
4. `optimizationSuggestions`（L411-414）：改为 `ref<OptimizationSuggestion[]>([])`，删除硬编码的 3 条数据
5. `collaborators`（L399-403）：改为 `ref<Collaborator[]>([])`，删除硬编码的 3 条数据
6. `tripTasks`（L405-409）：改为 `ref<TripTask[]>([])`，删除硬编码的 3 条数据

### 步骤7：添加缺失的 API 方法

**文件：** `frontend/src/api/route.api.ts`

添加 `getOptimizationSuggestions` 方法（调用 `GET /route-optimization/suggestions/{routeId}`）：
```typescript
// 在 routeApi 中添加
getOptimizationSuggestions(routeId: number) {
  return apiClient.get(`/route-optimization/suggestions/${routeId}`);
},
```

### 步骤8：验证

1. `cd frontend && npm run lint`（vue-tsc --noEmit）通过
2. `cd backend && mvn compile` 通过
3. 联调验证：
   - 首页展示真实景点数据（RECOMMENDATIONS、DESTINATIONS）
   - 协作弹窗加载真实协作者和任务
   - 优化建议加载真实数据
   - 平台统计加载真实数据

---

## 注意事项

1. **不可修改路由文件**（项目约束）
2. **保留 `SYSTEM_CONFIG`、`SYSTEM_STATS`、`TRAVEL_STYLES`** - 属于前端配置常量，后端无对应端点
3. **`resourceFiles` 和 `fileCategories` 已在 onMounted 中从 API 加载**，只需清理初始 Mock 值
4. **所有 API 调用使用 try-catch 包裹**，失败时保留空数组兜底，不影响 UI 展示
5. **模板中的 `v-for` 绑定不需要修改**，因为改为 `ref` 后 `.value` 会自动解包