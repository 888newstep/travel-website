# 验收证据索引

## 1. 证据说明

本索引记录 2026-08-20 在 Win11 本机完成的测试和压测。MySQL、Redis、JMeter 运行在本机；云 RabbitMQ 和真实高德调用因当前进程未配置凭据而保留为待验收项。

证据等级：

- **L1 单元测试**：验证分支、异常和组件协作。
- **L2 本地集成**：启动真实 Java 服务并连接本机 MySQL/Redis。
- **L3 并发验收**：JMeter 并发 + 最终数据库状态核验 + 自动清理。
- **L4 外部实测**：调用真实高德或云 RabbitMQ；当前尚未完成。

## 2. 后端全量测试

执行命令：

```powershell
.\mvnw.cmd -q -f backend\pom.xml clean test
```

当前 Surefire 汇总：

| 报告数 | Tests | Failures | Errors | Skipped |
|--------|-------|----------|--------|---------|
| 60 | 245 | 0 | 0 | 3 |

报告目录为 `backend/*/target/surefire-reports/`。关键报告包括：

- `backend/common/target/surefire-reports/TEST-travel.common.config.HttpIdempotencyFilterTest.xml`
- `backend/common/target/surefire-reports/TEST-travel.common.config.JwtAuthenticationFilterTest.xml`
- `backend/common/target/surefire-reports/TEST-travel.common.utils.AMapRouteServiceTest.xml`
- `backend/common/target/surefire-reports/TEST-travel.common.service.MessageProducerServiceTest.xml`
- `backend/common/target/surefire-reports/TEST-travel.common.service.MqMessageStatusServiceTest.xml`
- `backend/collection-service/target/surefire-reports/TEST-travel.collection.messaging.ReliableNotificationConsumerTest.xml`
- `backend/route-service/target/surefire-reports/TEST-travel.route.service.RouteOptimizationServiceImplTest.xml`
- `backend/route-service/target/surefire-reports/TEST-travel.route.service.RouteAttractionServiceImplTest.xml`

`target` 报告是本地构建产物，需要在新的环境中重新执行测试生成。

## 3. 并发与故障演练

| 场景 | 等级 | 并发与结果 | 性能摘要 | 最终一致性 | 证据目录 |
|------|------|------------|----------|------------|----------|
| 路线收藏同键幂等 | L3 | 100 并发：1 个 HTTP 200，99 个处理中 HTTP 409，0 失败 | 平均 3342.57 ms，P95 4686 ms，P99 4707 ms，20.08 req/s | 完成后响应可重放，收藏记录 1 条 | `run-logs/jmeter/20260820-105116-454/` |
| Redis 中断 | L2 | 幂等 Redis 停止后写请求 HTTP 503 | 恢复 Redis 后查询回到 HTTP 200 | 中断期间业务记录不增加；重复 SQL 被 MySQL 1062 拒绝 | `run-logs/redis-outage/20260820-113223-525/` |
| 景点点评 UPSERT | L3 | 100 个不同幂等键，100 个 HTTP 200，0 失败 | 平均 1316.13 ms，P95 1581 ms，P99 1598 ms，62.31 req/s | 响应点评 ID 唯一，最终记录 1 条 | `run-logs/jmeter-review/20260820-140022-953/` |
| 路线评论点赞 | L3 | 100 个不同用户，100 个 HTTP 200，0 失败 | 平均 1522.64 ms，P95 1946 ms，P99 1985 ms，49.95 req/s | `likes_count=100`，行为记录 100，不同用户 100 | `run-logs/jmeter-comment-like/20260820-145625-099/` |
| 路线优化 | L3 | 同一路线、100 个不同幂等键，100 个 HTTP 200，0 失败 | 平均 6508.34 ms，P95 7438 ms，P99 7532 ms，13.24 req/s | `changed=true` 仅 1 次；位置唯一连续；历史 1 条 | `run-logs/jmeter-route-optimization/20260820-153122-431/` |

每个成功目录都包含 `run-summary.json`；JMeter 场景还包含 `results.jtl`、`jmeter.log`、HTML 报告以及对应服务日志。脚本结束后会核对临时数据、端口和进程是否清理。

