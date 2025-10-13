package com.proximashare.exception;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.proximashare.dto.ErrorDetails;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Helper method to determine if stack trace should be included
    @Value("${app.error.include-stacktrace}")
    private boolean includeStackTrace;

    @Value("${app.environment.production:false}")
    private boolean isProductionEnvironment;

    /**
     * Handles FileNotFoundException, returning HTTP 404 (NOT_FOUND).
     */
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleFileNotFoundException(FileNotFoundException e) {
        // Only include stack trace if not in production or for specific debugging
        ErrorDetails errorDetails = new ErrorDetails(e.getMessage(), (!isProductionEnvironment && includeStackTrace) ? getStackTraceAsString(e) : null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDetails);
    }

    /**
     * Handles IllegalArgumentException, returning HTTP 400 (BAD_REQUEST).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorDetails errorDetails = new ErrorDetails(e.getMessage(), (!isProductionEnvironment && includeStackTrace) ? getStackTraceAsString(e) : null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    /**
     * Handles IllegalAccessException, returning HTTP 403 (FORBIDDEN).
     */
    @ExceptionHandler(IllegalAccessException.class)
    public ResponseEntity<ErrorDetails> handleIllegalAccessException(IllegalAccessException e) {
        ErrorDetails errorDetails = new ErrorDetails(e.getMessage(), (!isProductionEnvironment && includeStackTrace) ? getStackTraceAsString(e) : null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDetails);
    }

    /**
     * Handles MaxUploadSizeExceededException, returning HTTP 400 (BAD_REQUEST).
     * Provides a more user-friendly error message.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorDetails> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        // Provide a user-friendly message for file size limit
        String errorMessage = "File size exceeds the maximum allowed limit. Please upload a file smaller than 1GB.";
        ErrorDetails errorDetails = new ErrorDetails(errorMessage, (!isProductionEnvironment && includeStackTrace) ? getStackTraceAsString(e) : null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    /**
     * Handles MissingServletRequestPartException, returning HTTP 400 (BAD_REQUEST).
     * This occurs when a required multipart file parameter is missing.
     */
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<ErrorDetails> handleMissingServletRequestPart(
            org.springframework.web.multipart.support.MissingServletRequestPartException e) {
        String errorMessage = "Required file parameter is missing. Please provide a file to upload.";
        ErrorDetails errorDetails = new ErrorDetails(errorMessage,
                (!isProductionEnvironment && includeStackTrace) ? getStackTraceAsString(e) : null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    /**
     * Handles MethodArgumentNotValidException, returning HTTP 400 (BAD_REQUEST).
     * This occurs when a required validations are failing.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValid(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles HttpMessageNotReadableException, returning HTTP 500 (INTERNAL_SERVER_ERROR).
     * Provides a more user-friendly error message for FE dev.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDetails> HandleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        // Provide a user-friendly message for file size limit
        String errorMessage = "Invalid Json format, check the post request body.";
        ErrorDetails errorDetails = new ErrorDetails(errorMessage, (!isProductionEnvironment && includeStackTrace) ? getStackTraceAsString(e) : null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails);
    }

    /**
     * Catches all other unhandled exceptions, returning HTTP 500 (INTERNAL_SERVER_ERROR).
     * This should be the last @ExceptionHandler method to ensure it catches fallback exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGeneralException(Exception e) {
        // Log the exception for internal monitoring
        System.err.println("An unhandled error occurred: " + e.getMessage());
        e.printStackTrace(); // Log full stack trace internally

        // Determine message and stack trace exposure based on environment
        String userMessage = isProductionEnvironment ?
                "An unexpected error occurred. Please try again later." :
                (includeStackTrace ? "An unexpected server error occurred: " + e.getMessage() :
                        "An unexpected error occurred. Please try again later.");

        String stackTrace = (!isProductionEnvironment && includeStackTrace) ? getStackTraceAsString(e) : null;

        ErrorDetails errorDetails = new ErrorDetails(userMessage, stackTrace);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails);
    }

    // Helper method to get stack trace as a string
    private String getStackTraceAsString(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}