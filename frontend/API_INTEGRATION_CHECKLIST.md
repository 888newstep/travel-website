# 前后端接口对接清单

> 适用范围：`frontend` 与 `C:\Users\xiaohongfu\IdeaProjects\travel\backend`

## 1. 目的
- 明确前端 `src/api/*` 与后端 Controller 的映射关系。
- 标记已对齐接口、待确认接口、明显不一致接口。
- 作为联调和回归时的检查基线。

## 2. 全局约定
- 基础前缀：建议统一走网关 `VITE_API_BASE_URL=/api`。
- 认证：请求头统一 `Authorization: Bearer <token>`。
- 响应：后端统一 `Result<T>` 风格，前端只消费 `data`。
- 登录失效：401 时清理本地 token 并跳转登录页。

## 3. P0 主链路
### 3.1 登录与用户
| 前端接口 | 后端接口 | 状态 | 备注 |
|---|---|---|---|
| `POST /users/login` | `UserController#login` | 已对齐 | 返回 token |
| `POST /users/register` | `UserController#register` | 已对齐 | 注册后返回用户信息 |
| `GET /users/current` | `UserController#getCurrentUser` | 已对齐 | 当前用户信息 |
| `PUT /users/profile` | `UserController#updateProfile` | 已对齐 | 资料更新 |
| `POST /users/captcha` | `UserController#sendCaptcha` | 已对齐 | 手机验证码 |

### 3.2 景点与餐厅
| 前端接口 | 后端接口 | 状态 | 备注 |
|---|---|---|---|
| `GET /attractions` | `AttractionController#getAttractions` | 已对齐 | 景点列表 |
| `GET /attractions/{id}` | `AttractionController#getAttractionById` | 已对齐 | 景点详情 |
| `GET /attractions/city/{cityId}` | `AttractionController#getAttractionsByCity` | 已对齐 | 城市景点 |
| `GET /attractions/search` | `AttractionController#searchAttractions` | 已对齐 | 关键词搜索 |
| `GET /attractions/recommend` | `AttractionController#getRecommendations` | 已对齐 | 推荐列表 |
| `GET /restaurants/city/{cityId}` | `RestaurantController#getByCity` | 已对齐 | 餐厅列表 |
| `GET /restaurants/search` | `RestaurantController#search` | 已对齐 | 餐厅搜索 |

### 3.3 路线与优化
| 前端接口 | 后端接口 | 状态 | 备注 |
|---|---|---|---|
| `POST /routes` | `RouteController#createRoute` | 已对齐 | 新建路线 |
| `GET /routes/{id}` | `RouteController#getRoute` | 已对齐 | 路线详情 |
| `PUT /routes/{id}` | `RouteController#updateRoute` | 已对齐 | 路线更新 |
| `DELETE /routes/{id}` | `RouteController#deleteRoute` | 已对齐 | 需确认 `userId` 参数 |
| `GET /routes/my` | `RouteController#getMyRoutes` | 已对齐 | 我的路线 |
| `GET /routes/search` | `RouteController#searchRoutes` | 已对齐 | 标题搜索 |
| `GET /routes/city/{cityId}` | `RouteController#getRoutesByCity` | 已对齐 | 城市路线 |
| `GET /routes/count/{userId}` | `RouteController#getRouteCount` | 已对齐 | 数量统计 |
| `POST /routes/batch` | `RouteController#batchGetRoutes` | 已对齐 | 批量查询 |
| `POST /routes/{id}/copy` | `RouteController#copyRoute` | 已对齐 | 复制路线 |
| `PUT /routes/{id}/visibility` | `RouteController#setRouteVisibility` | 已对齐 | 可见性 |
| `POST /routes/smart/evaluate/{routeId}` | `RouteController#evaluateRouteQuality` | 已对齐 | 质量评估 |
| `POST /routes/smart/generate-personalized` | `RouteController#generatePersonalizedRoute` | 已对齐 | 个性化路线 |
| `POST /routes/smart/recommend-by-preference` | `RouteController#recommendByPreference` | 已对齐 | 偏好推荐 |
| `POST /routes/smart/compare` | `RouteController#compareRoutes` | 已对齐 | 路线对比 |
| `POST /routes/smart/optimize` | `RouteController#optimizeRoute` | 已对齐 | 优化入口 |
| `POST /routes/smart/real-time-adjustment/{routeId}` | `RouteController#getRealTimeAdjustment` | 已对齐 | 实时调整 |
| `GET /routes/smart/list` | `RouteController#getSmartRouteList` | 已对齐 | 智能列表 |
| `GET /routes/smart/themes` | `RouteController#getRouteThemes` | 已对齐 | 主题列表 |
| `GET /routes/smart/seasons` | `RouteController#getRouteSeasons` | 已对齐 | 季节列表 |

