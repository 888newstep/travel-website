# 前端设计与实施方案

> 前端仓库：C:\Users\xiaohongfu\IdeaProjects\travel\frontend
>
> 后端仓库：C:\Users\xiaohongfu\IdeaProjects\travel\backend

## 1. 目标
- 统一前端页面结构、交互规范、接口接入方式和工程组织。
- 在保留现有 React + TypeScript + Vite + React Router 技术栈的前提下，提升可维护性与扩展性。
- 为后端 gateway、user-service、
oute-service、ttraction-service、ile-service、collection-service 提供稳定接入边界。

## 2. 关键约束
- 当前项目已有较完整的页面与 API 文件，优先做结构收敛，不做推倒重来。
- 后端是 Spring Boot 微服务体系，前端应优先通过网关统一访问。
- 需要兼顾登录态、权限路由、文件上传、路线规划、AI 对话、实时数据等场景。
- 方案必须支持渐进式落地，避免一次性大改影响业务。

## 3. 现状判断
- 技术栈：React 19、TypeScript、Vite 6、React Router 7、Axios、Tailwind CSS 4。
- 当前路由已覆盖：主页、景点、路线、笔记、AI 聊天、餐厅、实时状态、个人中心、通知、反馈、文件、分享、路线优化、登录。
- 代码结构已具备 src/api、src/pages、src/components、src/hooks、src/lib、src/app 的雏形。
- 后端统一响应风格，适合封装通用请求层和错误处理层。

## 4. 方案选择
### 方案 A：保持 SPA 并分层治理
- 优点：改动小、落地快、与现有代码兼容度高。
- 缺点：SEO 一般，首屏依赖静态资源加载。

### 方案 B：升级 SSR
- 优点：首屏和 SEO 更强。
- 缺点：迁移成本高，与当前项目不匹配。

### 方案 C：微前端拆分
- 优点：适合超大团队和超大业务域。
- 缺点：复杂度和运维成本过高。

### 结论
- 推荐继续采用 **方案 A**，优先做工程化治理，而不是重构技术栈。

## 5. 总体设计
### 5.1 分层
- pp：入口、路由、全局 Provider、全局样式。
- pages：页面级编排，只负责组合业务。
- components：可复用 UI 与业务组件。
- pi：按后端服务域拆分接口文件。
- hooks：分页、防抖、轮询、表单状态等通用逻辑。
- lib：请求、鉴权、toast、错误映射等基础能力。

### 5.2 建议目录
`	ext
src/
  app/
  api/
  components/
    common/
    layout/
    business/
  hooks/
  lib/
  pages/
  constants/
  utils/
`

### 5.3 页面分区
- 导航类：主页、景点、路线、餐厅、AI。
- 运营类：笔记、分享、反馈、通知、文件。
- 个人类：登录、个人中心、收藏、统计、权限信息。
- 实时类：实时状态、动态刷新、告警与轮询。

## 6. 后端映射
### 6.1 接口入口
- 所有业务请求默认走后端 gateway。
- 前端只保留一个基础地址配置，避免硬编码服务地址。

### 6.2 服务映射
- user-service：登录、注册、用户信息、权限、统计。
- 
oute-service：路线规划、路线分享、路线优化、路线详情。
- ttraction-service：景点查询、景点详情、推荐、标签筛选。
- collection-service：收藏、取消收藏、收藏列表。
- ile-service：头像、附件、图片、导入导出文件。
- common：枚举、通用 DTO、统一返回体定义。

### 6.3 请求规范
- 统一 Axios 实例和拦截器。
- 自动注入 	oken。
- 统一处理超时、网络错误、401、403、500。
- 对后端统一返回体做二次解包，页面只消费业务数据。

## 7. 核心交互
### 7.1 导航与布局
- 顶部导航 + 左侧功能入口 + 主内容区 + 底部信息区。
- 登录态下展示头像、通知、收藏和快捷操作。
- 小屏场景切换为折叠导航。

### 7.2 权限与路由
- public：主页、登录、景点浏览、餐厅浏览、AI 入口。
- protected：个人中心、通知、反馈、文件、分享、路线优化。
- 路由守卫只负责鉴权，不混入业务逻辑。

### 7.3 状态管理
- 页面内状态优先用组件本地状态。
- 跨页面轻量状态建议放在极简全局状态层。
- 服务端状态按接口维度做缓存、刷新和失效控制。

