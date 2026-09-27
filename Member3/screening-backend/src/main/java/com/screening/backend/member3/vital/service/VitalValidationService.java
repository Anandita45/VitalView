package com.screening.backend.member3.vital.service;

import com.screening.backend.member3.exception.InvalidVitalDataException;
import com.screening.backend.member3.vital.dto.VitalIngestionRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * Validates incoming vital data from the Python AI worker before real-time ingestion.
 */
@Service
public class VitalValidationService {

    public static final double MIN_BPM = 1.0;
    public static final double MAX_BPM = 300.0;

    public static final double MIN_RESPIRATION = 1.0;
    public static final double MAX_RESPIRATION = 100.0;

    public static final double MIN_SIGNAL_QUALITY = 0.0;
    public static final double MAX_SIGNAL_QUALITY = 1.0;

    public static final int MAX_WAVEFORM_POINTS = 1000;

    // Allow timestamps up to 10 seconds in the future to account for minor clock drift
    public static final long FUTURE_TOLERANCE_MS = 10_000L;
    // Disallow timestamps older than 24 hours
    public static final long MAX_AGE_MS = 24 * 60 * 60 * 1000L;

    private final Clock clock;

    public VitalValidationService(Clock clock) {
        this.clock = clock;
    }

    public void validate(VitalIngestionRequest request) {
        if (request == null) {
            throw new InvalidVitalDataException("Vital ingestion request body must not be null.");
        }

        // 1. Timestamp validation
        if (request.getTimestamp() == null) {
            throw new InvalidVitalDataException("Timestamp is required.");
        }
        long now = clock.millis();
        if (request.getTimestamp() > now + FUTURE_TOLERANCE_MS) {
            throw new InvalidVitalDataException("Timestamp cannot be in the future.");
        }
        if (request.getTimestamp() < now - MAX_AGE_MS) {
            throw new InvalidVitalDataException("Timestamp is excessively old (older than 24 hours).");
        }

        // 2. BPM validation
        if (request.getBpm() == null) {
            throw new InvalidVitalDataException("BPM must not be null.");
        }
        if (request.getBpm() < MIN_BPM || request.getBpm() > MAX_BPM) {
            throw new InvalidVitalDataException(
                    String.format("BPM must be between %.1f and %.1f. Received: %.1f", MIN_BPM, MAX_BPM, request.getBpm()));
        }

        // 3. Respiration validation
        if (request.getRespiration() == null) {
            throw new InvalidVitalDataException("Respiration must not be null.");
        }
        if (request.getRespiration() < MIN_RESPIRATION || request.getRespiration() > MAX_RESPIRATION) {
            throw new InvalidVitalDataException(
                    String.format("Respiration must be between %.1f and %.1f. Received: %.1f",
                            MIN_RESPIRATION, MAX_RESPIRATION, request.getRespiration()));
        }

        // 4. Waveform validation
        if (request.getWaveform() == null || request.getWaveform().isEmpty()) {
            throw new InvalidVitalDataException("Waveform must not be null or empty.");
        }
        if (request.getWaveform().size() > MAX_WAVEFORM_POINTS) {
            throw new InvalidVitalDataException(
                    String.format("Waveform points exceed maximum allowed limit of %d. Received: %d",
                            MAX_WAVEFORM_POINTS, request.getWaveform().size()));
        }

        // 5. Signal Quality validation
        if (request.getSignalQuality() == null) {
            throw new InvalidVitalDataException("Signal quality must not be null.");
        }
        if (request.getSignalQuality() < MIN_SIGNAL_QUALITY || request.getSignalQuality() > MAX_SIGNAL_QUALITY) {
            throw new InvalidVitalDataException("Signal quality must be between 0.0 and 1.0.");
        }
    }
}
