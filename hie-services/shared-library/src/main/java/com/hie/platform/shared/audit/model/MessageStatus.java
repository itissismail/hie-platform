package com.hie.platform.shared.audit.model;

public enum MessageStatus {


    RECEIVED("message_received"),
    VALIDATED("message_validated"),
    PARSED("message_parsed"),
    PROCESSING("message_processing"),
    PROCESSED("message_processed"),
    TRANSFORMED("message_transformed"),
    ROUTED("message_routed"),
    STORED("message_stored"),
    PUBLISHED("message_published"),
    COMPLETED("message_completed"),
    FAILED("message_failed"),
    RETRY("message_retry"),
    CANCELLED("message_cancelled"),
    TIMEOUT("message_timeout"),
    QUEUED("message_queued"),
    RETRY_PENDING("retry_pending"), REGISTER_CLIENT("oAuth_client_register");;

    private final String value;

    MessageStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public String getValue() {
        return this.value;
    }

    public static MessageStatus fromString(String value) {
        for (MessageStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown MessageStatus: " + value);
    }
}
