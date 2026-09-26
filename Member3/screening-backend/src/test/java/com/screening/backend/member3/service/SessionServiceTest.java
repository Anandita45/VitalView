package com.screening.backend.member3.service;

import com.screening.backend.member3.ai.handler.ScreeningFinalizationHandler;
import com.screening.backend.member3.exception.FinalizationFailureException;
import com.screening.backend.member3.exception.InvalidSessionTransitionException;
import com.screening.backend.member3.exception.SessionNotFoundException;
import com.screening.backend.member3.exception.StabilizationNotCompleteException;
import com.screening.backend.member3.model.ScreeningSession;
import com.screening.backend.member3.model.SessionStatus;
import com.screening.backend.member3.repository.ScreeningSessionRepository;
import com.screening.backend.member3.websocket.publisher.SessionStatusPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private ScreeningSessionRepository sessionRepository;

    @Mock
    private SessionStatusPublisher statusPublisher;

    @Mock
    private ScreeningFinalizationHandler finalizationHandler;

    private Instant testNow;
    private Clock clock;
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        testNow = Instant.parse("2026-09-26T12:00:00Z");
        clock = Clock.fixed(testNow, ZoneId.of("UTC"));
        sessionService = new SessionService(sessionRepository, clock, statusPublisher, finalizationHandler);
    }

    private void advanceClockBy(Duration duration) {
        testNow = testNow.plus(duration);
        clock = Clock.fixed(testNow, ZoneId.of("UTC"));
        sessionService = new SessionService(sessionRepository, clock, statusPublisher, finalizationHandler);
    }

    @Test
    @DisplayName("1. Create session -> status is CREATED and createdAt is recorded")
    void testCreateSession() {
        when(sessionRepository.save(any(ScreeningSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScreeningSession created = sessionService.createSession();

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(SessionStatus.CREATED);
        assertThat(created.getCreatedAt()).isEqualTo(testNow);
        verify(sessionRepository).save(any(ScreeningSession.class));
    }

    @Test
    @DisplayName("2. CREATED -> INITIALIZING succeeds and publishes status")
    void testInitializeSessionSuccess() {
        UUID id = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(id, SessionStatus.CREATED, testNow);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ScreeningSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScreeningSession updated = sessionService.initializeSession(id);

        assertThat(updated.getStatus()).isEqualTo(SessionStatus.INITIALIZING);
        verify(sessionRepository).save(session);
        verify(statusPublisher).publishStatus(eq(id), eq(SessionStatus.INITIALIZING), any(Instant.class));
    }

    @Test
    @DisplayName("3. INITIALIZING -> STABILIZING succeeds and publishes status")
    void testStartStabilizationSuccess() {
        UUID id = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(id, SessionStatus.INITIALIZING, testNow);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ScreeningSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScreeningSession updated = sessionService.startStabilization(id);

        assertThat(updated.getStatus()).isEqualTo(SessionStatus.STABILIZING);
        verify(statusPublisher).publishStatus(eq(id), eq(SessionStatus.STABILIZING), any(Instant.class));
    }

    @Test
    @DisplayName("4. STABILIZING stores stabilizationStartedAt timestamp")
    void testStabilizingStoresTimestamp() {
        UUID id = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(id, SessionStatus.INITIALIZING, testNow);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ScreeningSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScreeningSession updated = sessionService.startStabilization(id);

        assertThat(updated.getStabilizationStartedAt()).isEqualTo(testNow);
    }

    @Test
    @DisplayName("5. Starting before 15 seconds fails with StabilizationNotCompleteException")
    void testStartBefore15SecondsFails() {
        UUID id = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(id, SessionStatus.STABILIZING, testNow);
        session.setStabilizationStartedAt(testNow);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));

        // Advance only 14 seconds
        advanceClockBy(Duration.ofSeconds(14));

        assertThatThrownBy(() -> sessionService.startScreening(id))
                .isInstanceOf(StabilizationNotCompleteException.class)
                .hasMessageContaining("The 15-second stabilization period has not completed");
    }

    @Test
    @DisplayName("6. Starting after 15 seconds succeeds")
    void testStartAfter15SecondsSucceeds() {
        UUID id = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(id, SessionStatus.STABILIZING, testNow);
        session.setStabilizationStartedAt(testNow);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ScreeningSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Advance 15 seconds
        advanceClockBy(Duration.ofSeconds(15));

        ScreeningSession updated = sessionService.startScreening(id);

        assertThat(updated.getStatus()).isEqualTo(SessionStatus.RUNNING);
        verify(statusPublisher).publishStatus(eq(id), eq(SessionStatus.RUNNING), any(Instant.class));
    }

    @Test
    @DisplayName("7. Successful start changes state to RUNNING and sets startedAt")
    void testSuccessfulStartSetsRunningAndTimestamp() {
        UUID id = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(id, SessionStatus.STABILIZING, testNow);
        session.setStabilizationStartedAt(testNow);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ScreeningSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        advanceClockBy(Duration.ofSeconds(20));

        ScreeningSession updated = sessionService.startScreening(id);

        assertThat(updated.getStatus()).isEqualTo(SessionStatus.RUNNING);
        assertThat(updated.getStartedAt()).isEqualTo(testNow);
    }

    @Test
    @DisplayName("8. Invalid transition fails (e.g. CREATED -> RUNNING)")
    void testInvalidTransitionFails() {
        UUID id = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(id, SessionStatus.CREATED, testNow);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.startScreening(id))
                .isInstanceOf(InvalidSessionTransitionException.class)
                .hasMessageContaining("Invalid session transition from CREATED to RUNNING");
    }

    @Test
    @DisplayName("9. Unknown session returns SessionNotFoundException")
    void testUnknownSessionThrowsException() {
        UUID unknownId = UUID.randomUUID();
        when(sessionRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getSession(unknownId))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining(unknownId.toString());

        assertThatThrownBy(() -> sessionService.initializeSession(unknownId))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    @DisplayName("10. COMPLETED -> RUNNING fails")
    void testCompletedToRunningFails() {
        UUID id = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(id, SessionStatus.COMPLETED, testNow);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.startScreening(id))
                .isInstanceOf(InvalidSessionTransitionException.class)
                .hasMessageContaining("Invalid session transition from COMPLETED to RUNNING");
    }

    @Test
    @DisplayName("11. FAILED -> RUNNING fails")
    void testFailedToRunningFails() {
        UUID id = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(id, SessionStatus.FAILED, testNow);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.startScreening(id))
                .isInstanceOf(InvalidSessionTransitionException.class)
                .hasMessageContaining("Invalid session transition from FAILED to RUNNING");
    }

    @Test
    @DisplayName("12. Finalization success: RUNNING -> FINALIZING -> COMPLETED")
    void testFinalizeSuccess() {
        UUID id = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(id, SessionStatus.RUNNING, testNow);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ScreeningSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(finalizationHandler.finalizeScreening(id)).thenReturn(true);

        ScreeningSession completed = sessionService.finalizeSession(id);

        assertThat(completed.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        verify(statusPublisher).publishStatus(eq(id), eq(SessionStatus.FINALIZING), any(Instant.class));
        verify(statusPublisher).publishStatus(eq(id), eq(SessionStatus.COMPLETED), any(Instant.class));
    }

    @Test
    @DisplayName("13. Finalization failure: RUNNING -> FINALIZING -> FAILED")
    void testFinalizeFailure() {
        UUID id = UUID.randomUUID();
        ScreeningSession session = new ScreeningSession(id, SessionStatus.RUNNING, testNow);
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ScreeningSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(finalizationHandler.finalizeScreening(id)).thenReturn(false);

        assertThatThrownBy(() -> sessionService.finalizeSession(id))
                .isInstanceOf(FinalizationFailureException.class);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.FAILED);
        verify(statusPublisher).publishStatus(eq(id), eq(SessionStatus.FINALIZING), any(Instant.class));
        verify(statusPublisher).publishStatus(eq(id), eq(SessionStatus.FAILED), any(Instant.class));
    }
}
