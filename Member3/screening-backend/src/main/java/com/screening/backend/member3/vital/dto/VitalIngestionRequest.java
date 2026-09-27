package com.screening.backend.member3.vital.dto;

import java.util.List;

/**
 * Ingestion request payload sent by the Python AI worker.
 */
public class VitalIngestionRequest {

    private Long timestamp;
    private Double bpm;
    private Double respiration;
    private List<Double> waveform;
    private Double signalQuality;

    public VitalIngestionRequest() {
    }

    public VitalIngestionRequest(Long timestamp, Double bpm, Double respiration,
                                 List<Double> waveform, Double signalQuality) {
        this.timestamp = timestamp;
        this.bpm = bpm;
        this.respiration = respiration;
        this.waveform = waveform;
        this.signalQuality = signalQuality;
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
