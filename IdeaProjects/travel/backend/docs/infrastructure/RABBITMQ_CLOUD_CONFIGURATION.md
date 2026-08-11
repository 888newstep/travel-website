# RabbitMQ 云端配置说明

## 当前结论

- `newagent` 和 `novel_agent` 中没有 RabbitMQ 配置，两个项目只把 `<CLOUD_HOST_PLACEHOLDER>` 用作 Milvus 云端地址。
- 本机在 2026-08-10 验证 `<CLOUD_HOST_PLACEHOLDER>:5672` 和 `<CLOUD_HOST_PLACEHOLDER>:15672` TCP 可达。
- 旅游项目暂按 RabbitMQ 与 Milvus 使用同一云服务器处理，但该地址仍可通过 `RABBITMQ_HOST` 覆盖，不能把该推断当作永久基础设施事实。

## 当前混合基础设施拓扑

| 依赖 | 运行位置 | 旅游后端默认入口 | 可覆盖变量 | 当前边界 |
| --- | --- | --- | --- | --- |
| MySQL | Win11 本机 | `127.0.0.1:3306/travel_website` | `DB_HOST`、`DB_PORT`、`DB_NAME` | 业务服务直接使用，必须先完成本机数据库初始化 |
| Redis | Win11 本机 | `127.0.0.1:6379/0` | `REDIS_HOST`、`REDIS_PORT`、`REDIS_DB` | 业务缓存、锁和幂等辅助依赖，当前未宣称 Redis 故障演练完成 |
| RabbitMQ | 云服务器 | `<CLOUD_HOST_PLACEHOLDER>:5672` | `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_VHOST` | 已具备生产端确认和可选发布状态持久化第一阶段，消费端可靠性仍属于 P8.4 未完成项 |
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

## 消息状态持久化第一阶段

旅游后端已落地 RabbitMQ 发布状态的第一阶段基础设施，但默认不启用：

- 五个业务服务统一读取 `MQ_STATUS_PERSISTENCE_ENABLED`，默认值为 `false`。
- 开启后，生产端先写入 `PENDING`，RabbitTemplate 调用返回后迁移到 `DISPATCHED`；`DISPATCHED` 不等于 broker confirm。
- publisher confirm ack 迁移到 `CONFIRMED`，nack 迁移到 `FAILED`，returned message 根据 AMQP `message_id` 迁移到 `RETURNED`。
- 状态更新失败只记录日志，不让 RabbitMQ 回调线程因本地 MySQL 故障崩溃，也不把状态未知伪造为发送失败。
- 已有数据库的增量迁移脚本为 `backend/docs/infrastructure/MQ_MESSAGE_STATUS_MIGRATION.sql`；执行迁移并验证表存在后，才允许设置 `MQ_STATUS_PERSISTENCE_ENABLED=true`。

本阶段不等同于完整 Outbox 或消息最终一致性方案：业务事务与状态表插入尚未绑定在同一事务中，消费端幂等、重试队列、DLX、补偿任务和真实云端 AMQP 认证仍需单独验收。

## 本轮验证证据

2026-08-11 已完成：

- `mvn -q -pl common -am "-DforkCount=1" "-DreuseForks=false" test`：15 个消息可靠性相关测试通过。
- `mvn -q -DskipTests compile`：全模块编译通过。
- `route-service` Surefire 报告已生成 105 个用例，报告内失败数、错误数和跳过数均为 0；本次 Maven 进程级命令超过 5 分钟未返回，不能记为完整命令成功。

尚未完成：

- 本机 `travel_website` 尚未由本轮自动执行迁移脚本；不执行破坏性数据库操作，也不假设数据库密码。
- 没有云端 RabbitMQ 生产用户名和密码，因此没有宣称 AMQP 登录、publisher confirm 实际成功或 returned 实际触发。

## 尚未宣称完成的部分

- 当前代码只有生产端，没有对应的 `@RabbitListener` 消费端；消息消费、重试队列和死信队列仍属于 P8.4 后续工作。
- 本轮没有给既有队列追加 DLX 参数，避免云端已存在同名队列时触发 RabbitMQ `PRECONDITION_FAILED`；拓扑变更需要版本化队列或迁移窗口。
