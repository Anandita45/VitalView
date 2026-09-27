package com.screening.backend.member3.exception;

/**
 * Thrown when incoming vital data from the Python AI worker fails ingestion validation rules.
 */
public class InvalidVitalDataException extends RuntimeException {

    public InvalidVitalDataException(String message) {
        super(message);
    }
}
