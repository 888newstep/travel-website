package travel.common.exception;

/**
 * 消息载荷无法通过业务校验时使用，避免无意义地重复投递同一条毒消息。
 */
public class NonRetryableMessageException extends RuntimeException {

    public NonRetryableMessageException(String message) {
        super(message);
    }

    public NonRetryableMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
