package com.hie.platform.shared.audit.exception;

/**
 * Custom exception for audit-related errors
 */
public class AuditException extends RuntimeException {

    public AuditException(String message) {
        super(message);
    }

    public AuditException(String message, Throwable cause) {
        super(message, cause);
    }
}