package com.hie.platform.messagerouter.service;

import com.hie.platform.shared.audit.annotation.AuditStep;
import com.hie.platform.shared.audit.context.ReactiveAuditContext;
import com.hie.platform.shared.audit.model.MessageStatus;
import com.hie.platform.shared.audit.service.AuditTrailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class IngestionServiceMock {

    @Autowired
    private AuditTrailService auditTrailService;

    /**
     * Reactive ingestion method that properly integrates with audit context
     */
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.RECEIVED)
    public Mono<String> ingestMessage(Mono<String> hl7MessageMono) {
        return hl7MessageMono
                .doOnNext(message -> log.debug("Starting ingestion of HL7 message: {}",
                        message != null ? message.substring(0, Math.min(100, message.length())) + "..." : "null"))
                .flatMap(this::processMessage)
                .doOnSuccess(correlationId -> log.debug("Successfully ingested message with correlation ID: {}", correlationId))
                .doOnError(error -> log.error("Failed to ingest message", error));
    }

    /**
     * Process the HL7 message step
     */
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.PROCESSING)
    public Mono<String> processMessage(String hl7Message) {
        return ReactiveAuditContext.getCurrentContext()
                .switchIfEmpty(createDefaultContext())
                .flatMap(context -> {
                    log.debug("Processing HL7 message for messageId: {}", context.getMessageId());

                    // Simulate processing time
                    return Mono.delay(Duration.ofMillis(100))
                            .then(Mono.fromCallable(() -> {
                                // Simulate message processing (validation, parsing, etc.)
                                if (hl7Message == null || hl7Message.trim().isEmpty()) {
                                    throw new IllegalArgumentException("HL7 message cannot be null or empty");
                                }

                                // Generate correlation ID for tracking
                                return UUID.randomUUID().toString();
                            }));
                })
                .contextWrite(context -> {
                    // Ensure we have audit context
                    return ReactiveAuditContext.getCurrentContext()
                            .map(auditContext -> context.put(ReactiveAuditContext.CONTEXT_KEY, auditContext))
                            .switchIfEmpty(Mono.just(context))
                            .block();
                });
    }

    /**
     * Validate the processed message
     */
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.VALIDATED)
    public Mono<String> validateMessage(String correlationId) {
        return Mono.fromCallable(() -> {
                    log.debug("Validating message with correlation ID: {}", correlationId);

                    // Simulate validation logic
                    if (correlationId == null || correlationId.trim().isEmpty()) {
                        throw new IllegalArgumentException("Correlation ID cannot be null or empty");
                    }

                    // Simulate validation success
                    return correlationId;
                })
                .delayElement(Duration.ofMillis(50)); // Simulate validation time
    }

    /**
     * Store the message (simulated)
     */
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.STORED)
    public Mono<String> storeMessage(String correlationId) {
        return ReactiveAuditContext.getCurrentContext()
                .flatMap(context -> {
                    log.debug("Storing message with correlation ID: {} and messageId: {}",
                            correlationId, context.getMessageId());

                    // Simulate storing to MinIO or database
                    return Mono.delay(Duration.ofMillis(200))
                            .then(Mono.fromCallable(() -> {
                                // Simulate storage location
                                String s3Location = String.format("s3://hl7-messages/%s/%s.hl7",
                                        context.getMessageId(), correlationId);
                                log.debug("Message stored at: {}", s3Location);
                                return s3Location;
                            }));
                });
    }

    /**
     * Complete ingestion pipeline
     */
    public Mono<String> completeIngestion(Mono<String> hl7MessageMono) {
        return ingestMessage(hl7MessageMono)
                .flatMap(this::validateMessage)
                .flatMap(this::storeMessage)
                .doOnSuccess(s3Location -> log.info("Ingestion pipeline completed successfully. Stored at: {}", s3Location))
                .doOnError(error -> log.error("Ingestion pipeline failed", error));
    }

    private Mono<ReactiveAuditContext> createDefaultContext() {
        return Mono.fromSupplier(() -> {
            UUID messageId = UUID.randomUUID();
            UUID correlationId = UUID.randomUUID();
            log.debug("Created default audit context with messageId: {}, correlationId: {}",
                    messageId, correlationId);
            return ReactiveAuditContext.create(messageId, correlationId, "message-router-service");
        });
    }


    public Mono<String> processWithContext(String message) {
        UUID messageId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        ReactiveAuditContext context = ReactiveAuditContext.create(
                messageId, correlationId, "my-service"
        );

        return processMessage(message)
                .contextWrite(context.toReactorContext());
    }
}
