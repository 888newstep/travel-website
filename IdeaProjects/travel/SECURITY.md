# 安全策略

## 支持的版本

| 版本 | 支持状态 |
|------|---------|
| 最新 Release | ✅ 积极维护 |
| 其他版本 | ❌ 不提供安全更新 |

## 报告安全漏洞

如果您发现了安全漏洞，**请不要公开披露**，而是通过以下方式私下报告：

1. 在 GitHub 上创建一个 [Security Advisory](https://github.com/888newstep/travel-website/security/advisories)
2. 或发送邮件至项目维护者

我们将在 **48 小时内**确认收到报告，并在修复后公开致谢（如您同意）。

## 安全最佳实践

### 部署安全

- 始终使用 `deploy/.env` 文件配置敏感信息，**不要硬编码任何密钥**（`deploy/.env` 已被 `.gitignore` 排除）
- 生产环境使用强 `JWT_SECRET`（至少 32 字节随机字符串）
- 数据库 / Redis / RabbitMQ 生产环境必须设置强密码，不要沿用 `.env.example` 的占位值
- 启用 HTTPS（Nginx 反向代理 + SSL 证书）
- 按需通过防火墙限制端口暴露（例如仅暴露网关 8090 / Nginx 8080，`druid` 监控面板默认仅放行 `127.0.0.1`）

### 凭据管理

项目通过以下方式保护凭据安全：

1. 所有 API Key 通过环境变量 `${VAR_NAME}` 注入（见 `deploy/.env.example`）
2. `deploy/.env` 文件已被 `.gitignore` 排除，不会提交到仓库
3. 提供 `.env.example` 模板，仅包含占位符值

### 依赖安全

- 定期执行 `mvn dependency-check` 扫描已知依赖漏洞
- GitHub Dependabot 自动检测依赖更新
- 前端定期执行 `npm audit` 检查依赖安全
- 及时应用安全补丁