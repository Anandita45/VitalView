package com.screening.backend.member3.controller;

import com.screening.backend.member3.exception.FinalizationFailureException;
import com.screening.backend.member3.exception.InvalidSessionTransitionException;
import com.screening.backend.member3.exception.Member3ExceptionHandler;
import com.screening.backend.member3.exception.SessionNotFoundException;
import com.screening.backend.member3.exception.StabilizationNotCompleteException;
import com.screening.backend.member3.model.ScreeningSession;
import com.screening.backend.member3.model.SessionStatus;
import com.screening.backend.member3.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private SessionController sessionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sessionController)
                .setControllerAdvice(new Member3ExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/member3/sessions creates a session with 201 Created")
    void testCreateSessionEndpoint() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-26T12:00:00Z");
        ScreeningSession session = new ScreeningSession(sessionId, SessionStatus.CREATED, now);

        when(sessionService.createSession()).thenReturn(session);

        mockMvc.perform(post("/api/member3/sessions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.createdAt").value(now.toString()));
    }

    @Test
    @DisplayName("GET /api/member3/sessions/{id} returns 200 with session details")
    void testGetSessionEndpoint() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-26T12:00:00Z");
        ScreeningSession session = new ScreeningSession(sessionId, SessionStatus.INITIALIZING, now);

        when(sessionService.getSession(sessionId)).thenReturn(session);

        mockMvc.perform(get("/api/member3/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.status").value("INITIALIZING"));
    }

    @Test
    @DisplayName("GET /api/member3/sessions/{id} unknown returns 404 structured error")
    void testGetUnknownSessionEndpoint() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionService.getSession(sessionId)).thenThrow(new SessionNotFoundException(sessionId));

        mockMvc.perform(get("/api/member3/sessions/{id}", sessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Screening session not found with ID: " + sessionId));
    }

    @Test
    @DisplayName("POST /api/member3/sessions/{id}/start before 15s returns 400 with STABILIZATION_NOT_COMPLETE")
    void testStartBefore15SecondsReturnsStructuredError() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionService.startScreening(sessionId)).thenThrow(new StabilizationNotCompleteException(5, 15));

        mockMvc.perform(post("/api/member3/sessions/{id}/start", sessionId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("STABILIZATION_NOT_COMPLETE"))
                .andExpect(jsonPath("$.message").value("The 15-second stabilization period has not completed."));
    }

    @Test
    @DisplayName("POST /api/member3/sessions/{id}/start with invalid transition returns 400 with INVALID_SESSION_TRANSITION")
    void testInvalidTransitionReturnsStructuredError() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionService.startScreening(sessionId))
                .thenThrow(new InvalidSessionTransitionException(SessionStatus.CREATED, SessionStatus.RUNNING));

        mockMvc.perform(post("/api/member3/sessions/{id}/start", sessionId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_SESSION_TRANSITION"))
                .andExpect(jsonPath("$.message").value("Invalid session transition from CREATED to RUNNING."));
    }

    @Test
    @DisplayName("POST /api/member3/sessions/{id}/finalize succeeds with COMPLETED session")
    void testFinalizeSessionSuccess() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-26T12:00:00Z");
        ScreeningSession session = new ScreeningSession(sessionId, SessionStatus.COMPLETED, now);
        session.setCompletedAt(now);

        when(sessionService.finalizeSession(sessionId)).thenReturn(session);

        mockMvc.perform(post("/api/member3/sessions/{id}/finalize", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/member3/sessions/{id}/finalize failure returns 400 with FINALIZATION_FAILURE")
    void testFinalizeSessionFailure() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionService.finalizeSession(sessionId))
                .thenThrow(new FinalizationFailureException(sessionId, "Finalization failed for session: " + sessionId));

        mockMvc.perform(post("/api/member3/sessions/{id}/finalize", sessionId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("FINALIZATION_FAILURE"))
                .andExpect(jsonPath("$.message").value("Finalization failed for session: " + sessionId));
    }
}
