# 核心链路时序图

本文只描述当前代码中已经接线的真实链路，类名、状态和失败语义均可在源码或测试中核对。项目不使用 Milvus；高德、通义千问、百度 AI 和云 RabbitMQ 都属于外部条件能力。

## 1. JWT 登录与鉴权

```mermaid
sequenceDiagram
    autonumber
    actor User as 用户
    participant Web as React 前端
    participant Gateway as Gateway/AuthGlobalFilter
    participant UserService as user-service
    participant DB as MySQL
    participant Jwt as JwtHelper
    participant Producer as MessageProducerService
    participant Redis as Redis 黑名单
    participant Biz as 目标业务服务

    User->>Web: 输入账号和密码
    Web->>Gateway: POST /api/users/login
    Gateway->>Gateway: 移除伪造的 X-User-* 请求头
    Gateway->>UserService: 登录路径公开放行
    UserService->>DB: 按用户名或手机号查询用户
    DB-->>UserService: 用户与 BCrypt 密码摘要
    UserService->>UserService: BCrypt 校验密码
    alt 账号或密码错误
        UserService-->>Web: 业务错误，不签发 Token
    else 校验成功
        UserService->>Jwt: 使用 JWT_SECRET 签发 Token
        opt 可靠通知生产者已开启
            UserService-->>Producer: 异步提交登录提醒
            Note over UserService,Producer: 发布失败只记录告警，不回滚登录
        end
        UserService-->>Web: 返回 Bearer Token
    end

    User->>Web: 发起需要认证的请求
    Web->>Gateway: Authorization: Bearer <token>
    Gateway->>Gateway: 校验签名、过期时间并解析 userId/userType
    alt Token 无效
        Gateway-->>Web: HTTP 401
    else Token 有效
        Gateway->>Biz: 转发请求并注入可信 X-User-* 头
        Biz->>Biz: JwtAuthenticationFilter 再次解析 Token
        Biz->>Redis: 查询 blacklist:token:<token>
        alt Token 已登出
            Biz-->>Web: 未建立认证上下文，Spring Security 拒绝
        else Token 可用
            Biz->>Biz: 写入 SecurityContext 与角色
            Biz->>Biz: Spring Security 校验路径和角色
            Biz-->>Web: 返回业务响应
        end
    end
```

### 设计要点

- Gateway 会先删除客户端提交的 `X-User-Id`、`X-User-Type`、`X-User-Role` 和 `X-Client-IP`，再写入可信值，避免请求头伪造。
- 业务服务仍通过 `JwtAuthenticationFilter` 建立 `SecurityContext`，并检查 Redis Token 黑名单；Gateway 与服务内鉴权形成两层防线。
- `JWT_SECRET` 为空或不足 32 个 UTF-8 字节时，Gateway 拒绝启动。
- 登录通知是旁路能力，RabbitMQ 故障不能影响登录主事务。

## 2. HTTP 幂等状态机

```mermaid
sequenceDiagram
    autonumber
    actor Client as 已认证客户端
    participant JwtFilter as JwtAuthenticationFilter
    participant IdemFilter as HttpIdempotencyFilter
    participant Redis as Redis
    participant Service as 业务服务
    participant DB as MySQL

    Client->>JwtFilter: 写请求 + Idempotency-Key
    JwtFilter->>JwtFilter: 建立认证上下文
    JwtFilter->>IdemFilter: 进入幂等过滤器
    IdemFilter->>IdemFilter: 计算 scope + method + path + body 指纹
    IdemFilter->>Redis: Lua SET NX，写入 PROCESSING:<token>

    alt Redis 不可用
        IdemFilter-->>Client: HTTP 503，业务逻辑不执行
    else 已存在 COMPLETED
        IdemFilter->>IdemFilter: 比对用户范围和请求指纹
        alt 同键不同请求
            IdemFilter-->>Client: HTTP 409
        else 完全相同请求
            IdemFilter-->>Client: 重放原状态码和响应体
            Note over IdemFilter,Client: Idempotency-Replayed: true
        end
    else 已存在 PROCESSING
        IdemFilter-->>Client: HTTP 409，仍在处理
    else 抢占成功
        IdemFilter->>Service: 执行业务逻辑
        Service->>DB: 事务写入，关键表唯一键最终兜底
        DB-->>Service: 提交或回滚
        Service-->>IdemFilter: HTTP 响应
        alt 2xx 或 4xx 且响应未超限
            IdemFilter->>Redis: Lua 比对 token 后写 COMPLETED + 原响应
            IdemFilter-->>Client: 返回首次响应
        else 5xx
            IdemFilter->>Redis: Lua 比对 token 后删除 PROCESSING
            IdemFilter-->>Client: 返回但不缓存，允许安全重试
        else 3xx、异步响应、SSE 或响应过大
            IdemFilter->>Redis: 保留 PROCESSING，等待 TTL 到期
            IdemFilter-->>Client: 返回但不缓存，避免业务副作用被重复执行
        end
    end
```

