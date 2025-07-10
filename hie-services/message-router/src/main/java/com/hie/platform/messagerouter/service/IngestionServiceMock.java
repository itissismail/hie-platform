package com.hie.platform.messagerouter.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
    public class IngestionServiceMock {
        public String ingest(String hl7Message) {
            // Upload to MinIO, log audit, publish to RabbitMQ, etc. (non-blocking if possible)
            return UUID.randomUUID().toString();
        }
    }
