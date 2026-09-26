package com.screening.backend.member3.exception;

import java.util.UUID;

/**
 * Thrown when a requested screening session does not exist.
 */
public class SessionNotFoundException extends RuntimeException {

    private final UUID sessionId;

    public SessionNotFoundException(UUID sessionId) {
        super("Screening session not found with ID: " + sessionId);
        this.sessionId = sessionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }
}
