package com.hie.platform.shared.audit.exception;

import lombok.Data;

@Data
public class ErrorResponse {
    private String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    // getters/setters (or use Lombok @Data)
}
