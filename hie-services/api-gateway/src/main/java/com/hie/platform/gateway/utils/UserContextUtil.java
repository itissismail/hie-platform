package com.hie.platform.gateway.utils;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class UserContextUtil {

    public static String getCurrentUserId(ServerHttpRequest request) {
        return request.getHeaders().getFirst("X-User-ID");
    }

    public static String getCurrentUserRole(ServerHttpRequest request) {
        return request.getHeaders().getFirst("X-User-Role");
    }

    public static boolean isAuthenticated(ServerHttpRequest request) {
        return getCurrentUserId(request) != null;
    }
}