### 设计要点

- 仅处理已认证的 `POST`、`PUT`、`PATCH`、`DELETE`；未携带幂等键的请求保持原语义。
- Redis 键按“认证主体 + 幂等键”做 SHA-256，处理中 TTL 默认 300 秒，完成态 TTL 默认 3 天。
- 请求指纹包含方法、完整路径、内容类型和请求体，防止同一键被用于不同业务请求。
- Redis 故障采用 fail-closed：返回 HTTP 503，不绕过幂等保护写数据库。
- `multipart/*`、SSE、超过 1 MiB 的请求或响应不进入响应复用流程；数据库唯一键仍是关键业务的最终防线。
- 只有明确的 5xx 失败会释放处理中锁；非 5xx 但无法复用响应时保留锁到 TTL，避免业务已成功却被重复执行。

## 3. 路线优化并发一致性

```mermaid
sequenceDiagram
    autonumber
    actor Client as 已认证用户
    participant Idem as HTTP 幂等过滤器
    participant Controller as RouteOptimizationController
    participant Lock as Redisson
    participant Tx as TransactionTemplate
    participant RouteDB as route_attractions
    participant History as Redis 优化历史

    Client->>Idem: POST /api/route-optimization/apply
    Idem->>Controller: 幂等键抢占成功后放行
    Controller->>Controller: 校验路线所有权
    Controller->>Lock: tryLock route-optimization:<routeId>
    Lock-->>Controller: 获得跨实例互斥锁
    Controller->>Tx: 开启数据库事务
    Tx->>RouteDB: SELECT 完整日程 ORDER BY day,order FOR UPDATE
    RouteDB-->>Tx: 锁定路线全部关系行
    Tx->>Tx: 校验完整景点集合、天数、位置连续性

    alt 请求提供 attractionOrder
        Tx->>Tx: 校验无重复、无跨路线、覆盖完整日程
    else 未提供显式顺序
        Tx->>Tx: 按天使用有效经纬度做最近邻排序
    end

    alt 顺序没有变化
        Tx-->>Controller: changed=false，不更新、不写历史
    else 顺序发生变化
        Tx->>RouteDB: UPDATE visit_order = -id 预留位置
        Tx->>RouteDB: 批量写回 1..N 最终顺序
        Note over Tx,RouteDB: 唯一键 (route_id, day_number, visit_order) 兜底
        Tx->>Tx: 提交事务
        Tx-->>History: afterCommit 写入一条优化历史
        Note over Tx,History: Redis 写失败只告警，不回滚 MySQL
    end
    Controller->>Lock: 释放分布式锁
    Controller-->>Idem: 返回成功响应
    Idem-->>Client: 返回并缓存幂等结果
```

### 设计要点

- Redisson 锁解决多实例并发，`SELECT ... FOR UPDATE` 解决事务级并发，数据库唯一键负责最终约束。
- 先写 `-id` 再写最终正序，消除两个景点互换位置时的临时唯一键冲突。
- 无变化请求不会重复更新，也不会重复写优化历史；100 个不同幂等键并发时仍只产生一次实际变更。
- 当前 `apply` 入口的自动排序是“按天、基于本地经纬度的最近邻顺序”；`optimizationType` 目前用于参数归一化和历史标识，不代表已经实现独立的时间、费用多目标求解器。

