# 智慧旅游项目业务与工程治理计划

## 1. 项目定位与约束

- 项目用于技术展示、秋招答辩，以及企业和学校场景的知识交流。
- 运行拓扑为 Win11 本机 MySQL、Redis、JMeter，云服务器 RabbitMQ。
- 项目不使用向量数据库，不纳入架构、测试或交付范围。
- 不建设短信计费、余额或账单能力；通知以站内通知和 RabbitMQ 为主，短信通道默认关闭。
- 交通路线与路况数据通过项目配置的 `AMAP_API_KEY` 调用高德开放平台。
- 不允许用随机数、固定景点、固定评分或固定节省时间伪装真实业务数据。

## 2. 关键假设

1. MySQL 中的 `route_attractions` 保存路线内景点的天数和访问顺序。
2. 景点表中的经纬度为高德路线规划的输入坐标。
3. 当前只有景点人流最新快照，没有可靠的历史人流和按日访问明细。
4. RabbitMQ 负责异步通知，不参与景点查询和高德交通请求的同步主链路。
5. 远程图片 URL 分析不是核心能力，默认关闭，仅允许显式域名白名单。

假设 1、2 已由 Mapper、实体、数据库迁移结果和交通单元测试验证；假设 3 已由现有表结构和服务实现验证；假设 4 与当前模块依赖一致；假设 5 已落实为默认拒绝策略。

## 3. 方案取舍

### 3.1 幂等性

- **采用：HTTP 幂等键 + Redis 状态机 + 数据库唯一键。** 优点是覆盖入口重试、并发请求和缓存故障；缺点是需要管理 TTL 和处理中状态恢复。
- **不采用：仅使用 Redis `SETNX`。** 实现简单，但 Redis 丢键或故障时无法提供最终一致性兜底。
- **不采用：所有接口强制幂等。** 查询接口和天然幂等更新没有收益，只会增加缓存与排障成本。

### 3.2 实时交通

- **采用：按路线相邻景点逐段调用高德驾车 API。** 不依赖城市 adcode，能获得每段距离、时长和 `tmcs` 路况。
- **不采用：城市级道路状态代替路线状态。** 城市范围过大，无法证明与用户路线相关。
- **降级原则：明确 `dataAvailable=false`。** 不用直线距离或固定拥堵等级冒充高德实时结果。

### 3.3 不完整业务能力

- **采用：返回不可用状态或业务异常。** 对展示项目更诚实，也便于解释数据边界。
- **不采用：随机趋势、固定设施、固定开放时间和固定优化收益。** 这些结果不可验证，会直接削弱项目可信度。

### 3.4 异常处理

- **采用：Controller 不捕获通用异常，由 `GlobalExceptionHandler` 统一映射 HTTP 状态、错误码和 `traceId`。**
- **不采用：`Result.error(e.getMessage())`。** 它会返回错误的 HTTP 200，并可能泄露 SQL、路径、密钥或第三方响应。

## 4. 分阶段执行计划

### P0 数据安全与越权防护

- [x] JWT 认证、角色校验和对象所有权校验。
- [x] 未知异常统一返回 HTTP 500，不向客户端回传内部异常原文。
- [x] AI 远程图片增加域名白名单、私网地址拦截、禁止重定向、连接超时、内容类型检查和 10MB 上限。
- [x] 清理 collection-service 的 54 处控制器异常原文回传，统一交由全局异常处理器生成 HTTP 状态和 `traceId`。
- [x] 清理 file-service 的 20 处控制器异常原文回传，避免文件路径和存储异常泄漏。
- [x] 删除 route-service 无人调用的 AI、景点、文件、反馈、收藏、分享和旅行笔记跨域占位方法。
- [x] 用户密码、验证码和刷新 Token 从请求对象 `toString()` 中排除，避免 Spring DEBUG 日志记录凭据明文。

### P1 幂等性与并发一致性

