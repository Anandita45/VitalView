package com.screening.backend.member3.vital.service;

import com.screening.backend.member3.exception.InvalidVitalDataException;
import com.screening.backend.member3.vital.dto.VitalIngestionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VitalValidationServiceTest {

    private VitalValidationService validationService;
    private Instant fixedNow;

    @BeforeEach
    void setUp() {
        fixedNow = Instant.parse("2026-09-26T12:00:00Z");
        Clock clock = Clock.fixed(fixedNow, ZoneId.of("UTC"));
        validationService = new VitalValidationService(clock);
    }

    private VitalIngestionRequest createValidRequest() {
        return new VitalIngestionRequest(
                fixedNow.toEpochMilli(),
                72.0,
                16.0,
                List.of(0.1, 0.2, 0.3),
                0.95
        );
    }

    @Test
    @DisplayName("Valid vital packet passes validation")
    void testValidVitalPasses() {
        VitalIngestionRequest request = createValidRequest();
        assertThatCode(() -> validationService.validate(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Null request body throws InvalidVitalDataException")
    void testNullRequestThrows() {
        assertThatThrownBy(() -> validationService.validate(null))
                .isInstanceOf(InvalidVitalDataException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Invalid BPM: null, zero, negative, or > 300 throws InvalidVitalDataException")
    void testInvalidBpm() {
        VitalIngestionRequest r1 = createValidRequest();
        r1.setBpm(null);
        assertThatThrownBy(() -> validationService.validate(r1)).isInstanceOf(InvalidVitalDataException.class);

        VitalIngestionRequest r2 = createValidRequest();
        r2.setBpm(0.0);
        assertThatThrownBy(() -> validationService.validate(r2)).isInstanceOf(InvalidVitalDataException.class);

        VitalIngestionRequest r3 = createValidRequest();
        r3.setBpm(-10.0);
        assertThatThrownBy(() -> validationService.validate(r3)).isInstanceOf(InvalidVitalDataException.class);

        VitalIngestionRequest r4 = createValidRequest();
        r4.setBpm(350.0);
        assertThatThrownBy(() -> validationService.validate(r4)).isInstanceOf(InvalidVitalDataException.class);
    }

    @Test
    @DisplayName("Invalid respiration: null, zero, negative, or > 100 throws InvalidVitalDataException")
    void testInvalidRespiration() {
        VitalIngestionRequest r1 = createValidRequest();
        r1.setRespiration(null);
        assertThatThrownBy(() -> validationService.validate(r1)).isInstanceOf(InvalidVitalDataException.class);

        VitalIngestionRequest r2 = createValidRequest();
        r2.setRespiration(0.0);
        assertThatThrownBy(() -> validationService.validate(r2)).isInstanceOf(InvalidVitalDataException.class);

        VitalIngestionRequest r3 = createValidRequest();
        r3.setRespiration(150.0);
        assertThatThrownBy(() -> validationService.validate(r3)).isInstanceOf(InvalidVitalDataException.class);
    }

    @Test
    @DisplayName("Invalid waveform: null, empty, or exceeding maximum points throws InvalidVitalDataException")
    void testInvalidWaveform() {
        VitalIngestionRequest r1 = createValidRequest();
        r1.setWaveform(null);
        assertThatThrownBy(() -> validationService.validate(r1)).isInstanceOf(InvalidVitalDataException.class);

        VitalIngestionRequest r2 = createValidRequest();
        r2.setWaveform(Collections.emptyList());
        assertThatThrownBy(() -> validationService.validate(r2)).isInstanceOf(InvalidVitalDataException.class);

        List<Double> largeWaveform = new ArrayList<>();
        for (int i = 0; i < 1005; i++) {
            largeWaveform.add(0.5);
        }
        VitalIngestionRequest r3 = createValidRequest();
        r3.setWaveform(largeWaveform);
        assertThatThrownBy(() -> validationService.validate(r3))
                .isInstanceOf(InvalidVitalDataException.class)
                .hasMessageContaining("exceed maximum allowed limit");
    }

    @Test
    @DisplayName("Invalid signal quality: null, < 0.0, or > 1.0 throws InvalidVitalDataException")
    void testInvalidSignalQuality() {
        VitalIngestionRequest r1 = createValidRequest();
        r1.setSignalQuality(null);
        assertThatThrownBy(() -> validationService.validate(r1)).isInstanceOf(InvalidVitalDataException.class);

        VitalIngestionRequest r2 = createValidRequest();
        r2.setSignalQuality(-0.1);
        assertThatThrownBy(() -> validationService.validate(r2)).isInstanceOf(InvalidVitalDataException.class);

        VitalIngestionRequest r3 = createValidRequest();
        r3.setSignalQuality(1.05);
        assertThatThrownBy(() -> validationService.validate(r3)).isInstanceOf(InvalidVitalDataException.class);
    }

    @Test
    @DisplayName("Invalid timestamp: null, future, or older than 24 hours throws InvalidVitalDataException")
    void testInvalidTimestamp() {
        VitalIngestionRequest r1 = createValidRequest();
        r1.setTimestamp(null);
        assertThatThrownBy(() -> validationService.validate(r1)).isInstanceOf(InvalidVitalDataException.class);

        // Future beyond 10s tolerance
        VitalIngestionRequest r2 = createValidRequest();
        r2.setTimestamp(fixedNow.toEpochMilli() + 15_000L);
        assertThatThrownBy(() -> validationService.validate(r2))
                .isInstanceOf(InvalidVitalDataException.class)
                .hasMessageContaining("future");

        // Older than 24 hours
        VitalIngestionRequest r3 = createValidRequest();
        r3.setTimestamp(fixedNow.toEpochMilli() - (25 * 60 * 60 * 1000L));
        assertThatThrownBy(() -> validationService.validate(r3))
                .isInstanceOf(InvalidVitalDataException.class)
                .hasMessageContaining("older than 24 hours");
    }
}