## 4. RabbitMQ 可靠通知与最终一致性

```mermaid
sequenceDiagram
    autonumber
    participant Biz as 业务服务
    participant Producer as MessageProducerService
    participant StatusDB as mq_message_status
    participant Rabbit as 云 RabbitMQ
    participant Consumer as ReliableNotificationConsumer
    participant Redis as Redis 幂等状态
    participant NotificationDB as notification
    participant Retry as TTL 重试队列
    participant DLQ as 死信队列

    Biz-->>Producer: 提交通知事件
    Producer->>Producer: 生成 messageId 和消息元数据
    opt 状态持久化已开启
        Producer->>StatusDB: INSERT PENDING
    end
    Producer->>Rabbit: 发布到可靠通知交换机
    Producer->>StatusDB: 标记 DISPATCHED（仅表示本地发送调用返回）
    Rabbit-->>Producer: publisher confirm / returned
    Producer->>StatusDB: CONFIRMED、RETURNED 或 FAILED

    Rabbit->>Consumer: 主队列投递，manual ACK
    Consumer->>Redis: SET NX PROCESSING:<token>
    alt 已是 COMPLETED
        Consumer->>Rabbit: ACK 重复消息
    else 正在 PROCESSING
        Consumer->>Retry: 发布下一档重试并等待 confirm
        alt 重试消息确认成功且未 returned
            Consumer->>Rabbit: ACK 原消息
        else 重试发布未确认
            Consumer->>Rabbit: NACK requeue 原消息
        end
    else 抢占成功
        Consumer->>NotificationDB: 按 source_message_id 幂等写通知
        Note over Consumer,NotificationDB: 唯一键 uk_notification_source_message 最终兜底
        alt 写入成功或唯一键竞争已收敛
            Consumer->>Redis: token 比对后标记 COMPLETED
            Consumer->>Rabbit: ACK
        else 可重试异常且未达上限
            Consumer->>Redis: 释放 PROCESSING
            Consumer->>Retry: 发布重试并等待 confirm
            Retry-->>Rabbit: TTL 到期，经 DLX 回流主交换机
            Consumer->>Rabbit: 确认重试发布后 ACK 原消息
        else 非法消息或达到重试上限
            Consumer->>DLQ: 发布死信并等待 confirm
            Consumer->>Rabbit: 确认死信发布后 ACK 原消息
        end
    end
```

### 设计要点

- 可靠通知拓扑、生产者、消费者和状态持久化均由独立开关控制，默认关闭；当前部署连接外部云 RabbitMQ，不在 Compose 中启动本地 broker。
- publisher confirm 与 mandatory returned callback 区分“到达交换机”和“无法路由”；`DISPATCHED` 不能被解释为消费成功。
- 三档 TTL 重试为 5 秒、30 秒、120 秒，不依赖 RabbitMQ 延迟插件；超过上限或载荷非法进入 DLQ。
- 只有目标重试/死信消息获得 broker confirm 且未 returned 后，消费者才 ACK 原消息，避免转发窗口丢消息。
- `mq_message_status` 已具备状态记录和补偿抢占原语，但当前没有接线中的定时补偿扫描任务，不能对外宣称“消息表自动定时重投”。

## 5. 代码入口

| 链路 | 主要实现 |
|------|----------|
| 认证 | `gateway/.../AuthGlobalFilter.java`、`common/.../JwtAuthenticationFilter.java`、`user-service/.../UserServiceImpl.java` |
| HTTP 幂等 | `common/.../HttpIdempotencyFilter.java`、`common/.../HttpIdempotencyService.java` |
| 路线优化 | `route-service/.../RouteOptimizationServiceImpl.java`、`RouteAttractionServiceImpl.java` |
| RabbitMQ | `common/.../MessageProducerService.java`、`ReliableNotificationRabbitConfig.java`、`collection-service/.../ReliableNotificationConsumer.java` |

测试和压测证据见 [EVIDENCE_INDEX.md](EVIDENCE_INDEX.md)。
