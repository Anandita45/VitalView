package com.screening.backend.member3.exception;

import java.util.UUID;

/**
 * Thrown when finalization orchestration with the AI worker or report service fails.
 */
public class FinalizationFailureException extends RuntimeException {

    private final UUID sessionId;

    public FinalizationFailureException(UUID sessionId) {
        super("Finalization orchestration failed for session ID: " + sessionId);
        this.sessionId = sessionId;
    }

    public FinalizationFailureException(UUID sessionId, String message) {
        super(message);
        this.sessionId = sessionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }
}
