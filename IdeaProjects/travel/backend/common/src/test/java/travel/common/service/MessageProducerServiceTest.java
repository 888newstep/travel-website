package travel.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import travel.common.config.RabbitMQConfig;
import travel.common.vo.NotificationMessageVO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MessageProducerServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MqMessageStatusService mqMessageStatusService;

    @Test
    void shouldAttachCorrelationDataWhenPublishingNotification() {
        MessageProducerService service = new MessageProducerService(
                rabbitTemplate, mqMessageStatusService, new ObjectMapper(), false, true);

        service.sendNotification(7, "NOTICE", "title", "content");

        ArgumentCaptor<CorrelationData> correlationCaptor = ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.RELIABLE_NOTIFICATION_EXCHANGE),
                eq(RabbitMQConfig.RELIABLE_NOTIFICATION_ROUTING_KEY),
                any(NotificationMessageVO.class),
                any(MessagePostProcessor.class),
                correlationCaptor.capture());
        assertNotNull(correlationCaptor.getValue().getId());
    }

    @Test
    void shouldPersistStatusAndAttachMessageMetadataWhenEnabled() {
        MessageProducerService service = new MessageProducerService(
                rabbitTemplate, mqMessageStatusService, new ObjectMapper(), true, true);

        service.sendNotification(7, "NOTICE", "title", "content");

        ArgumentCaptor<MessagePostProcessor> postProcessorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        ArgumentCaptor<CorrelationData> correlationCaptor =
                ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.RELIABLE_NOTIFICATION_EXCHANGE),
                eq(RabbitMQConfig.RELIABLE_NOTIFICATION_ROUTING_KEY),
                any(NotificationMessageVO.class),
                postProcessorCaptor.capture(),
                correlationCaptor.capture());

        String messageId = correlationCaptor.getValue().getId();
        verify(mqMessageStatusService).createPending(
                eq(messageId),
                eq("NotificationMessageVO"),
                eq(RabbitMQConfig.RELIABLE_NOTIFICATION_EXCHANGE),
                eq(RabbitMQConfig.RELIABLE_NOTIFICATION_ROUTING_KEY),
                anyString());
        verify(mqMessageStatusService).markDispatched(messageId);

        Message processed = postProcessorCaptor.getValue()
                .postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertEquals(messageId, processed.getMessageProperties().getMessageId());
        assertEquals("NotificationMessageVO", processed.getMessageProperties().getType());
    }

    @Test
    void shouldMarkMessageFailedWhenRabbitTemplateRejectsPublish() {
        MessageProducerService service = new MessageProducerService(
                rabbitTemplate, mqMessageStatusService, new ObjectMapper(), true, true);
        AmqpException publishException = new AmqpException("publish failed");
        doThrow(publishException).when(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.RELIABLE_NOTIFICATION_EXCHANGE),
                eq(RabbitMQConfig.RELIABLE_NOTIFICATION_ROUTING_KEY),
                any(NotificationMessageVO.class),
                any(MessagePostProcessor.class),
                any(CorrelationData.class));

        service.sendNotification(7, "NOTICE", "title", "content");

        ArgumentCaptor<String> messageIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqMessageStatusService).createPending(
                messageIdCaptor.capture(),
                eq("NotificationMessageVO"),
                eq(RabbitMQConfig.RELIABLE_NOTIFICATION_EXCHANGE),
                eq(RabbitMQConfig.RELIABLE_NOTIFICATION_ROUTING_KEY),
                anyString());
        verify(mqMessageStatusService).markFailed(eq(messageIdCaptor.getValue()), same(publishException));
        verify(mqMessageStatusService, never()).markDispatched(anyString());
    }

    @Test
    void shouldNotFabricateFailedStatusWhenDispatchedUpdateFails() {
        MessageProducerService service = new MessageProducerService(
                rabbitTemplate, mqMessageStatusService, new ObjectMapper(), true, true);
        doThrow(new IllegalStateException("local mysql unavailable"))
                .when(mqMessageStatusService).markDispatched(anyString());

        service.sendNotification(7, "NOTICE", "title", "content");

        verify(mqMessageStatusService, never()).markFailed(anyString(), any(Throwable.class));
    }

    @Test
    void shouldPublishNotificationToReliableTopologyWithMessageMetadataWhenEnabled() {
        MessageProducerService service = new MessageProducerService(
                rabbitTemplate, mqMessageStatusService, new ObjectMapper(), false, true);

        service.sendNotification(7, "NOTICE", "title", "content");

        ArgumentCaptor<MessagePostProcessor> postProcessorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        ArgumentCaptor<CorrelationData> correlationCaptor =
                ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.RELIABLE_NOTIFICATION_EXCHANGE),
                eq(RabbitMQConfig.RELIABLE_NOTIFICATION_ROUTING_KEY),
                any(NotificationMessageVO.class),
                postProcessorCaptor.capture(),
                correlationCaptor.capture());

        String messageId = correlationCaptor.getValue().getId();
        assertNotNull(messageId);
        Message processed = postProcessorCaptor.getValue()
                .postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertEquals(messageId, processed.getMessageProperties().getMessageId());
        assertEquals("NotificationMessageVO", processed.getMessageProperties().getType());
        verify(mqMessageStatusService, never()).createPending(
                anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldSkipNotificationWhenReliableProducerIsDisabled() {
        MessageProducerService service = new MessageProducerService(rabbitTemplate);

        service.sendNotification(7, "NOTICE", "title", "content");

        verifyNoInteractions(rabbitTemplate);
    }
}
