package com.screening.backend.member3.vital.service;

import com.screening.backend.member3.exception.InvalidSessionStateException;
import com.screening.backend.member3.exception.SessionNotFoundException;
import com.screening.backend.member3.model.ScreeningSession;
import com.screening.backend.member3.model.SessionStatus;
import com.screening.backend.member3.repository.ScreeningSessionRepository;
import com.screening.backend.member3.vital.dto.VitalIngestionRequest;
import com.screening.backend.member3.websocket.dto.VitalReadingMessage;
import com.screening.backend.member3.websocket.publisher.VitalPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VitalIngestionServiceTest {

    @Mock
    private ScreeningSessionRepository sessionRepository;

    @Mock
    private VitalValidationService validationService;

    @Mock
    private VitalPublisher vitalPublisher;

    private VitalIngestionService vitalIngestionService;

    @BeforeEach
    void setUp() {
        vitalIngestionService = new VitalIngestionService(sessionRepository, validationService, vitalPublisher);
    }

    private VitalIngestionRequest createSampleRequest() {
        return new VitalIngestionRequest(
                1727351234000L,
                74.5,
                16.2,
                List.of(0.12, 0.18, 0.23),
                0.96
        );
    }

    @Test
    @DisplayName("Valid vital accepted when session status is RUNNING")
    void testIngestVitalInRunningSession() {
        UUID sessionId = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(sessionId, SessionStatus.RUNNING, Instant.now());
        VitalIngestionRequest request = createSampleRequest();

        doNothing().when(validationService).validate(request);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        long latency = vitalIngestionService.ingestVital(sessionId, request);

        assertThat(latency).isGreaterThanOrEqualTo(0);
        ArgumentCaptor<VitalReadingMessage> captor = ArgumentCaptor.forClass(VitalReadingMessage.class);
        verify(vitalPublisher).publishVital(captor.capture());

        VitalReadingMessage published = captor.getValue();
        assertThat(published.getSessionId()).isEqualTo(sessionId);
        assertThat(published.getBpm()).isEqualTo(74.5);
        assertThat(published.getRespiration()).isEqualTo(16.2);
    }

    @Test
    @DisplayName("Valid vital accepted when session status is STABILIZING")
    void testIngestVitalInStabilizingSession() {
        UUID sessionId = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(sessionId, SessionStatus.STABILIZING, Instant.now());
        VitalIngestionRequest request = createSampleRequest();

        doNothing().when(validationService).validate(request);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        long latency = vitalIngestionService.ingestVital(sessionId, request);

        assertThat(latency).isGreaterThanOrEqualTo(0);
        verify(vitalPublisher).publishVital(any(VitalReadingMessage.class));
    }

    @Test
    @DisplayName("Unknown session throws SessionNotFoundException")
    void testUnknownSessionThrows() {
        UUID unknownId = UUID.randomUUID();
        VitalIngestionRequest request = createSampleRequest();

        doNothing().when(validationService).validate(request);
        when(sessionRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vitalIngestionService.ingestVital(unknownId, request))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    @DisplayName("Session in COMPLETED status rejects vital ingestion with InvalidSessionStateException")
    void testCompletedSessionRejectsVital() {
        UUID sessionId = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(sessionId, SessionStatus.COMPLETED, Instant.now());
        VitalIngestionRequest request = createSampleRequest();

        doNothing().when(validationService).validate(request);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> vitalIngestionService.ingestVital(sessionId, request))
                .isInstanceOf(InvalidSessionStateException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    @DisplayName("Session in FAILED status rejects vital ingestion with InvalidSessionStateException")
    void testFailedSessionRejectsVital() {
        UUID sessionId = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(sessionId, SessionStatus.FAILED, Instant.now());
        VitalIngestionRequest request = createSampleRequest();

        doNothing().when(validationService).validate(request);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> vitalIngestionService.ingestVital(sessionId, request))
                .isInstanceOf(InvalidSessionStateException.class)
                .hasMessageContaining("FAILED");
    }

    @Test
    @DisplayName("Session in CREATED status rejects vital ingestion with InvalidSessionStateException")
    void testCreatedSessionRejectsVital() {
        UUID sessionId = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(sessionId, SessionStatus.CREATED, Instant.now());
        VitalIngestionRequest request = createSampleRequest();

        doNothing().when(validationService).validate(request);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> vitalIngestionService.ingestVital(sessionId, request))
                .isInstanceOf(InvalidSessionStateException.class)
                .hasMessageContaining("CREATED");
    }
}
