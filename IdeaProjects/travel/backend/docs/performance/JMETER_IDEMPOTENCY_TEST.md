# JMeter 幂等性与并发一致性验收

## 1. 验收目标

验证同一登录用户对同一路线使用同一个 `Idempotency-Key` 发起 100 个并发请求时：

1. 只有一个请求真正执行业务写入。
2. 请求处理中到达的重复请求允许返回 HTTP 409。
3. 首次请求完成后，相同请求重放返回 HTTP 200，并带有 `Idempotency-Replayed: true`。
4. 原始成功响应与所有重放响应体完全一致，且 `data.collected=true`。
5. 最终收藏状态为 `true`，MySQL 中只有一条对应收藏记录。

HTTP 409 在本场景中不是系统错误，而是 Redis 幂等状态处于 `PROCESSING` 时的预期业务竞争结果。HTTP 429、5xx、连接失败、响应结构错误和重放内容不一致均判定为失败。

## 2. 测试结构

- `Setup`：生成共享幂等键，登录并提取 `$.data.token`，删除测试用户已有的目标路线收藏。
- `Concurrent requests`：默认启动 100 个线程，通过同步定时器同时调用收藏切换接口，所有线程共享 token、路线 ID、请求体和幂等键。
- `Teardown`：汇总原始 200、重放 200、处理中 409 和异常响应数量；再次重放；查询最终收藏状态。
- 数据库验收：按 `user_id + item_id + item_type + collection_type` 查询记录数，并核对唯一索引 `uk_user_item_action`。

测试不会在结束时删除最终收藏记录，便于执行数据库验收。下一次运行会在 `Setup` 阶段自动清理。

## 3. 前置条件

1. Win11 本机 MySQL、Redis 和 Nacos 已启动。
2. 网关、用户服务、路线服务和收藏服务已启动，网关默认地址为 `http://127.0.0.1:8090`。
3. 测试账号存在，密码正确，目标路线存在。
4. 收藏服务已启用 HTTP 幂等，Redis 数据库与应用配置一致。
5. `user_collection` 已应用唯一索引迁移：

```powershell
$env:MYSQL_PWD = '<本地 MySQL 密码>'
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' `
  --host=127.0.0.1 --port=3306 --user=root --database=travel_website `
  --execute="SOURCE backend/docs/infrastructure/USER_COLLECTION_ACTION_INDEX_MIGRATION.sql;"
```

## 4. 执行方式

密码优先通过本机环境变量提供，不要写入 JMX、脚本或 Git：

```powershell
$env:TEST_DATA_USER_PASSWORD = '<测试账号密码>'

.\ops\jmeter\run-idempotency-test.ps1 `
  -Gateway 'http://127.0.0.1:8090' `
  -Username 'zhangsan' `
  -RouteId 1 `
  -Threads 100
```

脚本会自动查找 `JMETER_HOME` 和 PATH 中的 JMeter，并直接启动 `ApacheJMeter.jar` 以保留可靠退出码。`-JMeterPath` 可传 JMeter 安装目录、`jmeter.bat` 或 `ApacheJMeter.jar` 路径。当前机器可显式指定：

```powershell
.\ops\jmeter\run-idempotency-test.ps1 `
  -JMeterPath 'E:\测试\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat' `
  -Username 'zhangsan' `
  -RouteId 1 `
  -Threads 100
```

每次结果写入 `run-logs/jmeter/<时间戳>/`：

- `results.jtl`：逐请求结果。
- `html-report/`：JMeter HTML 报告。
- `run-summary.json`：HTTP 分布、平均响应、P95、P99 和吞吐汇总。
- `jmeter.log`：JMeter 执行日志。

## 5. 数据库验收

JMeter 通过后执行只读 SQL。`actual_business_rows` 必须等于 1，两个验收结果必须为 `PASS`：

```powershell
$env:MYSQL_PWD = '<本地 MySQL 密码>'
$verifySql = (Resolve-Path '.\ops\jmeter\verify-route-collection-idempotency.sql').Path.Replace('\', '/')

& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' `
  --host=127.0.0.1 --port=3306 --user=root --database=travel_website --table `
  --execute="SET @test_username='zhangsan'; SET @test_route_id=1; SOURCE $verifySql;"
```

执行完毕后清理当前 PowerShell 会话中的密码：

```powershell
Remove-Item Env:TEST_DATA_USER_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
```

## 6. 通过标准

