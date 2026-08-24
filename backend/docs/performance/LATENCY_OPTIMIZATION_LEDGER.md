# 延迟优化总账

> 文档定位：本文件是项目后续多轮性能优化的唯一总记录。每次优化必须先记录基线、假设和验收门槛，再实施代码变更，最后回填真实数据、结论和回滚情况。

## 1. 优化目标与边界

### 1.1 总目标

在不降低认证安全、幂等正确性、数据库一致性和故障保护能力的前提下，持续降低核心 HTTP 链路的平均响应时间、P95 和 P99，并提高稳定吞吐量。

当前首要优化对象为：

```text
Gateway
  -> collection-service JWT/黑名单校验
  -> HTTP 幂等 Redis 状态机
  -> 收藏分布式锁
  -> route-service 可读性校验
  -> user-service 用户校验
  -> MySQL 写入
  -> Redis 缓存失效
  -> 幂等响应持久化
```

### 1.2 不可突破的边界

1. 不通过关闭 JWT、Redis 黑名单或对象权限校验换取性能。
2. 不在 Redis 故障时绕过幂等继续写库，继续保持 fail-closed HTTP 503。
3. 不把增加 Feign 超时当作性能优化；超时只能作为容量边界，不解决慢调用根因。
4. 不删除数据库唯一键、外键或分布式锁来制造更高吞吐。
5. 不把冷启动数据和稳态数据混在一起比较。
6. 不只看 JMeter 绿色结果，必须同时验证 HTTP 状态分布和数据库最终事实。
7. 项目不使用 Milvus；性能优化范围不包含向量数据库。

## 2. 指标口径

每轮至少记录以下指标：

| 类别 | 指标 | 说明 |
|------|------|------|
| 延迟 | Average、P50、P95、P99、Max | P95/P99 用于观察长尾，Max 只作为异常线索 |
| 吞吐 | req/s 或 msg/s | 必须注明线程数、测试时长和场景语义 |
| 正确性 | HTTP 200/409/403/503 分布 | 业务拒绝不等于技术失败 |
| 稳定性 | 错误率、超时数、连接池等待 | 不能只记录成功样本 |
| 数据事实 | 最终记录数、唯一键、计数器 | 验证并发后业务不变量 |
| 资源 | CPU、堆、GC、线程、连接池 | 用于判断瓶颈，不作为单独成功标准 |

### 2.1 冷启动与稳态必须分开

- **冷启动指标**：服务刚启动后的第一次完整请求，包含 DispatcherServlet、Seata、MyBatis、连接池和类加载成本。
- **稳态指标**：完成预热后连续执行的请求，反映正常运行期间的用户体验。
- 两类指标都保留，但不得用冷启动结果否定稳态优化，也不得用稳态结果掩盖启动后首请求过慢。

### 2.2 标准测试方法

1. 固定 JDK、MySQL、Redis、JMeter、线程数、测试账号、路线和本机电源模式。
2. 保存本轮 Git diff 或变更文件列表。
3. 冷启动场景执行 1 轮，单独记录。
4. 预热完整业务链路，不只探测 TCP 端口。
5. 稳态场景至少执行 5 轮，取中位数，同时记录最差一轮。
6. 若 5 轮 P95 变异系数超过 10%，本轮数据无效，应先排查环境噪声。
7. 每轮结束恢复数据库基线、关闭临时进程，并保留 `run-summary.json`、JTL、HTML 报告和服务日志。

## 3. 当前性能基线

### 3.1 历史稳态参考

证据：`run-logs/jmeter/20260822-185802-498/run-summary.json`。

| 样本 | Average | P95 | P99 | Throughput | HTTP 分布 | 数据事实 |
|------|---------|-----|-----|------------|-----------|----------|
| 100 同键并发 | 627.52ms | 644ms | 669ms | 51.23 req/s | 1 个 200、99 个 409 | 收藏记录 1 条 |

按响应码拆分：

| 响应 | 数量 | Average | Min | Max |
|------|------|---------|-----|-----|
| 409 处理中 | 99 | 614.17ms | 571ms | 669ms |
| 200 原始执行 | 1 | 1949ms | 1949ms | 1949ms |

该结果可作为稳态参考，但当时的启动、预热和代码版本与当前不完全相同，不能直接作为严格的 Before 数据。

