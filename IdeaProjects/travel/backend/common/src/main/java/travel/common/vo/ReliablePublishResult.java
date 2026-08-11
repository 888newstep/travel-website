package travel.common.vo;

/**
 * 可靠转发的确认结果。只有 confirmed=true 时，消费者才允许确认原消息。
 */
public record ReliablePublishResult(boolean confirmed, String publishId, String failureReason) {

    public static ReliablePublishResult success(String publishId) {
        return new ReliablePublishResult(true, publishId, null);
    }

    public static ReliablePublishResult failure(String publishId, String failureReason) {
        return new ReliablePublishResult(false, publishId, failureReason);
    }
}
