# RabbitMQ 云端配置说明

## 当前结论

- `newagent` 和 `novel_agent` 中没有 RabbitMQ 配置，两个项目只把云端地址（占位 `<CLOUD_HOST_PLACEHOLDER>`）用作 Milvus 云端地址。
- 本机在 2026-08-10 验证 `<CLOUD_HOST_PLACEHOLDER>:5672` 和 `<CLOUD_HOST_PLACEHOLDER>:15672` TCP 可达。
- 旅游项目当前按 RabbitMQ 与 Milvus 使用同一云服务器处理，但该地址仍可通过 `RABBITMQ_HOST` 覆盖，不能把该推断当作永久基础设施事实。
- RabbitMQ 可靠通知的代码链路已完成本地编译和 Mock 状态机验证；云端 AMQP 登录、publisher confirm、消费和故障恢复仍未验收。

## 当前混合基础设施拓扑

| 依赖 | 运行位置 | 旅游后端默认入口 | 可覆盖变量 | 当前边界 |
| --- | --- | --- | --- | --- |
| MySQL | Win11 本机 | `127.0.0.1:3306/travel_website` | `DB_HOST`、`DB_PORT`、`DB_NAME` | 业务服务直接使用，必须先完成本机数据库初始化 |
| Redis | Win11 本机 | `127.0.0.1:6379/0` | `REDIS_HOST`、`REDIS_PORT`、`REDIS_DB` | 业务缓存、锁和幂等辅助依赖，当前未宣称 Redis 故障演练完成 |
| RabbitMQ | 云服务器 | `<CLOUD_HOST_PLACEHOLDER>:5672` | `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_VHOST` | 已具备版本化可靠通知拓扑、生产确认、消费幂等/重试/DLQ 和可选状态补偿；真实云端链路仍未验收 |
| Milvus | 云服务器 | 不属于旅游后端当前运行时依赖 | 由 `newagent`/`novel_agent` 各自管理 | 旅游项目没有 Milvus SDK、Client 或集合配置，不能把 Milvus 检索能力计入本项目已交付能力 |

本机开发或测试可以使用以下非敏感环境变量；RabbitMQ 用户名和密码必须由运行环境注入：

```text
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=travel_website
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_DB=0
RABBITMQ_HOST=<CLOUD_HOST_PLACEHOLDER>
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
RABBITMQ_HOST=<CLOUD_HOST_PLACEHOLDER>
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

2026-08-11 已完成：

- `mvn -q -pl common -am test`：common 模块 36 个测试通过，失败 0、错误 0、跳过 0。
- `mvn -q -pl collection-service -am test`：common 36 个、collection-service 13 个测试通过，失败 0、错误 0、跳过 0。
- `mvn -q -DskipTests compile`：全模块编译通过。
- 测试覆盖可靠通知生产开关、Redis 幂等、publisher confirm/nack/returned、版本化拓扑、消费者重复投递/重试/DLQ/ACK 边界和状态补偿抢占。

2026-08-16 追加验证：

- 云端凭据已配置到 `deploy/.env`（只存放用户名/密码占位 `<cloud-user>/<cloud-password>`，真实值不落 Git），通过 Management API Basic Auth 认证成功。
- Management API `/api/overview` → **HTTP 200**，节点名 `rabbit@98a46279fbab`。
- AMQP TCP 5672 持续可达；PowerShell 无 PSCloudAMQP 模块无法做底层握手，但应用层凭据已通过 Management API 验证有效。
- 健康检查脚本 `deploy/scripts/check-infra-health.ps1` 可直接复现所有结果。

尚未完成：

- 本机 `travel_website` 尚未由本轮自动执行迁移脚本；不执行破坏性数据库操作，也不假设数据库密码。
- 仍未在真实 broker 上发送业务消息验证 publisher confirm、消费 ACK、retry/DLQ 完整链路——需要部署包含 `ReliableNotificationConsumer` 的服务后发一条测试消息确认。

## 尚未宣称完成的部分

- 本机 `travel_website` 尚未由本轮自动执行迁移脚本；本地 MySQL 密码和数据备份由部署者确认。
- **云端凭据已就绪**（2026-08-16 追加验证）：Management API Basic Auth 200 + 节点名确认可访问。但仍不宣称 AMQP 发布确认或消费链路完成——需要部署服务后发一条测试消息验证 publisher confirm / consumer ACK / retry / DLQ 完整链路。
- 尚未完成 Redis 不可用、RabbitMQ 断连、消费者重启和 DLQ 人工回放的真实故障演练；当前结论来自单元测试和契约替身。
- 本轮没有给既有队列追加 DLX 参数，避免云端同名队列触发 RabbitMQ `PRECONDITION_FAILED`；拓扑变更使用版本化资源和迁移窗口。