### 3.2 2026-08-23 当前可复现基线

证据：

- `run-logs/jmeter/20260823-220811-441/run-summary.json`
- `run-logs/jmeter-gateway-idempotency/20260823-220544-198/run-summary.json`

| 样本 | Average | P95 | P99 | Throughput | HTTP 分布 | 数据事实 |
|------|---------|-----|-----|------------|-----------|----------|
| 100 同键并发 | 3002.37ms | 3365ms | 3394ms | 13.46 req/s | 1 个 200、99 个 409、0 非预期 | 执行时 1 条，结束后恢复测试前 0 条 |

按响应码拆分：

| 响应 | 数量 | Average | Min | Max |
|------|------|---------|-----|-----|
| 409 处理中 | 99 | 2957.72ms | 2457ms | 3394ms |
| 200 原始执行 | 1 | 7423ms | 7423ms | 7423ms |

### 3.3 前一轮失败基线

证据：`run-logs/jmeter/20260823-215450-064/run-summary.json`。

| Average | P95 | P99 | Throughput | HTTP 分布 | 结果 |
|---------|-----|-----|------------|-----------|------|
| 2657.74ms | 2855ms | 2883ms | 15.30 req/s | 0 个 200、99 个 409、1 个非预期 404 | FAIL |

根因不是幂等互斥失败，而是收藏服务使用带访问统计副作用的公开路线详情接口，首次 Feign 调用超过 3 秒并被错误转换为“路线不存在”。该问题已经通过无副作用读取契约修复，详见第 8 节第 0 轮记录。

## 4. 当前延迟拆解

以下时间来自 `run-logs/jmeter-gateway-idempotency/20260823-220544-198/`，属于冷启动混合场景。

| 阶段 | 观测时间 | 估算耗时 | 结论 |
|------|----------|----------|------|
| JMeter 同时发起 100 请求 | 22:08:22 | - | 所有线程同一秒启动 |
| collection-service 进入成功请求 Dispatcher | 22:08:25.157 | 约 2.48s | Gateway、JWT、黑名单、Redis 幂等抢占及连接池等待需要进一步打点 |
| 请求体解析与进入 Controller | 22:08:25.159–26.581 | 约 1.42s | 首次 MVC/Jackson/安全链路存在冷启动开销 |
| 收藏锁与存在记录查询 | 22:08:26.587–26.625 | 约 38ms | 不是当前主要瓶颈 |
| route-service 可读性 Feign | 22:08:26.625–29.329 | 约 2.70s | 首次 Dispatcher、Seata、MyBatis、DB 和缓存写入明显冷启动 |
| user-service 用户 Feign | 22:08:29.329–29.597 | 约 268ms | 对当前用户的重复存在性校验，可考虑移除远程调用 |
| MySQL INSERT | 22:08:29.614–30.027 | 约 413ms | 首次 Mapper/连接/DEBUG SQL 日志可能放大耗时 |
| 三个缓存精确删除 | 22:08:30.042–30.063 | 约 21ms | 当前样本较低，但分页通配符失效仍存在风险 |
| Controller 完成 | 22:08:30.086 | - | 原始请求最终耗时 7423ms |

### 4.1 已确认的问题

1. 性能脚本使用开发配置，`travel` 和 Spring Web 均为 DEBUG，压测时同步写入大量 SQL 和 MVC 日志。
2. `JwtAuthenticationFilter` 对同一个 Token 分别调用 `isExpiration`、`getUserId`、`getUserType`，等价于重复解析 JWT 三次。
3. `JwtHelper` 每次解析都会重新读取秘密、生成 `SecretKey`、构建 `JwtParser`。
4. Gateway 每个请求也重新构造 `SecretKey` 和 `JwtParser`。
5. Redis Lettuce 开发池 `max-active=8`、`max-wait=3000ms`，100 并发下与约 3 秒长尾高度相关，但必须通过池等待指标继续验证。
6. 幂等抢占失败的请求至少执行一次 Lua 和一次 GET，热点同键场景产生额外 Redis 往返。
7. 收藏创建会远程查询当前用户；当前用户已经由 JWT 认证，数据库外键也能兜底用户被删除的竞态。
8. `CacheUtil.deleteByPattern` 使用 Redis `KEYS`，数据量增加后会阻塞 Redis，必须治理。

