package travel.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import travel.common.entity.messaging.MqMessageStatusRecord;
import travel.common.mapper.messaging.MqMessageStatusMapper;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqMessageStatusService {

    public static final String PENDING = "PENDING";
    public static final String DISPATCHED = "DISPATCHED";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String RETURNED = "RETURNED";
    public static final String FAILED = "FAILED";

    private final MqMessageStatusMapper mapper;

    public void createPending(String messageId, String messageType, String exchange,
                              String routingKey, String payloadJson) {
        MqMessageStatusRecord record = new MqMessageStatusRecord();
        record.setMessageId(requireText(messageId, "messageId"));
        record.setMessageType(requireText(messageType, "messageType"));
        record.setExchangeName(requireText(exchange, "exchange"));
        record.setRoutingKey(requireText(routingKey, "routingKey"));
        record.setPayloadJson(Objects.requireNonNull(payloadJson, "payloadJson不能为空"));
        record.setStatus(PENDING);
        record.setRetryCount(0);

        if (mapper.insert(record) != 1) {
            throw new IllegalStateException("消息状态记录创建失败: messageId=" + messageId);
        }
    }

    public void markDispatched(String messageId) {
        if (isBlank(messageId)) {
            log.warn("RabbitMQ dispatched callback 缺少 messageId");
            return;
        }
        logTransition(messageId, DISPATCHED, mapper.markDispatched(messageId));
    }

    public void markConfirmed(String messageId) {
        if (isBlank(messageId)) {
            log.warn("RabbitMQ confirm callback 缺少 messageId");
            return;
        }
        logTransition(messageId, CONFIRMED, mapper.markConfirmed(messageId));
    }

    public void markReturned(String messageId, Throwable cause) {
        if (messageId == null || messageId.isBlank()) {
            log.warn("RabbitMQ returned message 缺少 messageId");
            return;
        }
        logTransition(messageId, RETURNED, mapper.markReturned(messageId, formatError(cause)));
    }

    public void markFailed(String messageId, Throwable cause) {
        if (messageId == null || messageId.isBlank()) {
            return;
        }
        logTransition(messageId, FAILED, mapper.markFailed(messageId, formatError(cause)));
    }

    private void logTransition(String messageId, String targetStatus, int affectedRows) {
        if (affectedRows == 0) {
            log.debug("消息状态未迁移: messageId={}, targetStatus={}, 可能已被并发回调处理",
                    messageId, targetStatus);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String formatError(Throwable cause) {
        if (cause == null) {
            return "unknown broker failure";
        }
        String detail = cause.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = cause.getClass().getSimpleName();
        } else {
            detail = cause.getClass().getSimpleName() + ": " + detail;
        }
        return detail.length() > 1000 ? detail.substring(0, 1000) : detail;
    }
}
