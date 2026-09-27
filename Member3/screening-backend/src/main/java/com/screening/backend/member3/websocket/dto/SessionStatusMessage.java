package com.screening.backend.member3.websocket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.screening.backend.member3.model.SessionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * WebSocket broadcast message for session status transitions.
 * Destination: /topic/member3/session/{sessionId}/status
 */
public class SessionStatusMessage {

    private UUID sessionId;
    private SessionStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    public SessionStatusMessage() {
    }

    public SessionStatusMessage(UUID sessionId, SessionStatus status, Instant timestamp) {
        this.sessionId = sessionId;
        this.status = status;
        this.timestamp = timestamp;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
