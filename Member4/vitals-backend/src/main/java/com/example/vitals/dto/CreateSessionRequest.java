package com.example.vitals.dto;

import lombok.Data;

/**
 * Payload sent by the frontend / AI worker right after the rPPG pipeline
 * finishes processing a webcam scan.
 */
@Data
public class CreateSessionRequest {

    private String participantName;

    private Double estimatedHeartRate;

    private Double estimatedRespirationRate;
}