### 4.2 待验证假设

| 编号 | 假设 | 置信度 | 验证方式 |
|------|------|--------|----------|
| H1 | 约 3 秒的 409 长尾主要来自冷启动、JWT 重复解析和 Redis 连接池等待 | 高 | 增加阶段 Timer，分别跑冷/热 5 轮 |
| H2 | 关闭 DEBUG SQL/MVC 日志可显著降低首轮和高并发延迟 | 高 | 仅修改日志级别执行 A/B 测试 |
| H3 | 将 JWT 单次解析后，CPU 和 P95 会明显下降 | 高 | JFR/CPU 采样 + 同场景 A/B |
| H4 | 幂等“抢占或读取现值”合并为一次 Lua，可降低 409 路径延迟 | 中高 | Redis command latency 与 409 P95 对比 |
| H5 | 删除当前用户 Feign 校验可降低成功请求 200–300ms | 高 | 成功请求分段 Timer 和 A/B |
| H6 | 分页缓存 `KEYS` 失效会在数据增长后制造 Redis 长尾 | 高 | 构造 1 万分页 Key 后比较 KEYS、SCAN 和版本号方案 |
| H7 | 直接扩大线程池和连接池能解决问题 | 低 | 只有指标证明池等待后才调参，禁止盲目扩大 |

## 5. 分阶段优化方案

## P0：先建立可信测量体系

**目标：** 消除冷启动、日志和环境波动对结论的污染。

### 计划

1. 为并发编排脚本增加 `cold`、`warmup` 和 `steady` 三种模式。
2. 冷态轮之后执行独立预热轮，预热数据不进入稳态统计；当前 Win11 环境默认预热 5 轮。
3. 性能模式显式关闭 DEBUG 日志和 MyBatis SQL 输出。
4. 为以下阶段增加 Micrometer Timer：
   - `service.jwt`
   - `idempotency.claim`
   - `collection.toggle-locked`
   - `collection.route-feign`
   - `collection.user-feign`
   - `collection.lookup`
   - `collection.db-insert`
   - `collection.cache-invalidation`
   - `idempotency.complete`
5. 每轮输出环境快照、阶段耗时和 5 轮中位数。

### 初始验收门槛

- 同一代码稳态执行 5 轮，P95 变异系数不超过 10%。
- 冷启动与稳态分别生成摘要。
- 数据库基线恢复、端口清理和业务状态分布继续通过。

### 性能模式配置草案

```powershell
$performanceFlags = @(
    '--logging.level.root=WARN',
    '--logging.level.travel=INFO',
    '--logging.level.org.springframework.web=WARN',
    '--logging.level.org.mybatis=WARN',
    '--mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl',
    '--travel.performance.metrics-endpoints-enabled=true'
)
```

## P1：降低认证 CPU 和重复解析

**目标：** 保留 Gateway 与业务服务双层鉴权，但每层只解析一次 JWT。

### 方案

1. Gateway 启动时构造并缓存 `SecretKey` 和 `JwtParser`，请求中不再重复创建。
2. `JwtHelper` 增加一次解析返回完整 Principal 的方法。
3. `JwtAuthenticationFilter` 一次获得 userId、userType、expiration，不再解析三次。
4. Token 黑名单检查保持不变，不以安全降级换性能。

### 核心变更代码草案

```java
public record JwtPrincipal(Long userId, Integer userType, Date expiration) {
}

public static JwtPrincipal parsePrincipal(String token) {
    Claims claims = JWT_PARSER.parseSignedClaims(token).getPayload();
    Number userId = claims.get("userId", Number.class);
    Number userType = claims.get("userType", Number.class);
    return new JwtPrincipal(
            userId == null ? null : userId.longValue(),
            userType == null ? null : userType.intValue(),
            claims.getExpiration());
}
```

```java
JwtPrincipal principal = JwtHelper.parsePrincipal(token);
if (principal.expiration().before(new Date())) {
    filterChain.doFilter(request, response);
    return;
}
if (cacheUtil.exists("blacklist:token:" + token)) {
    filterChain.doFilter(request, response);
    return;
}

String role = Integer.valueOf(9).equals(principal.userType())
        ? "ROLE_ADMIN"
        : "ROLE_USER";
SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
                principal.userId(), null,
                List.of(new SimpleGrantedAuthority(role))));
```

