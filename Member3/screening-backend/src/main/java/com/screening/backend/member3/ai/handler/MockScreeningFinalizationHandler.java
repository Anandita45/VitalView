package com.screening.backend.member3.ai.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock implementation of {@link ScreeningFinalizationHandler} used for testing and local runs.
 * Another team member can easily replace this by providing their own Spring bean.
 */
@Component
@ConditionalOnMissingBean(name = "customScreeningFinalizationHandler")
public class MockScreeningFinalizationHandler implements ScreeningFinalizationHandler {

    private static final Logger log = LoggerFactory.getLogger(MockScreeningFinalizationHandler.class);

    private volatile boolean shouldFail = false;

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    public boolean isShouldFail() {
        return shouldFail;
    }

    @Override
    public boolean finalizeScreening(UUID sessionId) {
        log.info("MockScreeningFinalizationHandler invoked for session {}", sessionId);
        if (shouldFail) {
            log.warn("Mock finalization configured to simulate failure for session {}", sessionId);
            return false;
        }
        log.info("Mock finalization completed successfully for session {}", sessionId);
        return true;
    }
}