## 4. 社区与内容
### 4.1 路线收藏 / 评论 / 分享
| 前端接口 | 后端接口 | 状态 | 备注 |
|---|---|---|---|
| `POST /v1/route-collections/toggle` | `RouteCollectionController#toggleCollection` | 已对齐 | 推荐用这一条替代 collect/uncollect |
| `GET /v1/route-collections/list/{userId}` | `RouteCollectionController#getUserCollections` | 已对齐 | 用户收藏 |
| `GET /v1/route-collections/check` | `RouteCollectionController#checkCollected` | 已对齐 | 是否收藏 |
| `PUT /v1/route-collections/{collectionId}/notes` | `RouteCollectionController#updateCollectionNotes` | 已对齐 | 收藏备注 |
| `PUT /v1/route-collections/{collectionId}/public-status` | `RouteCollectionController#updatePublicStatus` | 已对齐 | 公开状态 |
| `GET /v1/route-collections/public` | `RouteCollectionController#getPublicCollections` | 已对齐 | 公开收藏 |
| `GET /v1/route-collections/categories/{userId}` | `RouteCollectionController#getCollectionCategories` | 已对齐 | 分类 |
| `GET /v1/route-collections/category/{userId}/{category}` | `RouteCollectionController#getCollectionsByCategory` | 已对齐 | 分类查询 |
| `DELETE /v1/route-collections/batch-remove` | `RouteCollectionController#batchRemoveCollections` | 已对齐 | 批量删除 |
| `POST /route-comments` | `RouteCommentController#createComment` | 已对齐 | 创建评论 |
| `GET /route-comments/route/{routeId}` | `RouteCommentController#getRouteComments` | 已对齐 | 评论列表 |
| `GET /route-comments/user/{userId}` | `RouteCommentController#getUserComments` | 已对齐 | 我的评论 |
| `POST /route-comments/{commentId}/toggle-like` | `RouteCommentController#toggleLikeComment` | 已对齐 | 推荐替代 like/unlike |
| `DELETE /route-comments/{commentId}` | `RouteCommentController#deleteComment` | 已对齐 | 删除评论 |
| `GET /route-comments/statistics/{routeId}` | `RouteCommentController#getCommentStatistics` | 已对齐 | 评论统计 |
| `GET /route-comments/{commentId}/replies` | `RouteCommentController#getCommentReplies` | 已对齐 | 回复列表 |
| `POST /route-comments/batch` | `RouteCommentController#getBatchComments` | 已对齐 | 批量评论 |
| `GET /route-comments/hot/{routeId}` | `RouteCommentController#getHotComments` | 已对齐 | 热门评论 |
| `GET /route-comments/latest/{routeId}` | `RouteCommentController#getLatestComments` | 已对齐 | 最新评论 |
| `GET /route-comments/search` | `RouteCommentController#searchComments` | 已对齐 | 搜索评论 |
| `GET /route-comments/high-rating/{routeId}` | `RouteCommentController#getHighRatingComments` | 已对齐 | 高评分评论 |
| `POST /route-share/generate` | `RouteShareController#createRouteShare / generateShareCode` | 已对齐 | 分享码生成 |
| `GET /route-share/validate` | `RouteShareController#validateShareCode` | 已对齐 | 校验分享码 |
| `GET /route-share/info/{shareCode}` | `RouteShareController#getShareInfo` | 已对齐 | 分享详情 |
| `GET /route-share/access/{shareCode}` | `RouteShareController#accessShareRoute` | 已对齐 | 访问分享 |
| `GET /route-share/user/{userId}` | `RouteShareController#getUserShares` | 已对齐 | 用户分享 |
| `DELETE /route-share/cancel/{id}` | `RouteShareController#cancelShare` | 已对齐 | 取消分享 |
| `PUT /route-share/update/{id}` | `RouteShareController#updateShare` | 已对齐 | 分享更新 |
| `POST /route-share/visit/{shareCode}` | `RouteShareController#recordVisit` | 已对齐 | 访问统计 |
| `GET /route-share/statistics/{id}` | `RouteShareController#getStatistics` | 已对齐 | 分享统计 |
| `GET /route-share/popular` | `RouteShareController#getPopularShares` | 已对齐 | 热门分享 |
| `POST /route-share/file/generate` | `RouteShareController#generateFileShare` | 已对齐 | 文件分享 |
| `GET /route-share/file/access/{shareCode}` | `RouteShareController#accessShareFile` | 已对齐 | 文件访问 |
| `POST /route-share/batch-cancel` | `RouteShareController#batchCancelShares` | 已对齐 | 批量取消 |
| `POST /travel-notes` | `TravelNoteController#createNote` | 已对齐 | 新建游记 |
| `PUT /travel-notes/{id}` | `TravelNoteController#updateNote` | 已对齐 | 游记更新 |
| `DELETE /travel-notes/{id}` | `TravelNoteController#deleteNote` | 已对齐 | 游记删除 |
| `GET /travel-notes/{id}` | `TravelNoteController#getNoteById` | 已对齐 | 游记详情 |
| `GET /travel-notes/list` | `TravelNoteController#getNotes` | 已对齐 | 游记列表 |
| `GET /travel-notes/user/{userId}` | `TravelNoteController#getUserTravelNotes` | 已对齐 | 用户游记 |
| `POST /travel-notes/{id}/toggle-like` | `TravelNoteController#toggleLike` | 已对齐 | 推荐替代 like/unlike |
| `POST /travel-notes/{id}/view` | `TravelNoteController#incrementViews` | 已对齐 | 浏览量 |
| `GET /travel-notes/search` | `TravelNoteController#searchNotes` | 已对齐 | 游记搜索 |
| `GET /travel-notes/hot` | `TravelNoteController#getHotNotes` | 已对齐 | 热门游记 |
| `GET /travel-notes/latest` | `TravelNoteController#getLatestNotes` | 已对齐 | 最新游记 |
| `POST /travel-notes/{noteId}/toggle-collect` | `TravelNoteController#toggleCollect` | 已对齐 | 推荐替代 collect/uncollect |

