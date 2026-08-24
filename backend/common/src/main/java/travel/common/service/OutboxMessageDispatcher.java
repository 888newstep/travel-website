package travel.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import travel.common.entity.messaging.MqMessageStatusRecord;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Outbox 投递器：数据库事务先落盘，定时任务再将消息投递到 RabbitMQ。
 *
 * <p>多实例场景通过数据库条件更新抢占记录；即使 broker 已接收但本地超时，
 * 也允许重复投递，消费端必须继续依靠业务幂等。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mq.outbox.enabled", havingValue = "true")
public class OutboxMessageDispatcher {

    private final RabbitTemplate rabbitTemplate;
    private final MqMessageStatusService statusService;
    private final int batchSize;
    private final int maxAttempts;
    private final long staleAfterSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public OutboxMessageDispatcher(
            RabbitTemplate rabbitTemplate,
            MqMessageStatusService statusService,
            @Value("${mq.outbox.batch-size:50}") int batchSize,
            @Value("${mq.outbox.max-attempts:5}") int maxAttempts,
            @Value("${mq.outbox.stale-after-seconds:60}") long staleAfterSeconds) {
        if (rabbitTemplate == null || statusService == null) {
            throw new IllegalArgumentException("outbox dependencies cannot be null");
        }
        if (batchSize < 1 || batchSize > 500 || maxAttempts < 1 || maxAttempts > 20
                || staleAfterSeconds < 1) {
            throw new IllegalArgumentException("invalid outbox dispatcher configuration");
        }
        this.rabbitTemplate = rabbitTemplate;
        this.statusService = statusService;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.staleAfterSeconds = staleAfterSeconds;
    }

    @Scheduled(fixedDelayString = "${mq.outbox.fixed-delay-ms:30000}")
    public void dispatchPendingMessages() {
        if (!running.compareAndSet(false, true)) {
            log.debug("上一次 Outbox 投递尚未结束，跳过本轮扫描");
            return;
        }
        try {
            LocalDateTime staleBefore = LocalDateTime.now().minusSeconds(staleAfterSeconds);
            List<MqMessageStatusRecord> candidates = statusService.findCompensationCandidates(
                    staleBefore, maxAttempts, batchSize);
            for (MqMessageStatusRecord candidate : candidates) {
                dispatchOne(candidate, staleBefore);
            }
        } catch (Exception exception) {
            log.error("Outbox 扫描失败", exception);
        } finally {
            running.set(false);
        }
    }

    private void dispatchOne(MqMessageStatusRecord candidate, LocalDateTime staleBefore) {
        if (candidate == null || candidate.getId() == null) {
            log.warn("忽略无效 Outbox 记录");
            return;
        }
        if (!statusService.claimForCompensation(candidate.getId(), staleBefore, maxAttempts)) {
            return;
        }

        try {
            String payload = candidate.getPayloadJson();
            if (payload == null || payload.isBlank()) {
                throw new IllegalStateException("Outbox payload cannot be blank");
            }
            Message message = MessageBuilder
                    .withBody(payload.getBytes(StandardCharsets.UTF_8))
                    .setMessageId(candidate.getMessageId())
                    .setType(candidate.getMessageType())
                    .setContentType("application/json")
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();
            rabbitTemplate.send(
                    candidate.getExchangeName(),
                    candidate.getRoutingKey(),
                    message,
                    new CorrelationData(candidate.getMessageId()));
            statusService.markCompensationDispatched(candidate.getId());
            log.debug("Outbox 消息已提交 RabbitMQ: id={}, messageId={}",
                    candidate.getId(), candidate.getMessageId());
        } catch (Exception exception) {
            LocalDateTime nextAttempt = LocalDateTime.now().plusSeconds(backoffSeconds(candidate.getRetryCount()));
            statusService.markCompensationFailed(candidate.getId(), exception, nextAttempt);
            log.warn("Outbox 消息投递失败，将重试: id={}, messageId={}",
                    candidate.getId(), candidate.getMessageId(), exception);
        }
    }

    private long backoffSeconds(Integer retryCount) {
        int attempt = retryCount == null ? 0 : Math.max(0, Math.min(retryCount, 10));
        return Math.min(900L, 30L * (1L << attempt));
    }
}
