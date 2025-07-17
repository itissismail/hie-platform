package com.hie.platform.shared.audit.model;

public enum MessageStatus {
    RECEIVED("received"),
    PROCESSING("processing"),
    UPLOADED("uploaded"),
    VALIDATED("validated"),
    COMPLETED("completed"),
    FAILED("failed"),
    RETRY_PENDING("retry_pending"), REGISTER_CLIENT("oAuth_client_register");

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
