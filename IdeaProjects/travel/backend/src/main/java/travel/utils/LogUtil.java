package travel.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class LogUtil {

    /**
     * 生成日志追踪ID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 获取当前请求信息
     */
    public static Map<String, String> getRequestInfo() {
        Map<String, String> requestInfo = new HashMap<>();
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getRequest() != null) {
                requestInfo.put("method", attributes.getRequest().getMethod());
                requestInfo.put("uri", attributes.getRequest().getRequestURI());
                requestInfo.put("queryString", attributes.getRequest().getQueryString());
                requestInfo.put("clientIp", attributes.getRequest().getRemoteAddr());
                requestInfo.put("userAgent", attributes.getRequest().getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.error("获取请求信息失败: {}", e.getMessage());
        }
        return requestInfo;
    }

    /**
     * 记录调试日志
     */
    public static void debug(String message, Object... args) {
        log.debug(message, args);
    }

    /**
     * 记录信息日志
     */
    public static void info(String message, Object... args) {
        log.info(message, args);
    }

    /**
     * 记录警告日志
     */
    public static void warn(String message, Object... args) {
        log.warn(message, args);
    }

    /**
     * 记录错误日志
     */
    public static void error(String message, Object... args) {
        log.error(message, args);
    }

    /**
     * 记录错误日志（包含异常）
     */
    public static void error(String message, Throwable throwable, Object... args) {
        log.error(message, args, throwable);
    }

    /**
     * 记录带追踪ID的错误日志
     */
    public static String errorWithTrace(String message, Throwable throwable, Object... args) {
        String traceId = generateTraceId();
        Map<String, String> requestInfo = getRequestInfo();
        log.error("[错误] {}: {}, 追踪ID: {}, 请求信息: {}", 
                message, traceId, requestInfo, args, throwable);
        return traceId;
    }

    /**
     * 记录业务操作日志
     */
    public static void biz(String operation, String detail, Object... args) {
        Map<String, String> requestInfo = getRequestInfo();
        log.info("[业务操作] {}: {}, 请求信息: {}", operation, detail, requestInfo);
    }

    /**
     * 记录系统操作日志
     */
    public static void sys(String operation, String detail, Object... args) {
        log.info("[系统操作] {}: {}", operation, detail, args);
    }

    /**
     * 记录性能日志
     */
    public static void perf(String operation, long startTime, Object... args) {
        long endTime = System.currentTimeMillis();
        long costTime = endTime - startTime;
        log.info("[性能] {}: 耗时 {}ms", operation, costTime);
    }

    /**
     * 记录接口调用日志
     */
    public static void api(String method, String uri, int statusCode, long costTime, Object... args) {
        Map<String, String> requestInfo = getRequestInfo();
        log.info("[接口调用] {} {}: 状态码={}, 耗时={}ms, 请求信息: {}",
                method, uri, statusCode, costTime, requestInfo);
    }

    /**
     * 记录第三方服务调用日志
     */
    public static void thirdParty(String service, String operation, boolean success, long costTime, Object... args) {
        log.info("[第三方服务] {}: {}: 成功={}, 耗时={}ms", service, operation, success, costTime);
    }
}