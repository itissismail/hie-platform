package com.hie.platform.shared.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Author M.Ismail
 *  Method-level annotation for automatic audit logging
 *  Date 15-July-2025
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditStep {
    String serviceName();
    String stepName();
}
