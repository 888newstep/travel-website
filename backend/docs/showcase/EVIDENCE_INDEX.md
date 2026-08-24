# 验收证据索引

## 1. 证据说明

本索引记录 2026-08-20 至 2026-08-23 完成的测试和压测。MySQL、Redis、JMeter 运行在 Win11 本机，RabbitMQ 运行在云服务器；高德真实路线规划已通过进程环境注入 Key 完成脱敏验收。

证据等级：

- **L1 单元测试**：验证分支、异常和组件协作。
- **L2 本地集成**：启动真实 Java 服务并连接本机 MySQL/Redis。
- **L3 并发验收**：JMeter 并发 + 最终数据库状态核验 + 自动清理。
- **L4 外部实测**：调用真实高德或云 RabbitMQ；两者均已完成真实外部验收。

## 2. 2026-08-23 六阶段验收

| 阶段 | 结论 | 核心事实 | 证据 |
|------|------|----------|------|
| 1. 全栈基线 | PASS | Gateway 与 5 个业务服务全部健康，使用 `e2e` 配置直连本机依赖，不依赖 Nacos | `run-logs/e2e/20260823-214149/startup-summary.json` |
| 2. 核心业务 | PASS | 公开浏览、登录、路线创建/日程/发布、真实高德交通、收藏评论、游记、分享、文件、通知和统计全链路通过 | `run-logs/e2e-acceptance/20260823-214703/run-summary.json` |
| 3. 权限异常 | PASS | 缺失/非法/过期 JWT、伪造身份头、跨用户对象访问、普通用户访问管理员接口均按预期拒绝 | `run-logs/e2e-acceptance/20260823-214703/run-summary.json` |
| 4. 依赖故障 | PASS | Redis 中断写请求 503 且不写库，恢复后可用；云 RabbitMQ 完成重试、DLQ、回放、重启和断连恢复 | `run-logs/redis-outage/20260823-214912-202/`、`run-logs/rabbitmq/20260823-214758/` |
| 5. 并发幂等 | PASS | 100 并发得到 1 个 200、99 个处理中 409、0 非预期响应，最终仅一条业务记录并恢复测试前基线 | `run-logs/jmeter/20260823-220811-441/`、`run-logs/jmeter-gateway-idempotency/20260823-220544-198/` |
| 6. 证据材料 | PASS | 验收脚本、JSON 摘要、服务日志、HTML 报告、能力边界和面试复盘材料均已落库 | [ACCEPTANCE_REPORT_20260823.md](ACCEPTANCE_REPORT_20260823.md) |

核心业务与权限矩阵共 62 项检查，62 项通过，临时路线、收藏、评论、游记、分享和文件均自动清理。

## 3. 后端全量测试

执行命令：

```powershell
.\mvnw.cmd -q -f backend\pom.xml clean test
```

当前 Surefire 汇总：

| 报告数 | Tests | Failures | Errors | Skipped |
|--------|-------|----------|--------|---------|
| 71 | 295 | 0 | 0 | 3 |

报告目录为 `backend/*/target/surefire-reports/`。关键报告包括：

- `backend/common/target/surefire-reports/TEST-travel.common.config.HttpIdempotencyFilterTest.xml`
- `backend/common/target/surefire-reports/TEST-travel.common.config.JwtAuthenticationFilterTest.xml`
- `backend/common/target/surefire-reports/TEST-travel.common.utils.AMapRouteServiceTest.xml`
- `backend/common/target/surefire-reports/TEST-travel.common.service.MessageProducerServiceTest.xml`
- `backend/common/target/surefire-reports/TEST-travel.common.service.MqMessageStatusServiceTest.xml`
- `backend/common/target/surefire-reports/TEST-travel.common.repository.RouteRepositoryTest.xml`
- `backend/collection-service/target/surefire-reports/TEST-travel.collection.messaging.ReliableNotificationConsumerTest.xml`
- `backend/route-service/target/surefire-reports/TEST-travel.route.controller.RouteControllerTest.xml`
- `backend/route-service/target/surefire-reports/TEST-travel.route.service.RouteOptimizationServiceImplTest.xml`
- `backend/route-service/target/surefire-reports/TEST-travel.route.service.RouteAttractionServiceImplTest.xml`

`target` 报告是本地构建产物，需要在新的环境中重新执行测试生成。

2026-08-23 最终全量测试模块结果：`common` 64、`user-service` 12、`attraction-service` 26、`route-service` 134、`collection-service` 44、`file-service` 3、`gateway` 12（其中 3 项需完整网关环境，已跳过）。

## 4. 并发与故障演练

