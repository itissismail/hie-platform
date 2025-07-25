package com.hie.platform.validation.service;

import com.hie.platform.shared.minio.service.MinioService;
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

    private final MinioService minIOService;
    private final ValidationProperties validationProperties;

    // HL7 segment patterns
    private static final Pattern MSH_PATTERN = Pattern.compile("^MSH\\|");
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("^[A-Z]{3}\\|");
    private static final Pattern FIELD_SEPARATOR_PATTERN = Pattern.compile("\\|");

    /**
     * Validate HL7 message from queue
     */
    public boolean validateHL7Message(QueueMessage queueMessage,String messageContent) {
        try {
            log.info("Starting HL7 validation - MessageId: {}", queueMessage.getMessageId());

            // Extract file information from payload
            Map<String, Object> payload = queueMessage.getPayload();
            String minioPath = (String) payload.get("minioPath");
            String hl7MessageType = (String) payload.get("hl7MessageType");
            String organizationId = (String) payload.get("organizationId");

            if (minioPath == null || minioPath.isEmpty()) {
                log.error("MinIO path is null or empty for message: {}", queueMessage.getMessageId());
                return false;
            }

            // Download HL7 content from MinIO
            String hl7Content = downloadHL7Content(minioPath);
            if (hl7Content == null || hl7Content.isEmpty()) {
                log.error("Failed to retrieve HL7 content from MinIO path: {}", minioPath);
                return false;
            }

            // Perform comprehensive validation
            ValidationResult validationResult = performValidation(hl7Content, hl7MessageType, organizationId);

            // Log validation result
            if (validationResult.isValid()) {
                log.info("HL7 validation successful - MessageId: {}, MessageType: {}",
                        queueMessage.getMessageId(), hl7MessageType);
                return true;
            } else {
                log.error("HL7 validation failed - MessageId: {}, Errors: {}",
                        queueMessage.getMessageId(), validationResult.getErrors().size());

                // Log individual validation errors
                validationResult.getErrors().forEach(error ->
                        log.error("Validation Error - Type: {}, Message: {}, Location: {}",
                                error.getErrorType(), error.getErrorMessage(), error.getLocation()));

                return false;
            }

        } catch (Exception e) {
            log.error("Exception during HL7 validation - MessageId: {}", queueMessage.getMessageId(), e);
            return false;
        }
    }

    /**
     * Download HL7 content from MinIO
     */
    private String downloadHL7Content(String minioPath) {
        try {
            log.debug("Downloading HL7 content from MinIO path: {}", minioPath);

            // Use the shared MinIO service to download file content
            byte[] fileBytes = minIOService.downloadFile(minioPath);

            if (fileBytes == null || fileBytes.length == 0) {
                log.error("No content retrieved from MinIO path: {}", minioPath);
                return null;
            }

            // Convert bytes to string (assuming UTF-8 encoding)
            String content = new String(fileBytes, "UTF-8");

            log.debug("Successfully downloaded HL7 content - Size: {} bytes", fileBytes.length);
            return content;

        } catch (Exception e) {
            log.error("Failed to download HL7 content from MinIO path: {}", minioPath, e);
            return null;
        }
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
                        .errorMessage("Message size exceeds maximum allowed limit")
                        .location("MESSAGE")
                        .severity("ERROR")
                        .build());
            }

            // Basic structure validation
            validateBasicStructure(hl7Content, errors);

            // MSH segment validation (mandatory)
            validateMSHSegment(hl7Content, errors);

            // Message type validation
            if (validationProperties.getHl7().getSupportedMessageTypes().contains(messageType)) {
                validateMessageType(hl7Content, messageType, errors);
            } else {
                errors.add(ValidationError.builder()
                        .errorType("UNSUPPORTED_MESSAGE_TYPE")
                        .errorMessage("Message type not supported: " + messageType)
                        .location("MSH.9")
                        .severity("ERROR")
                        .build());
            }

            // Segment validation
            if (validationProperties.getHl7().isValidateSegments()) {
                validateSegments(hl7Content, errors);
            }

            // Field validation
            if (validationProperties.getHl7().isValidateFields()) {
                validateFields(hl7Content, errors);
            }

            // Data type validation
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

        log.info("Validation completed - Valid: {}, Errors: {}, Warnings: {}, Duration: {}ms",
                result.isValid(),
                errors.stream().filter(e -> "ERROR".equals(e.getSeverity())).count(),
                errors.stream().filter(e -> "WARNING".equals(e.getSeverity())).count(),
                result.getValidationDurationMs());

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
                        .errorMessage("Invalid segment format at line " + (i + 1))
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

            String segmentType = line.substring(0, 3);
            String[] fields = line.split("\\|");

            // Validate field count based on segment type
            int expectedFieldCount = getExpectedFieldCount(segmentType);
            if (expectedFieldCount > 0 && fields.length < expectedFieldCount) {
                errors.add(ValidationError.builder()
                        .errorType("INSUFFICIENT_FIELDS")
                        .errorMessage("Segment " + segmentType + " should have at least " + expectedFieldCount + " fields, found: " + fields.length)
                        .location(segmentType)
                        .severity("WARNING")
                        .build());
            }
        }
    }

    /**
     * Validate data types
     */
    private void validateDataTypes(String hl7Content, List<ValidationError> errors) {
        // Implementation for data type validation
        // This is a placeholder - implement specific data type validation as needed
        log.debug("Data type validation completed");
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
            default -> 0; // No specific requirement
        };
    }
}