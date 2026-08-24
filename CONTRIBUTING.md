# 贡献指南

感谢您对智慧旅游系统（Smart Travel System）的关注！我们欢迎任何形式的贡献，包括但不限于：

## 如何贡献

### 报告 Bug

1. 在 [Issues](https://github.com/888newstep/travel-website/issues) 中搜索是否已有类似问题
2. 如果没有，创建新 Issue 并附上：
   - 运行环境（OS、JDK 版本、浏览器版本）
   - 复现步骤
   - 期望行为与实际行为
   - 相关日志或截图

### 提交 Pull Request

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/your-feature-name`
3. 提交变更：`git commit -m "feat: add xxx feature"`
4. 推送到分支：`git push origin feature/your-feature-name`
5. 创建 Pull Request（PR 描述中注明改动模块与测试结果）

### 开发规范

#### 代码风格

- 后端遵循阿里巴巴 Java 开发手册，使用 4 空格缩进
- 前端使用 2 空格缩进 + TypeScript 严格模式
- 所有类和方法添加有意义的 Javadoc / JSDoc 注释
- 新增功能需包含单元测试

#### Commit 规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

- `feat:` 新功能
- `fix:` Bug 修复
- `docs:` 文档更新
- `refactor:` 重构
- `test:` 测试相关
- `chore:` 构建/工具链变更

#### 分支管理

- `main` — 稳定发布分支
- `dev` — 开发主分支
- `feature/*` — 特性分支
- `fix/*` — 修复分支

### 本地开发环境

```bash
# 1. 克隆项目
git clone https://github.com/888newstep/travel-website.git
cd travel

# 2. 配置环境变量（数据库、Redis、JWT 等）
Copy-Item deploy\.env.example deploy\.env

# 3a. 后端手动启动（需先启动 Nacos、MySQL、Redis）
cd backend
mvn clean package -DskipTests
java -jar gateway/target/gateway-1.0-SNAPSHOT.jar
# 依次启动 user/attraction/route/collection/file 各服务

# 3b. 或 Docker Compose 一键启动（推荐）
docker compose -f deploy/docker-compose.yml up --build -d

# 3c. 前端
cd frontend
npm install
npm run dev
```

### 测试

提交前确保相关测试通过：

```bash
# 后端
cd backend
mvn test

# 前端
cd frontend
npm run lint
```

## 行为准则

本项目采用 [Contributor Covenant](CODE_OF_CONDUCT.md) 行为准则。请阅读并遵守。

## 问题反馈

如有任何问题，请通过 [Issues](https://github.com/888newstep/travel-website/issues) 反馈。