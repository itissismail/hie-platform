package com.hie.platform.shared.audit.model;

public enum MessageStatus {
    RECEIVED,
    PROCESSING,
    UPLOADED,
    VALIDATED,
    COMPLETED,
    FAILED,
    RETRY_PENDING
}