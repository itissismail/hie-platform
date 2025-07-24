package com.hie.platform.validation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Builder.Default
    private List<ValidationError> warnings = new ArrayList<>();

    private String summary;
    private int errorCount;
    private int warningCount;

    public void addError(ValidationError error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
        if ("ERROR".equals(error.getSeverity())) {
            errorCount++;
            valid = false;
        } else if ("WARNING".equals(error.getSeverity())) {
            warningCount++;
        }
    }

    public boolean hasErrors() {
        return errors != null && errors.stream().anyMatch(e -> "ERROR".equals(e.getSeverity()));
    }

    public boolean hasWarnings() {
        return errors != null && errors.stream().anyMatch(e -> "WARNING".equals(e.getSeverity()));
    }
}