### 风险与回滚

- 风险：静态缓存 Parser 后，测试中动态修改 JWT Secret 的用例可能受影响。
- 处理：优先改为 Spring 单例 `JwtTokenService` 注入固定 Secret，而不是不可重置的静态变量。
- 回滚：保留旧解析方法一轮，所有 JWT 过期、伪造、角色和黑名单测试通过后再删除。

## P2：降低 Redis 幂等热点路径往返

**目标：** 将热点同键冲突路径从“Lua 抢占 + GET”压缩为一次 Redis Lua。

### 当前问题

未抢到执行权的 99 个请求会先运行 CLAIM Lua，再执行 GET 读取状态。在 Redis 池较小、连接尚未预热时，会放大排队和网络往返。

### 核心变更代码草案

```java
private static final String CLAIMED_MARKER = "__CLAIMED__";

private static final DefaultRedisScript<String> CLAIM_OR_READ_SCRIPT =
        new DefaultRedisScript<>(
                "local existing = redis.call('get', KEYS[1]); "
                        + "if not existing then "
                        + "redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2]); "
                        + "return ARGV[3]; "
                        + "end; "
                        + "return existing;",
                String.class);

String result = redisTemplate.execute(
        CLAIM_OR_READ_SCRIPT,
        Collections.singletonList(key),
        processingValue,
        String.valueOf(processingTtl.toMillis()),
        CLAIMED_MARKER);

if (CLAIMED_MARKER.equals(result)) {
    return claimedResult(key, token, request);
}
return resolveExistingResult(key, request, result);
```

### 连接池策略

先采集 `lettuce.pool.active/idle/pending` 和 Redis command latency。只有确认存在等待，才逐级测试：

```text
max-active: 8 -> 16 -> 32
min-idle:   0 -> 4  -> 8
max-wait:   3000ms -> 500ms
```

不能直接把池调到 100；连接数过大可能把压力转移到 Redis 和操作系统。

### 初始验收门槛

- 稳态 409 路径 P95 小于 300ms，P99 小于 500ms。
- Redis pending wait 接近 0。
- Redis 中断仍返回 503，不能变成绕过幂等。

## P3：缩短成功业务链路

**目标：** 减少原始 HTTP 200 请求中的跨服务调用和锁持有时间。

### 3.1 删除当前用户重复 Feign 校验

收藏接口中的 `userId` 来自认证上下文，不是客户端请求体。正常情况下无需每次调用 user-service 再确认一次当前用户存在；用户在 Token 有效期内被删除的极端竞态由 `user_collection.user_id` 外键兜底。

核心变更代码草案：

```java
private RouteCollection createCollectionRecord(
        Integer routeId,
        Integer authenticatedUserId,
        Boolean isPublic,
        String notes) {
    Route route = routeService.getById(routeId.longValue());
    if (route == null) {
        throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
    }

    RouteCollection collection = new RouteCollection();
    collection.setRouteId(routeId);
    collection.setUserId(authenticatedUserId);
    collection.setItemType(ROUTE_ITEM_TYPE);
    collection.setCollectionType(COLLECT_ACTION);
    collection.setCollectionTime(LocalDateTime.now());
    collection.setIsPublic(Boolean.TRUE.equals(isPublic));
    collection.setNotes(notes);

    try {
        if (!save(collection)) {
            throw new BusinessException(ErrorCodeEnum.COLLECTION_CREATE_FAILED);
        }
    } catch (DataIntegrityViolationException exception) {
        // 外键失败时再映射为用户不存在，避免正常请求每次跨服务查询。
        throw mapCollectionIntegrityFailure(exception);
    }
    return collection;
}
```

### 3.2 缩小路线可读性响应

收藏服务只需要路线 ID、所有者、公开状态和发布状态，不需要完整路线详情。后续可将当前 `/routes/{id}/readable` 返回值收敛为最小 DTO：

```java
public record RouteAccessSnapshot(
        Integer routeId,
        Integer ownerId,
        boolean readable,
        String status) {
}
```

### 3.3 缩短锁内逻辑

- Feign 校验是否移出收藏锁，需要先解决“校验后路线被删除”的竞态和路线外键缺失问题。
- 在没有数据库约束或可靠事件清理前，不直接把所有校验移到锁外。
- 优先删除用户 Feign，再评估路线校验的位置。

