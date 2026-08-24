package travel.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import travel.common.service.MqMessageStatusService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitMQConfigTest {

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private ObjectProvider<MqMessageStatusService> statusServiceProvider;

    @Mock
    private MqMessageStatusService statusService;

    @Test
    void shouldPersistConfirmAndNackResults() {
        when(statusServiceProvider.getIfAvailable()).thenReturn(statusService);
        RabbitTemplate template = new RabbitMQConfig()
                .rabbitTemplate(connectionFactory, statusServiceProvider, true);

        RabbitTemplate.ConfirmCallback callback = (RabbitTemplate.ConfirmCallback)
                ReflectionTestUtils.getField(template, "confirmCallback");
        assertNotNull(callback);

        callback.confirm(new CorrelationData("message-1"), true, null);
        callback.confirm(new CorrelationData("message-2"), false, "channel closed");

        verify(statusService).markConfirmed("message-1");
        verify(statusService).markFailed(eq("message-2"), any(Throwable.class));
    }

    @Test
    void shouldPersistReturnedMessageByMessagePropertyId() {
        when(statusServiceProvider.getIfAvailable()).thenReturn(statusService);
        RabbitTemplate template = new RabbitMQConfig()
                .rabbitTemplate(connectionFactory, statusServiceProvider, true);

        RabbitTemplate.ReturnsCallback callback = (RabbitTemplate.ReturnsCallback)
                ReflectionTestUtils.getField(template, "returnsCallback");
        assertNotNull(callback);

        MessageProperties properties = new MessageProperties();
        properties.setMessageId("message-returned");
        ReturnedMessage returned = new ReturnedMessage(
                new Message(new byte[0], properties),
                312,
                "NO_ROUTE",
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_ROUTING_KEY);

        callback.returnedMessage(returned);

        verify(statusService).markReturned(eq("message-returned"), any(Throwable.class));
    }

    @Test
    void shouldIsolateStatusStoreFailureInsideConfirmCallback() {
        when(statusServiceProvider.getIfAvailable()).thenReturn(statusService);
        doThrow(new IllegalStateException("mysql unavailable"))
                .when(statusService).markConfirmed("message-1");
        RabbitTemplate template = new RabbitMQConfig()
                .rabbitTemplate(connectionFactory, statusServiceProvider, true);
        RabbitTemplate.ConfirmCallback callback = (RabbitTemplate.ConfirmCallback)
                ReflectionTestUtils.getField(template, "confirmCallback");

        assertDoesNotThrow(() -> callback.confirm(new CorrelationData("message-1"), true, null));
        verify(statusService).markConfirmed("message-1");
    }

    @Test
    void shouldSkipStatusStoreWhenPersistenceIsDisabled() {
        RabbitTemplate template = new RabbitMQConfig()
                .rabbitTemplate(connectionFactory, statusServiceProvider, false);
        RabbitTemplate.ConfirmCallback callback = (RabbitTemplate.ConfirmCallback)
                ReflectionTestUtils.getField(template, "confirmCallback");

        callback.confirm(new CorrelationData("message-1"), true, null);

        verify(statusServiceProvider, never()).getIfAvailable();
        verifyNoInteractions(statusService);
    }
}