- [x] 写接口支持 HTTP 幂等键与响应复用。
- [x] 注册和重置密码使用 Redis Lua 原子校验并消费一次性验证码，阻断并发重放。
- [x] RabbitMQ 消费使用 Redis 状态机与数据库唯一键双层幂等。
- [x] 路线优化使用分布式锁、数据库行锁、锁内事务、两阶段换位写入和同日顺序唯一约束；无变化请求不重复写历史。
- [x] 景点浏览量使用 SQL 原子自增。
- [x] 路线收藏在统一分布式锁内判断并切换，数据库唯一键兜底，新增/删除后同步失效状态与列表缓存。
- [x] 前端已登录写请求自动携带 `Idempotency-Key`，同一请求配置重试时复用原键。
- [x] 使用 JMeter 验证同一幂等键 100 并发只产生一次业务写入。
- [x] 执行 Redis 短暂不可用测试，确认入口 fail-closed 返回 HTTP 503 且不写库；数据库唯一键用独立重复写入测试验证。

### P2 真实数据与业务语义

- [x] 景点人流、热力图和历史统计不再生成随机数据。
- [x] 景点评分使用数据库聚合，餐厅跨城市查询条件已修复。
- [x] 景点详情不再虚构开放时间、设施、无障碍能力、季节活动和拍照点。
- [x] 路线访问趋势不再随机生成；没有明细表时明确返回不可用。
- [x] 路线统计分页改为真实数据库分页和排序字段白名单。
- [x] 未接入优化器的实时调整动作明确失败，不再返回固定节省时间和距离。
- [x] 个性化推荐缓存未命中时不再生成固定名称、评分和匹配分，暂无数据源时返回空列表。
- [x] 景点缺少有效坐标时不再错误使用北京天气，避免跨城市实时数据污染。
- [x] 路线质量评分统一归一化到 0–1，空评分安全跳过，无效路线天数明确拒绝。
- [x] 路线比较不再虚构固定游览时长和附加成本；未知耗时、缺失坐标明确返回空值。
- [x] 高级 AI 不再生成固定北京攻略、预算、安全评分和“景点1/餐厅1”；无可信数据源时返回业务不可用。
- [x] 删除未接线的智能行程、路线预测、备选路线、调整历史和重复优化接口，避免孤儿代码被误当成真实能力。
- [x] 短信关闭时不再伪装“发送成功”；本地验证码仅通过显式 `CAPTCHA_DEMO_MODE` 开启并返回，公开和生产环境默认关闭。
- [x] 前端统一保留后端业务错误消息，高德失败和 AI 数据不可用可直接展示真实原因。
- [x] 删除无后端实现的 AI 语音接口、前端入口和字典种子；前端使用固定能力清单，不读取数据库旧页签。
- [x] 通知接口未提供分页总量时，前端统计统一标注为“当前页”，不再冒充全量数据。
- [x] 游记列表响应统一展开 `travelNote` 包装并映射字段；详情只由后端原子增加一次浏览量。
- [x] 未建设游记评论明细表前移除评论区和评论接口，初始化评论数归零，并提供现有数据清理 SQL。
- [x] 单快照表不再执行“历史平均”聚合；历史和近 7 天接口统一返回 HTTP 503 与错误码 `20007`。
- [x] 前端删除以景点人流等级冒充道路交通的映射；交通指标继续只使用高德路线接口。
- [x] 删除无请求体实时批量刷新和无后端字典请求，避免固定 400/404 噪声。
- [x] 删除 `common` 中会随微服务发布的旧 Vue 构建包，避免暴露调用失效接口的第二套前端。
- [ ] 为路线访问行为新增明细表后，再实现按日趋势和留存分析。
- [ ] 将稳定业务响应中的裸 `Map<String, Object>` 继续迁移为 DTO。
- [x] 删除 `AIImageAnalysisServiceImpl` 的固定图片分析、固定相似景点结果及上传入口，只保留真实百度 URL 图像识别。
- [x] 删除 `AIMultimodalServiceImpl`、对应 Controller/DTO 和前端页签；图片未传入模型前不再暴露多模态能力。
- [x] 通义千问行程提示词和缓存键均包含目的地、偏好、天数和预算；缺 Key/供应商失败统一返回 HTTP 503 / `5006`。
- [x] AI 助手删除固定置信度、优化分数、最佳时间、游览时长和成功降级；供应商失败明确返回依赖错误。
- [x] `/route-optimization/apply` 仅开放 `distance`/`shortest`，时间、费用和综合目标待真实实现后再恢复。

