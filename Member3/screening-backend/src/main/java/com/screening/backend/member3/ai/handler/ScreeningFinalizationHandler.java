package com.screening.backend.member3.ai.handler;

import java.util.UUID;

/**
 * Interface contract for orchestrating screening finalization with the AI / report worker.
 * Team members implementing AI or report generation will provide a production implementation of this bean.
 */
public interface ScreeningFinalizationHandler {

    /**
     * Finalizes the screening session with the AI / report worker.
     *
     * @param sessionId the session identifier
     * @return true if finalization succeeded, false if it failed
     */
    boolean finalizeScreening(UUID sessionId);
}
