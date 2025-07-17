package com.hie.platform.gateway.config.roles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hie.platform.gateway.config.roles.AclConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
public class AclLoader {

    private AclConfig aclConfig;

    public AclLoader() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules(); // Optional but helps for advanced types

        aclConfig = mapper.readValue(
                new ClassPathResource("ACL.yml").getInputStream(),
                AclConfig.class
        );
    }

    public boolean isAllowed(String role, String path, String method) {
        return aclConfig.getRoles().stream()
                .filter(r -> r.getRoleName().equals(role))
                .flatMap(r -> r.getResources().entrySet().stream())
                .anyMatch(entry -> isPathMatched(path, entry.getKey()) &&
                        entry.getValue().contains(method.toUpperCase()));
    }

  /*  public boolean isAllowed(String role, String path, String method) {
        return aclConfig.getRoles().stream()
                .filter(r -> r.getRoleName().equals(role))
                .flatMap(r -> r.getResources().entrySet().stream())
                .anyMatch(entry ->
                        path.equals(entry.getKey()) &&
                                entry.getValue().contains(method.toUpperCase())
                );
    }*/

    private boolean isPathMatched(String requestPath, String resourcePath) {
        // Exact match
        if (requestPath.equals(resourcePath)) {
            return true;
        }

        // Wildcard match - check if resource path ends with /**
        if (resourcePath.endsWith("/**")) {
            String basePath = resourcePath.substring(0, resourcePath.length() - 3); // Remove /**
            return requestPath.startsWith(basePath);
        }

        return false;
    }

    // Optional: Helper method to get all resources for a role (for debugging/testing)
    public Map<String, List<String>> getResourcesForRole(String role) {
        return aclConfig.getRoles().stream()
                .filter(r -> r.getRoleName().equals(role))
                .findFirst()
                .map(r -> r.getResources())
                .orElse(Collections.emptyMap());
    }
}
