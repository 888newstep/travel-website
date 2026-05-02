
# Travel 项目开发规范指南
为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目基本信息

- **项目名称**：travel
- **作者**：xiaohongfu
- **工作目录**：`C:\Users\xiaohongfu\IdeaProjects\travel`
- **开发环境**：Windows 11
- **构建工具**：Maven
- **注释语言**：中文（请统一使用中文编写注释）

## 二、技术栈要求

- **主框架**：Spring Boot 3.5.13
- **语言版本**：Java 17 (Maven配置) / JDK 21.0.9 (运行环境)
- **核心依赖**：
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `mybatis-plus-boot-starter` (3.5.7)
  - `spring-boot-starter-data-redis`
  - `spring-boot-starter-security`
  - `lombok` (1.18.30)
  - `jjwt-api` (0.12.5) - JWT Token 生成
  - `jts-core` (1.19.0) - 空间几何计算
  - `fastjson` (2.0.43) - JSON 处理
  - `spring-retry` (2.0.6) - 重试机制

## 三、目录结构规范

本项目采用模块化分层结构，请严格按照以下目录树进行开发：

```text
travel
├── src
│   ├── main
│   │   ├── java
│   │   │   └── org
│   │   │       └── example
│   │   │           └── travel
│   │   │               ├── config              # 配置类（Security, Redis, MyBatis-Plus等）
│   │   │               ├── controller          # 控制层
│   │   │               │   ├── route_planning_controller
│   │   │               │   ├── travel_realtime_controller
│   │   │               │   ├── travel_recommendation_controller
│   │   │               │   └── user_community_controller
│   │   │               ├── dto                 # 数据传输对象
│   │   │               │   └── request        # 请求参数封装
│   │   │               ├── entity              # 数据库实体对象（JPA/MyBatis-Plus）
│   │   │               │   ├── route_planning
│   │   │               │   ├── travel_realtime
│   │   │               │   ├── travel_recommendation
│   │   │               │   ├── user_community
│   │   │               │   └── vo              # 视图对象（如有）
│   │   │               ├── enums               # 枚举类定义
│   │   │               ├── exception           # 全局异常处理
│   │   │               ├── mapper              # MyBatis-Plus Mapper 接口
│   │   │               │   ├── route_planning
│   │   │               │   ├── route_planning_mapper
│   │   │               │   ├── travel_realtime_mapper
│   │   │               │   ├── travel_recommendation_mapper
│   │   │               │   └── user_community_mapper
│   │   │               ├── repository          # JPA Repository 接口（如使用JPA）
│   │   │               ├── service             # 业务逻辑层接口
│   │   │               │   ├── route_planning
│   │   │               │   ├── travel_realtime
│   │   │               │   ├── travel_recommendation
│   │   │               │   └── user_community
│   │   │               │   └── impl            # 业务逻辑层实现
│   │   │               │       ├── route_planning
│   │   │               │       ├── travel_realtime
│   │   │               │       ├── travel_recommendation
│   │   │               │       └── user_community
│   │   │               └── utils               # 工具类
│   │   └── resources
│   │       ├── db                  # 数据库脚本
│   │       ├── doc                 # 项目文档
│   │       ├── static              # 静态资源
│   │       └── templates           # Thymeleaf 模板
│   └── test
│       └── java
│           └── org
│               └── example
│                   └── travel
│                       └── service             # 单元测试
```

## 四、分层架构规范

| 层级        | 职责说明                         | 开发约束与注意事项                                               |
|-------------|----------------------------------|----------------------------------------------------------------|
| **Controller** | 处理 HTTP 请求与响应，定义 API 接口 | 不得直接访问数据库，必须通过 Service 层调用；统一返回格式       |
| **Service**    | 实现业务逻辑、事务管理与数据校验   | 必须通过 Mapper 或 Repository 层访问数据库；返回 DTO/VO 而非 Entity |
| **Mapper**     | MyBatis-Plus 数据库访问接口        | 继承 `BaseMapper`；位于对应业务模块的 mapper 包下               |
| **Repository** | JPA 数据库访问接口（如使用）       | 继承 `JpaRepository`；使用 `@EntityGraph` 避免 N+1 查询问题     |
| **Entity**     | 映射数据库表结构                   | 不得直接返回给前端（需转换为 DTO/VO）；包名统一为 `entity`       |

### 接口与实现分离

- 所有业务逻辑通过接口定义（如 `UserService`），具体实现放在 `impl` 包中（如 `UserServiceImpl`）。
- 按照业务模块（如 `route_planning`, `user_community`）对 Service 和 Mapper 进行分包管理。

## 五、安全与性能规范

### 输入校验

- 使用 `@Valid` 与 JSR-303 校验注解（如 `@NotBlank`, `@Size` 等）。
  - 注意：Spring Boot 3.x 中校验注解位于 `jakarta.validation.constraints.*`。
- 禁止手动拼接 SQL 字符串，防止 SQL 注入攻击。优先使用 MyBatis-Plus 的 Wrapper 或 JPA。

### 安全认证

- 使用 **Spring Security** 进行权限控制。
- 使用 **JWT (jjwt)** 进行无状态身份认证。
- 敏感接口需添加相应的权限注解。

### 事务与缓存

- `@Transactional` 注解仅用于 **Service 层**方法。
- 避免在循环中频繁提交事务，影响性能。
- 使用 **Redis** 进行缓存数据存储，连接池使用 `commons-pool2`。
- 对于可能失败的操作，使用 **Spring Retry** (`@Retryable`) 进行自动重试。

## 六、代码风格规范

### 命名规范

| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `UserServiceImpl`     |
| 方法/变量  | lowerCamelCase       | `saveUser()`          |
| 常量       | UPPER_SNAKE_CASE     | `MAX_LOGIN_ATTEMPTS`  |

### 注释规范

- **强制要求**：所有类、方法、字段需添加 **Javadoc** 注释。
- **语言要求**：注释内容必须使用 **中文**，确保团队成员能快速理解业务逻辑。

### 类型命名规范（阿里巴巴风格）

| 后缀 | 用途说明                     | 示例         |
|------|------------------------------|--------------|
| DTO  | 数据传输对象                 | `UserDTO`    |
| Entity/DO | 数据库实体对象 (MyBatis/JPA)| `User`      |
| VO   | 视图展示对象                 | `UserVO`     |
| Query| 查询参数封装对象             | `UserQuery`  |

### 实体类简化工具

- 使用 Lombok 注解替代手动编写 getter/setter/构造方法：
  - `@Data`
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`
  - `@Slf4j` (用于日志)

### JSON 处理

- 统一使用 `Fastjson` (2.0.43) 或 `Jackson` 进行 JSON 序列化与反序列化。
- 时间类型统一使用 `LocalDateTime`，并配置 `jackson-datatype-jsr310` 模块支持。

## 七、编码原则总结

| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性                   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞，如 SQL 注入、XSS 等      |
