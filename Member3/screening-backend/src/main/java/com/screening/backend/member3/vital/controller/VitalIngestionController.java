package com.screening.backend.member3.vital.controller;

import com.screening.backend.member3.vital.dto.VitalIngestionRequest;
import com.screening.backend.member3.vital.service.VitalIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller receiving real-time vital signs ingestion from the Python AI worker.
 * Endpoint: POST /api/member3/sessions/{sessionId}/vitals
 */
@RestController
@RequestMapping("/api/member3/sessions/{sessionId}/vitals")
public class VitalIngestionController {

    private final VitalIngestionService vitalIngestionService;

    public VitalIngestionController(VitalIngestionService vitalIngestionService) {
        this.vitalIngestionService = vitalIngestionService;
    }

    @PostMapping
    public ResponseEntity<Void> ingestVital(@PathVariable("sessionId") UUID sessionId,
                                            @RequestBody VitalIngestionRequest request) {
        vitalIngestionService.ingestVital(sessionId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