- `main:toggle` 样本数等于线程数。
- 原始 HTTP 200 恰好 1 个。
- 其余并发响应只能是带重放语义的 HTTP 200 或处理中 HTTP 409。
- `teardown:replay` 返回 HTTP 200、`Idempotency-Replayed: true`，响应体与首次成功响应相同。
- `teardown:check` 返回 `data=true`。
- JMeter 失败样本为 0，非 200/409 响应为 0。
- MySQL 对应业务记录数为 1，唯一索引列顺序正确。

注意：100 并发下不要求出现固定数量的 409。若首次写入很快完成，部分并发请求会直接命中 `COMPLETED` 状态并返回重放 200，这同样符合幂等语义。

## 7. 故障定位

- HTTP 401：账号密码错误、JWT 未透传或 token 已失效。
- HTTP 404：测试路线不存在，或网关路由与服务上下文路径不一致。
- HTTP 409 且消息为幂等键已用于不同请求：并发线程的路径、请求体或 Content-Type 不一致。
- HTTP 429：触发网关或服务限流，应单独记录为限流配置问题，不能算作幂等通过。
- HTTP 503：Redis 或幂等存储不可用；当前设计选择失败关闭，避免绕过入口幂等。
- MySQL 记录数大于 1：唯一索引缺失或业务查询维度不一致，先执行索引迁移并检查重复历史数据。

## 8. 实际验收记录

2026-08-20 在 Win11 本机完成真实服务验收，使用 JMeter 5.6.3、Java 17、本机 MySQL 8 和带认证的本机 Redis。网关、用户服务、路线服务和收藏服务使用本机进程运行，RabbitMQ 通知能力在本场景中关闭，不参与同步收藏链路。

| 指标 | 结果 |
|---|---:|
| 并发线程数 | 100 |
| 原始 HTTP 200 | 1 |
| 并发阶段重放 HTTP 200 | 0 |
| 处理中 HTTP 409 | 99 |
| 非预期 HTTP 响应 | 0 |
| JMeter 失败样本 | 0 |
| 主阶段错误率 | 0% |
| 平均响应时间 | 3342.57ms |
| P95 | 4686ms |
| P99 | 4707ms |
| 吞吐 | 20.08 次/秒 |
| 收尾重放 | HTTP 200，响应头与响应体断言通过 |
| 最终收藏状态 | `true` |
| MySQL 最终业务记录 | 1 条，`PASS` |
| 唯一索引 | `uk_user_item_action`，`PASS` |

本次本地证据位于 `run-logs/jmeter/20260820-105116-454/`，该目录按项目约定不提交 Git。上述响应时间包含单机开发环境、调试日志、同步起跑和本机微服务通信成本，只作为并发正确性基线，不作为生产容量承诺。

## 9. Redis 中断与数据库唯一键

Redis 中断测试不能把“继续写库”作为通过标准。HTTP 幂等状态无法读取时，系统采用 fail-closed 策略返回 HTTP 503，避免请求绕过幂等入口；数据库唯一键作为独立的最终约束，通过重复插入返回 MySQL 1062 验证。

脚本使用独立的临时 Redis 端口和服务端口，不会停止本机 6379 Redis。运行前先完成后端打包，并配置本地 MySQL 凭据：

```powershell
$env:DB_USERNAME = '<本地 MySQL 用户>'
$env:DB_PASSWORD = '<本地 MySQL 密码>'
$env:TEST_DATA_USER_PASSWORD = '<测试账号密码>'

.\ops\jmeter\run-redis-outage-test.ps1 `
  -Username 'zhangsan' `
  -RouteId 1 `
  -RedisServerPath 'E:\Redis\redis-7.2.4\redis-server.exe'
