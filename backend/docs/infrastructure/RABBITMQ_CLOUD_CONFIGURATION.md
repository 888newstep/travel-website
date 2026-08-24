# RabbitMQ 云端配置说明

## 当前结论

- 当前配置的云 RabbitMQ 地址为 `49.234.187.76`；2026-08-22 已验证 `5672` AMQP 连接和 `15672` Management API 认证成功。
- 旅游项目只使用该云服务器上的 RabbitMQ；连接地址可通过 `RABBITMQ_HOST` 覆盖，部署时不得依赖硬编码主机。
- RabbitMQ 可靠通知的代码链路、数据库迁移、显式 Live IT 和一键验收脚本均已完成，并通过真实云端发布、消费、故障恢复和并发验收。

## 当前混合基础设施拓扑

| 依赖 | 运行位置 | 旅游后端默认入口 | 可覆盖变量 | 当前边界 |
| --- | --- | --- | --- | --- |
| MySQL | Win11 本机 | `127.0.0.1:3306/travel_website` | `DB_HOST`、`DB_PORT`、`DB_NAME` | 业务服务直接使用，必须先完成本机数据库初始化 |
| Redis | Win11 本机 | `127.0.0.1:6379/3` | `REDIS_HOST`、`REDIS_PORT`、`REDIS_DB` | 业务缓存、锁和幂等辅助依赖；收藏写链路中断与恢复演练已完成 |
| RabbitMQ | 云服务器 | `49.234.187.76:5672` | `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_VHOST` | `5672/15672` 已连通并完成真实链路验收；凭据只由未跟踪环境配置注入 |

本项目不使用向量数据库，不将其纳入架构、测试或交付范围。

本机开发或测试可以使用以下非敏感环境变量；RabbitMQ 用户名和密码必须由运行环境注入：

```text
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=travel_website
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_DB=3
RABBITMQ_HOST=49.234.187.76
RABBITMQ_PORT=5672
RABBITMQ_VHOST=/
MQ_STATUS_PERSISTENCE_ENABLED=false
MQ_STATUS_COMPENSATION_ENABLED=false
MQ_RELIABLE_NOTIFICATION_TOPOLOGY_ENABLED=false
MQ_RELIABLE_NOTIFICATION_PRODUCER_ENABLED=false
MQ_RELIABLE_NOTIFICATION_CONSUMER_ENABLED=false
```

## 必需环境变量

```text
RABBITMQ_HOST=49.234.187.76
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=<cloud-user>
RABBITMQ_PASSWORD=<cloud-password>
RABBITMQ_VHOST=/
```

密码、用户名和生产环境连接串不得提交到 Git。没有凭证时只能完成 TCP 可达性验证，不能证明 AMQP 认证和发布确认成功。

## 已落地配置

- 开启 `publisher-confirm-type=correlated`，每条消息生成唯一 `CorrelationData`。
- 开启 publisher returns，并设置 `template.mandatory=true`，交换机或路由键不存在时记录 returned message。
- 增加 5 秒连接超时和 30 秒心跳，避免云端网络异常造成无界等待。
- 保留模板重试并设置最大间隔和退避倍率；若开启消息状态持久化，重试必须结合幂等键使用。
- 日志区分“已提交等待 confirm”和“broker ack/nack”，不再把 `convertAndSend` 返回当作最终投递成功。

## 可靠通知 V1 拓扑

可靠通知不修改已有 `notification.queue`，使用独立的版本化资源：

- 主交换机/队列：`notification.reliable.exchange.v1`、`notification.reliable.queue.v1`。
- 三档 TTL 重试队列：5 秒、30 秒、120 秒，过期后通过 DLX 回流主交换机。
- 死信交换机/队列：`notification.reliable.dlx.v1`、`notification.reliable.dlq.v1`。
- 消费端使用手动 ACK；业务落库成功或失败消息转发获得 publisher confirm 后，才 ACK 原消息。
- `source_message_id` 唯一索引与 Redis `PROCESSING/COMPLETED` 双层幂等，数据库唯一键是最终兜底。

可靠拓扑的三个开关必须按顺序启用：先拓扑，再消费者，最后生产者。`MQ_RELIABLE_NOTIFICATION_PRODUCER_ENABLED=true` 前必须确认新队列已声明且消费者健康。

生产者开关关闭时，应用会直接跳过通知发布，不会回退到没有仓库内消费者的旧 `notification.queue`。登录等主业务不会因 RabbitMQ 未启用或发布失败而失败。

## 部署顺序与回滚

