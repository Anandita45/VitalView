package com.screening.backend.member3.vital.service;

import com.screening.backend.member3.exception.InvalidSessionStateException;
import com.screening.backend.member3.exception.SessionNotFoundException;
import com.screening.backend.member3.model.ScreeningSession;
import com.screening.backend.member3.model.SessionStatus;
import com.screening.backend.member3.repository.ScreeningSessionRepository;
import com.screening.backend.member3.vital.dto.VitalIngestionRequest;
import com.screening.backend.member3.websocket.dto.VitalReadingMessage;
import com.screening.backend.member3.websocket.publisher.VitalPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service managing real-time vital ingestion from Python AI worker,
 * fast validation, and non-blocking STOMP broadcasting.
 */
@Service
public class VitalIngestionService {

    private static final Logger log = LoggerFactory.getLogger(VitalIngestionService.class);

    private final ScreeningSessionRepository sessionRepository;
    private final VitalValidationService validationService;
    private final VitalPublisher vitalPublisher;

    public VitalIngestionService(ScreeningSessionRepository sessionRepository,
                                 VitalValidationService validationService,
                                 VitalPublisher vitalPublisher) {
        this.sessionRepository = sessionRepository;
        this.validationService = validationService;
        this.vitalPublisher = vitalPublisher;
    }

    /**
     * Ingests, validates, and streams vital readings to STOMP clients.
     *
     * @param sessionId the target screening session ID
     * @param request the vital readings payload
     * @return processing latency in milliseconds (publishedAt - receivedAt)
     */
    public long ingestVital(UUID sessionId, VitalIngestionRequest request) {
        long receivedAt = System.currentTimeMillis();

        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID must not be null.");
        }

        // 1. Lightweight validation
        validationService.validate(request);

        // 2. Session verification
        ScreeningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        SessionStatus currentStatus = session.getStatus();
        if (currentStatus != SessionStatus.STABILIZING && currentStatus != SessionStatus.RUNNING) {
            throw new InvalidSessionStateException(
                    sessionId,
                    currentStatus,
                    String.format("Vital ingestion is only allowed when session is STABILIZING or RUNNING. Current status: %s", currentStatus)
            );
        }

        // 3. Create broadcast message
        VitalReadingMessage message = new VitalReadingMessage(
                sessionId,
                request.getTimestamp(),
                request.getBpm(),
                request.getRespiration(),
                request.getWaveform(),
                request.getSignalQuality()
        );

        // 4. Publish to STOMP destination
        vitalPublisher.publishVital(message);

        // 5. Measure and record processing latency
        long publishedAt = System.currentTimeMillis();
        long processingLatencyMs = publishedAt - receivedAt;

        log.trace("Ingested and published vitals for session {} in {} ms", sessionId, processingLatencyMs);
        return processingLatencyMs;
    }
}
