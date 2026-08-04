# 智慧旅游系统 - 实习指南

## 项目概述

本项目是一个基于 Spring Boot 3.x 的智慧旅游系统，涵盖路线规划、AI推荐、用户社区等模块。

## 实习要求覆盖

### 1. Java后端开发要求

#### 1.1 技术栈
- **框架**: Spring Boot 3.5.8
- **JDK**: Java 17
- **数据库**: MySQL 8.0 + MyBatis-Plus 3.5.8
- **缓存**: Redis
- **安全**: Spring Security + JWT
- **文档**: Swagger/OpenAPI 3.0

#### 1.2 项目结构
```
travel/
├── src/main/java/travel/
│   ├── controller/          # 控制器层
│   │   ├── route_planning_controller/
│   │   ├── travel_recommendation_controller/
│   │   ├── travel_realtime_controller/
│   │   └── user_community_controller/
│   ├── service/             # 服务层
│   │   ├── impl/           # 实现类
│   │   └── interfaces/     # 接口定义
│   ├── entity/             # 实体类
│   ├── mapper/             # 数据访问层
│   ├── repository/         # 仓储层
│   ├── dto/                # 数据传输对象
│   ├── vo/                 # 视图对象
│   ├── enums/              # 枚举类
│   ├── exception/          # 异常处理
│   ├── utils/              # 工具类
│   └── config/             # 配置类
├── src/test/java/travel/   # 测试代码
└── src/main/resources/     # 配置文件
```

#### 1.3 核心功能模块
1. **路线规划模块**: 智能路线规划、实时调整、交通方式推荐
2. **AI推荐模块**: 智能问答、景点推荐、图像识别、多模态交互
3. **实时数据模块**: 景点实时状态、人流监控
4. **用户社区模块**: 游记分享、路线收藏、评论互动

#### 1.4 开发规范
- 统一返回格式: `Result<T>` (code, message, data, timestamp, success)
- 异常处理: 全局异常处理器 + 自定义业务异常
- 日志记录: SLF4J + Logback
- 接口文档: Swagger注解

### 2. 测试要求

#### 2.1 单元测试
```bash
# 运行所有单元测试
mvn test

# 运行特定测试类
mvn test -Dtest=APIControllerTest

# 生成测试报告
mvn test jacoco:report
```

#### 2.2 集成测试
- 使用 `@SpringBootTest` 进行集成测试
- 使用 MockMvc 测试控制器接口

#### 2.3 测试覆盖率
- 目标: 核心业务逻辑覆盖率 >= 80%
- 工具: JaCoCo
- 报告位置: `target/site/jacoco/index.html`

#### 2.4 API测试
- 使用 `ApiTest.java` 进行接口自动化测试
- 验证JSON响应结构一致性
- 测试用例覆盖主要业务场景

### 3. 运维要求

#### 3.1 Linux虚拟机部署

##### 环境准备
```bash
# 1. 安装JDK 17
sudo apt update
sudo apt install openjdk-17-jdk

# 2. 安装MySQL
sudo apt install mysql-server-8.0
sudo mysql_secure_installation

# 3. 安装Redis
sudo apt install redis-server

# 4. 配置防火墙
sudo ufw allow 8080/tcp
sudo ufw allow 3306/tcp
sudo ufw allow 6379/tcp
```

##### 应用部署
```bash
# 1. 克隆代码
git clone <repository-url>
cd travel

# 2. 执行部署脚本
chmod +x deploy/deploy.sh
./deploy/deploy.sh

# 3. 查看服务状态
sudo systemctl status travel-api

# 4. 查看日志
tail -f /var/log/travel/app.log
```

#### 3.2 Docker部署

```bash
# 1. 构建镜像
cp deploy/.env.example deploy/.env

# 2. 启动服务
docker compose -f deploy/docker-compose.yml up --build -d

# 3. 查看日志
docker compose -f deploy/docker-compose.yml logs -f gateway web

# 4. 停止服务
docker compose -f deploy/docker-compose.yml down
```

#### 3.3 监控与日志

##### 健康检查
```bash
# API健康检查
curl http://localhost:8080/api/health

# 详细健康检查
curl http://localhost:8080/api/health/detailed
```

##### 日志管理
- 应用日志: `/var/log/travel/app.log`
- 错误日志: `/var/log/travel/error.log`
- 访问日志: `/var/log/travel/access.log`
- 日志轮转: 使用 logrotate

#### 3.4 性能监控

##### JVM监控
```bash
# 查看JVM进程
jps -l

# 查看GC情况
jstat -gc <pid> 1000

# 生成堆转储
jmap -dump:format=b,file=heap.hprof <pid>

# 分析堆转储
jhat heap.hprof
```

##### 应用监控端点
- 健康检查: `/api/health`
- 系统信息: `/api/system/info`
- API统计: `/api/stats`
- 使用统计: `/api/usage`

## 实习任务清单

### 初级任务
- [ ] 熟悉项目结构和代码规范
- [ ] 完成本地环境搭建
- [ ] 运行单元测试并理解测试用例
- [ ] 编写一个简单的CRUD接口

### 中级任务
- [ ] 实现一个完整的业务功能模块
- [ ] 编写对应的单元测试和集成测试
- [ ] 配置Linux虚拟机并部署应用
- [ ] 配置监控和日志收集

### 高级任务
- [ ] 优化数据库查询性能
- [ ] 实现分布式缓存策略
- [ ] 设计高可用部署方案
- [ ] 编写自动化运维脚本

## 学习资源

### 官方文档
- [Spring Boot](https://spring.io/projects/spring-boot)
- [MyBatis-Plus](https://baomidou.com/)
- [Redis](https://redis.io/documentation)

### 推荐书籍
- 《Spring Boot实战》
- 《MyBatis技术内幕》
- 《Redis设计与实现》

## 常见问题

### Q: 如何修改数据库配置？
A: 编辑 `src/main/resources/application.properties` 或创建 `application-dev.properties`

### Q: 如何添加新的API接口？
A: 
1. 在对应模块的 controller 包下创建控制器
2. 使用 `@RestController` 和 `@RequestMapping` 注解
3. 返回类型统一使用 `Result<T>`
4. 添加 Swagger 注解生成文档

### Q: 如何进行数据库迁移？
A: 使用 `src/main/resources/db/migration/` 下的 Flyway 脚本

### Q: 如何查看API文档？
A: 启动应用后访问: http://localhost:8080/api/swagger-ui.html

## 联系方式

如有问题，请联系项目导师或查阅项目文档。

---

**最后更新**: 2026-04-17
**版本**: v1.0.0