1. 备份本机 `travel_website`，人工执行 `backend/docs/infrastructure/MQ_RELIABLE_NOTIFICATION_MIGRATION.sql`，确认 `notification.source_message_id` 和唯一索引存在。
2. 在云服务器确认 RabbitMQ 用户、密码、vhost、TLS/防火墙策略和目标拓扑权限；凭证只通过环境变量注入。
3. 部署包含 `ReliableNotificationRabbitConfig` 的版本，先设置 `MQ_RELIABLE_NOTIFICATION_TOPOLOGY_ENABLED=true`，保持 producer 和 consumer 关闭。
4. 启用 `MQ_RELIABLE_NOTIFICATION_CONSUMER_ENABLED=true`，发送一条测试消息，验证主队列、重试队列、DLQ、Redis 幂等和 MySQL 落库。
5. 迁移确认后设置 `MQ_STATUS_PERSISTENCE_ENABLED=true`；需要定时补偿的服务再设置 `MQ_STATUS_COMPENSATION_ENABLED=true`，并确认该服务启用了 Spring Scheduling。
6. 观察至少一个完整 confirm 和消费周期后，最后设置 `MQ_RELIABLE_NOTIFICATION_PRODUCER_ENABLED=true`。
7. 回滚时先关闭 producer，保留版本化队列和 consumer 处理存量消息；不要删除队列或直接改写既有队列参数。

## 消息状态持久化第一阶段

旅游后端已落地 RabbitMQ 发布状态的第一阶段基础设施，但默认不启用：

- 五个业务服务统一读取 `MQ_STATUS_PERSISTENCE_ENABLED`，默认值为 `false`。
- 开启后，生产端先写入 `PENDING`，RabbitTemplate 调用返回后迁移到 `DISPATCHED`；`DISPATCHED` 不等于 broker confirm。
- publisher confirm ack 迁移到 `CONFIRMED`，nack 迁移到 `FAILED`，returned message 根据 AMQP `message_id` 迁移到 `RETURNED`。
- 状态更新失败只记录日志，不让 RabbitMQ 回调线程因本地 MySQL 故障崩溃，也不把状态未知伪造为发送失败。
- 已有数据库的增量迁移脚本为 `backend/docs/infrastructure/MQ_MESSAGE_STATUS_MIGRATION.sql`；执行迁移并验证表存在后，才允许设置 `MQ_STATUS_PERSISTENCE_ENABLED=true`。
- 新增 `RETRYING` 状态和条件抢占补偿任务；补偿记录先原子抢占，再发送原始 JSON，confirm/returned/nack 回调负责最终状态收敛。
- 补偿任务默认关闭，可通过 `MQ_STATUS_COMPENSATION_BATCH_SIZE`、`MQ_STATUS_COMPENSATION_MAX_ATTEMPTS`、`MQ_STATUS_COMPENSATION_STALE_AFTER_SECONDS` 和 `MQ_STATUS_COMPENSATION_RETRY_DELAY_SECONDS` 调整。

本阶段不等同于完整 Outbox 或消息最终一致性方案：业务事务与状态表插入尚未绑定在同一事务中，补偿任务也不能替代真实云端故障演练。

## 本轮验证证据

2026-08-20 追加验证：

- 已执行 `MQ_RELIABLE_NOTIFICATION_MIGRATION.sql`，本机 `notification.source_message_id` 和 `uk_notification_source_message` 均存在。
- 可靠消息定向回归通过：`RabbitMQConfigTest`、`MessageProducerServiceTest`、`MqMessageStatusServiceTest`、`ReliableNotificationConsumerTest`、`NotificationServiceImplTest` 均为 0 failure、0 error。
- `ReliableNotificationLiveIT` 已离线编译，`ops/rabbitmq/run-reliable-notification-live-test.ps1` 已通过 Windows PowerShell 5.1 语法、纯 ASCII 和缺少主机时安全失败验证。
- 配置 `RABBITMQ_HOST` 后执行 `.\ops\rabbitmq\run-reliable-notification-live-test.ps1`；结果写入 `run-logs/rabbitmq/<timestamp>/run-summary.json`，日志写入前会脱敏主机、用户名和密码。

2026-08-22 本机与数据库验证：

- `mvn -q -pl common -am test`：common 模块 36 个测试通过，失败 0、错误 0、跳过 0。
- `mvn -q -pl collection-service -am test`：common 36 个、collection-service 13 个测试通过，失败 0、错误 0、跳过 0。
- `mvn -q -DskipTests compile`：全模块编译通过。
- 测试覆盖可靠通知生产开关、Redis 幂等、publisher confirm/nack/returned、版本化拓扑、消费者重复投递/重试/DLQ/ACK 边界和状态补偿抢占。

2026-08-22 云端 Live IT：

- `5672` TCP、AMQP 认证和 `15672` Management API 认证均成功。
- 发布消费、重复投递幂等、非法消息 DLQ、修正后 DLQ 回放、一次 retry 后 DLQ、消费者重启和强制连接恢复均通过。
- 100 条唯一消息并发消费全部落库，耗时 696ms，吞吐 143.68 msg/s。
- 脱敏证据位于 `run-logs/rabbitmq/20260822-205241/`。

## 尚未宣称完成的部分

- 本机通知幂等与消息状态迁移已经完成；云端主机地址已通过 `RABBITMQ_HOST` 注入。
- **云端用户名、密码和 vhost 仅存在于本地未跟踪配置，文档、日志和测试摘要不记录凭据。**
- RabbitMQ 断连恢复、消费者重启和修正后 DLQ 回放已经完成；尚未演练云主机整体宕机、跨可用区容灾和 TLS 证书轮换。
- 本轮没有给既有队列追加 DLX 参数，避免云端同名队列触发 RabbitMQ `PRECONDITION_FAILED`；拓扑变更使用版本化资源和迁移窗口。
