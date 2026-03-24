package com.example.tutorial.exception;

import java.util.Map;

public class ApiError {

    private final String code;
    private final String message;
    private final Map<String, String> details;

    public ApiError(String code, String message, Map<String, String> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
