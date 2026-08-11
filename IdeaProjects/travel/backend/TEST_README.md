# 测试基线文档

## 已创建的测试文件

### Gateway 模块
- **GatewayAuthTest.java**
  - 位置: gateway/src/test/java/travel/gateway/GatewayAuthTest.java
  - 测试内容:
    - 公开端点访问测试
    - 受保护端点无 token 访问测试
    - 受保护端点无效 token 访问测试

### Route-Service 模块

#### 控制器测试
- **RouteControllerTest.java**
  - 位置: route-service/src/test/java/travel/route/controller/RouteControllerTest.java
  - 测试内容:
    - 获取路线主题列表
    - 获取季节列表

#### 异常处理测试
- **GlobalExceptionHandlerTest.java**
  - 位置: route-service/src/test/java/travel/route/exception/GlobalExceptionHandlerTest.java
  - 测试内容:
    - 404 路径不存在测试
    - 400 参数验证失败测试
    - 500 系统异常响应测试

#### 服务测试
- **IntelligentRouteServiceTest.java**
  - 位置: route-service/src/test/java/travel/route/service/IntelligentRouteServiceTest.java
  - 测试内容:
    - SmartRouteItem DTO 构建器测试

## 如何运行测试

### 运行所有测试
`ash
cd backend
mvn test
`

### 运行特定模块测试
`ash
# Gateway 模块
cd backend/gateway
mvn test

# Route-Service 模块
cd backend/route-service
mvn test
`

### 运行特定测试类
`ash
# 运行 GatewayAuthTest
cd backend/gateway
mvn test -Dtest=GatewayAuthTest

# 运行 RouteControllerTest
cd backend/route-service
mvn test -Dtest=RouteControllerTest
`

## 测试覆盖范围

### Batch 3 最小测试基线要求
- [x] Gateway 鉴权链测试
- [x] Route 推荐主流程测试（部分）
- [x] 全局异常处理测试

### 当前测试状态
- 测试文件已创建
- 需要手动执行验证（Maven 超时问题）
- 建议修复 Maven 配置后运行完整测试套件

## 下一步建议

1. **修复 Maven 超时问题**
   - 检查网络连接
   - 清理 Maven 缓存: mvn dependency:purge-local-repository
   - 增加 Maven 超时配置

2. **运行测试验证**
   - 执行上述命令运行测试
   - 确保所有测试通过

3. **扩展测试覆盖**
   - 添加更多服务层测试
   - 添加集成测试
   - 添加性能测试

4. **进入 Batch 5**
   - 在测试基线稳定后
   - 开始拆分 route-service 大类
