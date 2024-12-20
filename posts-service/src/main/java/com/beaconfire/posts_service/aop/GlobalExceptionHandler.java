package com.beaconfire.posts_service.aop;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.beaconfire.posts_service.dto.DataResponse;
import com.beaconfire.posts_service.exception.InvalidAccessibilityException;
import com.beaconfire.posts_service.exception.InvalidPostStatusException;
import com.beaconfire.posts_service.exception.PostNotFoundException;
import com.beaconfire.posts_service.exception.ReplyNotFoundException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<String> handlePostNotFoundException(PostNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ReplyNotFoundException.class)
    public ResponseEntity<String> handleReplyNotFoundException(ReplyNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidPostStatusException.class)
    public ResponseEntity<String> handleInvalidPostStatusException(InvalidPostStatusException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DataResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String message = "Invalid request body. Please check the provided data.";
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException) {
            InvalidFormatException invalidFormatException = (InvalidFormatException) cause;

            if (invalidFormatException.getTargetType().isEnum()) {
                List<Object> validValues = Arrays.asList(invalidFormatException.getTargetType().getEnumConstants());
                message = "Invalid value provided. Accepted values are: " + String.join(", ", validValues.stream().map(Object::toString).toList());
            }
        }

        return createErrorResponse(message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        return new ResponseEntity<>("An unexpected error occurred: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(InvalidAccessibilityException.class)
    public ResponseEntity<String> handleInvalidAccessibilityException(InvalidAccessibilityException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
    private ResponseEntity<DataResponse> createErrorResponse(String message) {
        DataResponse response = DataResponse.builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DataResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Collect all validation error messages
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .reduce("", (acc, msg) -> acc.isEmpty() ? msg : acc + "; " + msg);

        // Create a response entity with the collected messages
        DataResponse response = DataResponse.builder()
                .success(false)
                .message("Validation failed: " + errorMessage)
                .data(null)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}