| 场景 | 等级 | 并发与结果 | 性能摘要 | 最终一致性 | 证据目录 |
|------|------|------------|----------|------------|----------|
| 路线收藏同键幂等 | L3 | 100 并发：1 个 HTTP 200，99 个处理中 HTTP 409，0 失败 | 平均 3342.57 ms，P95 4686 ms，P99 4707 ms，20.08 req/s | 完成后响应可重放，收藏记录 1 条 | `run-logs/jmeter/20260820-105116-454/` |
| Redis 中断 | L2 | 幂等 Redis 停止后写请求 HTTP 503 | 恢复 Redis 后查询回到 HTTP 200 | 中断期间业务记录不增加；重复 SQL 被 MySQL 1062 拒绝 | `run-logs/redis-outage/20260820-113223-525/` |
| 景点点评 UPSERT | L3 | 100 个不同幂等键，100 个 HTTP 200，0 失败 | 平均 1316.13 ms，P95 1581 ms，P99 1598 ms，62.31 req/s | 响应点评 ID 唯一，最终记录 1 条 | `run-logs/jmeter-review/20260820-140022-953/` |
| 路线评论点赞 | L3 | 100 个不同用户，100 个 HTTP 200，0 失败 | 平均 1522.64 ms，P95 1946 ms，P99 1985 ms，49.95 req/s | `likes_count=100`，行为记录 100，不同用户 100 | `run-logs/jmeter-comment-like/20260820-145625-099/` |
| 路线优化 | L3 | 同一路线、100 个不同幂等键，100 个 HTTP 200，0 失败 | 平均 6508.34 ms，P95 7438 ms，P99 7532 ms，13.24 req/s | `changed=true` 仅 1 次；位置唯一连续；历史 1 条 | `run-logs/jmeter-route-optimization/20260820-153122-431/` |
| 网关层路线收藏同键幂等 | L3 | 100 并发：1 个 HTTP 200，99 个 HTTP 409，0 失败 | 平均 627.52 ms，P95 644 ms，P99 669 ms，51.23 req/s | 收尾重放通过，收藏记录 1 条 | `run-logs/jmeter/20260822-185802-498/` |
| Redis 中断恢复复验 | L2 | 中断期间写请求 HTTP 503 | 恢复后查询 HTTP 200 | 中断期间记录不增加；重复 SQL 被 MySQL 1062 拒绝 | `run-logs/redis-outage/20260822-184544-146/` |
| 云 RabbitMQ 并发消费 | L4 | 100 条唯一消息全部消费落库 | 696 ms，143.68 msg/s | 数据库记录 100 条；同时通过重复幂等、retry/DLQ、回放、重启和断连恢复 | `run-logs/rabbitmq/20260822-205241/` |
| 路线访问分析 | L2 | 3 次认证访问、2 次匿名访问 | 真实 HTTP + MySQL/Redis；非容量测试 | 浏览量增加 5、明细 5、UV 3、哈希 64 位、原始身份列 0；清理通过 | `run-logs/route-visit/20260822-213119-067/` |
| 2026-08-23 网关收藏幂等复验 | L3 | 100 并发：1 个 HTTP 200、99 个处理中 HTTP 409、0 失败 | 平均 3002.37 ms，P95 3365 ms，P99 3394 ms，13.46 req/s | 最终记录 1 条；测试前 0 条，结束后成功恢复为 0 条 | `run-logs/jmeter/20260823-220811-441/`、`run-logs/jmeter-gateway-idempotency/20260823-220544-198/` |
| 2026-08-23 Redis 中断复验 | L2 | 中断期间 HTTP 503 | Redis 重启后恢复 | 中断期间记录不变；MySQL 1062 唯一键兜底 | `run-logs/redis-outage/20260823-214912-202/` |
| 2026-08-23 云 RabbitMQ 复验 | L4 | 100 条唯一消息全部消费 | 901 ms，110.99 msg/s | 重复幂等、retry/DLQ、修正回放、消费者重启和断连恢复全部通过 | `run-logs/rabbitmq/20260823-214758/` |
| 2026-08-23 路线访问分析复验 | L2 | 3 次认证访问、2 次匿名访问 | 真实 HTTP + MySQL/Redis | PV 5、UV 3、哈希 64 位、无原始身份列；数据库基线恢复通过 | `run-logs/route-visit/20260823-221116-104/` |

每个成功目录都包含 `run-summary.json`；JMeter 场景还包含 `results.jtl`、`jmeter.log`、HTML 报告以及对应服务日志。脚本结束后会核对临时数据、端口和进程是否清理。

统一指标口径和不适用项说明见 [VERIFICATION_METRICS_MATRIX_20260822.md](../performance/VERIFICATION_METRICS_MATRIX_20260822.md)。矩阵不会将 HTTP 百分位强行套用到 RabbitMQ，也不会为写一致性或 Redis 故障场景伪造缓存命中率。

## 5. 高德路线证据

### 已完成

