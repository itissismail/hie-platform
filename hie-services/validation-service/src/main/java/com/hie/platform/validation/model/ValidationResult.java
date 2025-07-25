package com.hie.platform.validation.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Result of HL7 validation process
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;
    private String messageType;
    private String organizationId;
    private LocalDateTime validationStartTime;
    private LocalDateTime validationEndTime;
    private long validationDurationMs;

    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();

    private int errorCount;
    private int warningCount;

    // Additional metadata
    private String validatedBy;
    private String validationVersion;
    private String messageId;

    /**
     * Check if validation has any errors (severity = ERROR)
     */
    public boolean hasErrors() {
        return errors.stream().anyMatch(error -> "ERROR".equals(error.getSeverity()));
    }

    /**
     * Check if validation has any warnings (severity = WARNING)
     */
    public boolean hasWarnings() {
        return errors.stream().anyMatch(error -> "WARNING".equals(error.getSeverity()));
    }

    /**
     * Get only error-level validation issues
     */
    public List<ValidationError> getErrorsOnly() {
        return errors.stream()
                .filter(error -> "ERROR".equals(error.getSeverity()))
                .toList();
    }

    /**
     * Get only warning-level validation issues
     */
    public List<ValidationError> getWarningsOnly() {
        return errors.stream()
                .filter(error -> "WARNING".equals(error.getSeverity()))
                .toList();
    }

    /**
     * Add a validation error
     */
    public void addError(ValidationError error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
        updateCounts();
    }

    /**
     * Add multiple validation errors
     */
    public void addErrors(List<ValidationError> newErrors) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.addAll(newErrors);
        updateCounts();
    }

    /**
     * Update error and warning counts
     */
    private void updateCounts() {
        if (errors != null) {
            errorCount = (int) errors.stream().filter(e -> "ERROR".equals(e.getSeverity())).count();
            warningCount = (int) errors.stream().filter(e -> "WARNING".equals(e.getSeverity())).count();
        }
    }

    /**
     * Set errors and update counts
     */
    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
        updateCounts();
    }
}