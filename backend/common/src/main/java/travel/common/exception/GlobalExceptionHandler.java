package travel.common.exception;

import travel.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Set<Integer> NOT_FOUND_CODES = Set.of(
            1005, 2001, 2004, 3001, 3002, 6001, 7001, 8001, 9001,
            21001, 22014, 22015, 25001, 26001, 27001);
    private static final Set<Integer> FORBIDDEN_CODES = Set.of(
            2002, 6007, 7002, 8002, 9009, 14003, 28001);
    private static final Set<Integer> UNAUTHORIZED_CODES = Set.of(
            1006, 1008, 1009, 24005, 28002);
    private static final Set<Integer> TIMEOUT_CODES = Set.of(
            5003, 10002, 11002, 12002, 13004, 16004, 17004, 18004, 19004, 20005);

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
        log.error("[{}] message={}, traceId={}, request={}",
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
        return errorData;
    }

    /**
     * 统一业务异常处理
     * 所有业务异常都使用BusinessException，统一错误码管理
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Map<String, Object>>> handleBusinessException(BusinessException e) {
        String traceId = generateTraceId();
        HttpStatus status = resolveBusinessStatus(e.getCode());
        log.warn("[业务异常] status={}, code={}, message={}, traceId={}, request={}",
                status.value(), e.getCode(), e.getMessage(), traceId, getRequestInfo());
        Map<String, Object> errorData = buildErrorData(e.getCode(), e.getMessage(), traceId);
        return ResponseEntity.status(status)
                .body(Result.error(e.getCode(), e.getMessage(), errorData));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Map<String, Object>>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        String traceId = generateTraceId();
        logException(e, "路径不存在", traceId);
        Map<String, Object> errorData = buildErrorData(404, "请求路径不存在", traceId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(404, "请求路径不存在", errorData));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, Object>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String traceId = generateTraceId();
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = bindingResult.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logException(e, "参数验证异常", traceId);
        Map<String, Object> errorData = buildErrorData(400, errorMessage, traceId);
        return ResponseEntity.badRequest().body(Result.error(400, errorMessage, errorData));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<Result<Map<String, Object>>> handleRequestValidationException(Exception e) {
        String traceId = generateTraceId();
        log.warn("[请求参数异常] message={}, traceId={}, request={}",
                e.getMessage(), traceId, getRequestInfo());
        String message = "请求参数格式错误";
        Map<String, Object> errorData = buildErrorData(400, message, traceId);
        return ResponseEntity.badRequest().body(Result.error(400, message, errorData));
    }

    /**
     * 统一系统异常处理
     * 所有未捕获的异常统一处理，避免泄露内部错误信息
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Map<String, Object>>> handleException(Exception e) {
        String traceId = generateTraceId();
        logException(e, "系统异常", traceId);
        Map<String, Object> errorData = buildErrorData(500, "系统异常，请稍后重试", traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "系统异常，请稍后重试", errorData));
    }

    private HttpStatus resolveBusinessStatus(int code) {
        if (UNAUTHORIZED_CODES.contains(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (FORBIDDEN_CODES.contains(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (NOT_FOUND_CODES.contains(code)) {
            return HttpStatus.NOT_FOUND;
        }
        if (TIMEOUT_CODES.contains(code)) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (code == 1015 || code == 5001 || code == 5002 || code == 5006 || code == 5007
                || code == 5008 || code == 5009 || (code >= 11001 && code <= 12010)
                || (code >= 13001 && code <= 13006) || code == 20001 || code == 20006 || code == 20007) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if ((code >= 4000 && code <= 4999) || code == 400) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code == 1004 || code == 2003 || code == 2006 || code == 2007 || code == 4007) {
            return HttpStatus.CONFLICT;
        }
        if (code == 2005 || (code >= 5000 && code <= 5999) || code == 500) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
