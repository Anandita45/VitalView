package com.screening.backend.member3.model;

/**
 * Lifecycle states for a Member 3 screening session.
 */
public enum SessionStatus {
    CREATED,
    INITIALIZING,
    STABILIZING,
    RUNNING,
    FINALIZING,
    COMPLETED,
    FAILED
}
