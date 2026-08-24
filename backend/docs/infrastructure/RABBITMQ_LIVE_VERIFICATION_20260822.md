# RabbitMQ 真实链路验证记录

## 环境

- RabbitMQ：`49.234.187.76:5672`
- RabbitMQ Management API：`49.234.187.76:15672`
- RabbitMQ / Management 版本：`4.3.3`
- vhost：`/`
- MySQL：`127.0.0.1:3306/travel_website`
- Redis：`127.0.0.1:6379/0`

## 连通性与认证

- AMQP `5672` TCP 连接成功。
- Management API 未认证时返回 `401`，使用运行环境凭据认证成功。
- Live IT 前置检查：MySQL、Redis、RabbitMQ TCP 均通过。
- 通知幂等和消息状态迁移均检查通过。

## Live IT 结果

执行脚本：

```powershell
.\ops\rabbitmq\run-reliable-notification-live-test.ps1
```

本次结果为 `success=true`，验证通过：

1. 发布消费：publisher confirm 为 `CONFIRMED`，Redis 幂等状态为 `COMPLETED`，数据库落库 1 条。
2. 重复投递：数据库仍为 1 条，证明 Redis 和数据库双重幂等生效。
3. 非重试异常：非法通知进入 DLQ。
4. 死信回放：修正非法消息后从 DLQ 回放，数据库落库 1 条。
5. 可重试异常：外键异常经过 1 次重试后进入 DLQ，数据库落库 0 条。
6. 消费者恢复：停止并重启监听容器后，积压消息恢复消费并落库 1 条。
7. 连接恢复：调用连接工厂强制断开后自动重连，后续消息正常落库 1 条。
8. 并发消费：100 条唯一消息落库 100 条，耗时 696ms，吞吐 143.68 msg/s。

完整证据：`run-logs/rabbitmq/20260822-205241/`

## 注意事项

- 本机 Redis 开启了 `requirepass`，`deploy/.env` 中的 `REDIS_PASSWORD` 不能留空；本次验证仅通过进程参数临时注入，未写入仓库。
- RabbitMQ 4.x 不接受 transient non-exclusive queue，Live IT 的探针队列已改为唯一持久队列，并在测试结束后删除。
