package com.hie.platform.gateway.config.roles;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RolePermission {
    private String roleName;
    private Map<String, List<String>> resources;

}