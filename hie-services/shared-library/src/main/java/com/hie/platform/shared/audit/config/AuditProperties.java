package com.hie.platform.shared.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author mismail
 * @description AuditProperties class handles ...
 * @since 16/07/2025
 */
@ConfigurationProperties(prefix = "audit")
@Data
@Deprecated
public class AuditProperties {
    private boolean enabled = true;
    private boolean async = true;
    private int batchSize = 100;
    private Duration flushInterval = Duration.ofSeconds(5);
    private int maxRetries = 3;
    private Duration retryDelay = Duration.ofSeconds(1);
    private boolean fallbackToFileLogging = false;
    private String fallbackLogPath = "/tmp/audit-fallback.log";
}
