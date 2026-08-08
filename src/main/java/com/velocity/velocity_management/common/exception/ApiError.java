package com.velocity.velocity_management.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ApiError {

    private LocalDateTime timestamp;

    private int status;

    private String message;

    private Map<String, String> errors;

}