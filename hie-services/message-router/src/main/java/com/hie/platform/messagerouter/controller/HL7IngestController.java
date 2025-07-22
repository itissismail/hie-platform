package com.hie.platform.messagerouter.controller;

import com.hie.platform.messagerouter.service.IngestionService;
import com.hie.platform.messagerouter.service.IngestionServiceMock;
import com.hie.platform.shared.audit.annotation.AuditStep;
import com.hie.platform.shared.audit.model.MessageStatus;
import com.hie.platform.shared.util.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/intake")
@Slf4j
public class HL7IngestController {

    private final IngestionServiceMock ingestionServiceMock;
    private final IngestionService ingestionService;


    public HL7IngestController(IngestionServiceMock ingestionServiceMock, IngestionService ingestionService) {
        this.ingestionServiceMock = ingestionServiceMock;
        this.ingestionService=ingestionService;
    }

    @PostMapping(value = "/messages",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.RECEIVED)
    public Mono<ResponseEntity<String>> receiveMessage(@RequestBody Mono<String> hl7MessageMono) {
        log.info("Received HL7 message for processing");

        return ingestionServiceMock.completeIngestion(hl7MessageMono)
                .map(s3Location -> ResponseEntity.ok()
                        .header("X-Storage-Location", s3Location)
                        .body("Message processed successfully and stored at: " + s3Location))
                .onErrorResume(error -> {
                    log.error("Failed to process message", error);
                    return Mono.just(ResponseEntity.badRequest()
                            .body("Failed to process message: " + error.getMessage()));
                });
    }

    /**
     * Endpoint to check message processing status
     */
    @GetMapping("/messages/{messageId}/status")
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.PROCESSING)
    public Mono<ResponseEntity<String>> getMessageStatus(@PathVariable String messageId) {
        log.info("Checking status for message: {}", messageId);

        return Mono.fromCallable(() -> {
            // Simulate status check
            return ResponseEntity.ok("Message " + messageId + " is being processed");
        });
    }

    @PostMapping("/ingest")
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.RECEIVED)
    public Mono<ResponseEntity<Map<String, Object>>> ingest(ServerHttpRequest request, @RequestBody Mono<String> hl7MessageMono) {
        log.debug("Ingest endpoint called");

        // Extract user context from headers set by gateway
        String userId = request.getHeaders().getFirst("X-User-ID");
        String userRole = request.getHeaders().getFirst("X-User-Role");

        String messageIdStr = request.getHeaders().getFirst(AppConstant.MESSAGE_ID_HEADER);
        String correlationIdStr = request.getHeaders().getFirst(AppConstant.CORRELATION_ID_HEADER);

        // Properly handle the reactive Mono<String>
        return hl7MessageMono
                .flatMap(hl7Message -> ingestionService.ingestReactive(hl7Message,messageIdStr,correlationIdStr)
                        .map(correlationId -> {
                            Map<String, Object> response = new HashMap<>();
                            response.put("status", "success");
                            response.put("correlationId", correlationId);
                            response.put("message", "HL7 message processed successfully");
                            return ResponseEntity.ok(response);
                        })
                        .onErrorResume(error -> {
                            log.error("Error processing HL7 message", error);
                            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(Map.of(
                                            "status", "error",
                                            "message", "Processing failed",
                                            "error", error.getMessage()
                                    )));
                        }));
    }

/*    @PostMapping("/process")
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.COMPLETED)
    public Mono<ResponseEntity<String>> processMessage(@RequestBody Mono<String> hl7MessageMono) {
        return Mono.just("Dummy Response, Process endpoint - to be implemented")
                .map(body -> ResponseEntity.ok(body))
                .doOnError(e -> log.error("Error occurred in /process endpoint", e))
                .onErrorResume(e -> {
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Internal error occurred: " + e.getMessage()));
                });
    }*/
}