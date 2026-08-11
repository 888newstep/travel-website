# 前端待修正清单与字段映射

> 本文档定位：把前面接口清单里“实际在用且后端缺失/不匹配”的问题，收敛成可直接执行的修正项。
>
> 关联文档：`FRONTEND_DESIGN_PLAN.md`、`API_INTEGRATION_CHECKLIST.md`
>
> 状态：前端侧已修正（见 2 节 ✅ 标注）；剩余待办集中在后端补接口。

## ⚠️ 当前进度
- ✅ 前端调用已统一到后端已有接口。
- ✅ 后端已补 `GET /attractions/{id}/nearby`、`POST /attractions/{id}/review`。
## 1. 结论先行

- 前后端主链路大多已对齐。
- 真正需要在“当前代码”里立即修正的，是景点附近/评论、路线智能推荐、实时交通、路线优化建议这几处。
- 其它不一致项多为“定义了但当前页面未使用”的预留接口，可以按需处理，不必一次改完。

## 2. 必须在当前代码修正的项

### 2.1 景点附近与景点评论（✅ 前后端均已补齐）
- 前端调用：`src/pages/AttractionsPage.tsx:261`、`src/pages/AttractionsPage.tsx:317` 调用 `attractionApi.getAttractionNearby`。
- 前端定义：`src/api/attraction.api.ts:82` 请求 `GET /attractions/{id}/nearby`。
- 后端现状：`AttractionController` 未提供 `/nearby` 与 `/review` 接口。
- 建议：优先在后端补 `GET /attractions/{id}/nearby` 与 `POST /attractions/{id}/review`；若短期内无法补，则前端先降级为“隐藏附近景点/评论提交入口”，避免每次请求都报错。

### 2.2 路线智能推荐
- 前端调用：`src/pages/RoutesPage.tsx:160` 调用 `intelligentRouteApi.getPopularRoutes`。
- 前端定义：`src/api/route.api.ts:43` 请求 `GET /routes/smart/popular`。
- 后端现状：`RouteController` 提供 `/smart/list`、`/smart/similar/{routeId}`，但未提供 `/smart/popular`。
- 建议：优先改前端调用 `GET /routes/smart/list`，并传 `type=popular` 等参数；若后端语义不同，再补 `/smart/popular`。

### 2.3 实时交通信息（✅ 前端已复用实时状态接口）
- 前端调用：`src/pages/RealtimeStatusPage.tsx:274` 调用 `realtimeApi.getTrafficInfo`。
- 前端定义：`src/api/realtime.api.ts:62` 请求 `GET /realtime-status/traffic/{id}`。
- 后端现状：`RealtimeStatusController` 提供 `POST /traffic-update`、`POST /traffic-batch`，但未提供 `GET /traffic/{id}`。
- 建议：优先在后端补一个“查询单景点交通信息”的 GET 接口；或前端改用 `GET /realtime-status/attraction/{id}` 聚合展示。

### 2.4 路线优化建议
- 前端调用：`src/pages/RouteOptimizationPage.tsx:64` 调用 `intelligentRouteApi.getOptimizationSuggestionsForRoute`。
- 前端定义：`src/api/route.api.ts:91` 请求 `GET /route-optimization/suggestions/{routeId}`。
- 后端现状：`RouteOptimizationController` 提供 `GET /suggestions/{routeId}`，路径已经是 `/route-optimization/suggestions/{routeId}`。
- 建议：此项路径已对齐，无需修改；只需确认 `RouteOptimizationPage.tsx:64` 是否真的能拿到数据。

## 3. 定义但当前未使用的预留接口

这些接口在 `src/api/*` 中有定义，但当前页面没有调用。可以按业务优先级逐步清理，不建议一次性删除。

| 前端接口 | 状态 | 建议 |
|---|---|---|
| `collectionApi.collectRoute` / `uncollectRoute` | 未使用 | 保留 `toggleCollection`，旧方法可删 |
| `collectionApi.addCollection` / `removeCollection` / `updateCollectionNote` | 未使用 | 统一收敛到 toggle 与 notes 系列 |
| `commentApi.likeComment` / `unlikeComment` | 未使用 | 保留 `toggleLikeComment` |
| `noteApi.likeNote` / `unlikeNote` / `collectNote` / `uncollectNote` | 未使用 | 保留 `toggleLikeNote` / `toggleCollectNote` |
| `intelligentRouteApi.getSeasonalRoutes` / `getThemeRoutes` | 未使用 | 确认是否走 `/smart/list` 参数 |
| `intelligentRouteApi.getOptimizationSuggestions` | 未使用 | 与 `/route-optimization/suggestions/{routeId}` 重复 |

## 4. 核心对象字段映射

### 4.1 用户 `User`
| 前端字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 用户 ID |
| `username` | string | 用户名 |
| `phone` | string | 手机号 |
| `avatar` | string | 头像 URL |
| `role` | 'admin' \| 'user' | 角色 |
| `stats.notes` | number | 游记数 |
| `stats.collections` | number | 收藏数 |
| `stats.shares` | number | 分享数 |