### P3 高德交通能力

- [x] 驾车路线请求使用 `extensions=all` 获取完整路况。
- [x] 按 `route_attractions` 顺序构建相邻景点交通分段。
- [x] 聚合总距离、总时长，并将高德路况映射为 `light/moderate/heavy/severe`。
- [x] API Key 缺失、坐标非法、路线不足两个有效景点时明确返回不可用。
- [x] 路线规划仅用本地坐标做顺序搜索，最终交通指标只调用一次高德；高德失败不再用固定速度、油耗或直线距离伪降级。
- [x] 六份初始化 SQL 删除固定 `route_transport` 数据，修复重复及跨城市路线关系；增量迁移后 `route_attractions=23`、跨城市关系为 0、`route_transport=0`。
- [x] 本地 HTTP 桩覆盖两点、多点、配额失败、超时、无路径、响应体上限、非法坐标和 API Key 日志脱敏。
- [x] 提供 Windows PowerShell 5.1 一键实测脚本，自动从 MySQL 选择两点和多点路线，并输出脱敏日志与 JSON 摘要。
- [ ] 使用真实 `AMAP_API_KEY` 完成一条两景点路线和一条多景点路线的集成验收。
- [x] 配额失败、超时和无路径降级证据已由 `AMapRouteServiceTest` 的 Surefire 报告记录；真实外呼证据待配置 Key 后补充。

### P4 RabbitMQ 可靠通知

- [x] publisher confirm、returns、手动 ACK、分级重试和 DLQ。
- [x] 消息 ID、消费幂等和可选状态补偿。
- [x] 可靠生产者关闭时不再向无消费者旧队列发布；通知失败与登录主链路隔离。
- [x] 两份 Compose 移除本地 RabbitMQ，统一通过环境变量连接云端 broker，并补齐 JWT、高德和演示模式配置入口。
- [x] 补齐 `notification.source_message_id` 幂等迁移并在本机执行，列和唯一索引均已验证存在。
- [x] 新增显式云端 Live IT 与 PowerShell 5.1 验收脚本，覆盖生产代码发布、broker confirm、消费落库、重复投递、一次真实重试和 DLQ。
- [ ] 在云端 broker 发送真实业务消息，验证 confirm、消费落库、重复投递和 DLQ。
- [ ] 演练消费者重启、RabbitMQ 断连和死信人工回放。

### P5 MySQL、Redis 与 JMeter

- [x] MySQL/Redis 地址支持环境变量覆盖，默认连接 Win11 本机服务。
- [x] 景点城市评分排序索引和 Redis 时间类型序列化已验证。
- [x] 建立路线收藏同一幂等键 100 并发场景，校验处理中 409、完成后响应重放和 MySQL 最终记录数。
- [x] 建立景点点评同一用户 100 并发 UPSERT 场景，每个线程使用不同幂等键，直接验证 MySQL 唯一键和原子更新；响应点评 ID 只有一个，最终业务记录数为 1。
- [x] 建立路线评论 100 个不同用户并发点赞场景，验证行为表唯一键、分布式锁和 SQL 原子计数；最终点赞数、行为记录数和不同用户数均为 100。
- [x] 建立路线优化 100 个不同幂等键并发场景，验证所有请求真实进入锁与事务，最终顺序唯一且优化历史仅一条。
- [ ] 继续建立消息消费并发场景。
- [ ] 每个场景记录吞吐、平均响应、P95、P99、错误率、数据库写入数和 Redis 命中率。
- [x] 收藏场景已区分原始 200、重放 200、处理中 409、限流 429、其他 4xx/5xx 与连接异常。