### 初始验收门槛

- 稳态原始 HTTP 200 小于 1000ms。
- 路线不存在、私有路线越权、用户被删除和数据库外键异常测试全部通过。

## P4：治理缓存失效和 Redis 阻塞风险

**目标：** 删除写链路中的 Redis `KEYS`，避免数据增长后出现全局阻塞和 P99 抖动。

### 方案比较

| 方案 | 优点 | 缺点 | 建议 |
|------|------|------|------|
| `KEYS pattern` | 实现最简单 | 阻塞 Redis，生产不可接受 | 删除 |
| `SCAN + UNLINK` | 渐进扫描，改动较小 | 仍需遍历，删除存在短暂延迟 | 近期采用 |
| 缓存版本号 | 写入只递增版本，不扫描 | 读取多一次版本获取，旧 Key 等 TTL | 中期采用 |
| 精确维护 Key 集合 | 精确删除 | 需要额外集合一致性维护 | 暂不采用 |

### SCAN + UNLINK 核心草案

```java
public void deleteByPattern(String pattern) {
    redisTemplate.execute(connection -> {
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(200)
                .build();
        try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
            List<byte[]> batch = new ArrayList<>(200);
            cursor.forEachRemaining(key -> {
                batch.add(key);
                if (batch.size() == 200) {
                    connection.keyCommands().unlink(batch.toArray(byte[][]::new));
                    batch.clear();
                }
            });
            if (!batch.isEmpty()) {
                connection.keyCommands().unlink(batch.toArray(byte[][]::new));
            }
        }
        return null;
    });
}
```

### 验收门槛

- 构造至少 1 万个匹配 Key，执行失效时 Redis 其他 GET 的 P99 不明显抬升。
- 收藏写入 P99 不因分页缓存数量线性增长。

## P5：数据库、线程池和 JVM 调优

**前提：** 只有 P0–P4 完成且指标证明资源池饱和后，才进入本阶段。

### 检查顺序

1. MySQL `EXPLAIN` 确认 `uk_user_item_action` 覆盖用户、目标、类型和动作查询。
2. Hikari 记录 active、idle、pending、acquire time，确认是否需要从 10 调整。
3. Tomcat 记录 busy threads、queue 和 rejected，确认线程池是否饱和。
4. Redis 记录 active、pending、command latency 和 server CPU。
5. 使用 JFR 检查 JWT、JSON、日志、锁和 GC 热点。
6. 使用固定堆大小减少测试期间动态扩容噪声，例如 `-Xms512m -Xmx512m`。

### 禁止行为

- 未采集 pending 指标时直接扩大 Hikari、Tomcat 或 Redis 池。
- 通过无限队列掩盖过载。
- 只提高超时，使慢请求等待更久。

## 6. 总体验收目标

以下是本机展示环境的初始工程目标，不是生产 SLA，后续可根据稳定基线修订：

| 场景 | P95 目标 | P99 目标 | 吞吐目标 | 正确性目标 |
|------|----------|----------|----------|------------|
| 稳态同键 409 路径 | ≤300ms | ≤500ms | ≥80 req/s | 99 个预期 409 |
| 稳态原始 200 路径 | ≤1000ms | ≤1500ms | N/A，单次执行业务 | 最终仅 1 条记录 |
| 冷启动原始 200 | ≤3000ms | 单样本不适用 | N/A | 首次请求不能超时或错误降级 |
| Redis 故障 | N/A | N/A | N/A | HTTP 503，数据库不写入 |
| 恢复后重放 | ≤500ms | ≤800ms | N/A | 响应一致且 `Idempotency-Replayed=true` |

## 7. 每轮优化记录模板

复制以下章节并按顺序追加，禁止覆盖历史数据。

```markdown
## 第 N 轮：标题

- 日期：YYYY-MM-DD
- 状态：计划中 / 实施中 / 已验证 / 已回滚
- 负责人：
- 变更文件：
- 证据目录：

### 问题与假设

### 优化前数据

| Threads | Average | P95 | P99 | Throughput | Error | DB Fact |
|---------|---------|-----|-----|------------|-------|---------|

### 方案与取舍

### 核心变更代码

### 优化后数据

| Threads | Average | P95 | P99 | Throughput | Error | DB Fact |
|---------|---------|-----|-----|------------|-------|---------|

### 数据对比

| 指标 | Before | After | 变化率 | 是否达标 |
|------|--------|-------|--------|----------|

### 回归测试

### 结论、遗留问题与下一轮

### 回滚说明
```

