# 智慧旅游系统 - 启动指南

## 快速启动（推荐）

### 1. 配置数据库密码

编辑 `deploy/.env` 文件，填入你的 MySQL 密码：

```env
DB_PASSWORD=你的MySQL密码
```

### 2. 一键启动

```bash
# Windows
start-all.bat

# 或手动分步启动
```

### 3. 访问系统

- **前端**: http://localhost:3000
- **Nacos 控制台**: http://localhost:8848/nacos (nacos/nacos)
- **API 网关**: http://localhost:8090

---

## 手动启动步骤

### 前置条件

确保以下服务已运行：
- ✓ MySQL 80 (端口 3306)
- ✓ Redis (端口 6379)
- ✓ Nacos (端口 8848)

### 启动 Nacos

```bash
cd nacos\nacos
bin\startup.cmd -m standalone
```

访问 http://localhost:8848/nacos 验证（默认账号：nacos/nacos）

### 启动后端微服务

```bash
# 1. 编译项目
cd backend
mvn clean package -DskipTests

# 2. 依次启动各服务（每个开一个终端）
# 终端 1: User Service
java -jar user-service\target\user-service-1.0-SNAPSHOT.jar

# 终端 2: Attraction Service
java -jar attraction-service\target\attraction-service-1.0-SNAPSHOT.jar

# 终端 3: Route Service
java -jar route-service\target\route-service-1.0-SNAPSHOT.jar

# 终端 4: Collection Service
java -jar collection-service\target\collection-service-1.0-SNAPSHOT.jar

# 终端 5: File Service
java -jar file-service\target\file-service-1.0-SNAPSHOT.jar

# 终端 6: Gateway
java -jar gateway\target\gateway-1.0-SNAPSHOT.jar
```

### 启动前端

```bash
cd frontend
npm run dev
```

访问 http://localhost:3000

---

## 快速开发模式（Mock API）

如果不想启动完整微服务，可以使用 Mock API：

```bash
# 终端 1: Mock API
cd mock-server
npm start

# 终端 2: 前端
cd frontend
npm run dev
```

测试账号：admin / 123456

---

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8090 | API 网关 |
| User Service | 8091 | 用户服务 |
| Attraction Service | 8092 | 景点服务 |
| Route Service | 8093 | 路线服务 |
| Collection Service | 8094 | 收藏服务 |
| File Service | 8095 | 文件服务 |
| Nacos | 8848 | 服务注册/配置中心 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Frontend | 3000 | 前端开发服务器 |

---

## 常见问题

### 1. 数据库连接失败

检查 `deploy/.env` 中的 `DB_PASSWORD` 是否正确。

### 2. Nacos 启动失败

确保 Java 17 已安装：
```bash
java -version
```

### 3. 端口被占用

检查并停止占用端口的进程，或修改配置文件中的端口。

### 4. 微服务注册失败

确保 Nacos 已启动且端口 8848 可访问。

---

## 停止服务

```bash
# 停止所有微服务
stop-all.bat

# 停止 Nacos
nacos\nacos\bin\shutdown.cmd
```