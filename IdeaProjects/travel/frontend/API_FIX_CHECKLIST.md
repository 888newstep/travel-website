# 前端待修正清单与字段映射

> 本文档定位：把前面接口清单里“实际在用且后端缺失/不匹配”的问题，收敛成可直接执行的修正项。
>
> 关联文档：`FRONTEND_DESIGN_PLAN.md`、`API_INTEGRATION_CHECKLIST.md`
>
> 状态：主展示链路已收敛；不存在的数据能力已下线，不再用模拟接口伪装。

## 当前进度
- 前端调用已统一到后端真实存在的接口，失效别名已删除。
- 景点附近、景点评价、智能路线、路线优化和收藏主链路已对齐。
- 实时页只展示景点当前状态；路线交通只使用项目配置的高德 API。
- 游记评论和历史客流没有明细数据源，相关入口已移除或明确返回不可用。

## 1. 结论先行

- 当前展示所需的前后端主链路已对齐。
- 写操作身份统一从 JWT 获取，前端不再提交可伪造的 `userId`。
- 收藏切换使用同一把分布式锁、数据库唯一约束和缓存失效；带 `Idempotency-Key` 的重试由服务端重放首次结果。
- 不使用 Milvus，不提供短信计费、余额或账单功能。

## 2. 必须在当前代码修正的项

### 2.1 景点附近与景点评价（已完成）
- `GET /attractions/{id}/nearby` 返回同城周边景点，并排除当前景点。
- `POST /attractions/{id}/review` 将评价写入 `attraction_review`，用户身份取 JWT。
- 前端提交后重新读取评价列表，不再传递 `userId`。

### 2.2 路线智能推荐（已完成）
- 热门、季节和主题路线统一请求 `GET /routes/smart/list`。
- `type=popular|seasonal|theme` 决定推荐类型，前端统一把 `routeId` 映射为 `id`。
- 不再请求不存在的 `/routes/smart/popular`。

### 2.3 实时状态与交通（已完成）
- 实时页只请求 `/realtime-status/*` 的景点状态，不把景点人流等级解释成道路交通。
- 历史平均和近 7 天统计因没有历史明细表，后端返回 HTTP 503 与错误码 `20007`。
- 路线分段交通由 `AMapRouteService` 调用项目配置的高德 API；没有 API 数据时明确降级，不生成虚假实时交通。

### 2.4 路线优化建议（已完成）
- 前后端统一使用 `GET /route-optimization/suggestions/{routeId}`。
- 后端读取建议前校验当前用户是否为路线所有者。
- 前端删除重复客户端方法，只保留一个强制走真实后端的调用入口。

## 3. 已清理的重复接口

以下旧定义已删除或统一，不再保留必然失败的兼容调用。

| 接口范围 | 状态 | 当前入口 |
|---|---|---|
| 收藏 `collect/uncollect/add/update-note` | 已删除 | `toggle`、`remove`、资源 ID 路径 |
| 评论 `like/unlike` | 已删除 | `toggle-like` |
| 游记 `like/unlike/collect/uncollect/comments` | 已删除 | `toggle-like`、`toggle-collect` |
| 智能路线热门/季节/主题别名 | 已统一 | `/routes/smart/list` + `type` |
| 路线优化建议重复方法 | 已删除 | `/route-optimization/suggestions/{routeId}` |
| UI 字典 `/dictionary/*` | 已删除 | 版本控制的静态能力清单 |

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
| `notes` | string | 收藏备注 |
| `category` | string | 分类 |
| `isPublic` | boolean | 是否公开 |
| `collectionTime` | string | 收藏时间 |
| `routeTitle` | string | 路线标题 |
| `routeCoverImage` | string | 路线封面 |
| `routeDurationDays` | number | 路线天数 |
| `routeDifficulty` | string | 路线难度 |

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

1. 主链路接口对齐：已完成。
2. 重复和虚假能力清理：已完成。
3. JWT 身份边界、收藏幂等和缓存一致性：已完成。
4. 真实 MySQL、Redis、RabbitMQ 与高德 API 联调：部署环境执行。

## 7. 结论

- 当前代码只展示有真实数据来源或明确降级语义的能力。
- MySQL 保存业务事实，Redis 承担缓存、分布式锁与 HTTP/MQ 幂等状态，RabbitMQ 只用于已接入的异步通知链路。
- 当前项目边界不包含 Milvus、短信计费、余额和账单。

## 8. 完成情况
- 前端：智能路线、实时状态、收藏、游记和 AI 展示接口已收敛；已登录写请求自动附带 `Idempotency-Key`。
- 后端：景点评价使用真实表；收藏切换在同一分布式锁内判断和写入，并由 `uk_user_item_action` 唯一索引兜底。
- 数据：游记无评论明细时评论数归零；历史客流明确不可用；路线交通只使用高德 API。
- 部署：现有数据库需按需执行 `backend/docs/infrastructure` 下的迁移脚本。
- 联调：完整 HTTP 验证需要启动 Nacos、网关、对应微服务以及本地 MySQL/Redis；云 RabbitMQ 按环境变量连接。
