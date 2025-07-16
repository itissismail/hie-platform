package com.hie.platform.gateway.controller;

import com.hie.platform.gateway.service.MessageIngestionService;

import com.hie.platform.shared.audit.dto.HL7Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/messages")
@Deprecated
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageIngestionService ingestionService;

    @PostMapping("/ingestMessage")
    public ResponseEntity<String> ingestMessage(@RequestBody String hl7Message) {
        try {
            logger.info("Received HL7 message for ingestion");

            HL7Message message = new HL7Message(hl7Message);
            //String correlationId = ingestionService.ingestMessage(message);
            String correlationId = "UUID-1234-ingestionService.ingestMessage(message)";

            return ResponseEntity.accepted()
                    .header("X-Correlation-ID", correlationId)
                    .body("Message accepted for processing");

        } catch (Exception e) {
            logger.error("Error ingesting message", e);
            return ResponseEntity.badRequest()
                    .body("Error processing message: " + e.getMessage());
        }

    }

    @PostMapping("/ingest")
    public Mono<ResponseEntity<String>> ingestMessage(@RequestBody Mono<String> hl7MessageMono) {
        return hl7MessageMono
                .doOnNext(msg -> logger.info("Received HL7 message for ingestion"))
                .flatMap(msg -> {
                    try {
                        HL7Message message = new HL7Message(msg);
                        //String correlationId = ingestionService.ingestMessage(message);
                        String correlationId = "UUID-1234-ingestionService.ingestMessage(message)";
                        return Mono.just(ResponseEntity
                                .accepted()
                                .header("X-Correlation-ID", correlationId)
                                .body("Message accepted for processing"));
                    } catch (Exception e) {
                        logger.error("Error ingesting message", e);
                        return Mono.just(ResponseEntity
                                .badRequest()
                                .body("Error processing message: " + e.getMessage()));
                    }
                });
    }

    @GetMapping("/status/{correlationId}")
    public ResponseEntity<String> getMessageStatus(@PathVariable String correlationId) {
     /*   try {
            String status = ingestionService.getMessageStatus(UUID.fromString(correlationId));
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error retrieving message status", e);
            return ResponseEntity.notFound().build();
        }*/
        return null;
    }
}