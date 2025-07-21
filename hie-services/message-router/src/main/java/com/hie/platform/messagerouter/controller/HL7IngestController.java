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
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
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
        return hl7MessageMono
                .flatMap(hl7Message -> {
                    log.debug("HL7 message received, length: {}", hl7Message.length());
                    log.debug("Creating Mono.fromCallable to wrap synchronous ingest() call");

                    return Mono.fromCallable(() -> {
                                return ingestionService.ingestReactive(hl7Message);
                               // return result; // This becomes the value emitted by the Mono
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnSubscribe(subscription -> {
                                log.debug("Mono subscribed - execution will start on boundedElastic scheduler");
                                log.debug("Subscription thread: {}", Thread.currentThread().getName());
                            })
                            .doOnNext(correlationId -> {
                                log.debug("Mono emitted value: {}", correlationId);
                                log.debug("doOnNext executing on thread: {}", Thread.currentThread().getName());
                            })
                            .doOnError(error -> {
                                log.error("Error occurred in Mono execution: {}", error.getMessage(), error);
                                log.debug("Error thread: {}", Thread.currentThread().getName());
                            })
                            .doOnTerminate(() -> {
                                log.debug("Mono execution terminated");
                                log.debug("Termination thread: {}", Thread.currentThread().getName());
                            })
                            .map(correlationId -> {
                                log.debug("Mapping result to ResponseEntity, correlationId: {}", correlationId);

                                Map<String, Object> response = new HashMap<>();
                                response.put("status", "success");
                                response.put("correlationId", correlationId);
                                response.put("message", "HL7 message processed successfully");

                                log.debug("Response map created: {}", response);
                                return ResponseEntity.ok(response);
                            });
                })
                .doOnError(error -> {
                    log.error("Error in outer flatMap: {}", error.getMessage(), error);
                });
    }

    @PostMapping("/process")
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.FAILED)
    public Mono<ResponseEntity<String>> processMessage(@RequestBody Mono<String> hl7MessageMono) {
        // Implementation placeholder
        return Mono.just(ResponseEntity.ok("Dummy Response, Process endpoint - to be implemented"));
    }
}