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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Service managing Member 3 screening session lifecycle, state transitions,
 * finalization orchestration, and real-time status publication over STOMP/WebSocket.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    public static final long STABILIZATION_DURATION_SECONDS = 15;

    private final ScreeningSessionRepository sessionRepository;
    private final Clock clock;
    private final SessionStatusPublisher statusPublisher;
    private final ScreeningFinalizationHandler finalizationHandler;

    public SessionService(ScreeningSessionRepository sessionRepository,
                          Clock clock,
                          SessionStatusPublisher statusPublisher,
                          ScreeningFinalizationHandler finalizationHandler) {
        this.sessionRepository = sessionRepository;
        this.clock = clock;
        this.statusPublisher = statusPublisher;
        this.finalizationHandler = finalizationHandler;
    }

    @Transactional
    public ScreeningSession createSession() {
        UUID id = UUID.randomUUID();
        Instant now = clock.instant();
        ScreeningSession session = new ScreeningSession(id, SessionStatus.CREATED, now);
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public ScreeningSession getSession(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new SessionNotFoundException(id));
    }

    @Transactional
    public ScreeningSession initializeSession(UUID id) {
        ScreeningSession session = getSession(id);
        if (session.getStatus() != SessionStatus.CREATED) {
            throw new InvalidSessionTransitionException(session.getStatus(), SessionStatus.INITIALIZING);
        }
        session.setStatus(SessionStatus.INITIALIZING);
        ScreeningSession saved = sessionRepository.save(session);
        statusPublisher.publishStatus(saved.getId(), saved.getStatus(), clock.instant());
        return saved;
    }

    @Transactional
    public ScreeningSession startStabilization(UUID id) {
        ScreeningSession session = getSession(id);
        if (session.getStatus() != SessionStatus.INITIALIZING) {
            throw new InvalidSessionTransitionException(session.getStatus(), SessionStatus.STABILIZING);
        }
        Instant now = clock.instant();
        session.setStabilizationStartedAt(now);
        session.setStatus(SessionStatus.STABILIZING);
        ScreeningSession saved = sessionRepository.save(session);
        statusPublisher.publishStatus(saved.getId(), saved.getStatus(), now);
        return saved;
    }

    @Transactional
    public ScreeningSession startScreening(UUID id) {
        ScreeningSession session = getSession(id);
        if (session.getStatus() != SessionStatus.STABILIZING) {
            throw new InvalidSessionTransitionException(session.getStatus(), SessionStatus.RUNNING);
        }

        Instant stabilizationStart = session.getStabilizationStartedAt();
        if (stabilizationStart == null) {
            throw new StabilizationNotCompleteException("Stabilization start time is missing.");
        }

        Instant now = clock.instant();
        long elapsedSeconds = Duration.between(stabilizationStart, now).toSeconds();
        if (elapsedSeconds < STABILIZATION_DURATION_SECONDS) {
            throw new StabilizationNotCompleteException(elapsedSeconds, STABILIZATION_DURATION_SECONDS);
        }

        session.setStatus(SessionStatus.RUNNING);
        session.setStartedAt(now);
        ScreeningSession saved = sessionRepository.save(session);
        statusPublisher.publishStatus(saved.getId(), saved.getStatus(), now);
        return saved;
    }

    @Transactional
    public ScreeningSession finalizeSession(UUID id) {
        ScreeningSession session = getSession(id);
        if (session.getStatus() != SessionStatus.RUNNING) {
            throw new InvalidSessionTransitionException(session.getStatus(), SessionStatus.FINALIZING);
        }

        // 1. Move to FINALIZING and publish
        Instant now = clock.instant();
        session.setStatus(SessionStatus.FINALIZING);
        session = sessionRepository.save(session);
        statusPublisher.publishStatus(session.getId(), session.getStatus(), now);

        // 2. Delegate to finalization handler
        boolean success = false;
        try {
            success = finalizationHandler != null && finalizationHandler.finalizeScreening(id);
        } catch (Exception ex) {
            log.error("Finalization handler encountered exception for session {}: {}", id, ex.getMessage(), ex);
            success = false;
        }

        if (success) {
            Instant completedTime = clock.instant();
            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(completedTime);
            ScreeningSession saved = sessionRepository.save(session);
            statusPublisher.publishStatus(saved.getId(), saved.getStatus(), completedTime);
            return saved;
        } else {
            Instant failedTime = clock.instant();
            session.setStatus(SessionStatus.FAILED);
            ScreeningSession saved = sessionRepository.save(session);
            statusPublisher.publishStatus(saved.getId(), saved.getStatus(), failedTime);
            throw new FinalizationFailureException(id, "Finalization failed for session: " + id);
        }
    }

    @Transactional
    public ScreeningSession failSession(UUID id) {
        ScreeningSession session = getSession(id);
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.FAILED) {
            throw new InvalidSessionTransitionException(session.getStatus(), SessionStatus.FAILED);
        }
        Instant now = clock.instant();
        session.setStatus(SessionStatus.FAILED);
        ScreeningSession saved = sessionRepository.save(session);
        statusPublisher.publishStatus(saved.getId(), saved.getStatus(), now);
        return saved;
    }
}
