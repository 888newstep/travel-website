# 智慧旅游系统 (Smart Travel System)

## 项目概述

智慧旅游系统是一个基于Spring Boot 3.x开发的综合性旅游服务平台，提供AI智能推荐、路线规划、实时数据、用户社区等功能。

## 技术栈

- **后端框架**: Spring Boot 3.5.8
- **JDK版本**: Java 17
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **ORM框架**: MyBatis-Plus 3.5.8
- **安全框架**: Spring Security + JWT
- **API文档**: Swagger/OpenAPI 3.0
- **构建工具**: Maven

## 项目结构

```
travel/
├── src/
│   ├── main/
│   │   ├── java/travel/
│   │   │   ├── controller/          # 控制器层
│   │   │   │   ├── route_planning_controller/
│   │   │   │   ├── travel_realtime_controller/
│   │   │   │   ├── travel_recommendation_controller/
│   │   │   │   └── user_community_controller/
│   │   │   ├── service/             # 服务层
│   │   │   │   ├── impl/
│   │   │   │   ├── route_planning/
│   │   │   │   ├── travel_realtime/
│   │   │   │   ├── travel_recommendation/
│   │   │   │   └── user_community/
│   │   │   ├── mapper/              # 数据访问层
│   │   │   ├── entity/              # 实体类
│   │   │   ├── dto/                 # 数据传输对象
│   │   │   ├── enums/               # 枚举类
│   │   │   ├── exception/           # 异常处理
│   │   │   ├── utils/               # 工具类
│   │   │   └── repository/          # 仓库层
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── db/                  # 数据库脚本
│   │       └── doc/                 # 文档
│   └── test/                        # 测试代码
├── deploy/                          # 部署脚本
├── docker/                          # Docker配置
├── docs/                            # 项目文档
├── scripts/                         # 运维脚本
└── pom.xml
```

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 本地开发环境搭建

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd travel
   ```

2. **配置数据库**
   - 创建MySQL数据库 `travel_website`
   - 执行数据库初始化脚本：`src/main/resources/db/init.sql`

3. **配置Redis**
   - 确保Redis服务已启动
   - 默认端口：6379

4. **修改配置**
   - 复制 `application-dev.properties` 并修改数据库和Redis连接信息

5. **启动应用**
   ```bash
   # 使用Maven
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   
   # 或使用脚本
   ./scripts/start.sh dev
   ```

6. **访问API文档**
   - Swagger UI: http://localhost:8080/api/swagger-ui.html
   - API Docs: http://localhost:8080/api/v3/api-docs

## 测试

### 运行单元测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=APIControllerTest

# 生成测试报告
mvn test jacoco:report
```

### 运行集成测试

```bash
# 使用测试配置文件
```

### API接口测试

```bash
# 使用curl测试
./scripts/api-test.sh

# 或使用httpie
http :8080/api/health
```

## 部署

### 本地虚拟机部署

1. **准备虚拟机环境**
   ```bash
   # 使用VirtualBox或VMware创建CentOS/Ubuntu虚拟机
   # 配置网络为桥接模式，确保与主机互通
   ```

2. **执行部署脚本**
   ```bash
   # 在虚拟机上执行
   ./deploy/deploy.sh
   ```

### Docker部署

```bash
# 构建镜像
cp deploy/.env.example deploy/.env

# 运行容器
docker compose -f deploy/docker-compose.yml up --build -d
```

### 生产环境部署

```bash
# 打包应用
mvn clean package -P prod

# 部署到服务器
./deploy/deploy.sh prod
```

## 运维监控

### 健康检查

```bash
# 检查服务状态
curl http://localhost:8080/api/health

# 详细健康检查
curl http://localhost:8080/api/health/detailed
```

### 日志查看

```bash
# 查看实时日志
tail -f logs/travel-app.log

# 查看错误日志
tail -f logs/error.log
```

### 性能监控

- Actuator端点: http://localhost:8080/api/actuator
- 指标数据: http://localhost:8080/api/actuator/metrics

## API接口规范

### 响应格式

所有API接口统一返回以下JSON结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1704067200000,
  "success": true
}
```

### 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未授权访问 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 核心功能模块

### 1. 路线规划 (Route Planning)
- 智能路线生成
- 路线优化算法
- 实时路线调整
- 多交通方式规划

### 2. 旅游推荐 (Travel Recommendation)
- AI智能推荐
- 景点信息查询
- 美食推荐
- 导游服务

### 3. 实时数据 (Real-time Data)
- 景点实时状态
- 人流密度监控
- 实时预警

### 4. 用户社区 (User Community)
- 用户管理
- 游记分享
- 路线收藏
- 评论互动

## 开发规范

### 代码规范
- 遵循阿里巴巴Java开发手册
- 使用Lombok简化代码
- 统一返回Result包装类

### 接口规范
- RESTful API设计
- 统一异常处理
- 参数校验使用@Valid

### 数据库规范
- 使用MyBatis-Plus进行CRUD操作
- 逻辑删除统一使用deleted字段
- 时间字段使用LocalDateTime

## 贡献指南

1. Fork项目到个人仓库
2. 创建功能分支：`git checkout -b feature/xxx`
3. 提交代码：`git commit -m "feat: xxx"`
4. 推送分支：`git push origin feature/xxx`
5. 创建Pull Request

## 许可证

[MIT License](LICENSE)

## 联系方式

- 项目维护者: [Your Name]
- 邮箱: [your.email@example.com]

---

**注意**: 本项目为实习项目，仅供学习和参考使用。
