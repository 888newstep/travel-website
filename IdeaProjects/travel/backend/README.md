# 智慧旅游系统 - 后端 (Smart Travel Backend)

基于 **Spring Boot 3.3.5 + Spring Cloud Alibaba 2023.0.3** 的微服务后端，6 大模块按业务域拆分，覆盖用户、景点、路线、社区、文件与网关全链路，内建 **AI 智能体矩阵** 与 **遗传算法路径优化**。

## 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Spring Boot 3.3.5, Spring Cloud 2023.0.3, Spring Cloud Alibaba 2023.0.3.2 |
| 语言 | Java 17 |
| 服务治理 | Nacos（注册 + 配置中心）, OpenFeign, LoadBalancer |
| 网关 | Spring Cloud Gateway, Sentinel |
| 持久层 | MySQL 8.0, MyBatis-Plus 3.5.8, Druid 1.2.23 |
| 缓存 | Redis, Redisson 3.37 |
| 消息 | RabbitMQ（可靠投递 + 定时重投 + Redis 幂等） |
| AI | DashScope（通义千问）2.14, 百度 AI SDK, OkHttp |
| 算法 | JTS 1.19, 自研遗传算法 TSP |
| 任务调度 | XXL-Job 2.4.1 |
| 鉴权 | Spring Security + JWT (jjwt 0.12.5) |
| API 文档 | Knife4j 4.5 / OpenAPI 3.0 |

## 模块结构

```
backend/
├── common/              # 公共层（被其余 module 依赖）
│   └── src/main/java/travel/common/
│       ├── config/      # MyBatis-Plus / Redis / Redisson / RabbitMQ / Sentinel / Security / Swagger
│       ├── entity/      # 20+ 张表的实体，按业务域分包（route_planning / travel_recommendation / user_community …）
│       ├── mapper/      # MyBatis-Plus Mapper
│       ├── service/     # DistributedLockService / 可靠消息投递 / Redis 幂等
│       ├── utils/       # Result / JwtHelper / RateLimiter / AMapRouteService / CacheUtil …
│       ├── exception/   # 全局异常处理 + 业务异常体系
│       ├── vo/ dto/     # 视图对象 / 请求 DTO
│       └── repository/  # 数据访问封装
├── gateway/             # 网关：路由 / JWT 鉴权 / CORS / Sentinel 限流
├── user-service/        # 用户注册登录、JWT 签发
├── attraction-service/  # 景点 / 城市 / 美食 / 实时状态
├── route-service/       # 核心域：路线、寻路算法、AI 智能体
│   └── src/main/java/travel/route/
│       ├── algorithm/        # GeneticAlgorithmTSP 遗传算法优化器
│       ├── controller/       # Route / RouteOptimization / AI 系列控制器
│       ├── dto/ai/           # 40+ AI 请求/响应模型
│       ├── service/          # 智能路线 / 优化 / 实时调整 / 个性化推荐 / 推荐算法
│       ├── feign/            # 跨服务调用客户端
│       └── job/              # XXL-Job 定时任务
├── collection-service/  # 游记 / 评论 / 收藏 / 分享 / 通知 / 反馈 / 统计
└── file-service/        # 文件上传 / 标签管理
```

## 快速开始

### 前置条件

- JDK 17+、Maven 3.8+
- MySQL 8.0（建库：`attraction-service/src/main/resources/db/init_complete.sql`，20 张表）
- Redis
- Nacos（`nacos/nacos` 内置发行版，或使用 `backend/nacos/nacos/bin/startup.cmd -m standalone`）

### 启动服务

```bash
cd backend
mvn clean package -DskipTests

# 1. 先启动 Nacos（standalone 模式）
../backend/nacos/nacos/bin/startup.cmd -m standalone

# 2. 依次启动各微服务（每个终端一个）
java -jar gateway/target/gateway-1.0-SNAPSHOT.jar           # 8090
java -jar user-service/target/user-service-1.0-SNAPSHOT.jar # 8091
java -jar attraction-service/target/attraction-service-1.0-SNAPSHOT.jar # 8092
java -jar route-service/target/route-service-1.0-SNAPSHOT.jar       # 8093
java -jar collection-service/target/collection-service-1.0-SNAPSHOT.jar # 8094
java -jar file-service/target/file-service-1.0-SNAPSHOT.jar         # 8095
```

> 更省事的做法：在项目根目录直接运行 `.\start-all.bat`（Windows）或使用 Docker Compose（`docker compose -f deploy/docker-compose.yml up --build -d`）。

### 配置方式

配置通过 **Nacos Config** 共享 + 环境变量覆盖：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 127.0.0.1 / 3306 / travel_website | MySQL 连接 |
| `DB_USERNAME` / `DB_PASSWORD` | - | 数据库账号（必须配置） |
| `REDIS_HOST` / `REDIS_PORT` | localhost / 6379 | Redis 连接 |
| `REDIS_PASSWORD` | - | Redis 密码（默认无） |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | <CLOUD_HOST_PLACEHOLDER> / 5672 | RabbitMQ 连接 |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | - | RabbitMQ 账号 |
| `JWT_SECRET` | - | JWT 签名密钥（生产必配强密钥） |
| `NACOS_SERVER_ADDR` | localhost:8848 | Nacos 地址 |
| `DRUID_USERNAME` / `DRUID_PASSWORD` | - | Druid 监控面板账号 |
| `AMAP_API_KEY` / `BAIDU_APP_ID` / `BAIDU_API_KEY` / `BAIDU_SECRET_KEY` / `QWEN_API_KEY` | - | 高德 / 百度 / 通义千问密钥 |

