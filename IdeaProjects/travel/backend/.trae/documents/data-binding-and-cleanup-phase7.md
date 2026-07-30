# 前后端数据流绑定 - 第七轮：修复 Stub 事件处理器 + API 路径对齐 + 清理零引用 Mapper

## Context

经过前六轮工作，Mock 数据已全面替换为 API 调用。本轮聚焦三个遗留问题：
1. **Stub 事件处理器**：多个 UI 事件处理器仍使用 `alert()`/`confirm()`/`prompt()` 作为反馈，或仅做客户端计算而不使用后端返回的真实数据
2. **API 路径不匹配**：前端通知和反馈 API 调用路径与后端不匹配
3. **零引用后台 Mapper**：5 个 Mapper 接口没有任何代码引用

---

## 当前状态分析

### 已绑定到后端 API 的功能 ✓
- 用户登录/注册/登出 → `userApi`
- 路线CRUD → `routeCrudApi`
- 收藏管理 → `collectionApi`
- 评论管理 → `commentApi`
- 笔记管理 → `noteApi`
- 分享 → `shareApi`
- AI聊天 → `aiApi`
- 文件管理 → `fileApi`
- 景点推荐 → `attractionApi`
- 实时预警 → `realtimeApi`
- 协作管理 → `tripCollaborationApi`
- 优化建议/历史 → `routeApi`

### 问题清单

#### A. Stub/未完成的事件处理器

| # | 函数 | 位置 | 问题 | 修复方案 |
|---|------|------|------|----------|
| 1 | `handleViewMap` | L1302 | 只显示 alert("地图功能开发中") | 删除该函数，模板改为 `@click="null"` 或移除按钮 |
| 2 | `handleEstimateBudget` | L1272 | 调用 AI API 但不使用返回结果，用客户端 `budget * duration` 伪造 | 使用 API 返回的真实预算数据 |
| 3 | `handleGenerateGuide` | L1258 | 调用 API 但不使用返回结果，硬编码 alert 消息 | 使用 API 返回的真实攻略数据 |
| 4 | `handleShowStats` | L1325 | 将 API 返回的 JSON 在 alert 中显示 | 输出到 console.log 并更新 UI 状态 |
| 5 | `selectItineraryPlan` | L1208 | 创建本地行程但不持久化到后端 | 调用 `routeApi.createRoutePlan()` 保存到后端 |
| 6 | `handleApplyOptimization` | L1381 | 使用 `confirm()` 和 `alert()` | 移除 confirm/alert，静默调用 `applyOptimization` |
| 7 | `handleDeleteFile` | L1356 | 使用 `confirm()` 和 `alert()` | 移除 confirm/alert，使用 console.log |
| 8 | `handleAddCategory` | L1306 | 使用 `prompt()` 和 `alert()` | 改为打开已有的 `showFileUploadModal` 或在 UI 中收集输入 |
| 9 | `handleCopyLink` | L1374 | 使用 `alert()` | 改为 console.log，用户操作已通过剪贴板完成 |
| 10 | `deleteNote` | L897 | 使用 `alert()` | 改为 console.log |

#### B. API 路径不匹配（精确对照）

| 前端调用 | 后端实际 | 差异 |
|----------|----------|------|
| `GET /users/notifications` | `GET /v1/notifications` | 路径前缀错误 |
| `PUT /users/notifications/{id}/read` | `PUT /v1/notifications/{id}/read` | 路径前缀错误 |
| `DELETE /users/notifications/{id}` | `DELETE /v1/notifications/{id}` | 路径前缀错误 |
| `GET /users/notifications/unread-count` | `GET /v1/notifications/unread-count` | 路径前缀错误 |
| `POST /users/notifications/mark-all-read` | `PUT /v1/notifications/read-all` | 路径前缀 + HTTP 方法 + 路径全部错误 |
| `POST /users/feedback` | `POST /feedback/submit` | 路径前缀 + 路径错误 |
| `GET /users/feedback/list` | `GET /feedback/list/{userId}` | 路径前缀 + 缺少 userId 路径参数 |

#### C. 零引用后端 Mapper（可安全删除）