### P6 秋招展示材料

- [x] 绘制认证、幂等、路线优化和 RabbitMQ 最终一致性时序图。
- [x] 准备“真实能力、降级能力、未实现能力”边界表，避免答辩中过度承诺。
- [x] 准备 5 分钟演示脚本：登录、景点查询、路线优化、高德交通、重复请求幂等、消息重试。
- [x] 为核心改动建立测试报告、JMeter 报告和关键日志索引。
- [ ] 答辩前按脱敏规范截取关键日志、数据库结果和 JMeter 报告页面。

P6 交付物：

- `docs/showcase/ARCHITECTURE_SEQUENCE_DIAGRAMS.md`
- `docs/showcase/CAPABILITY_BOUNDARIES.md`
- `docs/showcase/DEMO_SCRIPT_5_MINUTES.md`
- `docs/showcase/EVIDENCE_INDEX.md`

## 5. 验收命令

```powershell
# 后端全量编译
.\mvnw.cmd -q -f backend\pom.xml -DskipTests compile

# 高德、实时调整、AI 安全和景点详情定向回归
.\mvnw.cmd -q -f backend\pom.xml -pl route-service,attraction-service -am `
  "-Dtest=RouteRealTimeAdjustmentServiceImplTest,ThirdApiUtilTest,AIImageControllerTest,AIAdvancedControllerTest,AIAssistantControllerTest,AttractionDetailServiceImplTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test

# 修复本地路线关系并清除固定交通种子
$env:MYSQL_PWD = $env:DB_PASSWORD
mysql --host=$env:DB_HOST --port=$env:DB_PORT --user=$env:DB_USERNAME `
  --database=$env:DB_NAME --default-character-set=utf8mb4 `
  --execute="source backend/docs/infrastructure/AMAP_ROUTE_DATA_MIGRATION.sql"

# 配置真实 Key 后执行高德两点、多点外呼验收
$env:AMAP_API_KEY = '<真实高德 Web 服务 Key>'
.\ops\amap\run-amap-route-live-test.ps1

# 景点点评原子 UPSERT 100 并发验收
.\ops\jmeter\run-attraction-review-upsert-test.ps1 -Threads 100

# 路线评论 100 个不同用户并发点赞验收
.\ops\jmeter\run-route-comment-like-test.ps1 -Threads 100

# 路线优化 100 个不同幂等键并发验收
.\ops\jmeter\run-route-optimization-test.ps1 -Threads 100

# 前端静态检查
npm --prefix frontend run lint

