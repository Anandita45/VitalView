package com.screening.backend.member3.vital.controller;

import com.screening.backend.member3.exception.InvalidSessionStateException;
import com.screening.backend.member3.exception.InvalidVitalDataException;
import com.screening.backend.member3.exception.Member3ExceptionHandler;
import com.screening.backend.member3.exception.SessionNotFoundException;
import com.screening.backend.member3.model.SessionStatus;
import com.screening.backend.member3.vital.dto.VitalIngestionRequest;
import com.screening.backend.member3.vital.service.VitalIngestionService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VitalIngestionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VitalIngestionService vitalIngestionService;

    @InjectMocks
    private VitalIngestionController vitalIngestionController;

    private static final String VALID_PAYLOAD = """
            {
              "timestamp": 1727351234000,
              "bpm": 74.5,
              "respiration": 16.2,
              "waveform": [0.12, 0.18, 0.23],
              "signalQuality": 0.96
            }
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vitalIngestionController)
                .setControllerAdvice(new Member3ExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/member3/sessions/{sessionId}/vitals returns 202 Accepted on valid payload")
    void testIngestVitalAccepted() throws Exception {
        UUID sessionId = UUID.randomUUID();

        when(vitalIngestionService.ingestVital(eq(sessionId), any(VitalIngestionRequest.class)))
                .thenReturn(2L);

        mockMvc.perform(post("/api/member3/sessions/{sessionId}/vitals", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("POST /api/member3/sessions/{sessionId}/vitals with invalid vital data returns 400 and INVALID_VITAL_DATA")
    void testIngestVitalInvalidData() throws Exception {
        UUID sessionId = UUID.randomUUID();

        when(vitalIngestionService.ingestVital(eq(sessionId), any(VitalIngestionRequest.class)))
                .thenThrow(new InvalidVitalDataException("Signal quality must be between 0.0 and 1.0."));

        mockMvc.perform(post("/api/member3/sessions/{sessionId}/vitals", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_VITAL_DATA"))
                .andExpect(jsonPath("$.message").value("Signal quality must be between 0.0 and 1.0."));
    }

    @Test
    @DisplayName("POST /api/member3/sessions/{sessionId}/vitals with invalid session state returns 400 and INVALID_SESSION_STATE")
    void testIngestVitalInvalidSessionState() throws Exception {
        UUID sessionId = UUID.randomUUID();

        when(vitalIngestionService.ingestVital(eq(sessionId), any(VitalIngestionRequest.class)))
                .thenThrow(new InvalidSessionStateException(
                        sessionId,
                        SessionStatus.COMPLETED,
                        "Vital ingestion is only allowed when session is STABILIZING or RUNNING. Current status: COMPLETED"
                ));

        mockMvc.perform(post("/api/member3/sessions/{sessionId}/vitals", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_SESSION_STATE"))
                .andExpect(jsonPath("$.message").value("Vital ingestion is only allowed when session is STABILIZING or RUNNING. Current status: COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/member3/sessions/{sessionId}/vitals with unknown session returns 404 and SESSION_NOT_FOUND")
    void testIngestVitalSessionNotFound() throws Exception {
        UUID sessionId = UUID.randomUUID();

        when(vitalIngestionService.ingestVital(eq(sessionId), any(VitalIngestionRequest.class)))
                .thenThrow(new SessionNotFoundException(sessionId));

        mockMvc.perform(post("/api/member3/sessions/{sessionId}/vitals", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Screening session not found with ID: " + sessionId));
    }
}
