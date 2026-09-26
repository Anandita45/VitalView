package com.screening.backend.member3.websocket;

import com.screening.backend.member3.model.SessionStatus;
import com.screening.backend.member3.websocket.dto.SessionStatusMessage;
import com.screening.backend.member3.websocket.dto.VitalReadingMessage;
import com.screening.backend.member3.websocket.publisher.SessionStatusPublisher;
import com.screening.backend.member3.websocket.publisher.VitalPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SessionStatusPublisher statusPublisher;

    @InjectMocks
    private VitalPublisher vitalPublisher;

    @Test
    @DisplayName("Status publisher sends to correct destination /topic/member3/session/{sessionId}/status")
    void testStatusDestination() {
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-26T15:00:00Z");

        statusPublisher.publishStatus(sessionId, SessionStatus.RUNNING, now);

        String expectedDestination = "/topic/member3/session/" + sessionId + "/status";
        ArgumentCaptor<SessionStatusMessage> captor = ArgumentCaptor.forClass(SessionStatusMessage.class);

        verify(messagingTemplate).convertAndSend(eq(expectedDestination), captor.capture());
        SessionStatusMessage message = captor.getValue();
        assertThat(message.getSessionId()).isEqualTo(sessionId);
        assertThat(message.getStatus()).isEqualTo(SessionStatus.RUNNING);
        assertThat(message.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("Vital publisher sends to correct destination /topic/member3/session/{sessionId}/vitals")
    void testVitalDestination() {
        UUID sessionId = UUID.randomUUID();
        Long timestamp = 1727351234000L;
        List<Double> waveform = List.of(0.12, 0.18, 0.23);

        vitalPublisher.publishVital(sessionId, timestamp, 74.5, 16.2, waveform, 0.96);

        String expectedDestination = "/topic/member3/session/" + sessionId + "/vitals";
        ArgumentCaptor<VitalReadingMessage> captor = ArgumentCaptor.forClass(VitalReadingMessage.class);

        verify(messagingTemplate).convertAndSend(eq(expectedDestination), captor.capture());
        VitalReadingMessage message = captor.getValue();
        assertThat(message.getSessionId()).isEqualTo(sessionId);
        assertThat(message.getTimestamp()).isEqualTo(timestamp);
        assertThat(message.getBpm()).isEqualTo(74.5);
        assertThat(message.getRespiration()).isEqualTo(16.2);
        assertThat(message.getWaveform()).containsExactly(0.12, 0.18, 0.23);
        assertThat(message.getSignalQuality()).isEqualTo(0.96);
    }

    @Test
    @DisplayName("Session A publishes to session A's destination, never session B or global destination")
    void testSessionIsolation() {
        UUID sessionA = UUID.randomUUID();
        UUID sessionB = UUID.randomUUID();

        vitalPublisher.publishVital(sessionA, 1000L, 70.0, 15.0, List.of(0.1), 0.9);

        String destA = "/topic/member3/session/" + sessionA + "/vitals";
        String destB = "/topic/member3/session/" + sessionB + "/vitals";

        verify(messagingTemplate).convertAndSend(eq(destA), org.mockito.ArgumentMatchers.any(VitalReadingMessage.class));
        org.mockito.Mockito.verify(messagingTemplate, org.mockito.Mockito.never()).convertAndSend(eq(destB), org.mockito.ArgumentMatchers.any(Object.class));
        org.mockito.Mockito.verify(messagingTemplate, org.mockito.Mockito.never()).convertAndSend(eq("/topic/vitals"), org.mockito.ArgumentMatchers.any(Object.class));
    }
}