# 代码与边界检查
git diff --check -- backend frontend
rg -n -g '*.java' "Result\.error\([^\n]*getMessage\(\)" backend | rg -v "GlobalExceptionHandler"
```

本轮本地验证结果：前端 lint/build 通过，后端全量 compile 通过；2026-08-20 执行后端全量测试，当前 Surefire 报告共 245 项测试，0 failure、0 error、3 skipped。路线数据迁移执行后 `route_attractions=23`、跨城市关系为 0、`route_transport=0`，可稳定选出上海两点路线和北京三点路线；高德本地桩 8 项及路线联动测试全部通过。一键实测脚本已通过 Windows PowerShell 5.1 语法、纯 ASCII 和缺少 Key 时安全失败验证，真实高德外呼仍等待本机设置 `AMAP_API_KEY`。2026-08-20 使用 JMeter 5.6.3 完成路线收藏真实 100 并发验收：原始 HTTP 200 为 1，处理中 HTTP 409 为 99，失败样本和非预期 HTTP 响应均为 0；平均响应 3342.57ms，P95 4686ms，P99 4707ms，吞吐 20.08 次/秒；完成后重放响应和最终收藏状态通过，MySQL 业务记录数为 1，唯一索引验收通过。同日使用独立 Redis 16379、user-service 18091 和 collection-service 18094 完成故障演练：Redis 中断后写请求返回 HTTP 503，业务记录保持 1 条；Redis 重启后查询恢复为 HTTP 200；重复插入被 MySQL 1062 拒绝，记录数仍为 1。故障演练证据位于 `run-logs/redis-outage/20260820-113223-525/`，临时端口和进程均已清理。用户凭据日志脱敏回归测试同步通过，修复前生成的本地日志已完成脱敏。

2026-08-20 使用隔离 Redis 16380 和 attraction-service 18092 完成景点点评真实 100 并发 UPSERT 验收：100 个业务样本全部返回 HTTP 200，失败率 0%，平均响应 1316.13ms，P95 1581ms，P99 1598ms，吞吐 62.31 次/秒；JMeter 汇总断言确认响应中的点评 ID 只有一个，最终 MySQL 业务记录数为 1。脚本结束后临时用户、点评数据、Redis、Java 进程及端口全部清理，证据位于 `run-logs/jmeter-review/20260820-140022-953/`。`JwtAuthenticationFilterTest` 与 `AttractionDetailServiceImplTest` 定向回归同步通过。

同日完成路线评论业务修复与真实并发验收：评论列表和统计查询补齐 `route_id`、发布状态及顶级评论条件；删除改为软删除，避免回复因外键 `ON DELETE SET NULL` 被错误提升；评论点赞从 Redis 临时键改为 `user_collection` 持久化行为记录，使用 `uk_user_item_action` 唯一键、按评论和用户加分布式锁，并通过 SQL 原子增减 `likes_count`。同时移除实体和数据库列的默认五星，避免无评分建议被伪装成 5 分。使用隔离 Redis 16381 和 collection-service 18094 执行 100 个不同用户并发点赞，100 个样本全部 HTTP 200，失败率 0%，平均响应 1522.64ms，P95 1946ms，P99 1985ms，吞吐 49.95 次/秒；最终 `likes_count=100`、行为记录数 100、不同用户数 100。live probe 返回路线 1 的 28 条评论且跨路线记录为 0，统计总数 28，平均评分与 MySQL 的 4.763158 一致。临时用户、评论、行为记录、JWT CSV、Redis、Java 进程及端口全部清理，证据位于 `run-logs/jmeter-comment-like/20260820-145625-099/`。

同日完成路线优化并发一致性修复与真实验收：事务内使用 `SELECT ... FOR UPDATE` 锁定完整路线日程，先将旧 `visit_order` 临时改为负主键，再批量写回最终正序，避免交换位置时与 `uk_route_day_visit_order(route_id, day_number, visit_order)` 发生临时唯一键冲突；优化历史改为事务提交后写 Redis，缓存故障不再回滚已提交日程。使用隔离 Redis 16382 和 route-service 18093，以同一用户、100 个不同 `Idempotency-Key` 并发提交同一显式顺序，100 个样本全部 HTTP 200，失败率 0%，平均响应 6508.34ms，P95 7438ms，P99 7532ms，吞吐 13.24 次/秒；服务日志中 `changed=true` 仅 1 次、`changed=false` 99 次，最终 3 个位置均唯一且连续，优化历史仅 1 条。临时用户、路线、景点、Redis、Java 进程及端口全部清理，证据位于 `run-logs/jmeter-route-optimization/20260820-153122-431/`。

同日完成秋招展示材料收敛：重写根目录中英文 README 和后端 README，纠正 Compose 不包含 Nacos/本地 RabbitMQ、AI 条件能力、路线优化算法语义和消息状态表尚无自动补偿任务等过度承诺；新增四条核心时序图、能力边界矩阵、五分钟演示脚本和证据索引。审计同时发现上传图片分析、多模态推荐/搜索、通义千问行程目的地传递和 `optimizationType` 多目标语义仍需后续代码治理，已回填为 P2 待办，未计入完成能力。