- `AMapRouteServiceTest` 使用本地 HTTP 桩覆盖两点、多点、配额失败、超时、无路径、响应体上限、非法坐标和 API Key 日志脱敏。
- 路线联动逻辑按 `route_attractions` 顺序构造相邻景点分段，并只接受高德返回的距离、时长和 `tmcs`。
- 数据迁移执行后：`route_attractions=23`、跨城市关系 0、`route_transport=0`。
- 迁移脚本：[AMAP_ROUTE_DATA_MIGRATION.sql](../infrastructure/AMAP_ROUTE_DATA_MIGRATION.sql)。

### 真实外呼

2026-08-23 已配置真实 Web 服务 Key 并执行：

```powershell
$env:AMAP_API_KEY = '<真实高德 Web 服务 Key>'
.\ops\amap\run-amap-route-live-test.ps1
```

验收结果：

- 两点路线 3：31,491 米、3,057 秒、161 个 `tmcs` 实时路况分段。
- 三点路线 1：146.583 公里、187.983 分钟、过路费 54 元、55 个导航步骤。
- `AMapRouteLiveIT`：1 项测试，0 failure、0 error、0 skipped。
- 脱敏证据：`run-logs/amap/20260823-175126/`。
- 详细记录：[AMAP_ROUTE_LIVE_VERIFICATION_20260823.md](../infrastructure/AMAP_ROUTE_LIVE_VERIFICATION_20260823.md)。
- 地点搜索、天气和四类周边设施真实调用均返回 `status=1`；接口、配额保护和结果摘要见 [AMAP_BUSINESS_INTEGRATION_20260823.md](../infrastructure/AMAP_BUSINESS_INTEGRATION_20260823.md)。

此前缺少 Key 的安全失败证据仍保留在 `run-logs/amap/20260820-130903/run-summary.json`，可用于展示配置缺失时的明确降级。

## 6. RabbitMQ 证据

### 已完成

- publisher confirm、mandatory returned、手动 ACK、重试转发确认和 DLQ 分支均有单元测试。
- Redis 消费幂等和 `notification.source_message_id` 数据库唯一键已实现。
- 可靠通知 Live IT 源码：`backend/collection-service/src/test/java/travel/collection/messaging/ReliableNotificationLiveIT.java`。
- 云端验收脚本和配置说明：[RABBITMQ_CLOUD_CONFIGURATION.md](../infrastructure/RABBITMQ_CLOUD_CONFIGURATION.md)。
- 数据库迁移：[MQ_RELIABLE_NOTIFICATION_MIGRATION.sql](../infrastructure/MQ_RELIABLE_NOTIFICATION_MIGRATION.sql) 与 [MQ_MESSAGE_STATUS_MIGRATION.sql](../infrastructure/MQ_MESSAGE_STATUS_MIGRATION.sql)。
- 2026-08-23 云端 Live IT `success=true`：覆盖发布消费、重复幂等、非法消息 DLQ、修正后 DLQ 回放、retry 后 DLQ、消费者重启、强制连接恢复和 100 消息并发消费。
- 最新并发结果：100 条消息落库 100 条，耗时 901ms，吞吐 110.99 msg/s；证据为 `run-logs/rabbitmq/20260823-214758/`。

## 7. 可重复执行资产

| 目标 | 资产 |
|------|------|
| 路线收藏幂等 | `ops/jmeter/route-collection-idempotency.jmx`、`ops/jmeter/run-idempotency-test.ps1` |
| 隔离网关路线收藏幂等 | `ops/jmeter/run-live-gateway-idempotency-test.ps1` |
| Redis 故障 | `ops/jmeter/run-redis-outage-test.ps1` |
| 景点点评 | `ops/jmeter/attraction-review-upsert.jmx`、`ops/jmeter/run-attraction-review-upsert-test.ps1` |
| 评论点赞 | `ops/jmeter/route-comment-like-concurrency.jmx`、`ops/jmeter/run-route-comment-like-test.ps1` |
| 路线优化 | `ops/jmeter/route-optimization-concurrency.jmx`、`ops/jmeter/run-route-optimization-test.ps1` |
| 高德实测 | `ops/amap/run-amap-route-live-test.ps1` |
| RabbitMQ 实测 | `ops/rabbitmq/run-reliable-notification-live-test.ps1` |
| 路线访问分析实测 | `ops/jmeter/run-route-visit-analytics-live-test.ps1` |

路线访问的统计口径、隐私边界和迁移验证见 [ROUTE_VISIT_ANALYTICS_VERIFICATION_20260822.md](../infrastructure/ROUTE_VISIT_ANALYTICS_VERIFICATION_20260822.md)。

## 8. 证据使用原则

- 面试或答辩时优先展示 `run-summary.json`、数据库最终状态和关键日志，不只展示 JMeter 绿色结果。
- 任何外部能力都同时展示“成功证据”和“缺密钥/超时/配额失败”的降级证据。
- 不把单元测试当作真实云服务实测，不把本地桩响应当作高德生产响应。
- 不公开 JWT 密钥、数据库密码、RabbitMQ 凭据或高德/AI Key；日志和截图必须脱敏。
