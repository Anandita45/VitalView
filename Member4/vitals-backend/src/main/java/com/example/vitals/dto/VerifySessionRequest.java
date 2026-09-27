package com.example.vitals.dto;

import lombok.Data;

/**
 * Payload sent when the booth judge/volunteer enters the ground-truth
 * reading from a physical finger pulse oximeter for benchmarking.
 */
@Data
public class VerifySessionRequest {

    private Double groundTruthHeartRate;
}