### 4.2 景点 `Attraction`
| 前端字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 景点 ID |
| `name` | string | 名称 |
| `cityId` | number | 城市 ID |
| `description` | string | 描述 |
| `address` | string | 地址 |
| `rating` | number | 评分 |
| `price` | number | 价格 |
| `images` | string[] | 图片列表 |
| `openingHours` | string | 开放时间 |
| `contactInfo` | string | 联系方式 |
| `tags` | string[] | 标签 |

### 4.3 路线 `Route`
| 前端字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 路线 ID |
| `title` | string | 标题 |
| `description` | string | 描述 |
| `cityId` | number | 城市 ID |
| `durationDays` | number | 行程天数 |
| `difficulty` | string | 难度 |
| `coverImage` | string | 封面图 |
| `userId` | number | 创建人 |
| `viewCount` | number | 浏览量 |
| `likeCount` | number | 点赞数 |
| `isPublic` | boolean | 是否公开 |
| `createdAt` / `updatedAt` | string | 时间 |

### 4.4 收藏 `RouteCollection`
| 前端字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 收藏记录 ID |
| `userId` | number | 用户 ID |
| `routeId` | number | 路线 ID |
| `note` | string | 收藏备注 |
| `category` | string | 分类 |
| `isPublic` | boolean | 是否公开 |
| `createTime` | string | 创建时间 |

### 4.5 评论 `RouteComment`
| 前端字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 评论 ID |
| `routeId` | number | 路线 ID |
| `userId` | number | 用户 ID |
| `rating` | number | 评分 |
| `content` | string | 内容 |
| `images` | string[] | 图片 |
| `isAnonymous` | boolean | 是否匿名 |
| `replyTo` | number | 回复目标 |
| `likeCount` | number | 点赞数 |
| `createTime` | string | 创建时间 |

### 4.6 游记 `TravelNote`
| 前端字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 游记 ID |
| `title` | string | 标题 |
| `author` | string | 作者 |
| `image` | string | 封面 |
| `likes` | number | 点赞数 |
| `comments` | number | 评论数 |
| `excerpt` | string | 摘要 |
| `content` | string | 正文 |
| `isLiked` | boolean | 当前用户是否点赞 |
| `isCollected` | boolean | 当前用户是否收藏 |
| `createTime` | string | 创建时间 |

## 5. 字段映射风险点

- 时间字段前后端命名不一致：前端多用 `createTime`，但也用到 `createdAt` / `updatedAt`，联调时需统一。
- `id` 类型：后端多为 `Long`，前端定义为 `number`，在 JS 范围内通常没问题，但大 ID 需注意精度。
- 部分接口返回 `Map<String,Object>`，前端却用强类型接收，建议在页面层做一次字段归一化适配。

## 6. 修正顺序

1. 先修 2.1～2.4 中“实际在用且后端缺失”的接口。
2. 再清理“定义但未使用”的重复接口。
3. 最后统一时间字段与 `id` 类型约定。

## 7. 结论

- 当前真正要动手的接口不超过 4 组。
- 大多数“不一致”其实是预留接口未清理，不影响当前主链路。
- 建议按“先保主链路可用，再清理冗余定义”的顺序推进。

## 8. 完成情况
- ✅ 前端：`route.api.ts` 统一智能接口并映射 `routeId→id`；`realtime.api.ts` 交通复用实时状态接口；`attraction.api.ts` 恢复 nearby/review 真实调用。
- ✅ 后端：`AttractionController` 新增 `GET /attractions/{id}/nearby` 与 `POST /attractions/{id}/review`。
- ✅ nearby 语义升级：`GET /attractions/{id}/nearby` 现返回同城周边景点（按距离排序、排除自身），前端卡片展示距离；原“周边服务”模拟数据已弃用。
- ✅ 验证：前端 `npm run lint` 通过；后端 `mvn -pl attraction-service -am compile` 通过。
- ✅ 单测：新增 `AttractionDetailServiceImplTest`（点评落库、点评读取、周边景点排序），`mvn -pl attraction-service -am test` 通过（common 1 + attraction 3）。
- ⚠️ 联调提示：完整 curl 联调需拉起整套微服务栈（gateway 8090 / attraction 8092 / Nacos 8848），当前环境未启动，故以单测覆盖后端子逻辑。
- ✅ 后端评论已接入真实存储：新增 `attraction_review` 表 + `AttractionReview` 实体 + `AttractionReviewMapper`，`submitReview` 落库、`getAttractionReviews` 读库返回。
- ✅ 前端 `submitReview` 已通过 `userApi.getCurrentUser()` 获取当前用户并带上 `userId`，提交后重新拉取评论列表即时回显。
- ⚠️ 注意：数据库需执行 `common/src/main/resources/db/init_complete.sql` 中的 `attraction_review` 建表语句。