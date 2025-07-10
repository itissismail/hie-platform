package com.hie.platform.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, String>>> health() {
        Map<String, String> status = Map.of(
                "status", "UP",
                "service", "HIE API Gateway"
        );
        return Mono.just(ResponseEntity.ok(status));
    }
}
