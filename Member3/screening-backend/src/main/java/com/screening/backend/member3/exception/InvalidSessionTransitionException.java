package com.screening.backend.member3.exception;

import com.screening.backend.member3.model.SessionStatus;

/**
 * Thrown when an illegal state transition is attempted on a session.
 */
public class InvalidSessionTransitionException extends RuntimeException {

    private final SessionStatus currentStatus;
    private final SessionStatus targetStatus;

    public InvalidSessionTransitionException(SessionStatus currentStatus, SessionStatus targetStatus) {
        super(String.format("Invalid session transition from %s to %s.", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public InvalidSessionTransitionException(String message) {
        super(message);
        this.currentStatus = null;
        this.targetStatus = null;
    }

    public SessionStatus getCurrentStatus() {
        return currentStatus;
    }

    public SessionStatus getTargetStatus() {
        return targetStatus;
    }
}