## 5. 通知、反馈、统计、文件
| 前端接口 | 后端接口 | 状态 | 备注 |
|---|---|---|---|
| `GET /v1/notifications` | `NotificationController#getNotifications` | 已对齐 | 通知列表 |
| `GET /v1/notifications/unread-count` | `NotificationController#getUnreadCount` | 已对齐 | 未读数 |
| `PUT /v1/notifications/{id}/read` | `NotificationController#markAsRead` | 已对齐 | 标记已读 |
| `PUT /v1/notifications/read-all` | `NotificationController#markAllAsRead` | 已对齐 | 全部已读 |
| `DELETE /v1/notifications/{id}` | `NotificationController#deleteNotification` | 已对齐 | 删除通知 |
| `POST /feedback/submit` | `FeedbackController#submitFeedback` | 已对齐 | 反馈提交 |
| `GET /feedback/list/{userId}` | `FeedbackController#getFeedbackList` | 已对齐 | 用户反馈 |
| `GET /feedback/types` | `FeedbackController#getFeedbackTypes` | 已对齐 | 类型列表 |
| `GET /v1/user/stats` | `UserStatisticsController#getCurrentUserStats` | 已对齐 | 当前统计 |
| `GET /v1/user/stats/{userId}` | `UserStatisticsController#getUserStatsById` | 已对齐 | 指定用户统计 |
| `POST /resource-file/upload` | `ResourceFileController#uploadResourceFile` | 已对齐 | 单文件上传 |
| `POST /resource-file/batch-upload` | `ResourceFileController#batchUploadResourceFiles` | 已对齐 | 批量上传 |
| `GET /resource-file/{id}` | `ResourceFileController#getResourceFile` | 已对齐 | 文件详情 |
| `GET /resource-file/download/{id}` | `ResourceFileController#downloadResourceFile` | 已对齐 | 下载链接 |
| `DELETE /resource-file/delete/{id}` | `ResourceFileController#deleteResourceFile` | 已对齐 | 删除文件 |
| `GET /resource-file/list` | `ResourceFileController#getResourceFileList` | 已对齐 | 文件列表 |
| `GET /resource-file/search` | `ResourceFileController#searchResourceFiles` | 已对齐 | 文件搜索 |
| `GET /resource-file/statistics` | `ResourceFileController#getFileStatistics` | 已对齐 | 文件统计 |
| `GET /resource-file/category/list` | `ResourceFileController#getCategories` | 已对齐 | 分类列表 |
| `POST /resource-file/category/create` | `ResourceFileController#createCategory` | 已对齐 | 创建分类 |
| `PUT /resource-file/category/update/{id}` | `ResourceFileController#updateCategory` | 已对齐 | 更新分类 |
| `DELETE /resource-file/category/delete/{id}` | `ResourceFileController#deleteCategory` | 已对齐 | 删除分类 |
| `GET /resource-file/category/tree` | `ResourceFileController#getCategoryTree` | 已对齐 | 分类树 |
| `GET /resource-file/version/list/{fileId}` | `ResourceFileController#getFileVersions` | 已对齐 | 版本列表 |
| `GET /resource-file/version/history/{fileId}` | `ResourceFileController#getVersionHistory` | 已对齐 | 历史版本 |
| `POST /resource-file/version/compare` | `ResourceFileController#compareVersions` | 已对齐 | 版本对比 |

