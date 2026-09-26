package com.screening.backend.member3.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.screening.backend.member3.model.ScreeningSession;
import com.screening.backend.member3.model.SessionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Client response DTO representing a screening session.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScreeningSessionResponse {

    private UUID id;
    private SessionStatus status;
    private Instant createdAt;
    private Instant stabilizationStartedAt;
    private Instant startedAt;
    private Instant completedAt;

    public ScreeningSessionResponse() {
    }

    public ScreeningSessionResponse(UUID id, SessionStatus status, Instant createdAt,
                                    Instant stabilizationStartedAt, Instant startedAt, Instant completedAt) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
        this.stabilizationStartedAt = stabilizationStartedAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public static ScreeningSessionResponse fromEntity(ScreeningSession entity) {
        if (entity == null) {
            return null;
        }
        return new ScreeningSessionResponse(
                entity.getId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getStabilizationStartedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStabilizationStartedAt() {
        return stabilizationStartedAt;
    }

    public void setStabilizationStartedAt(Instant stabilizationStartedAt) {
        this.stabilizationStartedAt = stabilizationStartedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