## 8. 体验与性能
- 页面级懒加载，减少首屏包体积。
- 列表页支持分页、虚拟滚动或按需渲染。
- 搜索、筛选、联想输入做防抖。
- 高频轮询场景设置退避和停止条件。
- 图片、文件、地图类资源做压缩与尺寸控制。

## 9. 安全设计
- 	oken 仅保存在受控位置，退出登录必须清理。
- 敏感页面都走路由守卫。
- 表单先前端校验，最终以后端校验为准。
- 文件上传限制类型、大小和数量。
- 错误消息做脱敏，避免暴露内部实现。

## 10. 推荐实施顺序
### 第一阶段：工程收敛
- 统一请求层、错误处理、鉴权与全局布局。
- 整理 src/api、src/hooks、src/components 职责边界。
- 修复路由保护与登录态流转。

### 第二阶段：核心业务稳定
- 完善景点、路线、收藏、个人中心、文件上传。
- 统一列表、详情、空状态、加载态、错误态。

### 第三阶段：高级能力
- 接入 AI 对话、实时状态、路线优化、消息通知。
- 增加缓存、预取、局部刷新与操作反馈。

### 第四阶段：体验优化
- 适配移动端。
- 做主题、文案、无障碍与埋点优化。

## 11. 风险与应对
- **接口不稳定**：用统一适配层隔离变更。
- **页面耦合过高**：将通用逻辑下沉到 hooks 和 components。
- **权限混乱**：统一定义公开页和受保护页。
- **性能退化**：对大列表、轮询和 AI 流式场景单独治理。

## 12. 需要验证的假设
- 后端统一通过网关暴露接口，需要和后端实际部署方式确认。
- 当前前端大部分页面已可运行，适合做结构优化而不是从零重做。
- 登录态以 JWT 或类似 token 方式管理，需要和后端认证实现对齐。

## 13. 结论
当前最优解不是重写，而是：**保持现有 React SPA 架构，建立统一请求层、清晰页面分层、稳定权限路由和后端网关接入规范**。这样能以最小代价换取最大的可维护性提升。

## 14. 信息架构与页面蓝图
### 14.1 一级导航建议
- 首页：承载系统入口、热门景点、推荐路线、AI 快捷入口。
- 发现：景点、餐厅、城市维度浏览与筛选。
- 行程：路线生成、路线优化、路线分享、收藏路线。
- 社区：游记、评论、反馈、通知。
- 我的：登录、个人信息、文件、统计、偏好配置。

### 14.2 推荐路由结构
```text
/
/login
/attractions
/attractions/:id
/restaurants
/routes
/routes/:id
/optimization
/notes
/notes/:id
/share
/ai-chat
/realtime
/profile
/notifications
/feedback
/files
```

### 14.3 页面职责建议
- `HomePage`：聚合展示，不承载复杂编辑逻辑。
- `AttractionsPage`：列表筛选、详情抽屉、相关推荐。
- `RoutesPage`：路线查询、创建、收藏、分享、删除。
- `RouteOptimizationPage`：输入出发条件、优化策略、返回建议结果。
- `AIChatPage`：多轮对话、历史记录、推荐卡片、容错提示。
- `RealtimeStatusPage`：景点实时状态、流量趋势、刷新与退避机制。
- `UserProfilePage`：个人信息、统计概览、快捷入口。
- `FileManagementPage`：上传、下载、删除、预览、容量与类型校验。

## 15. 前后端接口映射清单
### 15.1 现有前端 API 与后端控制器映射
| 前端 API 文件 | 主要能力 | 建议后端控制器 |
|---|---|---|
| `src/api/attraction.api.ts` | 景点列表、详情、筛选 | `AttractionController` |
| `src/api/restaurant.api.ts` | 餐厅查询 | `RestaurantController` |
| `src/api/realtime.api.ts` | 实时状态 | `RealtimeStatusController` |
| `src/api/route.api.ts` | 路线生成、查询、管理 | `RouteController` |
| `src/api/ai.api.ts` | AI 对话、AI 行程规划 | `AIController`、`AIAssistantController`、`AISmartItineraryController` |
| `src/api/collection.api.ts` | 收藏路线 | `RouteCollectionController` |
| `src/api/comment.api.ts` | 路线评论 | `RouteCommentController` |
| `src/api/share.api.ts` | 路线分享 | `RouteShareController` |
| `src/api/note.api.ts` | 游记管理 | `TravelNoteController` |
| `src/api/notification-feedback.api.ts` | 通知、反馈 | `NotificationController`、`FeedbackController` |
| `src/api/file.api.ts` | 文件上传下载 | `ResourceFileController` |
| `src/api/user.api.ts` | 登录、用户资料 | `UserController` |
| `src/api/user-stats.api.ts` | 用户统计 | `UserStatisticsController` |
| `src/api/dictionary.api.ts` | 城市/字典类查询 | `CityController` 或公共字典接口 |

