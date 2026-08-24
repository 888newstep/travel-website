# 路线访问分析设计与验收

## 统计口径

- 路线详情每次成功读取都会计入一次 PV，包括路线所有者本人访问。
- 已认证 UV 使用用户 ID 经环境盐值计算的 SHA-256 标识。
- 匿名 UV 使用服务端观察到的远端地址和 User-Agent 计算哈希，不保存原始值。
- 回访人数指统计窗口内至少在两个不同自然日访问过的访客；回访率为回访人数除以 UV。
- 日趋势对无访问日期补零，查询窗口限制为 1 至 365 天，且仅路线所有者可查看。

## 一致性与降级

- `route.view_count` 原子自增与 `route_visit` 明细插入处于同一 MySQL 事务，任一步失败都会整体回滚。
- 路线详情缓存只在事务提交后失效；Redis 失效失败仅记录告警，不回滚已提交的访问事实。
- 访问分析写入失败不会阻断公开路线详情读取，但会记录带路线 ID 的告警，便于监控数据缺口。
- 生产环境应显式设置 `ROUTE_ANALYTICS_HASH_SALT`，也可回退使用 `JWT_SECRET`；盐值不得写入日志或仓库。

## 隐私与边界

- `route_visit` 不包含 IP、远端地址或 User-Agent 原文列，仅保存 64 位加盐 SHA-256 标识。
- 当前实现不信任客户端可伪造的任意 `X-Forwarded-For`。部署在可信网关后，应由网关清洗转发头，再通过受控的 `ForwardedHeaderFilter` 或容器配置恢复真实客户端地址。
- 匿名 UV 是近似指标：共享出口、User-Agent 变化和隐私代理都会影响去重精度，不应解释为实名用户数。
- 该明细用于产品访问分析，不用于安全审计或个人画像。

## 数据库迁移

执行 [ROUTE_VISIT_ANALYTICS_MIGRATION.sql](ROUTE_VISIT_ANALYTICS_MIGRATION.sql) 后，应存在：

- 表 `route_visit`；
- 索引 `idx_route_visit_date`、`idx_route_visitor_date`、`idx_route_visit_user_time`；
- 外键 `fk_route_visit_route`、`fk_route_visit_user`。

2026-08-22 已在本机 `travel_website` 执行并核验上述对象。

## 真实验收

执行：

```powershell
$env:TEST_DATA_USER_PASSWORD = '<测试用户密码>'
.\ops\jmeter\run-route-visit-analytics-live-test.ps1
```

脚本会启动隔离 Redis、user-service 和 route-service，对公开路线执行 3 次认证访问和 2 次匿名访问，并验证：

- 浏览量原子增加 5；
- 新增访问明细 5 条；
- 访客哈希去重结果为 3；
- 哈希长度均为 64，原始身份信息列为 0；
- 所有者统计接口的 PV/UV 不低于本轮新增值；
- 结束后删除本轮明细、恢复浏览量，并关闭隔离进程和端口。

本次结果为 `PASS`，脱敏证据位于 `run-logs/route-visit/20260822-213119-067/`。
