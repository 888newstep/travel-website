# travel 项目企业级加强 + 前端优化规划

> 版本：2026-11-19 · 状态：规划稿（待用户确认优先级后实施）
> 原则：**真收益优先于表演项**；每项标注工程价值与面试叙事价值；不破坏 298 测试证据链与已验收能力。

---

## 一、现状盘点（真实成色，基于代码核验）

### 1.1 已达标（不动）
| 能力 | 证据 |
|---|---|
| 测试 | 298 tests / 0 failure / 0 error，72 个测试类 |
| 安全基线 | CORS 白名单、JWT 密钥契约、安全响应头、生产关 Swagger、AI 接口鉴权（c2516b3） |
| 一致性 | Redis Lua 幂等状态机 + MySQL 唯一键 + Redisson 锁 + FOR UPDATE + 负 id 换位 |
| MQ | confirm + mandatory + 手动 ack + TTL 重试 + DLQ + 消费幂等 |
| 容器安全 | 生产 Compose 仅暴露 8080/8090，内部服务走 expose |

### 1.2 企业级差距矩阵
| 维度 | 现状 | 企业标准 | 差距 |
|---|---|---|---|
| 可观测性-metrics | micrometer-prometheus **依赖已在 common**，但无面板无采集 | Prometheus + Grafana 看板 | 半就绪，成本最低 |
| 可观测性-日志 | 默认 logback，无 traceId 贯穿 | MDC traceId + 结构化 JSON | 全缺 |
| 可观测性-追踪 | 无 | SkyWalking/OTel | 全缺（P2，2C8G 有取舍） |
| 高可用 | 每服务单副本、无 healthcheck 编排、无优雅停机 | 多实例 + 探针 + 优雅停机 | 全缺，多实例验证价值最高 |
| 限流 | 无（仅幂等防重） | 网关限流 + 业务配额 | 全缺 |
| 文件上传 | 扩展名黑名单 | 白名单 + 魔数 | 悬置项，可落 |
| CI/CD | CI 全绿门禁 | CI + 自动部署 | 无 CD |
| 数据层 | 5 服务单库（23 表） | 按服务分库 | 真分库成本高，P2 只做方案 |
| 前端 | 0 状态管理、0 测试、0 ErrorBoundary | 服务端状态库 + 测试 + 边界 | 前端专项 |

### 1.3 前端现状细节（本轮新核验）
- 依赖极简：React 19 + Vite 6 + TS 5.8 + Tailwind 4 + axios + react-router 7；**无 zustand/react-query/vitest**
- 优点：API 层 13 模块分层清晰；`lib/request.ts` 自建超时/NetworkError/AuthError 三类错误；页面有防抖 hook、`hasFetchedRef` 防重复拉取、错误分区提示——基础工程素养好
- 缺口：每个页面 8-10 个 useState 手动管理；切页重新拉数据无缓存；任何未捕获渲染异常直接白屏；55 个 tsx 零测试；`formatTime` 等工具函数疑似跨页复制

---

## 二、加强方案（分三期，P0 先行）

### P0：可观测性三件套 + 多实例一致性（真收益最高，约 2 个周末）

#### P0-1 traceId 全链路贯穿（后端，1 天）
- common 加 `TraceIdFilter`：入口生成/透传 `X-Trace-Id`，写 MDC；Gateway 生成，业务服务透传；响应头回传 `X-Trace-Id`
- logback pattern 加入 `%X{traceId}`；所有日志自动携带，跨服务用同一 traceId 串联
- MQ 链路：消息头带 traceId，消费端恢复 MDC（`ReliableNotificationConsumer` 入口处）
- 验证：单测 + 手工 curl 两跳服务看日志同 traceId
- 面试叙事："线上问题排查第一步：一个 traceId 从网关串到 MQ 消费端"

#### P0-2 Prometheus 指标暴露 + Grafana 面板（0.5 天）
- micrometer registry 已在，只需：actuator 暴露 `prometheus` 端点（仅内网/Tailnet）+ docker-compose 加 prometheus + grafana 两个容器
- 面板最小集：QPS、P95 延迟、JVM 内存/GC、DB 连接池、MQ 消费延迟、幂等 409 率
- 面试叙事："压测时有 Grafana 实时看板，不是只看 JTL 文件"

#### P0-3 多实例一致性验证（1 天，面试价值最高的单项）
- compose 把 collection-service 起两个副本（8094/8096）+ Gateway 负载均衡（或恢复 Nacos standalone）
- 用现有 `security_probe.py` + JMeter 打多实例：验证幂等状态机、唯一键、分布式锁在多副本下依然 1 写 99 重放
- 产出：多实例验收报告入 `backend/docs/showcase/`（沿用四级证据格式）
- 面试叙事："单机一致性很多项目有，我在多实例下重新验证过"

#### P0-4 优雅停机 + healthcheck（0.5 天）
- 全服务 `server.shutdown=graceful` + compose healthcheck + `depends_on: condition: service_healthy`
- MQ 消费端停机前 ack 完在途消息（`ContextClosedEvent` 监听）

### P1：限流 + 文件加固 + CD + 前端基建（约 2-3 个周末）

#### P1-1 限流（1.5 天）
- Gateway 层：`RequestRateLimiter`（Redis 令牌桶）按 IP+路径；登录/验证码单独更严阈值
- 业务层：AI 接口按 userId 日配额（Redis INCR + TTL，对应"INCR 原子性"面试题的真实落地）
- 回归：security_probe `--check-rate-limit` 打开