## 6. AI 能力
| 前端接口 | 后端接口 | 状态 | 备注 |
|---|---|---|---|
| `POST /ai/chat` | `AIController#chat` | 已对齐 | 通用聊天 |
| `POST /ai/recommend` | `AIController#recommend` | 已对齐 | 推荐 |
| `POST /ai/itinerary/generate` | `AIController#generateItinerary` | 已对齐 | 行程生成 |
| `POST /ai/assistant/chat` | `AIAssistantController#chat` | 已对齐 | 智能助手 |
| `POST /ai/assistant/ask` | `AIAssistantController#ask` | 已对齐 | 问答入口 |
| `GET /ai/assistant/optimize-route/{routeId}` | `AIAssistantController#optimizeRoute` | 已对齐 | 路线优化建议 |
| `GET /ai/assistant/attraction-intro/{attractionId}` | `AIAssistantController#getAttractionIntro` | 已对齐 | 景点介绍 |
| `POST /ai/advanced/plan` | `AIAdvancedController#plan` | 已对齐 | 高级规划 |
| `POST /ai/advanced/budget` | `AIAdvancedController#budget` | 后端保留、前端移除 | 无可信价格源，当前明确返回无数据 |
| `GET /ai/advanced/safety/{cityId}` | `AIAdvancedController#getSafetyAdvice` | 后端保留、前端移除 | 无可信安全数据源，当前明确返回无数据 |
| `POST /ai/advanced/chat` | `AIAdvancedController#advancedChatbot` | 已对齐 | 高级聊天 |
| `GET /ai/image-analysis/types` | `AIImageController#getImageAnalysisTypes` | 已对齐 | 图片分析类型 |
| `POST /ai/image-analysis` | `AIImageController#analyzeImage` | 已对齐 | 图片分析 |

## 7. 已完成的契约治理
| 范围 | 当前状态 |
|---|---|
| 路线收藏 | 统一使用 `toggle`、`check`、`remove` 和资源 ID 路径；用户身份只取 JWT，不再由前端提交 |
| 路线评论与游记互动 | 删除不存在的 `like/unlike/collect/uncollect` 旧别名，保留后端实际存在的接口 |
| 游记评论 | 当前无评论明细表和后端接口，前端评论区及虚假评论计数已移除 |
| 景点周边与点评 | 已对齐 `/attractions/{id}/nearby` 和 `/attractions/{id}/review`，点评用户取 JWT |
| 实时状态 | 删除无请求体批量更新调用，以及用景点人流冒充道路交通的前端映射 |
| 历史客流 | 当前只有最新快照；历史平均和近 7 天接口明确返回 HTTP 503 与错误码 `20007` |
| AI 多模态 | 后端没有真实图片入模链路，相关 Controller、Service、DTO、API 方法和前端页签已删除 |
| 路线优化 | 前端只展示并提交 `distance` 最近邻顺序优化，不再展示时间、费用或虚构评分收益 |
| 智能路线 | 热门、季节和主题统一走 `/routes/smart/list`，优化建议走 `/route-optimization/suggestions/{routeId}` |
| UI 字典 | 后端无字典路由，前端改用受版本控制的静态能力清单，不再发送必然 404 的请求 |

## 8. 联调检查项
- 登录后 token 是否正确注入 `Authorization`。
- 401 是否会清理 token 并跳登录页。
- 所有列表接口是否统一返回数组或分页结构。
- 文件上传是否支持 `multipart/form-data`。
- AI 接口是否需要流式返回或长轮询。
- 路线收藏、评论、分享是否存在参数名差异。

## 9. 后续联调重点
1. 使用真实账号验证收藏、点评和游记详情的 JWT 身份边界。
2. 使用真实 `AMAP_API_KEY` 验证路线分段交通，不以景点客流替代道路交通。
3. 游记评论和历史客流只有在增加明细表、唯一约束及对应测试后才能重新开放。
4. AI 语音能力无后端实现，前端入口和字典种子已移除。

## 10. 结论
- 当前前后端展示主链路已按实际 Controller 路径对齐。
- 无数据模型或无后端实现的能力不再出现在用户界面中。
- 后续新增能力必须同时具备数据来源、持久化模型、权限校验和回归测试。
