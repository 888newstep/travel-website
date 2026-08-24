# 智慧旅游系统后端

基于 Java 17、Spring Boot 3.3.5 和 Spring Cloud 的 Maven 多模块后端，面向秋招答辩与技术交流。后端重点展示安全、幂等、并发一致性、真实第三方数据治理和 RabbitMQ 可靠消费，不将未接入数据源的接口包装成完整产品能力。

## 模块

| 模块 | 类型 | 端口 | 职责 |
|------|------|------|------|
| `common` | 共享库 | - | 实体、Mapper、异常、安全、Redis、幂等、RabbitMQ、第三方客户端 |
| `gateway` | 应用 | 8090 | API 路由、JWT 验签、可信用户头、入口治理 |
| `user-service` | 应用 | 8091 | 注册、登录、Token、用户资料与密码 |
| `attraction-service` | 应用 | 8092 | 城市、景点、餐厅、点评与最新状态快照 |
| `route-service` | 应用 | 8093 | 路线、日程、优化、高德交通与条件 AI |
| `collection-service` | 应用 | 8094 | 收藏、评论、游记、分享、通知、反馈与统计 |
| `file-service` | 应用 | 8095 | 文件、分类、标签和版本 |

```text
backend/
├── common/
├── gateway/
├── user-service/
├── attraction-service/
├── route-service/
├── collection-service/
├── file-service/
├── docs/
└── pom.xml
```

## 技术栈

| 层级 | 技术与当前用途 |
|------|----------------|
| 基础框架 | Spring Boot 3.3.5、Spring Cloud 2023.0.3、Spring Cloud Alibaba 2023.0.3.2 |
| 数据 | MySQL 8、MyBatis-Plus 3.5.8、Druid 1.2.23 |
| 缓存与锁 | Redis、Redisson 3.37、Lua 原子操作 |
| 安全 | Spring Security、JWT (`jjwt` 0.12.5)、对象所有权校验 |
| 消息 | 云 RabbitMQ、publisher confirm、returned、manual ACK、TTL 重试、DLQ、消费幂等 |
| 外部调用 | 高德 Web 服务、DashScope 通义千问、百度 AI、OkHttp、并发舱壁与响应体上限 |
| API | Springdoc / Knife4j、统一 `Result<T>`、全局异常处理 |
| 测试 | JUnit 5、Mockito、Spring Boot Test、JMeter 5.6.3 |

## 运行模式

### Windows 本地模式

- 项目内置 Nacos 提供注册发现。
- MySQL、Redis 在 Win11 本机运行。
- RabbitMQ 指向外部云 broker；可靠通知开关默认关闭。
- 根目录 `start-all.bat` 会启动 Nacos 并构建、启动 6 个后端应用。

```powershell
cd ..
.\start-all.bat
```

`start-all.bat` 不读取 `deploy/.env`，本地参数应在当前 PowerShell 会话中通过环境变量设置。

### Docker Compose 模式

- Compose 启动 MySQL、Redis、5 个业务服务、Gateway 和 Web。
- Nacos、Sentinel 和 Seata 在该 profile 中关闭。
- Gateway 使用容器 DNS 静态路由。
- Compose 不启动 RabbitMQ，`RABBITMQ_*` 必须指向外部 broker。

```powershell
cd ..
Copy-Item deploy\.env.example deploy\.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d
```

## 关键配置

| 变量 | 说明 |
|------|------|
| `JWT_SECRET` | 必填，至少 32 个 UTF-8 字节 |
| `DB_HOST/PORT/NAME/USERNAME/PASSWORD` | MySQL 连接 |
| `REDIS_HOST/PORT/PASSWORD/DB` | Redis、缓存、分布式锁和幂等 |
| `RABBITMQ_HOST/PORT/USERNAME/PASSWORD/VHOST` | 云 RabbitMQ |
| `MQ_RELIABLE_NOTIFICATION_TOPOLOGY_ENABLED` | 声明可靠通知拓扑，默认 `false` |
| `MQ_RELIABLE_NOTIFICATION_PRODUCER_ENABLED` | 发布可靠通知，默认 `false` |
| `MQ_RELIABLE_NOTIFICATION_CONSUMER_ENABLED` | 启动可靠消费者，默认 `false` |
| `MQ_STATUS_PERSISTENCE_ENABLED` | 持久化消息状态，默认 `false` |
| `AMAP_API_KEY` | 高德真实路线和路况 |
| `QWEN_API_KEY` | 通义千问文本能力 |
| `BAIDU_APP_ID/API_KEY/SECRET_KEY` | 百度图像识别 |
| `CAPTCHA_DEMO_MODE` | 本地验证码展示，默认关闭，公开环境禁止启用 |

完整模板见 [../deploy/.env.example](../deploy/.env.example)。

## 主要 API

统一 Gateway 入口：`http://localhost:8090/api/**`。

