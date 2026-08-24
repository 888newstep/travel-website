package travel.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import travel.common.entity.messaging.MqMessageStatusRecord;
import travel.common.mapper.messaging.MqMessageStatusMapper;

import java.time.LocalDateTime;
import java.util.List;
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
    public static final String RETRYING = "RETRYING";

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

    /**
     * 写入事务 Outbox。调用方应在业务事务内调用，保证业务数据与消息记录原子提交。
     */
    public void enqueue(String messageId, String messageType, String exchange,
                        String routingKey, String payloadJson) {
        createPending(messageId, messageType, exchange, routingKey, payloadJson);
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

    public List<MqMessageStatusRecord> findCompensationCandidates(
            LocalDateTime staleBefore,
            int maxRetryCount,
            int limit) {
        if (staleBefore == null) {
            throw new IllegalArgumentException("staleBefore不能为空");
        }
        validatePositive(maxRetryCount, "maxRetryCount");
        validatePositive(limit, "limit");
        return mapper.findCompensationCandidates(staleBefore, maxRetryCount, limit);
    }

    /**
     * 使用带状态条件的 UPDATE 抢占一条补偿记录，避免多实例同时重发同一消息。
     */
    public boolean claimForCompensation(
            Long id,
            LocalDateTime staleBefore,
            int maxRetryCount) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("消息状态记录 id 必须为正数");
        }
        if (staleBefore == null) {
            throw new IllegalArgumentException("staleBefore不能为空");
        }
        validatePositive(maxRetryCount, "maxRetryCount");
        return mapper.claimForCompensation(id, staleBefore, maxRetryCount) == 1;
    }

    public void markCompensationDispatched(Long id) {
        if (id == null || id <= 0) {
            log.warn("补偿消息状态更新缺少有效 id: id={}", id);
            return;
        }
        logTransition(String.valueOf(id), DISPATCHED, mapper.markCompensationDispatched(id));
    }

    public void markCompensationFailed(
            Long id,
            Throwable cause,
            LocalDateTime nextAttemptTime) {
        if (id == null || id <= 0) {
            log.warn("补偿消息失败状态更新缺少有效 id: id={}", id);
            return;
        }
        if (nextAttemptTime == null) {
            throw new IllegalArgumentException("nextAttemptTime不能为空");
        }
        mapper.markCompensationFailed(id, formatError(cause), nextAttemptTime);
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

    private void validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + "必须为正数");
        }
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
