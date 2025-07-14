package com.hie.platform.gateway.config.roles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hie.platform.gateway.config.roles.AclConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

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
                .anyMatch(entry ->
                        path.equals(entry.getKey()) &&
                                entry.getValue().contains(method.toUpperCase())
                );
    }
}