### 核心业务

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/users/login` | 登录并签发 JWT |
| GET | `/api/attractions` | 景点列表 |
| POST | `/api/attractions/{id}/review` | 景点点评原子 UPSERT |
| GET | `/api/routes/{id}` | 路线详情 |
| GET | `/api/routes/my` | 当前用户路线 |
| POST | `/api/route-optimization/apply` | 应用完整顺序或按天最近邻优化 |
| GET | `/api/route-optimization/history/{routeId}` | Redis 优化历史 |
| POST | `/api/v1/route-collections/toggle` | 幂等切换路线收藏 |
| POST | `/api/route-comments/{commentId}/toggle-like` | 持久化评论点赞行为 |
| GET | `/api/v1/notifications` | 当前用户站内通知 |
| POST | `/api/resource-file/upload` | 文件上传 |

### AI 与外部能力

| 路径 | 状态 | 说明 |
|------|------|------|
| `/api/ai/chat`、`/api/ai/qa` | 条件可用 | 配置 `QWEN_API_KEY` 后调用通义千问 |
| `/api/ai/itinerary/generate` | 条件可用 | 生成式文本，不与本地景点和高德结果做强一致校验 |
| `/api/ai/advanced/plan` | 有限可用 | 本地约束调度器，不是大模型多目标优化 |
| `/api/ai/image-analysis` | 条件可用 | URL 白名单与 SSRF 防护后调用百度 AI |
| `/api/ai/advanced/guide`、`budget`、`safety` | 明确不可用 | 无可信数据源时抛出业务不可用 |
| 上传图片综合分析、相似景点、多模态推荐/搜索 | 遗留占位 | 不纳入展示，待删除或接入真实数据源 |

详细能力矩阵见 [docs/showcase/CAPABILITY_BOUNDARIES.md](docs/showcase/CAPABILITY_BOUNDARIES.md)。

## HTTP 幂等

`HttpIdempotencyFilter` 位于服务内 Spring Security 认证之后，处理已认证的 `POST`、`PUT`、`PATCH` 和 `DELETE`：

1. 读取 `Idempotency-Key`；
2. 对认证主体、方法、路径、内容类型和请求体计算指纹；
3. 使用 Redis Lua 抢占 `PROCESSING:<token>`；
4. 完成后保存状态码、内容类型和响应体；
5. 重复请求直接重放，处理中或同键异请求返回 409；
6. Redis 不可用返回 503，不进入业务逻辑。

默认处理中 TTL 为 300 秒，完成态 TTL 为 3 天。`multipart/*`、SSE 和超出 1 MiB 上限的内容不进入响应复用。

## 路线优化一致性

`RouteOptimizationServiceImpl` 的应用链路使用：

- Redisson 路线级分布式锁；
- `TransactionTemplate` 明确事务边界；
- `SELECT ... FOR UPDATE` 锁定完整路线日程；
- 完整集合、跨路线、重复景点和连续位置校验；
- `visit_order=-id` 预留后再写最终正序；
- `uk_route_day_visit_order(route_id, day_number, visit_order)` 唯一键；
- 事务提交后写 Redis 历史，缓存失败不回滚 MySQL。

当前 `optimizationType` 会被校验和记录，但自动排序路径尚未实现独立的时间、费用和综合目标函数。

## RabbitMQ 可靠通知

当前已接线的可靠通知链路包括：

- `MessageProducerService`：messageId、可选状态落库和 RabbitTemplate 发布；
- `RabbitMQConfig`：publisher confirm 与 mandatory returned callback；
- `ReliableNotificationRabbitConfig`：主队列、5/30/120 秒 TTL 重试队列和 DLQ；
- `ReliableNotificationConsumer`：手动 ACK、重试/DLQ 转发确认；
- `RedisMessageIdempotencyService`：`PROCESSING/COMPLETED` 快速路径；
- `notification.source_message_id`：MySQL 唯一键最终幂等；
- `mq_message_status`：可选状态持久化与补偿抢占原语。

当前没有自动定时扫描 `mq_message_status` 并重投的运行任务，因此不能描述为完整自动 Outbox 补偿。云端配置见 [docs/infrastructure/RABBITMQ_CLOUD_CONFIGURATION.md](docs/infrastructure/RABBITMQ_CLOUD_CONFIGURATION.md)。

## 数据库

六个业务模块保留同步的 `src/main/resources/db/init_complete.sql`，当前初始化 22 张表。Compose 使用 attraction-service 下的脚本初始化新数据卷。

重要增量迁移位于 `docs/infrastructure/`：

- `AMAP_ROUTE_DATA_MIGRATION.sql`
- `ATTRACTION_REVIEW_IDEMPOTENCY_MIGRATION.sql`
- `MQ_RELIABLE_NOTIFICATION_MIGRATION.sql`
- `MQ_MESSAGE_STATUS_MIGRATION.sql`
- `ROUTE_COMMENT_CONSISTENCY_MIGRATION.sql`
- `ROUTE_OPTIMIZATION_CONSISTENCY_MIGRATION.sql`
- `USER_COLLECTION_ACTION_INDEX_MIGRATION.sql`

执行迁移前应备份数据库，并按脚本末尾查询核对列、索引和数据数量。

## 测试

```powershell
# 全量测试
..\mvnw.cmd -q -f pom.xml clean test

# 路线优化定向测试
..\mvnw.cmd -q -f pom.xml -pl route-service -am `
  "-Dtest=RouteOptimizationServiceImplTest,RouteAttractionServiceImplTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

2026-08-20 本地全量结果为 245 tests、0 failure、0 error、3 skipped。并发指标和复现资产见 [docs/showcase/EVIDENCE_INDEX.md](docs/showcase/EVIDENCE_INDEX.md)。

## 展示文档

- [核心链路时序图](docs/showcase/ARCHITECTURE_SEQUENCE_DIAGRAMS.md)
- [项目能力边界](docs/showcase/CAPABILITY_BOUNDARIES.md)
- [五分钟演示脚本](docs/showcase/DEMO_SCRIPT_5_MINUTES.md)
- [验收证据索引](docs/showcase/EVIDENCE_INDEX.md)
- [业务与工程治理计划](docs/PROJECT_HARDENING_PLAN.md)

## License

[Apache License 2.0](../LICENSE)
