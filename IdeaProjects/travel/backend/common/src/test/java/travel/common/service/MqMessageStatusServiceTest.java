package travel.common.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.messaging.MqMessageStatusRecord;
import travel.common.mapper.messaging.MqMessageStatusMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqMessageStatusServiceTest {

    @Mock
    private MqMessageStatusMapper mapper;

    @Test
    void shouldCreatePendingRecordWithPublishMetadata() {
        when(mapper.insert(any(MqMessageStatusRecord.class))).thenReturn(1);
        MqMessageStatusService service = new MqMessageStatusService(mapper);

        service.createPending("message-1", "NotificationMessageVO",
                "notification.exchange", "notification.send", "{}");

        ArgumentCaptor<MqMessageStatusRecord> captor = ArgumentCaptor.forClass(MqMessageStatusRecord.class);
        verify(mapper).insert(captor.capture());
        MqMessageStatusRecord record = captor.getValue();
        assertEquals("message-1", record.getMessageId());
        assertEquals("NotificationMessageVO", record.getMessageType());
        assertEquals("notification.exchange", record.getExchangeName());
        assertEquals("notification.send", record.getRoutingKey());
        assertEquals("{}", record.getPayloadJson());
        assertEquals(MqMessageStatusService.PENDING, record.getStatus());
        assertEquals(0, record.getRetryCount());
    }

    @Test
    void shouldRejectBlankMessageIdBeforeWritingRecord() {
        MqMessageStatusService service = new MqMessageStatusService(mapper);

        assertThrows(IllegalArgumentException.class,
                () -> service.createPending(" ", "type", "exchange", "routing", "{}"));
        verify(mapper, never()).insert(any(MqMessageStatusRecord.class));
    }

    @Test
    void shouldRejectNullPayloadBeforeWritingRecord() {
        MqMessageStatusService service = new MqMessageStatusService(mapper);

        assertThrows(NullPointerException.class,
                () -> service.createPending("message-1", "type", "exchange", "routing", null));
        verify(mapper, never()).insert(any(MqMessageStatusRecord.class));
    }

    @Test
    void shouldDelegateConditionalStatusTransitions() {
        when(mapper.markDispatched("message-1")).thenReturn(1);
        when(mapper.markConfirmed("message-1")).thenReturn(1);
        when(mapper.markReturned(eq("message-1"), anyString())).thenReturn(1);
        when(mapper.markFailed(eq("message-1"), anyString())).thenReturn(1);
        MqMessageStatusService service = new MqMessageStatusService(mapper);

        service.markDispatched("message-1");
        service.markConfirmed("message-1");
        service.markReturned("message-1", new IllegalStateException("returned"));
        service.markFailed("message-1", new IllegalStateException("failed"));

        verify(mapper).markDispatched("message-1");
        verify(mapper).markConfirmed("message-1");
        verify(mapper).markReturned(eq("message-1"), eq("IllegalStateException: returned"));
        verify(mapper).markFailed(eq("message-1"), eq("IllegalStateException: failed"));
    }

    @Test
    void shouldTruncateLongBrokerErrorBeforePersisting() {
        String longMessage = "x".repeat(1200);
        when(mapper.markFailed(eq("message-1"), anyString())).thenReturn(1);
        MqMessageStatusService service = new MqMessageStatusService(mapper);

        service.markFailed("message-1", new IllegalStateException(longMessage));

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(mapper).markFailed(eq("message-1"), errorCaptor.capture());
        assertEquals(1000, errorCaptor.getValue().length());
    }

    @Test
    void shouldIgnoreBlankMessageIdFromBrokerCallback() {
        MqMessageStatusService service = new MqMessageStatusService(mapper);

        service.markReturned(" ", new IllegalStateException("returned"));
        service.markFailed(null, new IllegalStateException("failed"));

        verify(mapper, never()).markReturned(anyString(), anyString());
        verify(mapper, never()).markFailed(anyString(), anyString());
    }

    @Test
    void shouldTreatZeroAffectedRowsAsAnIdempotentNoOp() {
        when(mapper.markConfirmed("message-1")).thenReturn(0);
        MqMessageStatusService service = new MqMessageStatusService(mapper);

        service.markConfirmed("message-1");

        verify(mapper).markConfirmed("message-1");
    }
}