Docker Compose 场景下，同样以 `deploy/.env` 注入以上变量（参考根目录 [.env.example](../deploy/.env.example)）。

## 主要 API

网关统一入口：`http://localhost:8090/api/**`。所有响应使用统一结构 `Result<T>`：

```json
{ "code": 200, "message": "操作成功", "data": { }, "timestamp": 1704067200000, "success": true }
```

### 路由核心接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/routes/{id}` | 路线详情 |
| GET | `/api/routes/smart/list?cityId=&optimizationType=&limit=` | 智能路线列表（可指定优化类型） |
| GET | `/api/routes/search?title=` | 路线搜索 |
| POST | `/api/routes/{id}/copy` | 复制路线 |
| PUT | `/api/routes/{id}/visibility` | 设置可见性 |
| GET | `/api/route-optimization/suggestions/{routeId}` | 获取优化建议 |
| POST | `/api/route-optimization/apply` | 应用优化建议 |
| GET | `/api/route-optimization/history/{routeId}` | 优化历史 |

### AI 智能体接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | 智能对话（通义千问） |
| POST | `/api/ai/qa` | 旅行问答 |
| POST | `/api/ai/recommend` | AI 智能推荐 |
| POST | `/api/ai/itinerary/generate` | AI 生成行程 |
| POST | `/api/ai/smart-itinerary/generate` | 智能行程生成（高级） |
| POST | `/api/ai/smart-itinerary/optimize` | 智能行程优化 |
| POST | `/api/ai/multimodal/query` | 多模态查询（文本 + 图像） |
| POST | `/api/ai/multimodal/recommend` | 多模态推荐 |
| POST | `/api/ai/image-analysis` | 图像分析 |
| GET | `/api/ai/image-analysis/types` | 图像分析类型列表 |
| POST | `/api/ai/advanced/plan` | 智能路线规划（偏好 + 约束） |
| POST | `/api/ai/advanced/guide` | 生成旅行攻略 |
| GET | `/api/ai/advanced/safety/{cityId}` | 旅行安全建议 |
| POST | `/api/ai/advanced/budget` | 预算估算 |
| GET | `/api/ai/advanced/recommendations` | 个性化推荐 |
| POST | `/api/ai/advanced/chat` | 高级聊天（带上下文） |

### 社区与文件

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/users/login` | 登录（签发 JWT） |
| GET | `/api/attractions/**` | 景点 / 城市 / 美食查询 |
| GET | `/api/realtime-status/**` | 实时状态 |
| POST | `/api/travel-notes/**` | 游记 |
| POST | `/api/route-comments/**` | 评论 |
| POST | `/api/v1/route-collections/**` | 收藏 |
| POST | `/api/route-share/**` | 分享 |
| POST | `/api/file/upload` | 文件上传 |
| GET | `/api/actuator/health` | 健康检查 |

> 完整接口与字段说明详见 Knife4j（启动网关后访问 `http://localhost:8090/api/swagger-ui.html` 或各服务的 Swagger 页面）。

## 可靠消息投递（RabbitMQ）

为避免消息丢失与重复消费，common 模块提供一套生产级保障原语：

- `MessageProducerService` — 统一生产入口，支持确认与退回回调
- `MqMessageStatusRecord` / `MqMessageStatusService` — 消息状态落库
- `ReliableMessageRepublisher` / `RabbitReliableMessageRepublisher` — 定时扫描未确认消息并重投
- `RedisMessageIdempotencyService` — 基于 Redis 的消费幂等，防止重复处理

通过 `mq.status-persistence.enabled` 开关控制状态持久化（默认关闭）。

## 测试

```bash
# 全量测试
mvn test

# 单模块
cd route-service && mvn test

# 单个测试类
mvn test -Dtest=RouteControllerTest
```

已覆盖：Gateway 鉴权链（`GatewayAuthTest`）、全局异常处理、Route 推荐主流程、RabbitMQ 配置与可靠消息（`MessageProducerServiceTest`、`MqMessageStatusServiceTest`）等。

## 数据库

初始化脚本：`attraction-service/src/main/resources/db/init_complete.sql`

20 张表（utf8mb4 / InnoDB）：`user`、`city`、`attraction`、`route`、`route_attractions`、`transport`、`route_transport`、`restaurant`、`travel_note`、`travel_note_tags`、`user_collection`、`route_comment`、`route_share`、`notification`、`feedback`、`resource_file`、`file_tag`、`file_comment`、`attraction_realtime_status`、`ui_dictionary`。

## License

[Apache License 2.0](../LICENSE)