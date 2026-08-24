# 业务一致性改造执行手册

## 适用范围

本次改造覆盖路线生命周期、游记点赞、分享访问计数、文件下载、手机号唯一性和 JWT 刷新校验。

- 不接入 Milvus、Embedding 或 RAG。
- 不加入短信计费、余额、套餐或支付功能。
- 交通数据继续只通过项目配置的高德 API 获取。
- RabbitMQ 仍使用云端实例，MySQL 和 Redis 仍使用本机实例。

## 新数据库

新环境优先执行对应服务的 `src/main/resources/db/init_complete.sql`。六份初始化脚本已同步包含：

- 路线 `status`、`version` 字段和状态索引；公开种子路线回填为 `PUBLISHED`。
- `user.phone` 的 `uk_user_phone` 唯一约束，`NULL` 仍允许多个账号共存。

## 已有数据库

在 `travel_website` 数据库完成备份后，按以下顺序执行 common 模块迁移：

1. `V2__route_lifecycle.sql`
2. `V3__share_integrity.sql`
3. `V4__user_phone_unique.sql`

Windows PowerShell 示例：

```powershell
mysql -h 127.0.0.1 -P 3306 -u <username> -p travel_website < backend/common/src/main/resources/db/migration/V2__route_lifecycle.sql
mysql -h 127.0.0.1 -P 3306 -u <username> -p travel_website < backend/common/src/main/resources/db/migration/V3__share_integrity.sql
mysql -h 127.0.0.1 -P 3306 -u <username> -p travel_website < backend/common/src/main/resources/db/migration/V4__user_phone_unique.sql
```

如果 V4 因历史重复手机号失败，先执行以下查询处理业务冲突，再重试迁移：

```sql
SELECT phone, COUNT(*) AS duplicate_count
FROM user
WHERE phone IS NOT NULL
GROUP BY phone
HAVING COUNT(*) > 1;
```

当前项目没有自动接入 Flyway：初始化脚本不是完整版本化基线，直接自动执行会让新库和已有库走不同路径。迁移完成后再启动服务，避免运行时出现字段不存在或唯一键创建失败。

## RabbitMQ 状态持久化

只有在执行 `MQ_MESSAGE_STATUS_MIGRATION.sql` 并确认 `mq_message_status` 存在后，才设置 `MQ_STATUS_PERSISTENCE_ENABLED=true`。该能力记录发布状态和补偿候选，不等同于业务事务 Outbox；后续若将通知发布绑定到业务写事务，应再引入业务事件表和发布扫描器。

## 接口变化

- 路线公开操作必须通过 `POST /routes/{id}/publish`，发布前必须存在合法日程；`PUT /routes/{id}/visibility?isPublic=true` 兼容保留，但内部走同一发布流程。
- 游记详情是只读查询，浏览数通过 `POST /travel-notes/{id}/view` 增加，避免前端重复调用造成重复计数。
- 文件下载链接返回 `/api/resource-file/content/{id}`，实际内容由受权限控制的二进制接口返回，不再返回服务器绝对路径。
