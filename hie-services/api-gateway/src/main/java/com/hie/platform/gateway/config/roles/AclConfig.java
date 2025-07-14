package com.hie.platform.gateway.config.roles;

import lombok.Data;

import java.util.List;

@Data
public class AclConfig {
    private List<RolePermission> roles;

}

