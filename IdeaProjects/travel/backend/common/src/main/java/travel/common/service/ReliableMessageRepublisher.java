package travel.common.service;

import org.springframework.amqp.core.Message;
import travel.common.vo.ReliablePublishResult;

/**
 * 将消费失败的原始消息转发到重试队列或死信队列。
 */
public interface ReliableMessageRepublisher {

    ReliablePublishResult publishRetry(Message sourceMessage, int retryCount, String reason);

    ReliablePublishResult publishDeadLetter(Message sourceMessage, String reason);
}
