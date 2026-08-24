# RabbitMQ Outbox 启用手册

## 设计边界

- `mq_message_status` 同时承担消息状态记录和事务 Outbox 记录。
- 业务事务先写入 `PENDING`，定时任务再投递 RabbitMQ。
- 数据库条件更新负责多实例抢占；消费者仍必须使用 Redis 和数据库双重幂等。
- 当前已接入用户注册事务的欢迎通知，登录提醒仍是非关键旁路。

## 启用前提

1. 先备份 `travel_website`。
2. 手工执行 `backend/docs/infrastructure/MQ_MESSAGE_STATUS_MIGRATION.sql`。
3. 确认云端 `49.234.187.76:5672` 可达，并完成 RabbitMQ 拓扑、用户和 vhost 验证。
4. 确认可靠通知消费者已启动，再启用生产者和 Outbox。

## 配置顺序

```text
MQ_RELIABLE_NOTIFICATION_TOPOLOGY_ENABLED=true
MQ_RELIABLE_NOTIFICATION_CONSUMER_ENABLED=true
MQ_STATUS_PERSISTENCE_ENABLED=true
MQ_RELIABLE_NOTIFICATION_PRODUCER_ENABLED=true
MQ_OUTBOX_ENABLED=true
```

Outbox 默认关闭。未执行迁移或 RabbitMQ 网络未恢复时，不要打开 `MQ_OUTBOX_ENABLED` 做生产验证。

## 验收点

- 注册成功与 `mq_message_status` 的 `PENDING` 记录同事务提交；注册回滚时不应留下 Outbox 记录。
- 定时任务将记录抢占为 `RETRYING`，发送后进入 `DISPATCHED`，broker confirm 后进入 `CONFIRMED`。
- RabbitMQ 不可达时记录进入 `FAILED` 并按退避策略重试，不阻塞其他业务请求。
- 同一消息重复投递时，通知表的 `source_message_id` 唯一索引保证最终只落一条通知。