## 8. 分轮实施记录

## 第 0 轮：先修复副作用接口和 Feign 超时错误语义

- 日期：2026-08-23
- 状态：已验证
- 目标：先保证唯一业务请求正确完成，再进入纯延迟优化。
- 变更文件：
  - `backend/route-service/src/main/java/travel/route/controller/RouteController.java`
  - `backend/collection-service/src/main/java/travel/collection/feign/RouteFeignClient.java`
  - `backend/collection-service/src/main/java/travel/collection/service/RouteService.java`

### 优化前数据

| Average | P95 | P99 | HTTP 分布 | 结果 |
|---------|-----|-----|-----------|------|
| 2657.74ms | 2855ms | 2883ms | 0 个 200、99 个 409、1 个 404 | FAIL |

### 根因

收藏服务把“路线是否可读”绑定到公开详情接口。公开详情还会增加浏览量、插入访问明细并清理缓存，首次调用超过 Feign 3 秒读取超时，随后被包装层错误转换为 `ROUTE_NOT_EXIST`。

### 核心变更代码

```java
@GetMapping("/{id}/readable")
public Result<Route> getReadableRoute(@PathVariable Integer id) {
    Integer currentUserId = AuthenticatedUserSupport.requireIntegerUserId();
    Route route = findReadableRoute(id, currentUserId);
    if (route == null) {
        throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
    }
    return Result.success("读取路线成功", route);
}
```

```java
@GetMapping("/{id}/readable")
Result<Route> getReadableById(@PathVariable Integer id);
```

### 优化后数据

| Average | P95 | P99 | HTTP 分布 | 数据事实 | 结果 |
|---------|-----|-----|-----------|----------|------|
| 3002.37ms | 3365ms | 3394ms | 1 个 200、99 个 409 | 执行时 1 条，结束恢复基线 | PASS |

### 结论

- 正确性问题已解决，内部业务读取不再污染访问统计。
- 当前数据包含明显冷启动，不能据此判断纯性能回退。
- 下一轮必须先完成 P0 测量治理。

## 第 1 轮：冷启动与稳态基线拆分

- 日期：2026-08-24
- 状态：部分验证（测量体系和正确性通过，P95 稳定性门槛未通过）
- 目标：增加独立预热、性能日志配置和阶段 Timer，获得可比较的 5 轮稳态基线。
- 最终证据：`run-logs/jmeter-gateway-idempotency/20260823-235742-538/`
- 核心变更范围：
  - `backend/common/src/main/java/travel/common/performance/PerformanceStageRecorder.java`
  - `backend/common/src/main/java/travel/common/config/JwtAuthenticationFilter.java`
  - `backend/common/src/main/java/travel/common/config/HttpIdempotencyFilter.java`
  - `backend/collection-service/src/main/java/travel/collection/service/RouteService.java`
  - `backend/collection-service/src/main/java/travel/collection/service/UserService.java`
  - `backend/collection-service/src/main/java/travel/collection/service/impl/RouteCollectionServiceImpl.java`
  - `ops/jmeter/run-idempotency-test.ps1`
  - `ops/jmeter/run-live-gateway-idempotency-test.ps1`

### 问题、假设与验证

| 假设 | 结果 | 结论 |
|------|------|------|
| 1 个冷态轮即可完成业务预热 | 不成立 | P95 在后续多轮仍持续下降，最终改为 5 个独立预热轮 |
| 默认 Feign 3 秒读超时足以覆盖冷态路线校验 | 不成立 | 真正启用 Timer 后首次路线 Feign 曾超时；性能脚本改为 route 15 秒、user 10 秒，仅用于完整记录冷态，不作为延迟优化收益 |
| `xxl.job.executor.enabled=false` 能关闭 XXL-Job | 不成立 | 实际条件键为 `xxl.job.enabled`，错误配置会每 30 秒失败重连并污染本机测量 |
| Micrometer Timer 已在运行时启用 | 初次不成立，修复后成立 | `PerformanceStageRecorder` 的私有无参构造器被 Spring 选中，导致禁用实例；删除该构造器后阶段指标正常产生 |
| 5 轮稳态 P95 CV 可低于 10% | 暂不成立 | 最终 CV 为 12.02%，本轮不把该数据标记为正式稳定基线 |

