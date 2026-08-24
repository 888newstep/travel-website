package travel.common.utils;

import travel.common.exception.BusinessException;
import travel.common.exception.DatabaseException;
import travel.common.exception.FileException;
import travel.common.exception.NetworkException;
import travel.common.exception.NotFoundException;
import travel.common.exception.RedisException;
import travel.common.exception.ServiceException;
import travel.common.exception.TimeoutException;
import travel.common.exception.UnauthorizedException;
import travel.common.exception.ValidationException;
import travel.common.enums.ErrorCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionUtil {

    private static final Logger log = LoggerFactory.getLogger(ExceptionUtil.class);

    /**
     * 抛出业务异常
     */
    public static void throwBusinessException(String message) {
        throw new BusinessException(400, message);
    }

    /**
     * 抛出业务异常（带错误码）
     */
    public static void throwBusinessException(int code, String message) {
        throw new BusinessException(code, message);
    }

    /**
     * 抛出业务异常（带错误码枚举）
     */
    public static void throwBusinessException(ErrorCodeEnum errorCodeEnum) {
        throw new BusinessException(errorCodeEnum);
    }

    /**
     * 抛出验证异常
     */
    public static void throwValidationException(String message) {
        throw new ValidationException(400, message);
    }

    /**
     * 抛出验证异常（带错误码）
     */
    public static void throwValidationException(int code, String message) {
        throw new ValidationException(code, message);
    }

    /**
     * 抛出验证异常（带错误码枚举）
     */
    public static void throwValidationException(ErrorCodeEnum errorCodeEnum) {
        throw new ValidationException(errorCodeEnum);
    }

    /**
     * 抛出资源不存在异常
     */
    public static void throwNotFoundException(String message) {
        throw new NotFoundException(404, message);
    }

    /**
     * 抛出资源不存在异常（带错误码）
     */
    public static void throwNotFoundException(int code, String message) {
        throw new NotFoundException(code, message);
    }

    /**
     * 抛出资源不存在异常（带错误码枚举）
     */
    public static void throwNotFoundException(ErrorCodeEnum errorCodeEnum) {
        throw new NotFoundException(errorCodeEnum);
    }

    /**
     * 抛出未授权异常
     */
    public static void throwUnauthorizedException(String message) {
        throw new UnauthorizedException(401, message);
    }

    /**
     * 抛出未授权异常（带错误码）
     */
    public static void throwUnauthorizedException(int code, String message) {
        throw new UnauthorizedException(code, message);
    }

    /**
     * 抛出未授权异常（带错误码枚举）
     */
    public static void throwUnauthorizedException(ErrorCodeEnum errorCodeEnum) {
        throw new UnauthorizedException(errorCodeEnum);
    }

    /**
     * 抛出服务异常
     */
    public static void throwServiceException(String message) {
        throw new ServiceException(500, message);
    }

    /**
     * 抛出服务异常（带错误码）
     */
    public static void throwServiceException(int code, String message) {
        throw new ServiceException(code, message);
    }

    /**
     * 抛出服务异常（带错误码枚举）
     */
    public static void throwServiceException(ErrorCodeEnum errorCodeEnum) {
        throw new ServiceException(errorCodeEnum);
    }

    /**
     * 抛出数据库异常
     */
    public static void throwDatabaseException(String message) {
        throw new DatabaseException(500, message);
    }

    /**
     * 抛出数据库异常（带错误码）
     */
    public static void throwDatabaseException(int code, String message) {
        throw new DatabaseException(code, message);
    }

    /**
     * 抛出数据库异常（带错误码枚举）
     */
    public static void throwDatabaseException(ErrorCodeEnum errorCodeEnum) {
        throw new DatabaseException(errorCodeEnum);
    }

    /**
     * 抛出Redis异常
     */
    public static void throwRedisException(String message) {
        throw new RedisException(500, message);
    }

    /**
     * 抛出Redis异常（带错误码）
     */
    public static void throwRedisException(int code, String message) {
        throw new RedisException(code, message);
    }

    /**
     * 抛出Redis异常（带错误码枚举）
     */
    public static void throwRedisException(ErrorCodeEnum errorCodeEnum) {
        throw new RedisException(errorCodeEnum);
    }

    /**
     * 抛出文件异常
     */
    public static void throwFileException(String message) {
        throw new FileException(400, message);
    }

    /**
     * 抛出文件异常（带错误码）
     */
    public static void throwFileException(int code, String message) {
        throw new FileException(code, message);
    }

    /**
     * 抛出文件异常（带错误码枚举）
     */
    public static void throwFileException(ErrorCodeEnum errorCodeEnum) {
        throw new FileException(errorCodeEnum);
    }

    /**
     * 抛出网络异常
     */
    public static void throwNetworkException(String message) {
        throw new NetworkException(500, message);
    }

    /**
     * 抛出网络异常（带错误码）
     */
    public static void throwNetworkException(int code, String message) {
        throw new NetworkException(code, message);
    }

    /**
     * 抛出网络异常（带错误码枚举）
     */
    public static void throwNetworkException(ErrorCodeEnum errorCodeEnum) {
        throw new NetworkException(errorCodeEnum);
    }

    /**
     * 抛出超时异常
     */
    public static void throwTimeoutException(String message) {
        throw new TimeoutException(504, message);
    }

    /**
     * 抛出超时异常（带错误码）
     */
    public static void throwTimeoutException(int code, String message) {
        throw new TimeoutException(code, message);
    }

    /**
     * 抛出超时异常（带错误码枚举）
     */
    public static void throwTimeoutException(ErrorCodeEnum errorCodeEnum) {
        throw new TimeoutException(errorCodeEnum);
    }

    /**
     * 处理异常并记录日志
     */
    public static void handleException(Exception e, String operation) {
        log.error("{}失败: {}", operation, e.getMessage(), e);
    }

    /**
     * 处理异常并记录日志（带额外信息）
     */
    public static void handleException(Exception e, String operation, String extraInfo) {
        log.error("{}失败: {}, 额外信息: {}", operation, e.getMessage(), extraInfo, e);
    }

    /**
     * 检查对象是否为空，为空则抛出异常
     */
    public static void checkNotNull(Object obj, String message) {
        if (obj == null) {
            throwNotFoundException(message);
        }
    }

    /**
     * 检查字符串是否为空，为空则抛出异常
     */
    public static void checkNotEmpty(String str, String message) {
        if (str == null || str.trim().isEmpty()) {
            throwValidationException(message);
        }
    }

    /**
     * 检查条件是否为真，为假则抛出异常
     */
    public static void checkCondition(boolean condition, String message) {
        if (!condition) {
            throwBusinessException(message);
        }
    }

    /**
     * 检查条件是否为真，为假则抛出异常（带错误码）
     */
    public static void checkCondition(boolean condition, int code, String message) {
        if (!condition) {
            throwBusinessException(code, message);
        }
    }

    /**
     * 检查条件是否为真，为假则抛出异常（带错误码枚举）
     */
    public static void checkCondition(boolean condition, ErrorCodeEnum errorCodeEnum) {
        if (!condition) {
            throwBusinessException(errorCodeEnum);
        }
    }
}
