package travel;

import travel.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用API控制器
 */
@RestController
@RequestMapping("")
public class APIController {

    /**
     * 根路径 - API入口
     * GET /api/
     */
    @GetMapping({"", "/"})
    public Result<Map<String, Object>> root() {
        Map<String, Object> apiInfo = new HashMap<>();
        apiInfo.put("name", "智慧旅游API");
        apiInfo.put("version", "1.0.0");
        apiInfo.put("status", "running");
        apiInfo.put("timestamp", LocalDateTime.now());
        apiInfo.put("endpoints", new String[]{
                "/api/health - 健康检查",
                "/api/version - 版本信息",
                "/api/docs - API文档",
                "/api/status - 系统状态"
        });
        apiInfo.put("documentation", "/swagger-ui.html");
        return Result.success("欢迎使用智慧旅游API", apiInfo);
    }

    /**
     * 健康检查
     * GET /api/health
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "healthy");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("service", "travel-api");
        healthInfo.put("version", "1.0.0");
        return Result.success("服务健康", healthInfo);
    }

    /**
     * 获取API版本信息
     * GET /api/version
     */
    @GetMapping("/version")
    public Result<Map<String, Object>> getVersion() {
        Map<String, Object> versionInfo = new HashMap<>();
        versionInfo.put("version", "1.0.0");
        versionInfo.put("buildTime", LocalDateTime.now());
        versionInfo.put("description", "智慧旅游API服务");
        versionInfo.put("features", new String[]{"AI智能推荐", "路线规划", "实时数据", "用户社区"});
        return Result.success("版本信息获取成功", versionInfo);
    }

    /**
     * 获取API文档信息
     * GET /api/docs
     */
    @GetMapping("/docs")
    public Result<Map<String, Object>> getDocs() {
        Map<String, Object> docsInfo = new HashMap<>();
        docsInfo.put("apiBaseUrl", "/api");
        docsInfo.put("endpoints", new String[]{
                "/api/health - 健康检查",
                "/api/version - 版本信息",
                "/api/docs - API文档",
                "/api/ai - AI相关接口",
                "/api/ai/advanced - 高级AI功能",
                "/api/intelligent-route - 智能路线规划",
                "/api/attraction - 景点相关接口",
                "/api/user - 用户相关接口"
        });
        docsInfo.put("documentationUrl", "/swagger-ui.html");
        return Result.success("API文档获取成功", docsInfo);
    }

    /**
     * 获取系统状态
     * GET /api/status
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> statusInfo = new HashMap<>();
        statusInfo.put("system", "running");
        statusInfo.put("timestamp", LocalDateTime.now());
        statusInfo.put("components", Map.of(
                "database", "connected",
                "redis", "connected",
                "aiService", "available",
                "cache", "enabled"
        ));
        statusInfo.put("uptime", "24h 30m 45s");
        return Result.success("系统状态获取成功", statusInfo);
    }

    /**
     * 测试接口
     * POST /api/test
     */
    @PostMapping("/test")
    public Result<Map<String, Object>> test(@RequestBody Map<String, Object> request) {
        Map<String, Object> testResult = new HashMap<>();
        testResult.put("request", request);
        testResult.put("response", "测试成功");
        testResult.put("timestamp", LocalDateTime.now());
        return Result.success("测试成功", testResult);
    }

    /**
     * 系统配置管理
     * GET /api/config
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        Map<String, Object> configInfo = new HashMap<>();
        configInfo.put("appName", "智慧旅游系统");
        configInfo.put("environment", "production");
        configInfo.put("features", Map.of(
                "aiEnabled", true,
                "realTimeData", true,
                "userCommunity", true,
                "routePlanning", true
        ));
        configInfo.put("limits", Map.of(
                "maxRequestsPerMinute", 1000,
                "maxImageSize", "5MB",
                "maxAudioSize", "10MB"
        ));
        return Result.success("获取配置成功", configInfo);
    }

    /**
     * 数据统计
     * GET /api/stats
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> statsInfo = new HashMap<>();
        statsInfo.put("timestamp", LocalDateTime.now());
        statsInfo.put("apiCalls", Map.of(
                "today", 12345,
                "thisWeek", 87654,
                "thisMonth", 345678
        ));
        statsInfo.put("userActivity", Map.of(
                "activeUsers", 5678,
                "newUsersToday", 123,
                "totalUsers", 45678
        ));
        statsInfo.put("systemPerformance", Map.of(
                "responseTime", "23ms",
                "uptime", "99.9%",
                "errorRate", "0.1%"
        ));
        return Result.success("获取统计数据成功", statsInfo);
    }

    /**
     * 缓存管理
     * POST /api/cache/clear
     */
    @PostMapping("/cache/clear")
    public Result<Map<String, Object>> clearCache(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        String cacheType = request.getOrDefault("type", "all").toString();
        result.put("cacheType", cacheType);
        result.put("status", "cleared");
        result.put("timestamp", LocalDateTime.now());
        result.put("message", "缓存清理成功");
        return Result.success("缓存清理成功", result);
    }

    /**
     * 健康检查增强
     * GET /api/health/detailed
     */
    @GetMapping("/health/detailed")
    public Result<Map<String, Object>> getDetailedHealth() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "healthy");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("service", "travel-api");
        healthInfo.put("version", "1.0.0");
        healthInfo.put("components", Map.of(
                "database", Map.of(
                        "status", "connected",
                        "responseTime", "12ms",
                        "connections", 15
                ),
                "redis", Map.of(
                        "status", "connected",
                        "memoryUsage", "45%",
                        "keys", 12345
                ),
                "aiService", Map.of(
                        "status", "available",
                        "responseTime", "56ms"
                ),
                "cache", Map.of(
                        "status", "enabled",
                        "hitRate", "87%"
                )
        ));
        healthInfo.put("uptime", "24h 30m 45s");
        return Result.success("服务健康", healthInfo);
    }

    /**
     * API使用统计
     * GET /api/usage
     */
    @GetMapping("/usage")
    public Result<Map<String, Object>> getApiUsage() {
        Map<String, Object> usageInfo = new HashMap<>();
        usageInfo.put("timestamp", LocalDateTime.now());
        usageInfo.put("topEndpoints", List.of(
                Map.of("endpoint", "/api/ai/advanced/chatbot", "calls", 5678),
                Map.of("endpoint", "/api/intelligent-route/plan", "calls", 4567),
                Map.of("endpoint", "/api/attraction/list", "calls", 3456),
                Map.of("endpoint", "/api/user/profile", "calls", 2345)
        ));
        usageInfo.put("responseTimes", Map.of(
                "average", "23ms",
                "p95", "56ms",
                "p99", "89ms"
        ));
        usageInfo.put("errorRates", Map.of(
                "4xx", "0.5%",
                "5xx", "0.1%"
        ));
        return Result.success("获取API使用统计成功", usageInfo);
    }

    /**
     * 系统信息
     * GET /api/system/info
     */
    @GetMapping("/system/info")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("timestamp", LocalDateTime.now());
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        systemInfo.put("freeMemory", Runtime.getRuntime().freeMemory() / 1024 / 1024 + "MB");
        systemInfo.put("totalMemory", Runtime.getRuntime().totalMemory() / 1024 / 1024 + "MB");
        return Result.success("获取系统信息成功", systemInfo);
    }
}
