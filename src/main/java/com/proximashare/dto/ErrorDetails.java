package com.proximashare.dto;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {
    private String message;
    private String stackTrace; // Can be null if not needed in production

    // Default constructor for JSON deserialization
    public ErrorDetails() {
    }

    // Constructor to create an error response from an Exception
    public ErrorDetails(Exception e) {
        // Set the error message from the exception
        this.message = e.getMessage();

        // Convert the stack trace to a string for logging/debugging
        // It's often good practice to only include stack traces in
        // development environments, or for specific severe errors.
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        this.stackTrace = sw.toString();
    }

    // Constructor for a custom error message without an exception
    public ErrorDetails(String message) {
        this.message = message;
    }

    // Constructor for a custom error message with a specific stack trace string
    public ErrorDetails(String message, String stackTrace) {
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
}