| # | 文件 | 路径 |
|---|------|------|
| 1 | `WeatherDataMapper.java` | `mapper/travel_realtime_mapper/` |
| 2 | `TrafficDataMapper.java` | `mapper/travel_realtime_mapper/` |
| 3 | `FileCommentMapper.java` | `mapper/user_community_mapper/` |
| 4 | `RouteTransportMapper.java` | `mapper/route_planning_mapper/` |
| 5 | `RouteCollectionMapper.java` | `mapper/route_planning_mapper/` |

---

## 实施步骤

### 步骤1：修复 API 路径不匹配

**文件：** `frontend/src/api/notification-feedback.api.ts`

精确修改（对照后端 `NotificationController` 和 `FeedbackController`）：

```typescript
// notificationApi - 修改前 → 修改后
getNotifications:    '/users/notifications'              → '/v1/notifications'
markAsRead:          '/users/notifications/${id}/read'   → '/v1/notifications/${id}/read'
deleteNotification:  '/users/notifications/${id}'        → '/v1/notifications/${id}'
getUnreadCount:      '/users/notifications/unread-count' → '/v1/notifications/unread-count'
markAllAsRead:  POST '/users/notifications/mark-all-read' → PUT '/v1/notifications/read-all'

// feedbackApi - 修改前 → 修改后
submitFeedback:  POST '/users/feedback'       → POST '/feedback/submit'
getFeedbackList: GET  '/users/feedback/list'  → GET  '/feedback/list/${userId}'
```

**注意：** `getFeedbackList` 现在需要 `userId` 参数，调用方需传入用户 ID。

### 步骤2：删除零引用 Mapper

**删除 5 个文件：**
- `mapper/travel_realtime_mapper/WeatherDataMapper.java`
- `mapper/travel_realtime_mapper/TrafficDataMapper.java`
- `mapper/user_community_mapper/FileCommentMapper.java`
- `mapper/route_planning_mapper/RouteTransportMapper.java`
- `mapper/route_planning_mapper/RouteCollectionMapper.java`

### 步骤3：修复 Stub 事件处理器

**文件：** `frontend/src/App.vue`

#### 3.1 `handleViewMap`（L1302-1304）
- 删除整个函数体
- 模板中移除或禁用对应的按钮

#### 3.2 `handleEstimateBudget`（L1272-1286）
- 使用 API 返回的真实数据替代客户端计算
- 移除 `alert()`，改为 console.log

#### 3.3 `handleGenerateGuide`（L1258-1269）
- 使用 API 返回的攻略数据
- 移除 `alert()`，改为 console.log

#### 3.4 `handleShowStats`（L1325-1334）
- 移除 `alert(JSON.stringify(...))`
- 改为 console.log

#### 3.5 `selectItineraryPlan`（L1208-1226）
- 添加 `routeApi.createRoutePlan()` 调用持久化到后端
- 移除 `alert('请先登录')`，改为 console.warn

#### 3.6 `handleApplyOptimization`（L1381-1392）
- 移除 `confirm()` 和 `alert()` 调用
- 直接调用 `applyOptimization(suggestion)`

#### 3.7 `handleDeleteFile`（L1356-1367）
- 移除 `confirm()` 和 `alert()` 调用
- 改为 console.log

#### 3.8 `handleAddCategory`（L1306-1323）
- 移除 `prompt()` 和 `alert()` 调用
- 改为 console.log

#### 3.9 `handleCopyLink`（L1374-1379）
- 移除 `alert()`，改为 console.log

#### 3.10 `deleteNote`（L897-900）
- 移除 `alert()`，改为 console.log

### 步骤4：模板清理

**文件：** `frontend/src/App.vue`

- 移除或禁用 `handleViewMap` 对应的按钮（搜索 "handleViewMap" 在模板中的引用）

### 步骤5：验证

1. `cd frontend && npm run lint` 通过
2. `cd backend && mvn compile` 通过
3. 检查所有 console.log 替代 alert 的行为正确

---

## 注意事项

1. **不可修改路由文件**（项目约束）
2. **保留 `confirm()` 用于删除等危险操作** - 仅 `handleDeleteFile` 和 `handleApplyOptimization` 的 confirm 移除，其余删除类操作（如 `deleteRoute`）保留 confirm
3. **`prompt()` 的 `handleAddCategory`** - 改为直接打开已有的文件上传弹窗即可，无需 prompt
4. **通知 API 路径修改** - 需同步检查 `NotificationController` 的完整端点路径，确保所有方法匹配