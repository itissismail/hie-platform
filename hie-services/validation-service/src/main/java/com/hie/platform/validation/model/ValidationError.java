package com.hie.platform.validation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {

    private String errorType;
    private String errorMessage;
    private String location;
    private String severity; // ERROR, WARNING, INFO
    private String segmentType;
    private String fieldName;
    private String fieldValue;
    private LocalDateTime occurredAt;
    private String errorCode;
    private String description;
    private String suggestion;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Convenience methods for common error types
    public static ValidationError error(String type, String message, String location) {
        return ValidationError.builder()
                .errorType(type)
                .errorMessage(message)
                .location(location)
                .severity("ERROR")
                .occurredAt(LocalDateTime.now())
                .build();
    }

    public static ValidationError warning(String type, String message, String location) {
        return ValidationError.builder()
                .errorType(type)
                .errorMessage(message)
                .location(location)
                .severity("WARNING")
                .occurredAt(LocalDateTime.now())
                .build();
    }

    public static ValidationError info(String type, String message, String location) {
        return ValidationError.builder()
                .errorType(type)
                .errorMessage(message)
                .location(location)
                .severity("INFO")
                .occurredAt(LocalDateTime.now())
                .build();
    }
}