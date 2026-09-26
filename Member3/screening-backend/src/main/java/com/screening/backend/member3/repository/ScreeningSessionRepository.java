package com.screening.backend.member3.repository;

import com.screening.backend.member3.model.ScreeningSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Data access repository for {@link ScreeningSession}.
 */
@Repository
public interface ScreeningSessionRepository extends JpaRepository<ScreeningSession, UUID> {
}
