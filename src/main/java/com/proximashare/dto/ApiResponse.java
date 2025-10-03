package com.proximashare.dto;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    // Getters and Setters
    private final T data;
    private final String message;

    // Constructor
    public ApiResponse(T data, String message) {
        this.data = data;
        this.message = message;
    }
}
