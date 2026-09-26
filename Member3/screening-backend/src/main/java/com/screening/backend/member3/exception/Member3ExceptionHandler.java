package com.screening.backend.member3.exception;

import com.screening.backend.member3.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Controller advice providing structured error responses for Member 3 endpoints.
 */
@RestControllerAdvice(basePackages = "com.screening.backend.member3")
public class Member3ExceptionHandler {

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFound(SessionNotFoundException ex) {
        ErrorResponse response = new ErrorResponse("SESSION_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidSessionTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidSessionTransitionException ex) {
        ErrorResponse response = new ErrorResponse("INVALID_SESSION_TRANSITION", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InvalidSessionStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSessionState(InvalidSessionStateException ex) {
        ErrorResponse response = new ErrorResponse("INVALID_SESSION_STATE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InvalidVitalDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVitalData(InvalidVitalDataException ex) {
        ErrorResponse response = new ErrorResponse("INVALID_VITAL_DATA", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(StabilizationNotCompleteException.class)
    public ResponseEntity<ErrorResponse> handleStabilizationNotComplete(StabilizationNotCompleteException ex) {
        ErrorResponse response = new ErrorResponse(
                "STABILIZATION_NOT_COMPLETE",
                "The 15-second stabilization period has not completed."
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(FinalizationFailureException.class)
    public ResponseEntity<ErrorResponse> handleFinalizationFailure(FinalizationFailureException ex) {
        ErrorResponse response = new ErrorResponse("FINALIZATION_FAILURE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse response = new ErrorResponse(
                "INVALID_ARGUMENT",
                "Invalid parameter value: " + ex.getName()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse response = new ErrorResponse("INVALID_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