## 4. 高德路线证据

### 已完成

- `AMapRouteServiceTest` 使用本地 HTTP 桩覆盖两点、多点、配额失败、超时、无路径、响应体上限、非法坐标和 API Key 日志脱敏。
- 路线联动逻辑按 `route_attractions` 顺序构造相邻景点分段，并只接受高德返回的距离、时长和 `tmcs`。
- 数据迁移执行后：`route_attractions=23`、跨城市关系 0、`route_transport=0`。
- 迁移脚本：[AMAP_ROUTE_DATA_MIGRATION.sql](../infrastructure/AMAP_ROUTE_DATA_MIGRATION.sql)。

### 待完成

真实外呼预检结果位于 `run-logs/amap/20260820-130903/run-summary.json`，失败原因是当前进程未设置 `AMAP_API_KEY`。这属于可验证的安全失败，不是高德成功证据。

配置真实 Web 服务 Key 后执行：

```powershell
$env:AMAP_API_KEY = '<真实高德 Web 服务 Key>'
.\ops\amap\run-amap-route-live-test.ps1
```

验收必须同时得到一条两景点路线和一条多景点路线的脱敏响应摘要。

## 5. RabbitMQ 证据

### 已完成

- publisher confirm、mandatory returned、手动 ACK、重试转发确认和 DLQ 分支均有单元测试。
- Redis 消费幂等和 `notification.source_message_id` 数据库唯一键已实现。
- 可靠通知 Live IT 源码：`backend/collection-service/src/test/java/travel/collection/messaging/ReliableNotificationLiveIT.java`。
- 云端验收脚本和配置说明：[RABBITMQ_CLOUD_CONFIGURATION.md](../infrastructure/RABBITMQ_CLOUD_CONFIGURATION.md)。
- 数据库迁移：[MQ_RELIABLE_NOTIFICATION_MIGRATION.sql](../infrastructure/MQ_RELIABLE_NOTIFICATION_MIGRATION.sql) 与 [MQ_MESSAGE_STATUS_MIGRATION.sql](../infrastructure/MQ_MESSAGE_STATUS_MIGRATION.sql)。

### 待完成

预检结果位于 `run-logs/rabbitmq/20260820-133520/run-summary.json`，失败原因是当前进程未配置 `RABBITMQ_HOST`。因此目前不能声称已完成真实云 broker 验收。

配置云端凭据后，按云配置文档执行 Live IT，至少保留以下结果：

1. publisher confirm ACK；
2. 消费落库；
3. 同 messageId 重复投递只保留一条通知；
4. 一次真实 TTL 重试；
5. 非法消息或超过重试上限进入 DLQ；
6. 消费者重启和 RabbitMQ 断连恢复记录。

## 6. 可重复执行资产

| 目标 | 资产 |
|------|------|
| 路线收藏幂等 | `ops/jmeter/route-collection-idempotency.jmx`、`ops/jmeter/run-idempotency-test.ps1` |
| Redis 故障 | `ops/jmeter/run-redis-outage-test.ps1` |
| 景点点评 | `ops/jmeter/attraction-review-upsert.jmx`、`ops/jmeter/run-attraction-review-upsert-test.ps1` |
| 评论点赞 | `ops/jmeter/route-comment-like-concurrency.jmx`、`ops/jmeter/run-route-comment-like-test.ps1` |
| 路线优化 | `ops/jmeter/route-optimization-concurrency.jmx`、`ops/jmeter/run-route-optimization-test.ps1` |
| 高德实测 | `ops/amap/run-amap-route-live-test.ps1` |
| RabbitMQ 实测 | `ops/rabbitmq/run-reliable-notification-live-test.ps1` |

## 7. 证据使用原则

- 面试或答辩时优先展示 `run-summary.json`、数据库最终状态和关键日志，不只展示 JMeter 绿色结果。
- 任何外部能力都同时展示“成功证据”和“缺密钥/超时/配额失败”的降级证据。
- 不把单元测试当作真实云服务实测，不把本地桩响应当作高德生产响应。
- 不公开 JWT 密钥、数据库密码、RabbitMQ 凭据或高德/AI Key；日志和截图必须脱敏。
