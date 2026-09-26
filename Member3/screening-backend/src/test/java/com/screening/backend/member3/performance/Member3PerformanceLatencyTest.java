package com.screening.backend.member3.performance;

import com.screening.backend.member3.model.ScreeningSession;
import com.screening.backend.member3.model.SessionStatus;
import com.screening.backend.member3.repository.ScreeningSessionRepository;
import com.screening.backend.member3.vital.dto.VitalIngestionRequest;
import com.screening.backend.member3.vital.service.VitalIngestionService;
import com.screening.backend.member3.vital.service.VitalValidationService;
import com.screening.backend.member3.websocket.publisher.VitalPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Member3PerformanceLatencyTest {

    @Mock
    private ScreeningSessionRepository sessionRepository;

    @Mock
    private VitalPublisher vitalPublisher;

    @Test
    @DisplayName("Measure real server-side processing latency for vital ingestion path")
    void measureVitalIngestionLatency() {
        UUID sessionId = UUID.randomUUID();
        Instant fixedInstant = Instant.parse("2026-09-26T12:00:00Z");
        Clock clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        ScreeningSession session = new ScreeningSession(sessionId, SessionStatus.RUNNING, fixedInstant);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        VitalValidationService validationService = new VitalValidationService(clock);
        VitalIngestionService ingestionService = new VitalIngestionService(sessionRepository, validationService, vitalPublisher);

        VitalIngestionRequest request = new VitalIngestionRequest(
                fixedInstant.toEpochMilli(),
                72.5,
                16.0,
                List.of(0.12, 0.18, 0.24, 0.31, 0.15),
                0.98
        );

        // Warm up JVM (50 iterations)
        for (int i = 0; i < 50; i++) {
            ingestionService.ingestVital(sessionId, request);
        }

        // Measure benchmark iterations (200 packets)
        int iterations = 200;
        List<Double> latenciesMs = new ArrayList<>(iterations);

        for (int i = 0; i < iterations; i++) {
            long startNanos = System.nanoTime();
            ingestionService.ingestVital(sessionId, request);
            long endNanos = System.nanoTime();
            double latencyMs = (endNanos - startNanos) / 1_000_000.0;
            latenciesMs.add(latencyMs);
        }

        Collections.sort(latenciesMs);
        double minLatency = latenciesMs.get(0);
        double maxLatency = latenciesMs.get(iterations - 1);
        double avgLatency = latenciesMs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double p95Latency = latenciesMs.get((int) (iterations * 0.95));

        System.out.printf("=== MEMBER 3 PERFORMANCE BENCHMARK RESULTS (200 Iterations) ===%n");
        System.out.printf("Server-Side Ingestion Latency (request -> validation -> publish):%n");
        System.out.printf("  Min Latency: %.3f ms%n", minLatency);
        System.out.printf("  Avg Latency: %.3f ms%n", avgLatency);
        System.out.printf("  P95 Latency: %.3f ms%n", p95Latency);
        System.out.printf("  Max Latency: %.3f ms%n", maxLatency);
        System.out.printf("Note: Only server-side processing latency was measured. Network transit and browser rendering are outside this benchmark.%n");
        System.out.printf("================================================================%n");

        // Verify that server processing latency is well within reasonable bounds (< 50ms)
        assertThat(avgLatency).isLessThan(50.0);
    }
}
