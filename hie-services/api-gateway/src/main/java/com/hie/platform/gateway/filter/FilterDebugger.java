package com.hie.platform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author mismail
 * @description FilterDebugger class handles ...
 * @since 17/07/2025
 */
@Component
@Slf4j
public class FilterDebugger implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private List<GatewayFilterFactory> filterFactories;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.debug("Available Gateway Filters:");
        filterFactories.forEach(factory -> {
            log.debug("- " + factory.getClass().getSimpleName());
        });
    }
}
