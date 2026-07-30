package travel.common.utils;

import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ErrorUtil {

    /**
     * 创建业务异常
     */
    public static BusinessException createBusinessException(String message) {
        return new BusinessException(400, message);
    }

    /**
     * 创建业务异常（指定错误码）
     */
    public static BusinessException createBusinessException(int code, String message) {
        return new BusinessException(code, message);
    }

    /**
     * 创建业务异常（使用错误码枚举）
     */
    public static BusinessException createBusinessException(ErrorCodeEnum errorCode) {
        return new BusinessException(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 创建验证异常
     */
    public static ValidationException createValidationException(String message) {
        return new ValidationException(400, message);
    }

    /**
     * 创建验证异常（指定错误码）
     */
    public static ValidationException createValidationException(int code, String message) {
        return new ValidationException(code, message);
    }

    /**
     * 创建验证异常（使用错误码枚举）
     */
    public static ValidationException createValidationException(ErrorCodeEnum errorCode) {
        return new ValidationException(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 创建未找到异常
     */
    public static NotFoundException createNotFoundException(String message) {
        return new NotFoundException(404, message);
    }

    /**
     * 创建未找到异常（指定错误码）
     */
    public static NotFoundException createNotFoundException(int code, String message) {
        return new NotFoundException(code, message);
    }

    /**
     * 创建未找到异常（使用错误码枚举）
     */
    public static NotFoundException createNotFoundException(ErrorCodeEnum errorCode) {
        return new NotFoundException(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 创建未授权异常
     */
    public static UnauthorizedException createUnauthorizedException(String message) {
        return new UnauthorizedException(401, message);
    }

    /**
     * 创建未授权异常（指定错误码）
     */
    public static UnauthorizedException createUnauthorizedException(int code, String message) {
        return new UnauthorizedException(code, message);
    }

    /**
     * 创建未授权异常（使用错误码枚举）
     */
    public static UnauthorizedException createUnauthorizedException(ErrorCodeEnum errorCode) {
        return new UnauthorizedException(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 创建禁止访问异常
     */
    public static ForbiddenException createForbiddenException(String message) {
        return new ForbiddenException(403, message);
    }

    /**
     * 创建禁止访问异常（指定错误码）
     */
    public static ForbiddenException createForbiddenException(int code, String message) {
        return new ForbiddenException(code, message);
    }

    /**
     * 创建禁止访问异常（使用错误码枚举）
     */
    public static ForbiddenException createForbiddenException(ErrorCodeEnum errorCode) {
        return new ForbiddenException(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 创建超时异常
     */
    public static TimeoutException createTimeoutException(String message) {
        return new TimeoutException(504, message);
    }

    /**
     * 创建超时异常（指定错误码）
     */
    public static TimeoutException createTimeoutException(int code, String message) {
        return new TimeoutException(code, message);
    }

    /**
     * 创建超时异常（使用错误码枚举）
     */
    public static TimeoutException createTimeoutException(ErrorCodeEnum errorCode) {
        return new TimeoutException(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 创建服务异常
     */
    public static ServiceException createServiceException(String message) {
        return new ServiceException(500, message);
    }

    /**
     * 创建服务异常（指定错误码）
     */
    public static ServiceException createServiceException(int code, String message) {
        return new ServiceException(code, message);
    }

    /**
     * 创建服务异常（使用错误码枚举）
     */
    public static ServiceException createServiceException(ErrorCodeEnum errorCode) {
        return new ServiceException(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 创建数据库异常
     */
    public static DatabaseException createDatabaseException(String message) {
        return new DatabaseException(500, message);
    }

    /**
     * 创建数据库异常（指定错误码）
     */
    public static DatabaseException createDatabaseException(int code, String message) {
        return new DatabaseException(code, message);
    }

    /**
     * 创建数据库异常（使用错误码枚举）
     */
    public static DatabaseException createDatabaseException(ErrorCodeEnum errorCode) {
        return new DatabaseException(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 创建Redis异常
     */
    public static RedisException createRedisException(String message) {
        return new RedisException(500, message);
    }

    /**
     * 创建Redis异常（指定错误码）
     */
    public static RedisException createRedisException(int code, String message) {
        return new RedisException(code, message);
    }

    /**
     * 创建Redis异常（使用错误码枚举）
     */
    public static RedisException createRedisException(ErrorCodeEnum errorCode) {
        return new RedisException(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 创建文件异常
     */
    public static FileException createFileException(String message) {
        return new FileException(400, message);
    }

    /**
     * 创建文件异常（指定错误码）
     */
    public static FileException createFileException(int code, String message) {
        return new FileException(code, message);
    }

    /**
     * 创建文件异常（使用错误码枚举）
     */
    public static FileException createFileException(ErrorCodeEnum errorCode) {
        return new FileException(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 创建网络异常
     */
    public static NetworkException createNetworkException(String message) {
        return new NetworkException(500, message);
    }

    /**
     * 创建网络异常（指定错误码）
     */
    public static NetworkException createNetworkException(int code, String message) {
        return new NetworkException(code, message);
    }

    /**
     * 创建网络异常（使用错误码枚举）
     */
    public static NetworkException createNetworkException(ErrorCodeEnum errorCode) {
        return new NetworkException(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 构建错误响应数据
     */
    public static Map<String, Object> buildErrorResponse(int code, String message, String traceId) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", code);
        errorResponse.put("message", message);
        errorResponse.put("traceId", traceId);
        errorResponse.put("timestamp", new Date());
        errorResponse.put("request", LogUtil.getRequestInfo());
        return errorResponse;
    }

    /**
     * 检查对象是否为null
     */
    public static void checkNotNull(Object object, String message) {
        if (object == null) {
            throw createNotFoundException(message);
        }
    }

    /**
     * 检查条件是否为true
     */
    public static void checkCondition(boolean condition, String message) {
        if (!condition) {
            throw createValidationException(message);
        }
    }

    /**
     * 检查条件是否为true（使用错误码枚举）
     */
    public static void checkCondition(boolean condition, ErrorCodeEnum errorCode) {
        if (!condition) {
            throw createValidationException(errorCode);
        }
    }

    /**
     * 检查参数是否为null或空
     */
    public static void checkParamNotNull(Object param, String paramName) {
        if (param == null) {
            throw createValidationException("参数 " + paramName + " 不能为空");
        }
        if (param instanceof String && ((String) param).trim().isEmpty()) {
            throw createValidationException("参数 " + paramName + " 不能为空");
        }
    }

    /**
     * 检查参数是否在指定范围内
     */
    public static void checkParamRange(int param, int min, int max, String paramName) {
        if (param < min || param > max) {
            throw createValidationException("参数 " + paramName + " 必须在 " + min + " 到 " + max + " 之间");
        }
    }

    /**
     * 检查参数是否在指定范围内
     */
    public static void checkParamRange(long param, long min, long max, String paramName) {
        if (param < min || param > max) {
            throw createValidationException("参数 " + paramName + " 必须在 " + min + " 到 " + max + " 之间");
        }
    }

    /**
     * 检查参数是否在指定范围内
     */
    public static void checkParamRange(double param, double min, double max, String paramName) {
        if (param < min || param > max) {
            throw createValidationException("参数 " + paramName + " 必须在 " + min + " 到 " + max + " 之间");
        }
    }
}