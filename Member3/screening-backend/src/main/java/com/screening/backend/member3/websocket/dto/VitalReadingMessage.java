package com.screening.backend.member3.websocket.dto;

import java.util.List;
import java.util.UUID;

/**
 * WebSocket broadcast message for real-time vital readings.
 * Destination: /topic/member3/session/{sessionId}/vitals
 */
public class VitalReadingMessage {

    private UUID sessionId;
    private Long timestamp;
    private Double bpm;
    private Double respiration;
    private List<Double> waveform;
    private Double signalQuality;

    public VitalReadingMessage() {
    }

    public VitalReadingMessage(UUID sessionId, Long timestamp, Double bpm,
                               Double respiration, List<Double> waveform, Double signalQuality) {
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.bpm = bpm;
        this.respiration = respiration;
        this.waveform = waveform;
        this.signalQuality = signalQuality;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Double getBpm() {
        return bpm;
    }

    public void setBpm(Double bpm) {
        this.bpm = bpm;
    }

    public Double getRespiration() {
        return respiration;
    }

    public void setRespiration(Double respiration) {
        this.respiration = respiration;
    }

    public List<Double> getWaveform() {
        return waveform;
    }

    public void setWaveform(List<Double> waveform) {
        this.waveform = waveform;
    }

    public Double getSignalQuality() {
        return signalQuality;
    }

    public void setSignalQuality(Double signalQuality) {
        this.signalQuality = signalQuality;
    }
}