### 核心变更代码

```java
public static PerformanceStageRecorder disabled() {
    return new PerformanceStageRecorder(null, false);
}

public void record(String stage, long startedAtNanos, String outcome) {
    if (!enabled || meterRegistry == null || startedAtNanos <= 0L) {
        return;
    }
    long durationNanos = Math.max(0L, System.nanoTime() - startedAtNanos);
    Timer.builder(METRIC_NAME)
            .tag("stage", normalize(stage, "unknown"))
            .tag("outcome", normalize(outcome, "unknown"))
            .register(meterRegistry)
            .record(durationNanos, TimeUnit.NANOSECONDS);
}
```

```powershell
$roundDefinitions.Add([pscustomobject]@{ name = 'cold'; mode = 'cold' })
# 预热轮不进入稳态统计，避免把 JIT、连接池和首次 Feign 成本混入基线。
1..$WarmupRuns | ForEach-Object { ... mode = 'warmup' }
1..$SteadyStateRuns | ForEach-Object { ... mode = 'steady' }
```

性能模式同时固定 `-Xms512m -Xmx512m`、关闭 DEBUG/MyBatis SQL 日志、关闭 XXL-Job，并仅在本机回环地址开放 Actuator 指标读取。

### 冷态与预热数据

| 轮次 | Average | P50 | P95 | P99 | Throughput | 原始 200 | 冲突 409 |
|------|---------|-----|-----|-----|------------|----------|----------|
| cold | 3001.29ms | 2949ms | 3291ms | 3414ms | 13.56/s | 7360ms | 2957.26ms |
| warmup-01 | 1277.60ms | 1211ms | 1816ms | 1913ms | 47.98/s | 2079ms | 1269.51ms |
| warmup-02 | 1052.98ms | 1083ms | 1588ms | 1611ms | 59.92/s | 1640ms | 1047.05ms |
| warmup-03 | 1265.66ms | 1308ms | 1482ms | 1536ms | 61.69/s | 1618ms | 1262.10ms |
| warmup-04 | 1082.26ms | 1175ms | 1357ms | 1379ms | 70.08/s | 1375ms | 1079.30ms |
| warmup-05 | 885.63ms | 1020ms | 1160ms | 1177ms | 78.19/s | 1273ms | 881.72ms |

### 五轮稳态数据

| 轮次 | Average | P50 | P95 | P99 | Throughput | 原始 200 | 冲突 409 |
|------|---------|-----|-----|-----|------------|----------|----------|
| steady-01 | 890.42ms | 929ms | 1047ms | 1062ms | 89.61/s | 1110ms | 888.20ms |
| steady-02 | 629.05ms | 675ms | 1066ms | 1084ms | 81.50/s | 1221ms | 623.07ms |
| steady-03 | 587.50ms | 633ms | 851ms | 899ms | 97.85/s | 1007ms | 583.26ms |
| steady-04 | 676.07ms | 699ms | 841ms | 876ms | 105.82/s | 936ms | 673.44ms |
| steady-05 | 747.50ms | 708ms | 1131ms | 1150ms | 76.92/s | 1294ms | 741.98ms |
| **中位数** | **676.07ms** | - | **1047ms** | **1062ms** | **89.61/s** | **1110ms** | **673.44ms** |

- P95 变异系数：`12.02%`，高于 `10%` 门槛，因此本轮数据只作为“候选稳态基线”。
- 11 个业务轮次均为 1 次原始执行、99 次处理中冲突、0 非预期响应；脚本也允许完成后的合法结果重放，核心约束是始终只有 1 次原始执行。
- 执行期间数据库目标记录为 1 条，结束后恢复原始 1 条基线；端口和进程均已清理。

### 阶段 Timer 结果

