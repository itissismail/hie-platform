package com.hie.platform.shared.rabbitmq.consumer.processor;

import lombok.Builder;
import lombok.Data;

/**
 * Result of message processing
 */
@Data
@Builder
public class ProcessingResult {
    private boolean success;
    private String errorMessage;
    private String nextQueue; // Optional: if message should be forwarded to another queue
    private Object processedData; // Optional: processed data that might be needed

    public static ProcessingResult success() {
        return ProcessingResult.builder()
                .success(true)
                .build();
    }

    public static ProcessingResult success(String nextQueue) {
        return ProcessingResult.builder()
                .success(true)
                .nextQueue(nextQueue)
                .build();
    }

    public static ProcessingResult success(String nextQueue, Object processedData) {
        return ProcessingResult.builder()
                .success(true)
                .nextQueue(nextQueue)
                .processedData(processedData)
                .build();
    }

    public static ProcessingResult failure(String errorMessage) {
        return ProcessingResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    public static ProcessingResult failure(String errorMessage, Exception exception) {
        return ProcessingResult.builder()
                .success(false)
                .errorMessage(errorMessage + ": " + exception.getMessage())
                .build();
    }
}