```

通过标准：Redis 停止后写请求返回 HTTP 503，测试业务行数保持 1；Redis 重启后收藏查询恢复；重复插入返回 MySQL 1062，业务行数仍为 1。进入演练主流程后，每个阶段写入 `execution.log`；无论通过还是失败均生成 `run-summary.json`，失败摘要会包含阶段、异常类型、异常消息和脚本调用栈。密码、数据库凭据、可执行文件和端口等前置校验失败时会直接终止，不创建运行目录。

### 9.1 实际验收记录

2026-08-20 在 Win11 本机完成独立 Redis 故障演练，本机常驻 6379 Redis 未停止，RabbitMQ 不参与该同步链路。

| 验收项 | 结果 |
|---|---:|
| 临时 Redis 端口 | 16379 |
| 临时服务端口 | user-service 18091、collection-service 18094 |
| Redis 中断后的写请求 | HTTP 503，响应体 `code=503` |
| 中断前业务记录数 | 1 |
| 中断期间业务记录数 | 1 |
| Redis 重启后的恢复探针 | HTTP 200，收藏状态为 `true` |
| MySQL 重复插入 | 退出码 1，错误码 1062 |
| 重复插入后的业务记录数 | 1 |
| 进程与端口清理 | 16379、18091、18094 均无监听 |
| 最终结果 | `PASS` |

本次证据位于 `run-logs/redis-outage/20260820-113223-525/`，该目录按项目约定不提交 Git。

## 10. 景点点评并发 UPSERT 验收

### 10.1 验收目标与方案取舍

该场景验证同一用户对同一景点并发提交点评时，数据库唯一键和原子 UPSERT 能保证最终只有一条点评记录。与路线收藏场景不同，100 个线程刻意使用不同的 `Idempotency-Key`，避免 HTTP 幂等层直接重放响应，从而让所有请求真实进入 MySQL 竞争同一个 `user_id + attraction_id` 唯一键。

- 采用 `INSERT ... ON DUPLICATE KEY UPDATE` 和 `LAST_INSERT_ID(id)`，保证新增与更新都返回同一个点评 ID。
- 运行前自动应用 `ATTRACTION_REVIEW_IDEMPOTENCY_MIGRATION.sql`，校验并补齐数据库唯一约束。
- 默认启动带随机密码的隔离 Redis 16380，不读取或清空本机常驻 Redis 数据；需要复用现有 Redis 时显式传入 `-UseExistingRedis`。
- 自动创建临时用户、生成项目规则兼容的 HS256 JWT、构建并启动 attraction-service 18092，服务上下文显式固定为 `/api`。
- 无论成功或失败，均删除临时点评和用户，并通过唯一 JVM 运行标记清理 Java 子进程及隔离 Redis。

### 10.2 执行命令

数据库连接默认从进程环境变量或 `deploy/.env` 读取，JMeter 从 `JMETER_HOME` 或 PATH 自动发现：

```powershell
.\ops\jmeter\run-attraction-review-upsert-test.ps1 -Threads 100
```

如需显式指定工具或数据库配置，可使用脚本参数 `-JMeterPath`、`-MySqlPath`、`-DbHost`、`-DbPort`、`-DbName`、`-DbUsername` 和 `-DbPassword`。脚本及 JMX 分别位于：

- `ops/jmeter/run-attraction-review-upsert-test.ps1`
- `ops/jmeter/attraction-review-upsert.jmx`

每次运行的 `results.jtl`、HTML 报告、JMeter 日志、服务日志、Redis 日志、构建日志和 `run-summary.json` 写入 `run-logs/jmeter-review/<时间戳>/`。

### 10.3 通过标准

- `review:upsert` 样本数等于线程数，全部为 HTTP 200，失败样本为 0。
- 所有响应均为项目标准成功结构，`data.id` 有效且不同响应中的点评 ID 只有一个。
- 最终确定性更新返回 HTTP 200，数据库中对应用户和景点只有一条记录，评分与内容等于最终请求值。
- 临时用户和点评被删除，18092、16380 无监听，不残留带本次运行标记的 Java 进程。
- PowerShell runner 保持纯 ASCII，并通过 Windows PowerShell 5.1 Parser；JMX 保持有效 XML。

### 10.4 实际验收记录

2026-08-20 在 Win11 本机使用 JMeter 5.6.3、Java 17、MySQL 8 和隔离 Redis 完成验收。

| 指标 | 结果 |
|---|---:|
| 并发线程数 | 100 |
| 业务样本数 | 100 |
| HTTP 200 | 100 |
| 失败样本 | 0 |
| 错误率 | 0% |
| 平均响应时间 | 1316.13ms |
| P95 | 1581ms |
| P99 | 1598ms |
| 最大响应时间 | 1601ms |
| 吞吐 | 62.31 次/秒 |
| 响应中的不同点评 ID | 1 |
| MySQL 最终业务记录 | 1 条 |
| 最终确定性更新 | HTTP 200，评分 5，内容校验通过 |
| 临时数据、进程和端口清理 | `PASS` |
| 最终结果 | `PASS` |

本次证据位于 `run-logs/jmeter-review/20260820-140022-953/`，该目录按项目约定不提交 Git。响应时间包含单机开发环境、DEBUG 日志和本地数据库竞争成本，只作为并发正确性基线，不作为生产容量承诺。

## 11. 路线评论并发点赞验收

### 11.1 业务修复与并发模型

该场景使用 100 个不同的临时用户同时点赞同一条临时路线评论。每个线程携带独立 JWT 和独立 `Idempotency-Key`，因此请求不会被 HTTP 幂等层直接合并，能够真实验证多个事务同时更新同一个 `likes_count` 字段时的正确性。

- 点赞事实写入统一 `user_collection` 行为表，`item_type='route_comment'`、`collection_type='like'`。
- `uk_user_item_action(user_id, item_id, item_type, collection_type)` 保证同一用户对同一评论最多一条点赞行为。
- 同一评论和用户的状态切换使用 Redis/Redisson 分布式锁串行化，不同用户仍可并发执行。
- 评论计数使用 `likes_count = likes_count + 1` 和 `GREATEST(likes_count - 1, 0)` 原子 SQL，避免不同用户并发时发生读改写丢失。
- 评论删除改为软删除并清理点赞行为，避免物理删除触发 `reply_to ON DELETE SET NULL` 后将回复错误提升为顶级评论。
- 评论列表与统计补齐路线、发布状态和顶级评论条件；评分实体与数据库列不再默认 5 分，数据库 `NULL` 不会被伪装为五星。

迁移脚本 `backend/docs/infrastructure/ROUTE_COMMENT_CONSISTENCY_MIGRATION.sql` 可重复执行，负责规范点赞计数、评分默认值、行为表唯一键及评论查询复合索引。

### 11.2 执行命令

```powershell
.\ops\jmeter\run-route-comment-like-test.ps1 -Threads 100
```

runner 默认执行以下步骤：

1. 启动带随机密码的隔离 Redis 16381。
2. 创建一名临时评论作者、100 名临时点赞用户和一条初始点赞数为 0 的评论。
3. 为每名点赞用户生成项目规则兼容的短期 HS256 JWT，并写入仅供本次运行使用的临时 CSV。
4. 构建并启动 collection-service 18094，显式使用 `/api` 上下文。
5. 先验证评论列表无跨路线记录、统计总数与 MySQL 一致、空评分不会进入平均值。
6. 使用 JMeter 同步释放 100 个点赞请求，随后校验评论计数、行为行数和不同用户数。
7. 删除 JWT CSV、临时用户、评论与行为记录，并停止隔离 Redis 和带本次标记的 Java 进程。

测试资产：

- `ops/jmeter/run-route-comment-like-test.ps1`
- `ops/jmeter/route-comment-like-concurrency.jmx`

### 11.3 通过标准

- `comment-like:toggle` 样本数等于线程数，全部 HTTP 200，`data.liked=true`，失败样本为 0。
- JMeter 收集到的不同用户数等于线程数。
- MySQL `route_comment.likes_count`、`user_collection` 点赞行为行数和不同用户数均等于线程数。
- 评论列表中所有 `routeId` 均等于目标路线，统计总数和平均评分与数据库直接聚合一致。
- 临时 JWT CSV 不保留，临时数据为 0，18094、16381 无监听，不残留带运行标记的 Java 进程。
- PowerShell runner 为纯 ASCII，并通过 Windows PowerShell 5.1 Parser；JMX 为有效 XML。

### 11.4 实际验收记录

2026-08-20 在 Win11 本机使用 JMeter 5.6.3、Java 17、MySQL 8 和隔离 Redis 完成正式验收。

| 指标 | 结果 |
|---|---:|
| 并发用户数 | 100 |
| 业务样本数 | 100 |
| HTTP 200 | 100 |
| 失败样本 | 0 |
| 错误率 | 0% |
| 平均响应时间 | 1522.64ms |
| P95 | 1946ms |
| P99 | 1985ms |
| 最大响应时间 | 1998ms |
| 吞吐 | 49.95 次/秒 |
| MySQL 最终点赞数 | 100 |
| 点赞行为记录数 | 100 |
| 不同点赞用户数 | 100 |
| 路线列表跨路线记录 | 0 |
| 评论统计总数 | 期望 28，实际 28 |
| 平均评分 | MySQL 4.763158，接口 4.76315789 |
| 临时数据、JWT CSV、进程和端口清理 | `PASS` |
| 最终结果 | `PASS` |

本次证据位于 `run-logs/jmeter-comment-like/20260820-145625-099/`，该目录按项目约定不提交 Git。该结果用于证明并发正确性，不代表生产容量上限。

## 12. 路线优化并发一致性验收

### 12.1 业务修复与并发模型

该场景使用同一名临时路线所有者，以 100 个不同的 `Idempotency-Key` 同时提交同一个显式景点顺序。不同幂等键确保请求不会被 HTTP 幂等层合并，所有请求都会真实进入路线级分布式锁、数据库事务和最终状态判断。

- 事务内通过 `SELECT ... FOR UPDATE` 锁定该路线全部 `route_attractions`，即使 Redis 锁租约异常也由 MySQL 行锁继续串行化写入。
- `uk_route_day_visit_order(route_id, day_number, visit_order)` 为同一天同一位置提供数据库最终约束，`day_number` 和 `visit_order` 均改为非空。
- 换位采用两阶段写入：先把整条路线的旧顺序更新为 `-id`，再写回完整的正数连续顺序，避免交换 `1 ↔ 2` 时产生临时唯一键冲突。
- 每次事务重新读取最终顺序；首个请求完成后，其余相同请求识别为无变化，不重复更新数据库，也不重复写优化历史。
- 优化历史在事务提交后写入 Redis；Redis 历史缓存异常只记录告警，不再反向回滚已经提交的 MySQL 日程。
- 自动排序缺少景点或有效经纬度时明确返回无数据，不再用不可验证的任意顺序继续执行。

迁移脚本 `backend/docs/infrastructure/ROUTE_OPTIMIZATION_CONSISTENCY_MIGRATION.sql` 会先规范现有天数和顺序，再补齐非空列及复合唯一索引。六份 `init_complete.sql` 已同步相同结构。

### 12.2 执行命令

```powershell
.\ops\jmeter\run-route-optimization-test.ps1 -Threads 100
```

runner 默认执行以下步骤：

1. 启动带随机密码的隔离 Redis 16382，不读取或清空本机常驻 Redis 数据。
2. 应用路线优化一致性迁移，创建临时用户、路线和 3 个临时景点，初始顺序为 `1,2,3`。
3. 生成项目规则兼容的短期 HS256 JWT，构建并启动 route-service 18093，关闭无关的 RabbitMQ、Nacos 和 XXL-JOB 外联。
4. 使用 JMeter 同步释放 100 个请求，每个线程使用不同幂等键，目标顺序统一为 `3,1,2`。
5. 校验全部业务响应、MySQL 最终顺序、位置连续性、唯一索引列顺序及 Redis 优化历史条数。
6. 删除临时用户、路线、景点，停止隔离 Redis 和带本次运行标记的 Java 进程，并复查 18093、16382 无监听。

测试资产：

- `ops/jmeter/run-route-optimization-test.ps1`
- `ops/jmeter/route-optimization-concurrency.jmx`
- `backend/docs/infrastructure/ROUTE_OPTIMIZATION_CONSISTENCY_MIGRATION.sql`

### 12.3 通过标准

- `route-optimization:apply` 样本数等于线程数，全部 HTTP 200、`data=true`，失败样本为 0。
- 最终路线恰好 3 条关系，`day_number=1`，`visit_order` 为连续的 `1..3`，不存在负数、空值或重复位置。
- 最终景点顺序与提交顺序完全一致，唯一索引列顺序为 `route_id,day_number,visit_order`。
- 服务日志中只有一次 `changed=true`，其余请求均为 `changed=false`；优化历史恰好 1 条。
- 临时数据为 0，18093、16382 无监听，不残留带运行标记的 Java 进程。
- PowerShell runner 为纯 ASCII，并通过 Windows PowerShell 5.1 Parser；JMX 为有效 XML。

### 12.4 实际验收记录

2026-08-20 在 Win11 本机使用 JMeter 5.6.3、Java 17、MySQL 8 和隔离 Redis 完成正式验收。

| 指标 | 结果 |
|---|---:|
| 并发线程数 | 100 |
| 业务样本数 | 100 |
| HTTP 200 | 100 |
| 失败样本 | 0 |
| 错误率 | 0% |
| 平均响应时间 | 6508.34ms |
| P95 | 7438ms |
| P99 | 7532ms |
| 最大响应时间 | 7539ms |
| 吞吐 | 13.24 次/秒 |
| `changed=true` | 1 次 |
| `changed=false` | 99 次 |
| 最终位置数 / 不同位置数 | 3 / 3 |
| 最终顺序 | `183,181,182`，与期望一致 |
| 优化历史 | 1 条 |
| 唯一索引 | `route_id,day_number,visit_order` |
| 临时数据、进程和端口清理 | `PASS` |
| 最终结果 | `PASS` |

本次证据位于 `run-logs/jmeter-route-optimization/20260820-153122-431/`，该目录按项目约定不提交 Git。响应时间包含单机开发环境、100 个请求争用同一业务锁、数据库连接池排队和调试框架成本，只作为并发正确性基线，不作为生产容量承诺。
