package com.screening.backend.member3.integration;

import com.screening.backend.member3.ai.handler.MockScreeningFinalizationHandler;
import com.screening.backend.member3.model.ScreeningSession;
import com.screening.backend.member3.model.SessionStatus;
import com.screening.backend.member3.repository.ScreeningSessionRepository;
import com.screening.backend.member3.service.SessionService;
import com.screening.backend.member3.vital.dto.VitalIngestionRequest;
import com.screening.backend.member3.vital.service.VitalIngestionService;
import com.screening.backend.member3.websocket.dto.SessionStatusMessage;
import com.screening.backend.member3.websocket.dto.VitalReadingMessage;
import com.screening.backend.member3.websocket.publisher.SessionStatusPublisher;
import com.screening.backend.member3.websocket.publisher.VitalPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
class Member3EndToEndIntegrationTest {

    @Autowired
    private ScreeningSessionRepository sessionRepository;

    @Autowired
    private MockScreeningFinalizationHandler mockFinalizationHandler;

    @MockitoBean
    private SessionStatusPublisher statusPublisher;

    @MockitoBean
    private VitalPublisher vitalPublisher;

    private Instant currentInstant;
    private SessionService sessionService;
    private VitalIngestionService vitalIngestionService;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        currentInstant = Instant.parse("2026-09-26T14:00:00Z");
        Clock testClock = Clock.fixed(currentInstant, ZoneId.of("UTC"));

        mockFinalizationHandler.setShouldFail(false);

        sessionService = new SessionService(
                sessionRepository,
                testClock,
                statusPublisher,
                mockFinalizationHandler
        );

        com.screening.backend.member3.vital.service.VitalValidationService validationService =
                new com.screening.backend.member3.vital.service.VitalValidationService(testClock);

        vitalIngestionService = new VitalIngestionService(
                sessionRepository,
                validationService,
                vitalPublisher
        );
    }

    private void advanceTestTimeBy(Duration duration) {
        currentInstant = currentInstant.plus(duration);
        Clock updatedClock = Clock.fixed(currentInstant, ZoneId.of("UTC"));
        sessionService = new SessionService(
                sessionRepository,
                updatedClock,
                statusPublisher,
                mockFinalizationHandler
        );
        com.screening.backend.member3.vital.service.VitalValidationService validationService =
                new com.screening.backend.member3.vital.service.VitalValidationService(updatedClock);
        vitalIngestionService = new VitalIngestionService(
                sessionRepository,
                validationService,
                vitalPublisher
        );
    }

    @Test
    @DisplayName("Complete Member 3 Lifecycle: Create -> Initialize -> Stabilize -> 15s wait -> Start -> Vital Ingestion -> Finalize -> Complete")
    void testCompleteMember3Workflow() {
        // Step 1: Create session
        ScreeningSession session = sessionService.createSession();
        UUID sessionId = session.getId();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.CREATED);

        // Step 2: Initialize
        ScreeningSession initialized = sessionService.initializeSession(sessionId);
        assertThat(initialized.getStatus()).isEqualTo(SessionStatus.INITIALIZING);
        verify(statusPublisher, atLeastOnce()).publishStatus(eq(sessionId), eq(SessionStatus.INITIALIZING), any(Instant.class));

        // Step 3: Start stabilization
        ScreeningSession stabilizing = sessionService.startStabilization(sessionId);
        assertThat(stabilizing.getStatus()).isEqualTo(SessionStatus.STABILIZING);
        assertThat(stabilizing.getStabilizationStartedAt()).isEqualTo(currentInstant);
        verify(statusPublisher, atLeastOnce()).publishStatus(eq(sessionId), eq(SessionStatus.STABILIZING), any(Instant.class));

        // Step 4: Advance 15 seconds using test Clock (no Thread.sleep)
        advanceTestTimeBy(Duration.ofSeconds(15));

        // Step 5: Start screening
        ScreeningSession running = sessionService.startScreening(sessionId);
        assertThat(running.getStatus()).isEqualTo(SessionStatus.RUNNING);
        assertThat(running.getStartedAt()).isEqualTo(currentInstant);
        verify(statusPublisher, atLeastOnce()).publishStatus(eq(sessionId), eq(SessionStatus.RUNNING), any(Instant.class));

        // Step 6: Send vital packet from Python AI worker
        VitalIngestionRequest vitalPacket = new VitalIngestionRequest(
                currentInstant.toEpochMilli(),
                75.0,
                16.5,
                List.of(0.12, 0.19, 0.25, 0.18),
                0.97
        );
        long latencyMs = vitalIngestionService.ingestVital(sessionId, vitalPacket);
        assertThat(latencyMs).isGreaterThanOrEqualTo(0);

        // Step 7: Verify vital published to /topic/member3/session/{id}/vitals
        verify(vitalPublisher).publishVital(any(VitalReadingMessage.class));

        // Step 8 & 9: Finalize session (mock finalization completes)
        ScreeningSession completed = sessionService.finalizeSession(sessionId);

        // Step 10: Status becomes COMPLETED
        assertThat(completed.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();

        // Step 11: COMPLETED status is published
        verify(statusPublisher, atLeastOnce()).publishStatus(eq(sessionId), eq(SessionStatus.FINALIZING), any(Instant.class));
        verify(statusPublisher, atLeastOnce()).publishStatus(eq(sessionId), eq(SessionStatus.COMPLETED), any(Instant.class));
    }
}
