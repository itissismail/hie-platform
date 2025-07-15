package com.hie.platform.shared.audit.annotation;

import com.hie.platform.shared.audit.config.AuditAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(AuditAutoConfiguration.class)
public @interface EnableAuditTrail {
}