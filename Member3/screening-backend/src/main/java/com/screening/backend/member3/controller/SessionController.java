package com.screening.backend.member3.controller;

import com.screening.backend.member3.dto.ScreeningSessionResponse;
import com.screening.backend.member3.model.ScreeningSession;
import com.screening.backend.member3.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for Member 3 screening session lifecycle orchestration.
 * All endpoints are hosted under /api/member3/sessions.
 */
@RestController
@RequestMapping("/api/member3/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<ScreeningSessionResponse> createSession() {
        ScreeningSession session = sessionService.createSession();
        return ResponseEntity.status(HttpStatus.CREATED).body(ScreeningSessionResponse.fromEntity(session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScreeningSessionResponse> getSession(@PathVariable("id") UUID id) {
        ScreeningSession session = sessionService.getSession(id);
        return ResponseEntity.ok(ScreeningSessionResponse.fromEntity(session));
    }

    @PostMapping("/{id}/initialize")
    public ResponseEntity<ScreeningSessionResponse> initializeSession(@PathVariable("id") UUID id) {
        ScreeningSession session = sessionService.initializeSession(id);
        return ResponseEntity.ok(ScreeningSessionResponse.fromEntity(session));
    }

    @PostMapping("/{id}/stabilize")
    public ResponseEntity<ScreeningSessionResponse> startStabilization(@PathVariable("id") UUID id) {
        ScreeningSession session = sessionService.startStabilization(id);
        return ResponseEntity.ok(ScreeningSessionResponse.fromEntity(session));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ScreeningSessionResponse> startScreening(@PathVariable("id") UUID id) {
        ScreeningSession session = sessionService.startScreening(id);
        return ResponseEntity.ok(ScreeningSessionResponse.fromEntity(session));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<ScreeningSessionResponse> finalizeSession(@PathVariable("id") UUID id) {
        ScreeningSession session = sessionService.finalizeSession(id);
        return ResponseEntity.ok(ScreeningSessionResponse.fromEntity(session));
    }
}
