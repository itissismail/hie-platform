package com.hie.platform.messagerouter.controller;

import com.hie.platform.messagerouter.service.IngestionServiceMock;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/ingest")
public class HL7IngestController {

    private final IngestionServiceMock ingestionService;

    public HL7IngestController(IngestionServiceMock ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> ingest(ServerHttpRequest request, @RequestBody Mono<String> hl7MessageMono) {
    /*    return hl7MessageMono.flatMap(msg -> {
            String correlationId = ingestionService.ingest(msg);
            return Mono.just(ResponseEntity
                    .accepted()
                    .header("X-Correlation-ID", correlationId)
                    .body("Accepted for processing"));
        }).onErrorResume(e -> Mono.just(ResponseEntity
                .badRequest()
                .body("Error: " + e.getMessage())));*/
        // Extract user context from headers set by gateway
        String userId = request.getHeaders().getFirst("X-User-ID");
        String userRole = request.getHeaders().getFirst("X-User-Role");
        String correlationId = ingestionService.ingest(hl7MessageMono.toString());

        Map<String, Object> response = Map.of(
                "message", "Messages retrieved successfully",
                "authenticatedUser", userId != null ? userId : "anonymous",
                "userRole", userRole != null ? userRole : "none",
                "correlationId", correlationId,
                "timestamp", System.currentTimeMillis()
        );

        return Mono.just(ResponseEntity.ok(response));
    }

}
