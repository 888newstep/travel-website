# 智慧旅游线路系统 - 数据链路详解

## 目录

- [一、整体架构概览](#一整体架构概览)
- [二、请求链路基础流程](#二请求链路基础流程)
- [三、各微服务数据链路](#三各微服务数据链路)
  - [3.1 attraction-service 景点服务](#31-attraction-service-景点服务)
  - [3.2 route-service 路线服务](#32-route-service-路线服务)
  - [3.3 collection-service 收藏服务](#33-collection-service-收藏服务)
  - [3.4 user-service 用户服务](#34-user-service-用户服务)
  - [3.5 file-service 文件服务](#35-file-service-文件服务)
- [四、跨服务调用链路](#四跨服务调用链路)
- [五、异步消息链路](#五异步消息链路)
- [六、定时任务链路](#六定时任务链路)
- [七、缓存策略详解](#七缓存策略详解)
- [八、数据库优化链路](#八数据库优化链路)
- [九、第三方 API 集成](#九第三方-api-集成)
- [十、数据一致性保障](#十数据一致性保障)

---

## 一、整体架构概览

### 1.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         前端层 (React + TypeScript)                  │
│  • Vite 构建工具                                                      │
│  • Axios HTTP 客户端                                                  │
│  • React Router 路由管理                                              │
└────────────────────────────┬────────────────────────────────────────┘
                             │ HTTP/HTTPS
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    网关层 (Spring Cloud Gateway :8090)                │
│  • AuthGlobalFilter: JWT 校验 + 用户信息透传                          │
│  • 路由分发: 基于 Path 谓词匹配                                       │
│  • Sentinel 限流熔断                                                  │
│  • CORS 跨域处理                                                      │
└────────────────────────────┬────────────────────────────────────────┘
                             │
              ┌──────────────┼──────────────┬──────────────┐
              │              │              │              │
              ▼              ▼              ▼              ▼
┌─────────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ attraction-     │ │ route-       │ │ collection-  │ │ user-        │
│ service :8091   │ │ service:8092 │ │ service:8093 │ │ service:8094 │
│                 │ │              │ │              │ │              │
│ • 景点 CRUD     │ │ • 路线规划   │ │ • 收藏管理   │ │ • 用户认证   │
│ • 实时状态      │ │ • AI 优化    │ │ • 评论互动   │ │ • 权限管理   │
│ • 餐厅推荐      │ │ • 智能对话   │ │ • 游记分享   │ │ • 个人信息   │
│ • 城市信息      │ │ • 数据统计   │ │ • 通知推送   │ │              │
└────────┬────────┘ └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
         │                 │                │                │
         └─────────────────┴────────────────┴────────────────┘
                             │
              ┌──────────────┼──────────────┬──────────────┐
              │              │              │              │
              ▼              ▼              ▼              ▼
┌─────────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ MySQL           │ │ Redis        │ │ RabbitMQ     │ │ 第三方 API   │
│ travel_website  │ │ • 缓存       │ │ • 通知队列   │ │ • 高德地图   │
│ • 景点数据      │ │ • 分布式锁   │ │ • 缓存更新   │ │ • 百度 AI    │
│ • 路线数据      │ │ • 计数器     │ │ • 异步任务   │ │ • 通义千问   │
│ • 用户数据      │ │ • 排行榜     │ │              │ │ • 短信服务   │
└─────────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
```

### 1.2 技术栈总览

| 层级 | 技术组件 | 说明 |
|------|---------|------|
| 前端 | React 18 + TypeScript + Vite | SPA 单页应用 |
| 网关 | Spring Cloud Gateway | 统一入口、路由、鉴权 |
| 服务注册 | Nacos | 服务发现与配置中心 |
| 服务通信 | OpenFeign + LoadBalancer | 同步调用 + 负载均衡 |
| 熔断限流 | Sentinel | 流量控制、熔断降级 |
| 分布式事务 | Seata | AT 模式保证最终一致性 |
| 数据库 | MySQL 8.0 + MyBatis-Plus | 持久化存储 |
| 缓存 | Redis + Redisson | 缓存 + 分布式锁 |
| 消息队列 | RabbitMQ | 异步解耦 |
| 定时任务 | XXL-Job + @Scheduled | 分布式调度 |
| API 文档 | Knife4j + SpringDoc | OpenAPI 3.0 |

---

## 二、请求链路基础流程

### 2.1 标准请求处理流程

```
1. 前端发起请求
   ↓
2. Gateway 接收请求
   ├─ 检查白名单 → 放行
   ├─ 提取 JWT Token → 校验签名和有效期
   ├─ 解析 Claims → 提取 userId、role
   ├─ 注入请求头 X-User-Id、X-User-Role
   └─ 路由到目标服务
   ↓
3. 目标服务接收请求
   ├─ Controller 层: 参数校验、权限检查
   ├─ Service 层: 业务逻辑、缓存查询、分布式锁
   ├─ Mapper 层: 数据库操作
   └─ 返回 Result<T> 统一响应
   ↓
4. Gateway 转发响应
   ↓
5. 前端接收响应
   ├─ Axios 拦截器处理
   ├─ 统一错误处理 (401/403/404/500)
   └─ 数据解包 (提取 data 字段)
```

### 2.2 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

### 2.3 错误处理链路

```
业务异常 → BusinessException → GlobalExceptionHandler → Result.error()
参数异常 → MethodArgumentNotValidException → Result.error(400)
权限异常 → AccessDeniedException → Result.error(403)
系统异常 → Exception → Result.error(500)
```

---

## 三、各微服务数据链路

### 3.1 attraction-service 景点服务

#### 3.1.1 景点详情查询链路

```
GET /api/attractions/{id}
  ↓
AttractionController.getAttraction()
  ↓
AttractionServiceImpl.getById(id)
  │
  ├─ 1. BloomFilter 检查
  │     └─ attractionBloomFilterService.mightContain(id)
  │         ├─ false → 直接返回 null (防穿透)
  │         └─ true → 继续查询
  │
  ├─ 2. 空值缓存检查
  │     └─ cacheUtil.exists("attraction:null:{id}")
  │         ├─ true → 返回 null (防穿透)
  │         └─ false → 继续查询
  │
  ├─ 3. Redis 缓存查询
  │     └─ cacheUtil.get("attraction:detail:{id}", Attraction.class)
  │         ├─ 命中 → 返回缓存数据
  │         └─ 未命中 → 继续查询数据库
  │
  ├─ 4. MySQL 数据库查询
  │     └─ super.getById(id) → SELECT * FROM attraction WHERE id = ?
  │
  ├─ 5. 缓存写入
  │     ├─ 数据存在 → cacheUtil.set("attraction:detail:{id}", data, 1h)
  │     └─ 数据不存在 → cacheUtil.set("attraction:null:{id}", true, 5min)
  │
  └─ 6. 返回结果
```

**关键代码位置**:
- `AttractionServiceImpl.java:48-85`
- `AttractionBloomFilterService.java:47-50`

#### 3.1.2 景点列表查询链路

```
GET /api/attractions
  ↓
AttractionController.getAttractions()
  ↓
AttractionServiceImpl.list()
  │
  ├─ 1. Redis 缓存查询
  │     └─ cacheUtil.get("attraction:all", List.class)
  │         ├─ 命中 → 返回缓存数据
  │         └─ 未命中 → 继续查询数据库
  │
  ├─ 2. MySQL 数据库查询
  │     └─ super.list() → SELECT * FROM attraction
  │
  ├─ 3. 缓存写入
  │     └─ cacheUtil.set("attraction:all", attractions, 2h)
  │
  └─ 4. 返回结果
```

#### 3.1.3 按城市查询景点链路

```
GET /api/attractions/city/{cityId}
  ↓
AttractionServiceImpl.getByCityId(cityId)
  │
  ├─ 1. Redis 缓存查询
  │     └─ cacheUtil.get("attraction:city:{cityId}", List.class)
  │
  ├─ 2. MySQL 数据库查询 (使用覆盖索引)
  │     └─ QueryWrapper: WHERE city_id = ? ORDER BY rating DESC
  │         索引: idx_city_rating_cover(city_id, rating DESC, name, id)
  │         优势: 无需回表，直接从索引返回数据
  │
  ├─ 3. 缓存写入 (TTL=1h)
  │
  └─ 4. 返回结果
```

#### 3.1.4 游标分页查询链路

```
GET /api/attractions/cursor?cityId=1&cursor=3.50:5&size=10
  ↓
AttractionServiceImpl.getByCursor(cityId, cursor, size)
  │
  ├─ 1. 解析游标
  │     └─ CursorPageResult.decodeCursor("3.50:5")
  │         → lastRating=3.50, lastId=5
  │
  ├─ 2. MySQL 查询 (使用覆盖索引)
  │     └─ SELECT * FROM attraction
  │        WHERE city_id = 1
  │          AND (rating < 3.50 OR (rating = 3.50 AND id < 5))
  │        ORDER BY rating DESC, id DESC
  │        LIMIT 11  (多取 1 条判断 hasMore)
  │
  ├─ 3. 构造响应
  │     ├─ records: 前 10 条数据
  │     ├─ hasMore: 是否有更多数据
  │     ├─ lastRating: 最后一条的评分
  │     └─ lastId: 最后一条的 ID
  │
  └─ 4. 返回 CursorPageResult
```

**性能对比**:
- 偏移分页: `LIMIT offset, size` → 页码越大越慢
- 游标分页: `WHERE (rating, id) < (lastRating, lastId)` → 性能恒定

#### 3.1.5 实时数据同步链路

```
@Scheduled(fixedRate = 15 * 60 * 1000)  // 每 15 分钟
  ↓
SyncRealtimeDataTask.syncRealtimeData()
  │
  ├─ 1. 查询需要同步的景点
  │     └─ attractionRealtimeStatusService.selectNeedSyncStatus(15)
  │
  ├─ 2. 调用第三方 API (模拟)
  │     └─ thirdApiUtil.getRealtimeData(statusList)
  │         ├─ 生成随机天气数据
  │         ├─ 生成随机温度数据
  │         └─ 生成随机人流数据
  │
  ├─ 3. 批量更新数据库
  │     └─ attractionRealtimeStatusService.batchUpdateStatus(updatedList)
  │
  └─ 4. 更新 Redis 缓存
        └─ cacheUtil.set("realtime_status:{attractionId}", status, 15min)
```

#### 3.1.6 BloomFilter 刷新链路

```
@Scheduled(cron = "0 0/30 * * * ?")  // 每 30 分钟
  ↓
AttractionBloomFilterService.refreshBloomFilter()
  │
  ├─ 1. 查询所有景点 ID
  │     └─ attractionMapper.selectList(null) → List<Integer> ids
  │
  ├─ 2. 创建新的 BloomFilter
  │     └─ BloomFilter.create(Funnels.integerFunnel(), expectedInsertions, 0.01)
  │
  ├─ 3. 填充数据
  │     └─ ids.forEach(newFilter::put)
  │
  └─ 4. 原子替换
        └─ bloomFilterRef.set(newFilter)
```

---

### 3.2 route-service 路线服务

#### 3.2.1 路线创建链路

```
POST /api/routes
  ↓
RouteController.createRoute()
  ↓
RouteServiceImpl.createRoute()
  │
  ├─ 1. 参数校验
  │     └─ 检查必填字段、城市是否存在
  │
  ├─ 2. 写入 MySQL
  │     └─ routeMapper.insert(route)
  │
  ├─ 3. 处理景点关联
  │     └─ routeAttractionMapper.batchInsert(routeId, attractionIds)
  │
  ├─ 4. 处理交通关联
  │     └─ routeTransportMapper.batchInsert(routeId, transports)
  │
  ├─ 5. 发送异步消息 (可选)
  │     └─ messageProducerService.sendNotification(userId, "路线创建成功")
  │
  └─ 6. 返回结果
```

#### 3.2.2 AI 智能路线规划链路

```
POST /api/ai/smart-itinerary
  ↓
AIController.generateSmartItinerary()
  ↓
AISmartItineraryServiceImpl.generate()
  │
  ├─ 1. 收集用户需求
  │     ├─ 城市、天数、偏好
  │     └─ 预算、人数、特殊要求
  │
  ├─ 2. 查询相关数据
  │     ├─ attractionFeignClient.getByCityId(cityId)
  │     └─ 查询餐厅、交通信息
  │
  ├─ 3. 构造 Prompt
  │     └─ 组装系统提示词 + 用户数据
  │
  ├─ 4. 调用通义千问 API
  │     └─ qwenService.chat(prompt)
  │         ├─ HTTP POST → 通义千问 API
  │         └─ 返回生成的路线方案
  │
  ├─ 5. 解析 AI 响应
  │     └─ 提取景点、时间安排、交通建议
  │
  ├─ 6. 缓存结果 (可选)
  │     └─ cacheUtil.set("ai:itinerary:{userId}:{cityId}", result, 1h)
  │
  └─ 7. 返回智能路线
```

#### 3.2.3 路线优化链路

```
POST /api/route-optimization/optimize
  ↓
RouteOptimizationController.optimizeRoute()
  ↓
RouteOptimizationServiceImpl.optimize()
  │
  ├─ 1. 获取原始路线
  │     └─ routeService.getById(routeId)
  │
  ├─ 2. 获取景点坐标
  │     └─ attractionFeignClient.getById(attractionId) → latitude, longitude
  │
  ├─ 3. 计算距离矩阵
  │     └─ 使用 Haversine 公式计算景点间距离
  │
  ├─ 4. 应用优化算法
  │     ├─ 贪心算法: 每次选择最近的未访问景点
  │     ├─ 动态规划: 小规模景点 (≤10) 最优解
  │     └─ 遗传算法: 大规模景点 (≥20) 近似解
  │
  ├─ 5. 重新排序景点
  │     └─ 生成优化后的游览顺序
  │
  ├─ 6. 计算优化效果
  │     ├─ 原始总距离 vs 优化后总距离
  │     └─ 节省时间百分比
  │
  └─ 7. 返回优化方案
```

#### 3.2.4 XXL-Job 定时任务链路

```
XXL-Job 调度中心
  ↓
TravelJobHandler.refreshAttractionHeat()  // 每 30 分钟
  │
  ├─ 1. 清除热度缓存
  │     └─ cacheUtil.deleteByPattern("attraction:heat:*")
  │
  ├─ 2. 重新计算热度
  │     └─ 基于浏览量、收藏数、评分综合计算
  │
  └─ 3. 更新缓存
        └─ cacheUtil.set("attraction:heat:lastUpdate", now, 30min)

TravelJobHandler.refreshPopularRoutesCache()  // 每小时
  │
  ├─ 1. 清除热门路线缓存
  │     └─ cacheUtil.deleteByPattern("route:popular:*")
  │
  └─ 2. 清除推荐缓存
        └─ cacheUtil.deleteByPattern("route:recommendation:*")

TravelJobHandler.cleanExpiredData()  // 每天凌晨 2 点
  │
  ├─ 1. 清理过期分享
  │     └─ cacheUtil.deleteByPattern("share:expired:*")
  │
  └─ 2. 清理过期 AI 缓存
        └─ cacheUtil.deleteByPattern("ai:cache:expired:*")
```

---

### 3.3 collection-service 收藏服务

#### 3.3.1 路线收藏切换链路（JWT + HTTP 幂等 + 分布式锁）

```
POST /api/v1/route-collections/toggle
Header: Idempotency-Key: <客户端请求标识>
Body: { "routeId": 1 }
  ↓
HttpIdempotencyFilter
  ├─ Redis 原子占位成功 → 执行业务
  ├─ 相同键处理中 → HTTP 409
  └─ 相同键已完成且请求指纹一致 → 重放首次响应
  ↓
RouteCollectionController.toggleCollection()
  └─ userId 只从 JWT SecurityContext 获取
  ↓
RouteCollectionServiceImpl.toggleCollection(routeId, userId)
  │
  ├─ 1. 获取统一的 Redisson 分布式锁
  │     └─ distributedLockService.executeWithLock("collection:{userId}:{routeId}")
  │         ├─ 获取锁成功 → 继续执行
  │         └─ 获取锁失败 → 返回系统繁忙错误
  │
  ├─ 2. 在锁内查询当前事实
  │     └─ WHERE user_id=? AND item_id=? AND item_type='route' AND collection_type='collect'
  │
  ├─ 3. 已存在 → 删除收藏；不存在 → 校验路线和用户后插入
  │     └─ UNIQUE KEY uk_user_item_action
  │        (user_id, item_id, item_type, collection_type) 作为数据库最终兜底
  │
  ├─ 4. 删除状态、分页列表、路线计数及公开列表缓存
  │
  ├─ 5. 返回切换后的 collected 状态
  │
  └─ 6. 释放锁，并将成功响应写入 HTTP 幂等记录
        └─ lock.unlock()
```

**关键代码位置**:
- `HttpIdempotencyFilter.java`
- `RouteCollectionController.java`
- `RouteCollectionServiceImpl.java`
- `DistributedLockService.java`
- `USER_COLLECTION_ACTION_INDEX_MIGRATION.sql`

#### 3.3.2 取消收藏链路

```
DELETE /api/v1/route-collections/remove?routeId={routeId}
  ↓
RouteCollectionServiceImpl.uncollectRoute(routeId, userId)
  │
  ├─ 1. 获取 collection:{userId}:{routeId} 分布式锁
  │
  ├─ 2. 查询 route + collect 收藏记录
  │
  ├─ 3. 存在则删除；不存在也返回成功，保证 DELETE 语义幂等
  │
  ├─ 4. 删除相关缓存
  │
  └─ 5. 返回 true
```

#### 3.3.3 评论发布链路

```
POST /api/route-comments
  ↓
RouteCommentController.createComment()
  ↓
RouteCommentServiceImpl.createComment()
  │
  ├─ 1. 参数校验
  │     └─ 检查路线是否存在、用户是否有权限
  │
  ├─ 2. 写入评论
  │     └─ routeCommentMapper.insert(comment)
  │
  ├─ 3. 更新路线评论数
  │
  ├─ 4. 删除评论列表缓存
  │     └─ cacheUtil.delete("route_comment:route:{routeId}")
  │
  ├─ 5. 发送通知给路线作者 (异步)
  │     └─ messageProducerService.sendNotification(authorId, "新评论")
  │
  └─ 6. 返回评论
```

#### 3.3.4 游记分享链路

```
POST /api/travel-notes
  ↓
TravelNoteController.createNote()
  ↓
TravelNoteServiceImpl.createNote()
  │
  ├─ 1. 参数校验
  │
  ├─ 2. 写入游记
  │     └─ travelNoteMapper.insert(note)
  │
  ├─ 3. 处理标签
  │     └─ travelNoteTagsMapper.batchInsert(noteId, tags)
  │
  ├─ 4. 删除用户游记列表缓存
  │
  ├─ 5. 发送分享通知 (可选)
  │
  └─ 6. 返回游记
```

---

### 3.4 user-service 用户服务

#### 3.4.1 用户注册链路

```
POST /api/users/register
  ↓
UserController.register()
  ↓
UserServiceImpl.register()
  │
  ├─ 1. 参数校验
  │     └─ 检查用户名、邮箱是否已存在
  │
  ├─ 2. 密码加密
  │     └─ BCryptPasswordEncoder.encode(password)
  │
  ├─ 3. 写入用户数据
  │     └─ userMapper.insert(user)
  │
  ├─ 4. 发送欢迎通知 (异步)
  │     └─ messageProducerService.sendNotification(userId, "欢迎注册")
  │
  └─ 5. 返回用户信息
```

#### 3.4.2 用户登录链路

```
POST /api/users/login
  ↓
UserController.login()
  ↓
UserServiceImpl.login()
  │
  ├─ 1. 查询用户
  │     └─ userMapper.selectByUsername(username)
  │
  ├─ 2. 验证密码
  │     └─ BCryptPasswordEncoder.matches(rawPassword, encodedPassword)
  │
  ├─ 3. 生成 JWT Token
  │     └─ Jwts.builder()
  │         .setSubject(userId)
  │         .claim("role", user.getRole())
  │         .signWith(secretKey)
  │         .compact()
  │
  ├─ 4. 记录登录日志 (可选)
  │
  └─ 5. 返回 Token
```

#### 3.4.3 验证码生成链路

```
GET /api/users/captcha
  ↓
UserController.getCaptcha()
  ↓
UserServiceImpl.generateCaptcha()
  │
  ├─ 1. 生成随机验证码
  │     └─ 4 位数字 + 字母
  │
  ├─ 2. 生成验证码图片
  │     └─ BufferedImage → Base64
  │
  ├─ 3. 存入 Redis
  │     └─ cacheUtil.set("captcha:{uuid}", code, 5min)
  │
  └─ 4. 返回图片 Base64 + UUID
```

---

### 3.5 file-service 文件服务

#### 3.5.1 文件上传链路

```
POST /api/file/upload
  ↓
FileController.upload()
  ↓
FileServiceImpl.upload()
  │
  ├─ 1. 文件校验
  │     ├─ 检查文件大小
  │     ├─ 检查文件类型
  │     └─ 检查文件内容 (防病毒)
  │
  ├─ 2. 生成文件 ID
  │     └─ UUID.randomUUID()
  │
  ├─ 3. 计算文件 Hash
  │     └─ MD5/SHA256 → 用于去重
  │
  ├─ 4. 存储文件
  │     ├─ 本地存储: /data/files/{year}/{month}/{fileId}.{ext}
  │     └─ 对象存储: OSS/S3 (生产环境)
  │
  ├─ 5. 写入文件元数据
  │     └─ fileMapper.insert(fileMeta)
  │
  ├─ 6. 返回文件信息
  │     └─ fileId, url, size, type
  │
  └─ 7. 异步生成缩略图 (图片)
        └─ messageProducerService.sendAsyncTask("thumbnail", fileId)
```

#### 3.5.2 文件下载链路

```
GET /api/file/download/{fileId}
  ↓
FileController.download()
  ↓
FileServiceImpl.download()
  │
  ├─ 1. 查询文件元数据
  │     └─ fileMapper.selectById(fileId)
  │
  ├─ 2. 权限检查
  │     └─ 检查用户是否有权限下载
  │
  ├─ 3. 读取文件
  │     └─ FileInputStream / OSS.getObject()
  │
  ├─ 4. 设置响应头
  │     └─ Content-Type, Content-Disposition
  │
  └─ 5. 流式输出
        └─ response.getOutputStream().write()
```

---

## 四、跨服务调用链路

### 4.1 Feign 调用流程

```
collection-service 需要路线信息
  ↓
RouteFeignClient.getById(routeId)
  ↓
OpenFeign 拦截器
  ├─ 从 Ribbon/LoadBalancer 获取 route-service 实例
  ├─ 构造 HTTP 请求: GET http://route-service/routes/{id}
  └─ 添加请求头 (透传 X-User-Id 等)
  ↓
route-service 接收请求
  ↓
RouteController.getById()
  ↓
返回 Result<Route>
  ↓
OpenFeign 解码器
  └─ Jackson 反序列化 → Route 对象
  ↓
collection-service 继续业务逻辑
```

### 4.2 典型跨服务调用场景

#### 场景 1: 收藏服务 → 路线服务

```
RouteCollectionServiceImpl
  ↓
RouteFeignClient.getById(routeId)
  ↓
route-service: GET /routes/{id}
  ↓
返回路线详情 (标题、封面、天数等)
  ↓
组装 RouteCollectionVO 返回前端
```

#### 场景 2: 路线服务 → 景点服务

```
RouteOptimizationServiceImpl
  ↓
AttractionFeignClient.getById(attractionId)
  ↓
attraction-service: GET /attractions/{id}
  ↓
返回景点详情 (坐标、名称等)
  ↓
计算距离矩阵，优化路线
```

#### 场景 3: 收藏服务 → 用户服务

```
RouteCollectionServiceImpl
  ↓
UserFeignClient.getById(userId)
  ↓
user-service: GET /users/{id}
  ↓
返回用户信息 (昵称、头像等)
  ↓
组装收藏记录 VO
```

### 4.3 Feign 降级处理

```java
@FeignClient(name = "route-service", fallback = RouteFeignFallback.class)
public interface RouteFeignClient {
    @GetMapping("/{id}")
    Result<Route> getById(@PathVariable Integer id);
}

@Component
public class RouteFeignFallback implements RouteFeignClient {
    @Override
    public Result<Route> getById(Integer id) {
        // 降级逻辑: 返回缓存数据或默认值
        return Result.error("服务暂时不可用");
    }
}
```

---

## 五、异步消息链路

### 5.1 RabbitMQ 消息模型

```
Producer (生产者)
  ↓
Exchange (交换机)
  ├─ notification.exchange (Topic)
  ├─ cache.update.exchange (Topic)
  └─ async.task.exchange (Topic)
  ↓
Binding (路由键)
  ├─ notification.send
  ├─ cache.update
  └─ async.task.execute
  ↓
Queue (队列)
  ├─ notification.queue (持久化)
  ├─ cache.update.queue (持久化)
  └─ async.task.queue (持久化)
  ↓
Consumer (消费者)
  ├─ NotificationConsumer
  ├─ CacheUpdateConsumer
  └─ AsyncTaskConsumer
```

### 5.2 通知消息链路

```
业务操作 (如: 路线被收藏)
  ↓
MessageProducerService.sendNotification()
  │
  ├─ 1. 构造消息对象
  │     └─ NotificationMessageVO(userId, type, title, content, extras, timestamp)
  │
  ├─ 2. 发送到 RabbitMQ
  │     └─ rabbitTemplate.convertAndSend(exchange, routingKey, message)
  │
  └─ 3. 消息持久化
        └─ Queue: durable=true, Message: deliveryMode=PERSISTENT

Consumer 接收消息
  ↓
NotificationConsumer.onMessage()
  │
  ├─ 1. 反序列化消息
  │     └─ Jackson2JsonMessageConverter → NotificationMessageVO
  │
  ├─ 2. 写入通知表
  │     └─ notificationMapper.insert(notification)
  │
  ├─ 3. 推送实时通知 (WebSocket/SSE)
  │     └─ 如果用户在线，实时推送
  │
  └─ 4. 发送短信/邮件 (可选)
        └─ thirdApiUtil.sendSmsNotification()
```

### 5.3 缓存更新消息链路

```
数据变更 (如: 景点信息更新)
  ↓
MessageProducerService.sendCacheUpdate()
  │
  ├─ 1. 构造消息
  │     └─ CacheUpdateMessageVO(cacheKey, operation, data, timestamp)
  │
  └─ 2. 发送到 RabbitMQ
        └─ exchange: cache.update.exchange, routingKey: cache.update

Consumer 接收消息
  ↓
CacheUpdateConsumer.onMessage()
  │
  ├─ 1. 解析操作类型
  │     ├─ DELETE → 删除缓存
  │     ├─ UPDATE → 更新缓存
  │     └─ INVALIDATE → 批量失效
  │
  └─ 2. 执行缓存操作
        └─ cacheUtil.delete(key) / cacheUtil.set(key, value)
```

### 5.4 异步任务消息链路

```
耗时操作 (如: 生成缩略图)
  ↓
MessageProducerService.sendAsyncTask()
  │
  └─ 发送到 async.task.queue

Consumer 接收消息
  ↓
AsyncTaskConsumer.onMessage()
  │
  ├─ 1. 解析任务类型
  │     └─ taskType: "thumbnail" / "export" / "import"
  │
  ├─ 2. 执行任务
  │     ├─ thumbnail → 生成缩略图
  │     ├─ export → 导出 Excel/PDF
  │     └─ import → 批量导入数据
  │
  ├─ 3. 更新任务状态
  │     └─ taskMapper.updateStatus(taskId, status)
  │
  └─ 4. 通知用户任务完成
        └─ messageProducerService.sendNotification(userId, "任务完成")
```

---

## 六、定时任务链路

### 6.1 Spring @Scheduled 任务

#### attraction-service 定时任务

```
1. BloomFilter 刷新 (每 30 分钟)
   └─ AttractionBloomFilterService.refreshBloomFilter()
       ├─ 查询所有景点 ID
       ├─ 创建新的 BloomFilter
       ├─ 填充数据
       └─ 原子替换

2. 实时数据同步 (每 15 分钟)
   └─ SyncRealtimeDataTask.syncRealtimeData()
       ├─ 查询需要同步的景点
       ├─ 调用第三方 API
       ├─ 批量更新数据库
       └─ 更新 Redis 缓存

3. 全量同步 (每天凌晨 3 点)
   └─ SyncRealtimeDataTask.fullSyncRealtimeData()
       ├─ 查询所有景点
       ├─ 调用第三方 API
       └─ 批量更新数据库
```

### 6.2 XXL-Job 分布式任务

#### route-service 定时任务

```
1. 景点热度刷新 (每 30 分钟)
   └─ TravelJobHandler.refreshAttractionHeat()
       ├─ 清除热度缓存
       ├─ 重新计算热度
       └─ 更新缓存

2. 热门路线缓存刷新 (每小时)
   └─ TravelJobHandler.refreshPopularRoutesCache()
       ├─ 清除 route:popular:*
       └─ 清除 route:recommendation:*

3. 路线数据同步 (每 15 分钟)
   └─ TravelJobHandler.syncRouteData()
       ├─ 清除 route:sync:*
       └─ 更新同步时间戳

4. 过期数据清理 (每天凌晨 2 点)
   └─ TravelJobHandler.cleanExpiredData()
       ├─ 清理过期分享
       └─ 清理过期 AI 缓存
```

### 6.3 XXL-Job 调度流程

```
XXL-Job 调度中心 (独立部署)
  ↓
定时触发任务
  ↓
HTTP 调用 → route-service /xxl-job/trigger
  ↓
TravelJobHandler 执行任务
  ↓
返回执行结果
  ↓
调度中心记录日志
```

---

## 七、缓存策略详解

### 7.1 缓存 Key 设计规范

```
格式: {业务}:{子业务}:{标识}

示例:
  attraction:all                          # 所有景点列表
  attraction:detail:123                   # 景点 123 的详情
  attraction:city:1                       # 城市 1 的景点
  attraction:null:456                     # 景点 456 的空值标记
  route:popular:1                         # 城市 1 的热门路线
  route_collection:user:789               # 用户 789 的收藏列表
  realtime_status:123                     # 景点 123 的实时状态
```

### 7.2 缓存 TTL 策略

| 数据类型 | TTL | 说明 |
|---------|-----|------|
| 景点列表 | 2h | 变化频率低 |
| 景点详情 | 1h | 中等频率 |
| 空值标记 | 5min | 防穿透，短过期 |
| 实时状态 | 15min | 与同步频率一致 |
| 搜索结果 | 30min | 用户搜索频繁 |
| 验证码 | 5min | 安全考虑 |
| Token | 24h | 登录有效期 |

### 7.3 缓存更新策略

```
1. 主动删除 (Cache Aside)
   └─ 写操作后删除相关缓存
       ├─ updateById() → delete("attraction:detail:{id}")
       └─ removeById() → delete("attraction:all")

2. 定期刷新
   └─ XXL-Job 定时清理过期缓存
       └─ deleteByPattern("route:popular:*")

3. 消息驱动
   └─ RabbitMQ 异步更新缓存
       └─ CacheUpdateConsumer → cacheUtil.delete(key)
```

### 7.4 缓存穿透防护

```
1. BloomFilter 前置过滤
   └─ 查询前检查 ID 是否存在
       ├─ false → 直接返回 null
       └─ true → 继续查询

2. 空值缓存
   └─ 查询结果为 null 时缓存空值
       └─ cacheUtil.set("attraction:null:{id}", true, 5min)

3. 参数校验
   └─ 过滤非法 ID
       └─ if (id <= 0) return null;
```

### 7.5 缓存雪崩防护

```
1. TTL 随机化
   └─ 基础 TTL ± 随机波动
       └─ set(key, value, 1h + random(-5min, +5min))

2. 多级缓存
   └─ 本地缓存 + Redis
       ├─ Caffeine (本地) → 热点数据
       └─ Redis (分布式) → 全量数据

3. 熔断降级
   └─ Sentinel 限流
       └─ 缓存失效时限制请求速率
```

---

## 八、数据库优化链路

### 8.1 索引设计

#### attraction 表索引

```sql
-- 单列索引
INDEX idx_city (city_id)              # 按城市查询
INDEX idx_rating (rating DESC)        # 按评分排序
INDEX idx_name (name)                 # 按名称搜索

-- 覆盖索引 (新增)
INDEX idx_city_rating_cover (city_id, rating DESC, name, id)
  # 场景: SELECT id, name, rating FROM attraction WHERE city_id = ? ORDER BY rating DESC
  # 优势: 无需回表，直接从索引返回数据
```

### 8.2 查询优化

#### 优化前 (偏移分页)

```sql
-- 第 1000 页
SELECT * FROM attraction
WHERE city_id = 1
ORDER BY rating DESC
LIMIT 9990, 10;

-- 问题: 需要扫描并跳过前 9990 行
```

#### 优化后 (游标分页)

```sql
-- 使用上一页最后一条的 (rating, id) 作为游标
SELECT * FROM attraction
WHERE city_id = 1
  AND (rating < 3.50 OR (rating = 3.50 AND id < 5))
ORDER BY rating DESC, id DESC
LIMIT 10;

-- 优势: 无论第几页，都只扫描 10 行
```

### 8.3 EXPLAIN 分析

```sql
EXPLAIN SELECT id, name, rating
FROM attraction
WHERE city_id = 1
ORDER BY rating DESC;

-- 期望结果:
-- type: ref
-- key: idx_city_rating_cover
-- Extra: Using index (覆盖索引生效)
```

---

## 九、第三方 API 集成

### 9.1 高德地图 API

```
场景: 路线规划、交通查询
  ↓
ThirdApiUtil.getTransportData(from, to)
  ↓
HTTP POST → https://restapi.amap.com/v3/direction/transit
  ↓
解析响应 → 路线、时间、费用
```

### 9.2 百度 AI API

```
场景: 图像分析、OCR 识别
  ↓
BaiduAIService.analyzeImage(imageUrl)
  ↓
HTTP POST → https://aip.baidubce.com/rest/2.0/image-classify/v1/advanced_general
  ↓
解析响应 → 图像标签、置信度
```

### 9.3 通义千问 API

```
场景: 智能对话、路线生成
  ↓
QwenService.chat(prompt)
  ↓
HTTP POST → https://dashscope.aliyuncs.com/api/v1/chat
  ↓
解析响应 → AI 生成的文本
```

### 9.4 短信服务

```
场景: 验证码、通知
  ↓
ThirdApiUtil.sendSmsNotification(phone, template)
  ↓
HTTP POST → 短信服务商 API
  ↓
返回发送结果
```

---

## 十、数据一致性保障

### 10.1 缓存一致性

```
问题: 数据库更新后，缓存未同步
  ↓
解决方案:
  1. 写操作后主动删除缓存
     └─ updateById() → delete("attraction:detail:{id}")
  
  2. 异步消息最终一致性
     └─ RabbitMQ → CacheUpdateConsumer → 更新缓存
  
  3. 定时任务兜底
     └─ XXL-Job → 定期清理过期缓存
```

### 10.2 分布式事务

```
场景: 跨服务数据一致性
  ↓
Seata AT 模式
  ├─ 1. TM 开启全局事务
  ├─ 2. RM 执行本地事务 + 记录 undo_log
  ├─ 3. TM 提交/回滚
  └─ 4. TC 协调各分支事务
```

### 10.3 分布式锁

```
场景: 防止并发冲突
  ↓
Redisson 分布式锁
  ├─ 可重入锁: RLock
  ├─ 公平锁: RFairLock
  └─ 联锁: RLock[]
```

### 10.4 幂等性设计

```
场景: 防止重复提交
  ↓
实现方式:
  1. 唯一索引 (数据库层)
     └─ UNIQUE KEY uk_user_route (user_id, route_id)
  
  2. Token 机制 (应用层)
     └─ 生成 token → 提交时校验 → 删除 token
  
  3. 状态机 (业务层)
     └─ 检查状态 → 执行操作 → 更新状态
```

---

## 附录

### A. 关键代码位置索引

| 功能模块 | 文件路径 | 关键方法 |
|---------|---------|---------|
| BloomFilter | `AttractionBloomFilterService.java` | `mightContain()`, `refreshBloomFilter()` |
| 分布式锁 | `DistributedLockService.java` | `executeWithLock()` |
| 缓存工具 | `CacheUtil.java` | `get()`, `set()`, `delete()` |
| 消息生产 | `MessageProducerService.java` | `sendNotification()`, `sendCacheUpdate()` |
| 游标分页 | `AttractionServiceImpl.java` | `getByCursor()`, `comparePagination()` |
| JWT 校验 | `AuthGlobalFilter.java` | `filter()` |
| Feign 客户端 | `RouteFeignClient.java` | `getById()` |

### B. 配置文件位置

| 配置类型 | 文件路径 |
|---------|---------|
| 网关路由 | `gateway/src/main/resources/application.yml` |
| Redis 配置 | `common/src/main/resources/application.properties` |
| RabbitMQ 配置 | `common/src/main/resources/application.properties` |
| MyBatis 配置 | `common/src/main/resources/application.properties` |

### C. 数据库表清单

| 表名 | 所属模块 | 说明 |
|-----|---------|------|
| attraction | attraction-service | 景点表 |
| city | attraction-service | 城市表 |
| restaurant | attraction-service | 餐厅表 |
| attraction_realtime_status | attraction-service | 景点实时状态 |
| route | route-service | 路线表 |
| route_attractions | route-service | 路线-景点关联 |
| route_transport | route-service | 路线-交通关联 |
| transport | route-service | 交通工具表 |
| user | user-service | 用户表 |
| user_collection | collection-service | 收藏表 |
| route_comment | collection-service | 评论表 |
| travel_note | collection-service | 游记表 |
| route_share | collection-service | 分享表 |
| notification | collection-service | 通知表 |

---

**文档版本**: v1.0  
**最后更新**: 2026-08-06  
**维护者**: 智旅项目团队
