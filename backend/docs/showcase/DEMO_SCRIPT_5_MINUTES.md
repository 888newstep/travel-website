# 五分钟演示脚本

## 1. 演示目标

五分钟内证明三件事：

1. 项目不是只有页面和 CRUD，而是有认证、幂等、并发一致性和可靠消息设计；
2. 交通数据坚持使用真实高德 API，缺少 Key 时明确失败；
3. 所有结论都能落到源码、数据库状态、JMeter 报告或外部实测记录。

不在主演示中使用上传图片固定分析、多模态固定推荐、预算、安全评分和无数据源的个性化推荐。

## 2. 演示前准备

### 2.1 必备环境

- Win11 本机 MySQL、Redis、JDK 17、Maven、Node.js 和 JMeter 5.6.3。
- 后端 8090-8095 端口可用，前端默认 `http://localhost:3000`。
- 一个可登录演示用户，以及一条属于该用户、至少包含两个有效坐标景点的路线。
- `JWT_SECRET` 至少 32 个 UTF-8 字节。

建议提前设置，不在终端历史或投屏中显示真实秘密：

```powershell
$env:DEMO_USERNAME = '<演示账号>'
$env:DEMO_PASSWORD = '<演示密码>'
$env:DEMO_ROUTE_ID = '<属于演示用户的路线 ID>'
$env:DEMO_CITY_ID = '1'
```

### 2.2 启动与健康检查

推荐使用不依赖 Nacos 的可复现验收模式：

```powershell
.\ops\e2e\start-e2e-stack.ps1
npm --prefix frontend run dev
```

确认入口：

```powershell
Invoke-RestMethod http://127.0.0.1:8090/actuator/health
Invoke-RestMethod http://127.0.0.1:8090/api/attractions | Select-Object -ExpandProperty success
```

Docker Compose 是另一套模式：它使用容器 DNS 静态路由，不启动 Nacos，也不启动本地 RabbitMQ。

### 2.3 外部能力预检

高德现场演示必须提前配置真实 Web 服务 Key：

```powershell
$env:AMAP_API_KEY = '<真实高德 Web 服务 Key>'
.\ops\amap\run-amap-route-live-test.ps1
```

云 RabbitMQ 现场演示必须提前配置 `RABBITMQ_HOST`、端口、账号、密码和 vhost，并执行：

```powershell
.\ops\rabbitmq\run-reliable-notification-live-test.ps1
```

如果任一预检失败，现场展示失败摘要和降级设计，不临时伪造成功数据。

## 3. 五分钟时间轴

### 00:00-00:30 项目定位与架构

打开根目录 `README.md` 的架构图，并口述：

> 这是一个面向秋招和技术交流的工程化旅游展示项目。前端是 React 19，后端按用户、景点、路线、社区、文件和网关拆成六个运行服务。Win11 本机使用 MySQL、Redis 和 JMeter，RabbitMQ 在云端；交通只调用高德，不使用 Milvus，也不做短信计费。

立即指出三种运行模式：

- E2E 验收脚本：固定端口直连，不依赖 Nacos；
- 传统本地脚本：可使用 Nacos 注册发现；
- Docker Compose：静态容器 DNS，外接云 RabbitMQ，无 Nacos 容器。

### 00:30-01:05 登录与双层鉴权

可使用前端登录页，也可在 PowerShell 中执行：

```powershell
$baseUrl = 'http://127.0.0.1:8090/api'
$loginBody = @{
    username = $env:DEMO_USERNAME
    password = $env:DEMO_PASSWORD
} | ConvertTo-Json -Compress

$login = Invoke-RestMethod -Method Post `
    -Uri "$baseUrl/users/login" `
    -ContentType 'application/json' `
    -Body $loginBody
