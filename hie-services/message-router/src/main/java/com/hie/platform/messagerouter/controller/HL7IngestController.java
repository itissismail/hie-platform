package com.hie.platform.messagerouter.controller;

import com.hie.platform.messagerouter.service.IngestionServiceMock;
import com.hie.platform.shared.audit.annotation.AuditStep;
import com.hie.platform.shared.audit.model.MessageStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/intake")
@Slf4j
public class HL7IngestController {

    private final IngestionServiceMock ingestionService;

    public HL7IngestController(IngestionServiceMock ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.RECEIVED)
    public Mono<ResponseEntity<Map<String, Object>>> ingest(ServerHttpRequest request, @RequestBody Mono<String> hl7MessageMono) {
        log.debug("Ingest endpoint called");

        // Extract user context from headers set by gateway
        String userId = request.getHeaders().getFirst("X-User-ID");
        String userRole = request.getHeaders().getFirst("X-User-Role");

        // Properly handle the reactive Mono<String>
        return hl7MessageMono.flatMap(hl7Message -> {
            log.debug("Processing HL7 message of length: {}", hl7Message != null ? hl7Message.length() : 0);

            // Now properly pass the actual message content
            String correlationId = ingestionService.ingest(hl7Message);
            //String correlationId = UUID.randomUUID().toString();

            Map<String, Object> response = Map.of(
                    "message", "Message accepted for processing",
                    "authenticatedUser", userId != null ? userId : "anonymous",
                    "userRole", userRole != null ? userRole : "none",
                    "correlationId", correlationId,
                    "timestamp", System.currentTimeMillis()
            );

            log.debug("Returning response with correlation ID: {}", correlationId);
            return Mono.just(ResponseEntity.ok(response));
        }).doOnError(error -> {
            log.error("Error processing ingest request", error);
        });
    }

    @PostMapping("/process")
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.FAILED)
    public Mono<ResponseEntity<String>> processMessage(@RequestBody Mono<String> hl7MessageMono) {
        // Implementation placeholder
        return Mono.just(ResponseEntity.ok("Dummy Response, Process endpoint - to be implemented"));
    }
}