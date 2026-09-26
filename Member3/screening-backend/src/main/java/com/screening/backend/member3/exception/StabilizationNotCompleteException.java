package com.screening.backend.member3.exception;

/**
 * Thrown when attempting to start a screening before the 15-second stabilization period has elapsed.
 */
public class StabilizationNotCompleteException extends RuntimeException {

    private final long elapsedSeconds;
    private final long requiredSeconds;

    public StabilizationNotCompleteException(long elapsedSeconds, long requiredSeconds) {
        super(String.format("The %d-second stabilization period has not completed. Elapsed: %d second(s).",
                requiredSeconds, elapsedSeconds));
        this.elapsedSeconds = elapsedSeconds;
        this.requiredSeconds = requiredSeconds;
    }

    public StabilizationNotCompleteException(String message) {
        super(message);
        this.elapsedSeconds = 0;
        this.requiredSeconds = 15;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public long getRequiredSeconds() {
        return requiredSeconds;
    }
}