#### P1-2 文件上传白名单 + 魔数（1 天）
- `FileStoragePolicy` 黑名单 → 业务白名单（jpg/png/webp/pdf/docx/xlsx 等实际用到的）
- 魔数校验：读文件头 magic bytes 与声明类型比对（手写 `FileTypeDetector` 或用 Apache Tika，取舍：Tika 重，手写 20 行够用）
- SVG/HTML 默认拒绝；下载强制 `Content-Disposition: attachment`

#### P1-3 CD 到云服务器（1 天）
- GitHub Actions：tag → 构建镜像 → ssh 到腾讯云 → `docker compose pull && up -d`（secrets 管理私钥）
- 面试叙事："提交即部署，不用手动 scp"

#### P1-4 前端基建三件（2 天）
- **@tanstack/react-query**：替换页面 useState 拉取模式——切页缓存、自动去重、失效重取；页面代码量预计减 30-50%
- **ErrorBoundary**：路由级包裹，白屏变降级 UI
- **vitest + testing-library**：先给 `lib/request.ts`（超时/错误分类）、`lib/auth.ts`（token 管理）、1 个核心页面（NotesPage 防抖搜索）补测试——前端从 0 测试到有测试框架

### P2：讲清取舍即可，不必全做

| 项 | 取舍结论 |
|---|---|
| SkyWalking 全链路追踪 | 2C8G 上 agent 开销 ~10-15% 内存，演示项目**不装**；面试口径："有 traceId 日志串联已够，APM 是上量后的事" |
| 按服务分库 | 真分库改造成本高、业务量不需要；做**schema 分离过渡**或只写分库方案文档（A/B 表归属、跨库查询怎么办） |
| JWT 轮换（kid + 多密钥池） | 单实例演示场景收益低；文档化方案即可 |
| 暗色模式 / i18n | 前端加分项，Tailwind 4 `dark:` 变体半天可做，排在 P1-4 之后按心情 |
| Seata | **继续不启用**：无 @GlobalTransactional 场景，开了是空转；保持"预留 + 最终一致性"叙事 |

---

## 三、前端优化专项明细（P1-4 展开）

| 项 | 现状痛点 | 方案 | 验收 |
|---|---|---|---|
| 服务端状态 | 15 页各写 fetch+loading+error 三板斧，切页即失 | react-query 封装 `useNotes/useAttractions/...` hooks，staleTime 按数据类型（榜单 5min/详情 1min/实时状态 0） | 页面 hooks 数减半；切页秒开 |
| 错误边界 | 渲染异常白屏 | `ErrorBoundary` 包 `router.tsx` 每个路由；降级 UI 复用 StatusNotice | 手工抛异常验证降级 |
| 前端测试 | 55 文件 0 测试 | vitest + @testing-library；先覆盖 request/auth/1 页面 | `npm run test` 入 CI |
| 工具函数去重 | formatTime 等跨页复制 | 提 `lib/format.ts` | 各页 import |
| 构建分层 | env.d.ts 单文件 | `.env.development` / `.env.production`（API 地址、是否 mock） | build 产物不含 dev 地址 |
| 幂等键体验 | axios 拦截器已生成幂等键（好） | 补充：失败重试时**复用**同键的显式注释与单测 | request 层单测 |

---

## 四、实施顺序与时间预算

| 周期 | 内容 | 预估 |
|---|---|---|
| 第 1 周末 | P0-1 traceId + P0-2 Prometheus/Grafana | 1.5 天 |
| 第 2 周末 | P0-3 多实例验证 + P0-4 优雅停机 | 1.5 天 |
| 第 3 周末 | P1-1 限流 + P1-2 文件白名单 | 2.5 天 |
| 第 4 周末 | P1-4 前端 react-query + ErrorBoundary + vitest | 2 天 |
| 第 5 周末 | P1-3 CD + 文档收尾（showcase 报告 + README 更新） | 1.5 天 |
| 穿插 | P2 文档化项（分库方案、JWT 轮换、SkyWalking 取舍）写入 README "演进路线" | 0.5 天 |

- 工作日纪律不变（≤30 分钟话术层），集中在周末批量执行
- 每期结束跑全量测试 + security_probe，push 前必须全绿
- 每期产出对应 showcase 证据，直接转化为面试素材（traceId 日志截图、Grafana 面板截图、多实例验收报告）

## 五、验收标准（全部完成时的 Definition of Done）

1. 一个请求从 Gateway → 业务 → MQ，日志同 traceId 可串联
2. Grafana 实时看到压测 QPS/P95 与幂等 409 率
3. collection 双副本下跑 100 并发幂等：1 成功 99 重放，两实例日志均有痕迹
4. `docker compose stop` 期间在途消息不丢（优雅停机验证）
5. 登录接口 10 次超限返回 429
6. 上传 `evil.php.jpg`、伪装 MIME 的 exe 被魔数校验拒绝
7. push 到 main → 云服务器自动更新（CD 生效）
8. 前端：切页缓存生效、渲染异常显示降级 UI、`npm run test` 绿
9. 全量 Maven 测试 ≥298 全绿，README "演进路线"章节写清 P2 取舍

## 六、风险与不做清单

- **不破坏现有验收**：所有改动不改幂等/锁/MQ/优化算法核心逻辑，只在外围加层
- **2C8G 容量**：+prometheus/grafana 两个容器 ≈ +400MB，可行；Grafana 不用时不常驻可降级为按需启动
- **react-query 引入是行为变化**：切页从"重新请求"变"用缓存"，需逐页核对实时性敏感页（实时路况 staleness=0）
- **CD 私钥安全**：GitHub secrets 只放 deploy 专用受限账号的 key，不用 root
- 不做：真分库、Seata 启用、K8s、微前端、SSR——成本与当前阶段不匹配
