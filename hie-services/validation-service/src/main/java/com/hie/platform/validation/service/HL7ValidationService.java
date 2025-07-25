package com.hie.platform.validation.service;

import com.hie.platform.shared.rabbitmq.producer.model.QueueMessage;
import com.hie.platform.validation.config.ValidationProperties;
import com.hie.platform.validation.model.ValidationResult;
import com.hie.platform.validation.model.ValidationError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class HL7ValidationService {

    private final ValidationProperties validationProperties;

    // HL7 segment patterns
    private static final Pattern MSH_PATTERN = Pattern.compile("^MSH\\|");
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("^[A-Z]{3}\\|");
    private static final Pattern FIELD_SEPARATOR_PATTERN = Pattern.compile("\\|");

    /**
     * Validate HL7 message content
     * NOTE: MinIO download is now handled by AbstractMessageConsumer
     *
     * @param queueMessage The queue message containing metadata
     * @param hl7Content The actual HL7 content retrieved from MinIO
     * @return true if validation successful, false otherwise
     */
    public boolean validateHL7Message(QueueMessage queueMessage, String hl7Content) {
        try {
            log.info("Starting HL7 validation - MessageId: {}", queueMessage.getMessageId());

            // Extract metadata from payload
            Map<String, Object> payload = queueMessage.getPayload();
            String hl7MessageType = (String) payload.get("hl7MessageType");
            String organizationId = (String) payload.get("organizationId");

            // Validate input parameters
            if (hl7Content == null || hl7Content.trim().isEmpty()) {
                log.error("HL7 content is null or empty for message: {}", queueMessage.getMessageId());
                return false;
            }

            // Perform comprehensive validation
            ValidationResult validationResult = performValidation(hl7Content, hl7MessageType, organizationId);

            // Log validation result
            if (validationResult.isValid()) {
                log.info("HL7 validation successful - MessageId: {}, MessageType: {}, Duration: {}ms",
                        queueMessage.getMessageId(), hl7MessageType, validationResult.getValidationDurationMs());

                // Store validation result in payload for downstream services
                payload.put("validationResult", createValidationSummary(validationResult));

                return true;
            } else {
                log.error("HL7 validation failed - MessageId: {}, Errors: {}, Warnings: {}",
                        queueMessage.getMessageId(),
                        validationResult.getErrorCount(),
                        validationResult.getWarningCount());

                // Log individual validation errors
                validationResult.getErrors().forEach(error ->
                        log.error("Validation Error - Type: {}, Message: {}, Location: {}, Severity: {}",
                                error.getErrorType(), error.getErrorMessage(), error.getLocation(), error.getSeverity()));

                // Store validation errors in payload for analysis
                payload.put("validationErrors", validationResult.getErrors());

                return false;
            }

        } catch (Exception e) {
            log.error("Exception during HL7 validation - MessageId: {}", queueMessage.getMessageId(), e);
            return false;
        }
    }

    /**
     * Create validation summary for downstream services
     */
    private Map<String, Object> createValidationSummary(ValidationResult validationResult) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("isValid", validationResult.isValid());
        summary.put("messageType", validationResult.getMessageType());
        summary.put("organizationId", validationResult.getOrganizationId());
        summary.put("validationStartTime", validationResult.getValidationStartTime());
        summary.put("validationEndTime", validationResult.getValidationEndTime());
        summary.put("validationDurationMs", validationResult.getValidationDurationMs());
        summary.put("errorCount", validationResult.getErrorCount());
        summary.put("warningCount", validationResult.getWarningCount());

        // Include only error/warning counts, not full details to keep payload size manageable
        if (!validationResult.getErrors().isEmpty()) {
            summary.put("hasErrors", true);
            summary.put("errorTypes", validationResult.getErrors().stream()
                    .map(ValidationError::getErrorType)
                    .distinct()
                    .toList());
        }

        return summary;
    }

    /**
     * Perform comprehensive HL7 validation
     */
    private ValidationResult performValidation(String hl7Content, String messageType, String organizationId) {
        ValidationResult result = new ValidationResult();
        result.setValidationStartTime(LocalDateTime.now());
        result.setMessageType(messageType);
        result.setOrganizationId(organizationId);

        List<ValidationError> errors = new ArrayList<>();

        try {
            // Check message size limits
            if (hl7Content.length() > validationProperties.getHl7().getMaxMessageSize()) {
                errors.add(ValidationError.builder()
                        .errorType("SIZE_LIMIT_EXCEEDED")
                        .errorMessage("Message size (" + hl7Content.length() + " bytes) exceeds maximum allowed limit (" +
                                validationProperties.getHl7().getMaxMessageSize() + " bytes)")
                        .location("MESSAGE")
                        .severity("ERROR")
                        .build());
            }

            // Basic structure validation
            validateBasicStructure(hl7Content, errors);

            // MSH segment validation (mandatory)
            validateMSHSegment(hl7Content, errors);

            // Message type validation
            if (messageType != null && validationProperties.getHl7().getSupportedMessageTypes().contains(messageType)) {
                validateMessageType(hl7Content, messageType, errors);
            } else if (messageType != null) {
                errors.add(ValidationError.builder()
                        .errorType("UNSUPPORTED_MESSAGE_TYPE")
                        .errorMessage("Message type not supported: " + messageType)
                        .location("MSH.9")
                        .severity("ERROR")
                        .build());
            }

            // Conditional validations based on configuration
            if (validationProperties.getHl7().isValidateSegments()) {
                validateSegments(hl7Content, errors);
            }

            if (validationProperties.getHl7().isValidateFields()) {
                validateFields(hl7Content, errors);
            }

            if (validationProperties.getHl7().isValidateDatatypes()) {
                validateDataTypes(hl7Content, errors);
            }

        } catch (Exception e) {
            log.error("Exception during validation process", e);
            errors.add(ValidationError.builder()
                    .errorType("VALIDATION_EXCEPTION")
                    .errorMessage("Validation process failed: " + e.getMessage())
                    .location("SYSTEM")
                    .severity("ERROR")
                    .build());
        }

        result.setErrors(errors);
        result.setValid(errors.stream().noneMatch(error -> "ERROR".equals(error.getSeverity())));
        result.setValidationEndTime(LocalDateTime.now());
        result.setValidationDurationMs(
                java.time.Duration.between(result.getValidationStartTime(), result.getValidationEndTime()).toMillis());

        // Calculate error and warning counts
        long errorCount = errors.stream().filter(e -> "ERROR".equals(e.getSeverity())).count();
        long warningCount = errors.stream().filter(e -> "WARNING".equals(e.getSeverity())).count();

        result.setErrorCount((int) errorCount);
        result.setWarningCount((int) warningCount);

        log.info("Validation completed - Valid: {}, Errors: {}, Warnings: {}, Duration: {}ms",
                result.isValid(), errorCount, warningCount, result.getValidationDurationMs());

        return result;
    }

    /**
     * Validate basic HL7 structure
     */
    private void validateBasicStructure(String hl7Content, List<ValidationError> errors) {
        // Check if message is empty
        if (hl7Content == null || hl7Content.trim().isEmpty()) {
            errors.add(ValidationError.builder()
                    .errorType("EMPTY_MESSAGE")
                    .errorMessage("HL7 message is empty")
                    .location("MESSAGE")
                    .severity("ERROR")
                    .build());
            return;
        }

        // Check for MSH segment at the beginning
        if (!hl7Content.startsWith("MSH|")) {
            errors.add(ValidationError.builder()
                    .errorType("MISSING_MSH_HEADER")
                    .errorMessage("Message must start with MSH segment")
                    .location("MESSAGE")
                    .severity("ERROR")
                    .build());
        }

        // Validate segment structure
        String[] lines = hl7Content.split("\r");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() && !SEGMENT_PATTERN.matcher(line).find()) {
                errors.add(ValidationError.builder()
                        .errorType("INVALID_SEGMENT_FORMAT")
                        .errorMessage("Invalid segment format at line " + (i + 1) + ": " + line.substring(0, Math.min(line.length(), 50)))
                        .location("LINE_" + (i + 1))
                        .severity("ERROR")
                        .build());
            }
        }
    }

    /**
     * Validate MSH segment
     */
    private void validateMSHSegment(String hl7Content, List<ValidationError> errors) {
        String[] lines = hl7Content.split("\r");
        if (lines.length == 0) return;

        String mshSegment = lines[0].trim();
        String[] fields = mshSegment.split("\\|");

        // MSH must have at least 12 fields
        if (fields.length < 12) {
            errors.add(ValidationError.builder()
                    .errorType("INSUFFICIENT_MSH_FIELDS")
                    .errorMessage("MSH segment must have at least 12 fields, found: " + fields.length)
                    .location("MSH")
                    .severity("ERROR")
                    .build());
        }

        // Validate sending application (MSH.3)
        if (fields.length > 2 && (fields[2] == null || fields[2].trim().isEmpty())) {
            errors.add(ValidationError.builder()
                    .errorType("MISSING_SENDING_APPLICATION")
                    .errorMessage("Sending application (MSH.3) is required")
                    .location("MSH.3")
                    .severity("ERROR")
                    .build());
        }

        // Validate message type (MSH.9)
        if (fields.length > 8 && (fields[8] == null || fields[8].trim().isEmpty())) {
            errors.add(ValidationError.builder()
                    .errorType("MISSING_MESSAGE_TYPE")
                    .errorMessage("Message type (MSH.9) is required")
                    .location("MSH.9")
                    .severity("ERROR")
                    .build());
        }

        // Validate timestamp (MSH.7)
        if (fields.length > 6 && (fields[6] == null || fields[6].trim().isEmpty())) {
            errors.add(ValidationError.builder()
                    .errorType("MISSING_TIMESTAMP")
                    .errorMessage("Timestamp (MSH.7) is required")
                    .location("MSH.7")
                    .severity("WARNING")
                    .build());
        }
    }

    /**
     * Validate message type
     */
    private void validateMessageType(String hl7Content, String expectedMessageType, List<ValidationError> errors) {
        String[] lines = hl7Content.split("\r");
        if (lines.length == 0) return;

        String mshSegment = lines[0].trim();
        String[] fields = mshSegment.split("\\|");

        if (fields.length > 8) {
            String actualMessageType = fields[8];
            if (!expectedMessageType.equals(actualMessageType)) {
                errors.add(ValidationError.builder()
                        .errorType("MESSAGE_TYPE_MISMATCH")
                        .errorMessage("Expected message type: " + expectedMessageType + ", but found: " + actualMessageType)
                        .location("MSH.9")
                        .severity("WARNING")
                        .build());
            }
        }
    }

    /**
     * Validate segments
     */
    private void validateSegments(String hl7Content, List<ValidationError> errors) {
        String[] lines = hl7Content.split("\r");
        Set<String> requiredSegments = getRequiredSegments();
        Set<String> foundSegments = new HashSet<>();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.length() >= 3) {
                String segmentType = trimmedLine.substring(0, 3);
                foundSegments.add(segmentType);
            }
        }

        // Check for missing required segments
        for (String requiredSegment : requiredSegments) {
            if (!foundSegments.contains(requiredSegment)) {
                errors.add(ValidationError.builder()
                        .errorType("MISSING_REQUIRED_SEGMENT")
                        .errorMessage("Required segment missing: " + requiredSegment)
                        .location(requiredSegment)
                        .severity("ERROR")
                        .build());
            }
        }
    }

    /**
     * Validate fields within segments
     */
    private void validateFields(String hl7Content, List<ValidationError> errors) {
        String[] lines = hl7Content.split("\r");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            if (line.length() < 3) {
                continue; // Skip invalid lines
            }

            String segmentType = line.substring(0, 3);
            String[] fields = line.split("\\|");

            // Validate field count based on segment type
            int expectedFieldCount = getExpectedFieldCount(segmentType);
            if (expectedFieldCount > 0 && fields.length < expectedFieldCount) {
                errors.add(ValidationError.builder()
                        .errorType("INSUFFICIENT_FIELDS")
                        .errorMessage("Segment " + segmentType + " should have at least " + expectedFieldCount + " fields, found: " + fields.length)
                        .location(segmentType + "_LINE_" + (i + 1))
                        .severity("WARNING")
                        .build());
            }
        }
    }

    /**
     * Validate data types
     */
    private void validateDataTypes(String hl7Content, List<ValidationError> errors) {
        // Basic data type validation - can be extended based on requirements
        String[] lines = hl7Content.split("\r");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.length() < 3) continue;

            String segmentType = line.substring(0, 3);

            // Example: Validate date formats in MSH.7 (timestamp)
            if ("MSH".equals(segmentType)) {
                String[] fields = line.split("\\|");
                if (fields.length > 6 && fields[6] != null && !fields[6].isEmpty()) {
                    if (!isValidHL7DateTime(fields[6])) {
                        errors.add(ValidationError.builder()
                                .errorType("INVALID_DATE_FORMAT")
                                .errorMessage("Invalid date/time format in MSH.7: " + fields[6])
                                .location("MSH.7")
                                .severity("WARNING")
                                .build());
                    }
                }
            }
        }

        log.debug("Data type validation completed");
    }

    /**
     * Validate HL7 date/time format
     */
    private boolean isValidHL7DateTime(String dateTime) {
        // HL7 date format: YYYYMMDD[HHMM[SS[.SSSS]]][+/-ZZZZ]
        // This is a basic validation - can be enhanced
        return dateTime.matches("\\d{8}(\\d{4}(\\d{2}(\\.\\d{1,4})?)?)?([+-]\\d{4})?");
    }

    /**
     * Get required segments based on message type
     */
    private Set<String> getRequiredSegments() {
        Set<String> required = new HashSet<>();
        required.add("MSH"); // Always required
        // Add more based on message type if needed
        return required;
    }

    /**
     * Get expected field count for segment types
     */
    private int getExpectedFieldCount(String segmentType) {
        return switch (segmentType) {
            case "MSH" -> 12;
            case "PID" -> 10;
            case "EVN" -> 5;
            case "PV1" -> 20;
            case "OBX" -> 11;
            case "ORC" -> 12;
            default -> 0; // No specific requirement
        };
    }
}