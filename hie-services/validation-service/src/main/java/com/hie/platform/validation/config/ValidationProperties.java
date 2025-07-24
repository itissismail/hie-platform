package com.hie.platform.validation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
//@Configuration
@ConfigurationProperties(prefix = "validation")
public class ValidationProperties {

    private Hl7 hl7 = new Hl7();

    @Data
    public static class Hl7 {
        private boolean strictMode = true;
        private boolean validateSegments = true;
        private boolean validateFields = true;
        private boolean validateDatatypes = true;
        private List<String> supportedMessageTypes = new ArrayList<>();
        private long validationTimeout = 30000L; // 30 seconds
        private long maxMessageSize = 1048576L; // 1MB
    }
}