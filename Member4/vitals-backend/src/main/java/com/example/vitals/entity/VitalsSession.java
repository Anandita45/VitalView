package com.example.vitals.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vitals_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalsSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String participantName;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Double estimatedHeartRate;

    private Double estimatedRespirationRate;

    private Double groundTruthHeartRate;

    private Double absoluteError;

    private Double percentageAccuracy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;
}