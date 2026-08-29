# travel 项目安全审查与本地探测说明

审查日期：2026-08-29

## 1. 测试边界

本次仅针对用户授权的本地 `travel` 工作区进行安全审查。

- 默认动态目标：`http://127.0.0.1:8090`
- 不扫描公网，不绕过真实账号，不删除或修改业务数据
- 默认不注册账号、不上传文件、不调用真实生成式 AI、不执行高并发压力测试
- 动态探测脚本：`ops/security/security_probe.py`

## 2. 使用方法

先启动本地测试环境，再执行：

```bash
python ops/security/security_probe.py
```

同时检查 8091—8095 是否可以绕过 Gateway 直连：

```bash
python ops/security/security_probe.py --check-direct-services
```

执行最多 12 个只读 GET 请求，轻量检查是否存在限流响应：

```bash
python ops/security/security_probe.py --check-rate-limit
```

报告默认输出到：

```text
ops/security/security-report.json
```

脚本默认拒绝非本机目标。只有在明确授权的 RFC1918 私网测试环境中，才可使用：

```bash
python ops/security/security_probe.py \
  --base-url http://192.168.1.10:8090 \
  --allow-private-target
```

## 3. 已确认的正向安全设计

1. Gateway 在转发前删除客户端提供的 `X-User-Id`、`X-User-Type`、`X-User-Role`、`X-Client-IP`，再根据 JWT 重建可信身份头。
2. Gateway 启动时要求 JWT 密钥非空且至少 32 字节。
3. 业务服务仍执行 JWT 解析和 Spring Security 授权，不完全依赖 Gateway 转发头。
4. 文件保存使用 UUID 存储名，并对解析后的路径执行 `normalize + startsWith(rootDir)` 检查，具备路径穿越防护。
5. 文件接口按 `uploadUserId` 查询，下载、修改、删除和版本操作均有所有权校验。
6. HTTP 幂等采用 Redis Lua 原子状态机；关键写入另有 MySQL 唯一约束兜底。
7. Actuator 性能端点默认关闭；开启后配置为仅允许本机回环访问。
8. `.env` 实际文件未纳入 Git，仓库只跟踪 `.env.example`。

## 4. 本轮已落地优化（2026-08-29）

### 已完成：AI 统一鉴权

- Gateway 已移除对 `/api/ai/**` 的整体公开放行；
- 业务服务将 `/ai/**` 统一调整为 `authenticated()`；
- Gateway 增加 AI 匿名访问回归测试，覆盖聊天、问答、行程生成、路线优化和图片分析；
- 原有普通公开路线、景点、城市和游记读取规则不受影响。

当前效果：未携带有效 Bearer Token 的 AI 请求在 Gateway 返回 401，业务服务仍保留第二层认证。

### 已完成：内部端口隔离

默认 `deploy/docker-compose.yml` 仅发布：

- Web：8080
- Gateway：8090

MySQL 3306、Redis 6379、业务服务 8091—8095 已改为仅在 Docker 内部网络通过 `expose` 访问。

新增 `deploy/docker-compose.local-ports.yml`：只有本地开发者显式加载时，才将内部端口绑定到 `127.0.0.1`，不会监听全部宿主网卡。

### 验证结果

- common 安全相关测试：13 个通过；
- Gateway 鉴权测试：10 个通过；
- 合计：23 个测试，0 failure，0 error；
- 两份 Compose YAML 解析通过；
- 默认 Compose 静态检查确认仅存在 8080/8090 两组 `ports`；
- 当前机器没有 Docker CLI，因此尚未执行真实容器启动验证。

## 5. 剩余风险与修复优先级

### 已缓解：公开 AI 接口可能被匿名滥用

**静态证据**：

- Gateway 的 `isPublicRequest` 对 `/api/ai` 和 `/api/ai/**` 直接返回公开。
- 业务安全配置中存在 `authorize.requestMatchers("/ai/**").permitAll()`。
- AI 接口包含聊天、问答、行程生成、图片分析、预算规划等可能触发外部模型调用的接口。

**影响**：

- API Key 成本消耗和配额耗尽；
- 大量慢请求占满线程池或 HTTP 连接池；
- 匿名用户可反复提交超长 Prompt；
- 某些本应鉴权的具体 AI 路径可能受宽泛公开规则影响，形成规则维护风险。

**建议**：

1. 删除 Gateway 对整个 `/api/ai/**` 的无条件放行，改为明确端点白名单。
2. 生成式 AI 默认要求登录；如产品必须支持游客，只开放一个低成本入口。
3. 按 `userId + IP + endpoint` 设置令牌桶、并发上限、单分钟和单日配额。
4. 限制消息、systemPrompt、context 和图片大小；设置模型调用总超时。
5. 建立每日预算、熔断开关和用量告警，预算达到阈值后自动降级为静态答案。
6. 对 AI 输入做指令与数据边界隔离；不要允许客户端自由指定内部 system prompt。

### 已缓解：Docker 部署暴露内部基础设施和全部业务服务端口

**静态证据**：`deploy/docker-compose.yml` 将 MySQL 3306、Redis 6379、业务服务 8091—8095 和 Gateway 8090 均映射到宿主机。

**影响**：

- 攻击者可绕过 Gateway，直接接触业务服务；
- MySQL、Redis 弱口令或配置错误时直接暴露；
- Gateway 的头清洗、统一限流和访问日志不再构成唯一入口。

**建议**：