### 15.2 建议统一响应适配
后端已体现统一返回体思路，前端建议统一适配为：

```ts
interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
  success: boolean
}
```

建议页面层只拿到 `data`，不要在页面里重复判断 `code` 与 `success`。

### 15.3 接口联调优先级
- P0：登录、用户信息、景点列表、路线列表、路线创建。
- P1：收藏、评论、分享、文件上传、通知。
- P2：AI 对话、实时状态、统计报表、反馈。

## 16. 可执行开发任务拆解
### 16.1 Sprint 1：基础设施治理
- 抽离统一请求客户端，合并超时、重试、鉴权与错误提示。
- 统一 `ProtectedRoute` 行为，处理 token 失效跳转。
- 补充全局布局壳与页面容器规范。
- 产出物：稳定请求层、统一路由守卫、基础布局框架。

### 16.2 Sprint 2：核心业务闭环
- 完善景点列表与详情流程。
- 打通路线创建、查询、删除、收藏、分享。
- 整理个人中心与统计页展示逻辑。
- 产出物：用户从浏览到生成路线的主链路闭环。

### 16.3 Sprint 3：社区与文件能力
- 打通游记、评论、反馈、通知。
- 完善文件上传、预览、删除、异常回滚。
- 补齐空状态、加载态、操作反馈。
- 产出物：社区互动与资源管理闭环。

### 16.4 Sprint 4：高级能力与体验优化
- 打通 AI 问答、AI 行程规划、实时状态。
- 引入懒加载、缓存、轮询退避和移动端优化。
- 统一埋点、错误收集和性能监测。
- 产出物：高价值能力上线与整体体验提升。

## 17. 页面验收标准
### 17.1 通用验收项
- 页面首次进入有加载态。
- 请求失败有错误提示与重试入口。
- 空数据有空状态提示。
- 提交类操作有按钮防重复机制。
- 删除类操作有二次确认。
- 受保护页面在未登录时自动跳登录。

### 17.2 文件上传验收项
- 限制文件类型与大小。
- 上传中展示进度或处理中状态。
- 失败后可以重试，不污染列表状态。
- 成功后立即刷新或局部插入新数据。

### 17.3 AI 与实时页验收项
- AI 响应失败时保留上下文与重试入口。
- 实时轮询可暂停、恢复，并在离开页面后停止。
- 长时间无数据变化时自动降频，防止请求风暴。

## 18. 联调清单
- 确认网关基础地址与各服务转发前缀。
- 确认登录接口字段、token 头名称、过期策略。
- 确认上传接口 `Content-Type`、大小限制、回显结构。
- 确认路线、评论、分享、通知等分页参数格式。
- 确认 AI 接口是否支持流式返回。
- 确认实时状态接口刷新频率与限流策略。

## 19. 当前假设验证结果
- **假设 1：前端为 React SPA**：成立，已从 `package.json` 与 `src/app/router.tsx` 验证。
- **假设 2：后端为微服务拆分**：成立，已从 `backend` 下多个 service 模块验证。
- **假设 3：前后端已有基础接口映射**：基本成立，前端 `src/api` 文件与后端控制器命名高度对应。
- **假设 4：建议通过网关统一访问**：高概率成立，但仍需确认 `gateway` 的真实路由前缀与鉴权配置。

## 20. 下一步建议
- 先按 `P0` 接口梳理出一份“接口字段对照表”。
- 再把 `RoutesPage`、`AttractionsPage`、`LoginPage` 三条主链路优先打通。
- 最后再补齐 AI、实时状态、通知和文件等增强能力。

## 21. 关联文档
- 详细接口联调清单见：API_INTEGRATION_CHECKLIST.md


## 22. 待修正与字段映射
- 待修正接口与字段映射见：API_FIX_CHECKLIST.md