$token = $login.data.token
$authHeaders = @{ Authorization = "Bearer $token" }
$login | ConvertTo-Json -Depth 6
```

口述重点：

> 登录由 BCrypt 校验密码并签发 JWT。后续请求先经过 Gateway 验签和可信请求头重建，再由业务服务检查 Token 过期和 Redis 黑名单，最后进入 Spring Security。登录通知失败不会回滚登录。

不要在屏幕上打印完整 Token；正式演示可只输出 `$token.Substring(0, 16) + '...'`。

### 01:05-01:35 景点与真实数据边界

```powershell
$attractions = Invoke-RestMethod -Method Get -Uri "$baseUrl/attractions"
$attractions.data | Select-Object -First 3 id,name,cityId,rating,latitude,longitude | Format-Table
```

口述重点：

> 景点、城市和餐厅来自 MySQL。评分使用数据库聚合；没有历史客流明细时，历史平均接口会返回不可用，而不是随机生成趋势。

如果需要展示实时状态，只展示最新快照，并明确它不是历史预测。

### 01:35-02:25 路线优化并发一致性

先查看建议和历史：

```powershell
$routeId = [int]$env:DEMO_ROUTE_ID
Invoke-RestMethod -Headers $authHeaders -Uri "$baseUrl/route-optimization/suggestions/$routeId" |
    ConvertTo-Json -Depth 8
Invoke-RestMethod -Headers $authHeaders -Uri "$baseUrl/route-optimization/history/$routeId" |
    ConvertTo-Json -Depth 8
```

应用一次优化：

```powershell
$optimizationKey = 'demo-opt-' + [guid]::NewGuid().ToString('N')
$optimizationHeaders = @{
    Authorization = "Bearer $token"
    'Idempotency-Key' = $optimizationKey
}
$optimizationBody = @{
    routeId = $routeId
    optimizationType = 'distance'
} | ConvertTo-Json -Compress

Invoke-RestMethod -Method Post `
    -Uri "$baseUrl/route-optimization/apply" `
    -Headers $optimizationHeaders `
    -ContentType 'application/json' `
    -Body $optimizationBody | ConvertTo-Json -Depth 8
```

同时打开 [ARCHITECTURE_SEQUENCE_DIAGRAMS.md](ARCHITECTURE_SEQUENCE_DIAGRAMS.md) 的路线优化时序图，口述：

> 同一路线先取 Redisson 锁，再在事务中 `SELECT FOR UPDATE` 锁完整日程。换位时先把旧位置写成负主键，再写回 1 到 N，并由三列唯一键兜底。无变化请求不更新、不重复写历史；Redis 历史缓存失败也不回滚 MySQL。

随后展示预先完成的并发摘要：

```powershell
Get-Content run-logs\jmeter-route-optimization\20260820-153122-431\run-summary.json
```

一句话结论：100 个不同幂等键全部进入业务链路，但实际变更只有一次，最终位置唯一连续、历史一条。

### 02:25-03:20 HTTP 幂等现场重放

使用路线收藏做最直观的重复提交演示：

```powershell
$idempotencyKey = 'demo-collection-' + [guid]::NewGuid().ToString('N')
$writeHeaders = @{
    Authorization = "Bearer $token"
    'Idempotency-Key' = $idempotencyKey
}
$collectionBody = @{ routeId = $routeId } | ConvertTo-Json -Compress

$first = Invoke-WebRequest -UseBasicParsing -Method Post `
    -Uri "$baseUrl/v1/route-collections/toggle" `
    -Headers $writeHeaders `
    -ContentType 'application/json' `
    -Body $collectionBody

$second = Invoke-WebRequest -UseBasicParsing -Method Post `
    -Uri "$baseUrl/v1/route-collections/toggle" `
    -Headers $writeHeaders `
    -ContentType 'application/json' `
    -Body $collectionBody

[pscustomobject]@{
    FirstStatus = $first.StatusCode
    SecondStatus = $second.StatusCode
    Replayed = $second.Headers['Idempotency-Replayed']
    SameBody = ($first.Content -eq $second.Content)
} | Format-List
```

预期：第二次响应包含 `Idempotency-Replayed: true`，响应体与第一次完全一致，收藏不会再次反向切换。

口述重点：

> 幂等范围绑定当前用户，指纹包含方法、路径和请求体。同键不同请求返回 409；处理中返回 409；Redis 不可用时返回 503 且不写库。数据库唯一键负责最后一道防线。

### 03:20-04:05 高德真实交通

若已通过真实 Key 预检，展示最新成功摘要：

```powershell
$latestAmapRun = Get-ChildItem run-logs\amap -Directory |
    Sort-Object Name -Descending |
    Select-Object -First 1
