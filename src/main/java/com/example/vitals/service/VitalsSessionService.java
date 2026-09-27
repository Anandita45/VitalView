package com.example.vitals.service;

import com.example.vitals.dto.CreateSessionRequest;
import com.example.vitals.dto.VerifySessionRequest;
import com.example.vitals.entity.SessionStatus;
import com.example.vitals.entity.VitalsSession;
import com.example.vitals.repository.VitalsSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class VitalsSessionService {

    private final VitalsSessionRepository repository;

    /**
     * Creates a new session record the moment the AI worker finishes
     * estimating heart rate / respiration rate from the webcam feed.
     */
    public VitalsSession createSession(CreateSessionRequest request) {
        if (request.getParticipantName() == null || request.getParticipantName().isBlank()) {
            throw new IllegalArgumentException("participantName is required");
        }
        if (request.getEstimatedHeartRate() == null) {
            throw new IllegalArgumentException("estimatedHeartRate is required");
        }

        VitalsSession session = VitalsSession.builder()
                .participantName(request.getParticipantName().trim())
                .timestamp(LocalDateTime.now())
                .estimatedHeartRate(request.getEstimatedHeartRate())
                .estimatedRespirationRate(request.getEstimatedRespirationRate())
                .status(SessionStatus.PENDING_VERIFICATION)
                .build();

        return repository.save(session);
    }

    /**
     * Benchmarks a session against a ground-truth reading taken from a
     * physical finger pulse oximeter, then persists the computed metrics.
     *
     * Absolute Error   = |estimated - actual|
     * Percentage Error = (|estimated - actual| / actual) * 100
     * Accuracy         = max(0, 100 - percentageError)
     */
    public VitalsSession verifySession(Long id, VerifySessionRequest request) {
        VitalsSession session = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session not found with id: " + id));

        Double groundTruth = request.getGroundTruthHeartRate();
        if (groundTruth == null || groundTruth <= 0) {
            throw new IllegalArgumentException("groundTruthHeartRate must be a positive number");
        }

        double estimated = session.getEstimatedHeartRate();
        double absoluteError = Math.abs(estimated - groundTruth);
        double percentageError = (absoluteError / groundTruth) * 100.0;
        double accuracy = Math.max(0.0, 100.0 - percentageError);

        session.setGroundTruthHeartRate(groundTruth);
        session.setAbsoluteError(round2(absoluteError));
        session.setPercentageAccuracy(round2(accuracy));
        session.setStatus(SessionStatus.VERIFIED);

        return repository.save(session);
    }

    public List<VitalsSession> getAllSessions() {
        return repository.findAll();
    }

    public VitalsSession getSessionById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session not found with id: " + id));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}