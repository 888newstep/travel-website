# 智慧旅游系统 - 前端 (Travel Frontend)

基于 `React 19 + TypeScript + Vite 6 + React Router 7` 的单页应用，作为智慧旅游微服务平台（Spring Cloud Alibaba 后端）的 Web 端。页面在近期从 Vue 迁移到 React。

## 技术栈

- `React 19`
- `TypeScript`
- `Vite 6`
- `React Router 7`
- `Axios`
- `Tailwind CSS 4`

## 快速开始

```bash
npm install       # 安装依赖
npm run dev       # 开发模式（经 scripts/vite-dev.mjs 启动，规避 Node 18 的 crypto.getRandomValues 问题）
npm run build     # 生产构建
npm run preview   # 预览构建产物
npm run lint      # 类型检查（tsc --noEmit）
```

开发服务器默认运行于 `http://localhost:3000`。

## 目录结构

```
frontend/
├── src/
│   ├── app/           # 应用入口（main.tsx / App.tsx / providers / router）
│   ├── pages/         # 页面：首页 / 景点 / 路线 / AI 对话 / 实时 / 社区 / 个人中心等
│   ├── components/    # 通用组件与布局（Header / Footer / Drawer / Toast …）
│   ├── api/           # 按业务域封装的 API 客户端（如 ai.api.ts / route.api.ts / attraction.api.ts）
│   ├── lib/           # 请求封装（request.ts）与鉴权（auth.ts，localStorage 存储 token）
│   ├── hooks/         # 自定义 hooks（AI 聊天状态 / 关键字同步等）
│   ├── constants/     # 常量
│   └── utils/         # 工具函数（api.ts：Axios 实例与拦截器）
├── scripts/           # vite-dev.mjs 等本地开发脚本
├── index.html
├── vite.config.ts
└── tsconfig.json
```

## 环境变量

参考 `.env.example`：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `VITE_API_BASE_URL` | `/api` | API 基础路径 |
| `VITE_API_TIMEOUT` | `30000` | 请求超时（毫秒） |
| `VITE_PROXY_TARGET` | `http://localhost:8090` | 后端网关代理目标地址 |
| `VITE_APP_HOST` | `127.0.0.1` | 开发服务器监听地址 |
| `VITE_APP_PORT` | `3000` | 开发服务器端口 |
| `VITE_APP_TITLE` | 智慧旅游系统 | 页面标题 |

### 场景说明

- **场景 A（推荐）**：后端跑在本机（本地 MySQL/Redis，云端中间件）——使用默认 `VITE_PROXY_TARGET=http://localhost:8090` 即可。
- **场景 B**：后端部署到云服务器——编辑 `.env` 中的 `VITE_PROXY_TARGET`，或启动时注入（设置环境变量后 `npm run dev`）。

## 页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | HomePage | 首页 |
| `/attractions` | AttractionsPage | 景点 |
| `/routes` | RoutesPage | 路线 |
| `/notes` | NotesPage | 游记 |
| `/ai-chat` | AIChatPage | AI 智能助手 |
| `/restaurants` | RestaurantPage | 美食 |
| `/realtime` | RealtimeStatusPage | 实时数据 |
| `/profile` | UserProfilePage | 个人中心（需登录） |
| `/notifications` | NotificationPage | 通知（需登录） |
| `/feedback` | FeedbackPage | 反馈（需登录） |
| `/files` | FileManagementPage | 文件管理（需登录） |
| `/share` | RouteSharePage | 路线分享（需登录） |
| `/optimization` | RouteOptimizationPage | 路线优化（需登录） |
| `/login` | LoginPage | 登录 |

## 技术要点

- Dev Server 通过 `scripts/vite-dev.mjs` 启动，以规避 Node 18 对 `crypto.getRandomValues` 的兼容问题。
- API 请求统一走 `src/lib/request.ts`（Axios 拦截器：附加 token、统一错误处理）。
- 登录态存储在 `localStorage`（`token` / `username`），由 `src/lib/auth.ts` 管理。
- 路由使用 `BrowserRouter + Routes + Link + Navigate`（React Router 7），受保护页面由 `ProtectedRoute` 包裹，未登录自动跳转登录页。

## 安全与维护

- 生产构建使用 `npm ci` 安装锁定版本依赖，`npm audit` 异常时可参考 `SECURITY_NOTES.md` 处理（`react-router-dom` / `react-router` 等）。
- 若后台网关位于 **8090**，请通过 `VITE_PROXY_TARGET` 指定，勿在前端代码中硬编码后端地址。
- 本地联调服务器仅监听 `127.0.0.1`（`VITE_APP_HOST`），以免意外对外暴露。

## License

[Apache License 2.0](../../../LICENSE)