package com.proximashare.app;

import java.io.FileNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.proximashare.dto.ErrorDetails;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Helper method to determine if stack trace should be included
    // @Value("${app.error.include-stacktrace}")
    // private boolean includeStackTrace;
    private boolean isProductionEnvironment = false; // Set to true for production deployments

    /**
     * Handles FileNotFoundException, returning HTTP 404 (NOT_FOUND).
     */
    @ResponseBody
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleFileNotFoundException(FileNotFoundException e) {
        // Only include stack trace if not in production or for specific debugging
        ErrorDetails errorDetails = new ErrorDetails(e.getMessage(), isProductionEnvironment ? null : getStackTraceAsString(e));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDetails);
    }

    /**
     * Handles IllegalArgumentException, returning HTTP 400 (BAD_REQUEST).
     */
    @ResponseBody
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorDetails errorDetails = new ErrorDetails(e.getMessage(), isProductionEnvironment ? null : getStackTraceAsString(e));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    /**
     * Handles IllegalAccessException, returning HTTP 403 (FORBIDDEN).
     */
    @ResponseBody
    @ExceptionHandler(IllegalAccessException.class)
    public ResponseEntity<ErrorDetails> handleIllegalAccessException(IllegalAccessException e) {
        ErrorDetails errorDetails = new ErrorDetails(e.getMessage(), isProductionEnvironment ? null : getStackTraceAsString(e));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDetails);
    }

    /**
     * Handles MaxUploadSizeExceededException, returning HTTP 400 (BAD_REQUEST).
     * Provides a more user-friendly error message.
     */
    @ResponseBody
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorDetails> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        // Provide a user-friendly message for file size limit
        String errorMessage = "File size exceeds the maximum allowed limit. Please upload a file smaller than 1GB.";
        ErrorDetails errorDetails = new ErrorDetails(errorMessage, isProductionEnvironment ? null : getStackTraceAsString(e));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    /**
     * Catches all other unhandled exceptions, returning HTTP 500 (INTERNAL_SERVER_ERROR).
     * This should be the last @ExceptionHandler method to ensure it catches fallback exceptions.
     */
    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGeneralException(Exception e) {
        // Log the exception for internal monitoring
        System.err.println("An unhandled error occurred: " + e.getMessage());
        e.printStackTrace(); // Log full stack trace internally

        // Determine message and stack trace exposure based on environment
        String userMessage = isProductionEnvironment ?
                "An unexpected error occurred. Please try again later." :
                "An unexpected server error occurred: " + e.getMessage();

        String stackTrace = isProductionEnvironment ? null : getStackTraceAsString(e);

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