1. 生产 Compose 只发布前端 8080 和 Gateway 8090。
2. MySQL、Redis、8091—8095 删除 `ports`，改用内部网络或 `expose`。
3. 如本地调试必须映射，绑定 `127.0.0.1:端口:端口`，不要监听 `0.0.0.0`。
4. 使用独立生产 Compose override，避免开发端口配置误进入服务器。
5. 即使服务不可公网访问，也保留业务服务 JWT 验证，形成纵深防御。

### P1：JWT 密钥派生方式和服务间配置一致性需统一

Gateway 使用：

```java
Keys.hmacShaKeyFor(Base64.getEncoder().encode(jwtSecret.getBytes(UTF_8)))
```

需要核对 `JwtHelper` 的签名和验签是否采用完全相同的密钥处理方式。当前必须避免 Gateway 与业务服务对同一个环境变量做不同编码或不同算法解释。

**建议**：

1. 抽取唯一 JWT Key Provider，Gateway 与业务服务共享同一实现或同一测试向量。
2. 明确环境变量究竟是“原始随机串”还是“Base64 编码后的密钥”，只允许一种格式。
3. 强制校验 `exp`、`iat`、`iss`、`aud`、`jti`，设置可接受时钟偏差。
4. 增加密钥版本 `kid` 和轮换机制；访问令牌使用较短有效期，刷新令牌单独管理。
5. CI 增加跨模块契约测试：user-service 签发的令牌必须同时通过 Gateway 和业务服务验证。

### P1：公开 Swagger/OpenAPI 增加攻击面

Gateway 和业务服务允许匿名访问 Swagger 与 `/v3/api-docs`。

**建议**：

- local/dev profile 保留；prod profile 禁止或要求管理员鉴权；
- 确保文档不暴露内部管理接口、示例密钥、真实服务器地址和敏感字段。

### P1：文件上传采用扩展名黑名单，缺少内容验证

当前已有：大小限制、批量数量限制、文件名净化、危险扩展名黑名单、UUID 存储名和路径穿越保护。这些设计是有效的，但扩展名黑名单仍可被以下方式绕过：双扩展名、未知危险格式、MIME 欺骗、压缩炸弹、带脚本的 SVG/HTML/Office 文件。

**建议**：

1. 从黑名单改为业务必需类型白名单。
2. 同时校验扩展名、请求 Content-Type 和文件魔数；不一致则拒绝。
3. SVG、HTML、XML 默认拒绝或做严格净化。
4. 图片进行解码后重新编码；Office/PDF 可接入异步病毒扫描。
5. 文件存储目录不得由 Web 容器直接执行；下载统一 `Content-Disposition: attachment`。
6. 增加用户总容量、每日上传量和批量总字节数限制。

### P1：登录、注册、验证码和公开访问统计需要独立防滥用策略

公开写入口包括登录、注册、验证码、游记浏览计数和分享访问统计。HTTP 幂等不能替代频率限制。

**建议**：

- 登录：IP + 账号双维度限流，指数退避，失败次数告警；
- 注册/验证码：图形验证码或行为验证、手机号/邮箱频控；
- 浏览计数：去重窗口、签名或服务端 Session，防止刷量；
- 分享访问：限制同 IP/同分享 ID 的写入频率；
- 对公开写接口使用比普通 GET 更严格的 Sentinel/Gateway 限额。

### P2：安全响应头和服务指纹需要统一治理

建议在 Gateway 添加：

- `Content-Security-Policy`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY` 或 CSP `frame-ancestors`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy`
- HTTPS 环境下 `Strict-Transport-Security`

并删除 `Server`、`X-Powered-By`、`X-Application-Context` 等指纹头。

### P2：缺少独立的算法安全与资源上限验证

路线规划使用遗传算法和最近邻算法。项目已有景点数量上限，但还应验证：

- `maxDays`、景点数量、预算和偏好字符串的边界；
- GA 迭代数和种群大小不可由客户端任意放大；
- 高德外呼应具备超时、并发上限、缓存和断路器；
- 相同规划请求可缓存或合并，防止重复计算与重复外呼；
- 对异常经纬度、重复景点、空列表进行快速失败。

## 6. 动态探测覆盖项

脚本当前覆盖：

1. 匿名访问受保护写接口；
2. 伪造内部用户/角色头；
3. 畸形 JWT 和 `alg=none` JWT；
4. Swagger 和 Actuator 暴露；
5. SQL 注入型搜索输入；
6. 反射型 XSS 输入；
7. 4KB 超长查询；
8. 路径穿越型资源请求；
9. CORS 恶意 Origin 预检；
10. 安全响应头和服务指纹；
11. 应鉴权的 AI 路线优化端点；
12. 可选：8091—8095 直连；
13. 可选：最多 12 个 GET 的轻量限流探测。

## 7. 当前验证状态

已完成：

- Python 编译检查通过；
- `--help` 参数检查通过；
- 公网目标保护通过，`https://example.com` 被脚本主动拒绝。

未完成：

- 2026-08-29 检查时，本地 `127.0.0.1:8090` Gateway 未启动，因此没有生成有效动态攻击结果。
- 启动本地测试栈后，应重新执行脚本，并以生成的 `security-report.json` 为准。

## 8. 建议补入 CI 的安全门禁

1. 运行现有单元与集成测试；
2. 增加 Gateway/服务授权矩阵测试；
3. 增加 Semgrep 或 CodeQL SAST；
4. 增加依赖漏洞扫描（OWASP Dependency-Check / Trivy）；
5. 增加 Git secret scanning（Gitleaks）；
6. 在临时 E2E 环境运行 `security_probe.py`；
7. 关键安全断言失败时阻止合并。

推荐先修复两个 P0：**AI 整体公开**与**内部端口全部映射**。这两项改动小、解释清楚、面试收益也最高。
