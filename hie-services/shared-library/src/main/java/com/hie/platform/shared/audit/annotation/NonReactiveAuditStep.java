package com.hie.platform.shared.audit.annotation;

import com.hie.platform.shared.audit.model.MessageStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author M.Ismail
 * Method-level annotation for automatic non-reactive audit logging
 * Date 28-July-2025
 *
 * This annotation is specifically designed for message-based services (ValidationConsumer, etc.)
 * that process QueueMessage objects and use non-reactive patterns.
 *
 * Key differences from @AuditStep:
 * - Uses NonReactiveAuditTrailService instead of reactive services
 * - Works with ThreadLocal context instead of Reactor context
 * - Optimized for message processing workflows
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NonReactiveAuditStep {
    String serviceName();
    MessageStatus stepName();

    /**
     * Whether to generate a new messageId for this service step.
     * Default: true (recommended for most services)
     * Set to false only if you want to preserve the incoming messageId
     */
    boolean generateNewMessageId() default true;

    /**
     * Whether to extract audit context from QueueMessage automatically.
     * Default: true (recommended for message-based services)
     */
    boolean extractFromQueueMessage() default true;
}