Get-Content (Join-Path $latestAmapRun.FullName 'run-summary.json')
```

口述重点：

> 项目按路线中的相邻景点逐段调用高德驾车 API，聚合真实距离、时长和 `tmcs` 路况。Key 缺失、配额失败、超时或无路径时返回 `dataAvailable=false`，不会用固定速度或直线距离冒充高德结果。

若现场 Key 不可用，直接展示 `run-logs/amap/20260820-130903/run-summary.json` 的安全失败，以及 `run-logs/amap/20260823-175126/` 的已完成真实外呼证据。

### 04:05-04:45 RabbitMQ 可靠通知

展示 [ARCHITECTURE_SEQUENCE_DIAGRAMS.md](ARCHITECTURE_SEQUENCE_DIAGRAMS.md) 的 RabbitMQ 时序图和最新 Live IT 摘要：

```powershell
$latestRabbitRun = Get-ChildItem run-logs\rabbitmq -Directory |
    Sort-Object Name -Descending |
    Select-Object -First 1
Get-Content (Join-Path $latestRabbitRun.FullName 'run-summary.json')
```

口述重点：

> 生产者使用 messageId、publisher confirm 和 mandatory returned；消费者手动 ACK，Redis 状态机做快速幂等，`source_message_id` 唯一键做最终兜底。失败消息进入 5 秒、30 秒、120 秒 TTL 重试队列，超过上限进入 DLQ。只有重试或死信消息获得 broker confirm 后才 ACK 原消息。

同时主动说明：

> 状态表具备补偿抢占原语，当前已接入可控的 Outbox 调度配置；面试时仍要区分“消息可靠投递”与“跨服务强一致事务”，不能把最终一致性描述为强一致。

若现场云连接异常，展示 `run-logs/rabbitmq/20260820-133520/run-summary.json` 的预检失败和 `run-logs/rabbitmq/20260823-214758/run-summary.json` 的已完成 L4 外部验收。

### 04:45-05:00 总结

结尾建议：

> 这个项目的价值不是功能数量，而是把失败场景做实：重复请求如何收敛、Redis 故障为什么 fail-closed、路线换位如何避免唯一键冲突、消息重试如何避免先 ACK 后丢失，以及外部数据不可用时为什么宁可返回不可用也不造数据。后端全量测试、六阶段业务验收和多组真实 100 并发结果都可以复现。

## 4. 常见追问与回答

| 追问 | 建议回答 |
|------|----------|
| 为什么 HTTP 幂等不用数据库表？ | Redis 保存处理中状态和响应更适合高频入口；关键业务仍用数据库唯一键做最终兜底。若要跨 3 天长期审计，可再引入幂等记录表。 |
| 为什么 Redis 故障返回 503？ | 绕过幂等继续写会把基础设施故障放大成重复业务数据，展示项目选择 fail-closed。 |
| 为什么路线优化既要 Redisson 又要行锁？ | Redisson 降低跨实例竞争，行锁保证事务内数据库事实；任一层单独使用都不能覆盖所有失效窗口。 |
| 为什么换位要先写负数？ | 唯一键在每条 UPDATE 时都会检查，直接交换 1 和 2 会临时冲突；负主键提供不重叠的过渡空间。 |
| RabbitMQ 为什么不能先 ACK 再重发？ | 重发失败会造成原消息已确认、新消息未入 broker 的永久丢失；因此必须先等目标发布确认。 |
| 为什么不用 Milvus？ | 当前业务没有已验证的向量检索需求和数据集，引入只会增加展示复杂度；文本 AI 与结构化 MySQL/Redis 已足够覆盖目标知识点。 |
| AI 生成内容可信么？ | 它是条件可用的辅助文本，不作为价格、安全、开放时间和交通事实；真实交通只认高德。 |

## 5. 演示失败预案

- 前端异常：立即切换到本文 PowerShell 命令，后端能力仍可完整展示。
- Redis 异常：展示 HTTP 503 和 `run-logs/redis-outage/.../run-summary.json`，不要临时关闭幂等。
- 高德异常：展示 `dataAvailable=false`、本地桩覆盖和失败摘要。
- RabbitMQ 异常：展示 confirm/retry/DLQ 测试与预检失败，不修改开关伪造消费成功。
- AI Key 异常：跳过 AI；AI 不是五分钟主演示链路。
