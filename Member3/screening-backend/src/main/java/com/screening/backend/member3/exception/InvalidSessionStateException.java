package com.screening.backend.member3.exception;

import com.screening.backend.member3.model.SessionStatus;

import java.util.UUID;

/**
 * Thrown when an operation is requested on a session that is in an inappropriate lifecycle state.
 */
public class InvalidSessionStateException extends RuntimeException {

    private final UUID sessionId;
    private final SessionStatus currentStatus;

    public InvalidSessionStateException(UUID sessionId, SessionStatus currentStatus, String message) {
        super(message);
        this.sessionId = sessionId;
        this.currentStatus = currentStatus;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public SessionStatus getCurrentStatus() {
        return currentStatus;
    }
}