| 阶段 | 冷态平均 | 稳态平均中位数 | 稳态范围 | 结论 |
|------|----------|----------------|----------|------|
| `service.jwt.authenticated` | 79.077ms | 49.680ms | 36.747–150.653ms | JWT 重复解析仍有明显 CPU/Redis 黑名单成本，进入 P1 |
| `idempotency.claim` | 1449.505ms | 104.817ms | 38.200–298.221ms | 100 个同键请求竞争时是 409 路径主要波动源，进入 P2 |
| `collection.route-feign` | 2735.543ms | 274.299ms | 213.863–617.024ms | 冷态代价最高，稳态仍有明显网络和下游波动 |
| `collection.user-feign` | 235.684ms | 210.065ms | 201.604–343.383ms | 成功链路存在可删除的重复用户校验，进入 P3 |
| `collection.toggle-locked` | 3414.663ms | 672.905ms | 498.235–1015.387ms | 成功链路锁内包含两个 Feign、查询、写库和缓存失效 |
| `collection.lookup` | 178.188ms | 41.200ms | 29.083–79.542ms | 可接受，但仍需结合索引执行计划确认 |
| `collection.db-insert` | 382.304ms | 19.411ms | 14.842–54.251ms | 稳态写库不是首要瓶颈 |
| `collection.cache-invalidation` | 17.203ms | 4.708ms | 3.874–6.287ms | 当前单次成本低，但 `KEYS` 仍有数据规模风险，留到 P4 |
| `idempotency.complete` | 11.297ms | 1.720ms | 1.281–3.635ms | 非主要瓶颈 |

### 回归测试

- `PerformanceStageRecorderTest`：1/1 通过。
- `JwtAuthenticationFilterTest`：3/3 通过。
- `HttpIdempotencyFilterTest`：10/10 通过。
- `RouteServiceTest`：1/1 通过。
- `RouteCollectionServiceImplTest`：2/2 通过。
- 四服务 Reactor 打包成功；最终实测数据库基线恢复为 `true`。

### 结论与下一轮

1. P0 的冷热拆分、独立预热、性能日志、阶段 Timer、响应分布和数据库恢复已经完成。
2. P95 CV 仍为 12.02%，说明本地 Win11 单机同时运行 JMeter、网关和三个业务服务时仍有约 10% 以上调度波动；正式 Before/After 应使用同一脚本成对比较，并至少重复两组。
3. 下一轮优先执行 P1：JWT 单次解析和 Parser/SecretKey 复用；随后执行 P2：合并幂等 Lua 返回，减少失败者 Redis 往返。
4. 不把测试用 Feign 超时上调计为性能收益；它只保证冷态慢调用能够被完整测量，真实 2.7 秒冷态耗时仍保留在数据中。

## 9. 优化看板

| 轮次 | 状态 | 核心变化 | P95 Before | P95 After | P99 Before | P99 After | 正确性 |
|------|------|----------|------------|-----------|------------|-----------|--------|
| 0 | 已验证 | 无副作用路线读取契约 | 2855ms | 3365ms | 2883ms | 3394ms | 从 FAIL 修复为 PASS；数据不可直接比较 |
| 1 | 部分验证 | 冷热分离、5 轮预热、Timer、关闭 DEBUG/XXL-Job | 3365ms（混合冷态） | 1047ms（稳态中位） | 3394ms（混合冷态） | 1062ms（稳态中位） | 11 轮正确；P95 CV 12.02% 未达标 |
| 2 | 待排期 | JWT 单次解析 | - | - | - | - | 待执行 |
| 3 | 待排期 | 幂等 Lua 合并、Redis 池验证 | - | - | - | - | 待执行 |
| 4 | 待排期 | 删除当前用户 Feign、缩小 DTO | - | - | - | - | 待执行 |
| 5 | 待排期 | SCAN/UNLINK 或缓存版本号 | - | - | - | - | 待执行 |

## 10. 自我检查清单

每轮结束前确认：

- [x] 优化数据来自相同硬件和相同场景。
- [x] 冷启动和稳态没有混用。
- [x] P95、P99、吞吐和错误率均有记录。
- [x] HTTP 状态分布符合场景语义。
- [x] 数据库最终事实和唯一键约束通过。
- [ ] Redis 故障仍然 fail-closed。
- [ ] JWT、越权、黑名单和角色测试通过。
- [x] 没有把测试用超时上调计为性能收益，真实慢调用仍由 Timer 记录。
- [ ] 没有使用 Redis `KEYS` 新增阻塞链路。
- [x] 临时数据、端口和进程已清理。
- [x] 核心变更代码、证据目录和回滚方式已回填。
