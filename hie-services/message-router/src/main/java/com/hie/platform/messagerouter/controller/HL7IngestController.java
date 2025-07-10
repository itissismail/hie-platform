package com.hie.platform.messagerouter.controller;

import com.hie.platform.messagerouter.service.IngestionServiceMock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ingest")
public class HL7IngestController {

    private final IngestionServiceMock ingestionService;

    public HL7IngestController(IngestionServiceMock ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public Mono<ResponseEntity<String>> ingest(@RequestBody Mono<String> hl7MessageMono) {
        return hl7MessageMono.flatMap(msg -> {
            String correlationId = ingestionService.ingest(msg);
            return Mono.just(ResponseEntity
                    .accepted()
                    .header("X-Correlation-ID", correlationId)
                    .body("Accepted for processing"));
        }).onErrorResume(e -> Mono.just(ResponseEntity
                .badRequest()
                .body("Error: " + e.getMessage())));
    }

}
