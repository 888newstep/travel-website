package travel.common.exception;

import travel.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 生成异常追踪ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 获取当前请求信息
     */
    private Map<String, String> getRequestInfo() {
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
     * 记录异常日志
     */
    private void logException(Exception e, String exceptionType, String traceId) {
        Map<String, String> requestInfo = getRequestInfo();
        log.error("[{}] {}: {}, 追踪ID: {}, 请求信息: {}", 
                exceptionType, e.getMessage(), traceId, requestInfo, e);
    }

    /**
     * 构建异常响应数据
     */
    private Map<String, Object> buildErrorData(int code, String message, String traceId) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("code", code);
        errorData.put("message", message);
        errorData.put("traceId", traceId);
        errorData.put("timestamp", new Date());
        errorData.put("request", getRequestInfo());
        return errorData;
    }

    /**
     * 统一业务异常处理
     * 所有业务异常都使用BusinessException，统一错误码管理
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Map<String, Object>> handleBusinessException(BusinessException e) {
        String traceId = generateTraceId();
        logException(e, "业务异常", traceId);
        Map<String, Object> errorData = buildErrorData(e.getCode(), e.getMessage(), traceId);
        return Result.error(e.getCode(), e.getMessage(), errorData);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Map<String, Object>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        String traceId = generateTraceId();
        logException(e, "路径不存在", traceId);
        Map<String, Object> errorData = buildErrorData(404, "请求路径不存在", traceId);
        return Result.error(404, "请求路径不存在", errorData);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String traceId = generateTraceId();
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = bindingResult.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logException(e, "参数验证异常", traceId);
        Map<String, Object> errorData = buildErrorData(400, errorMessage, traceId);
        return Result.error(400, errorMessage, errorData);
    }

    /**
     * 统一系统异常处理
     * 所有未捕获的异常统一处理，避免泄露内部错误信息
     */
    @ExceptionHandler(Exception.class)
    public Result<Map<String, Object>> handleException(Exception e) {
        String traceId = generateTraceId();
        logException(e, "系统异常", traceId);
        Map<String, Object> errorData = buildErrorData(500, "系统异常，请稍后重试", traceId);
        return Result.error(500, "系统异常，请稍后重试", errorData);
    }
}