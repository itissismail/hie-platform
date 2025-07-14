package com.hie.platform.auth.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// Client.java
@Entity
@Table(name = "clients")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "client_id", unique = true, nullable = false)
    private String clientId;

    @Column(name = "client_secret", nullable = false)
    private String clientSecret;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_roles", nullable = false)
    private String clientRoles; // Comma-separated roles

    @Column(name = "grant_types")
    private String grantTypes = "client_credentials";

    @Column(name = "scopes")
    private String scopes = "read,write";

    @Column(name = "access_token_validity")
    private Integer accessTokenValidity = 3600;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors, getters, setters
    public Client() {}

    public Client(String clientName, String clientRoles) {
        this.clientName = clientName;
        this.clientRoles = clientRoles;
        this.clientId = generateClientId();
        this.clientSecret = generateClientSecret();
    }

    private String generateClientId() {
        return "client_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String generateClientSecret() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public List<String> getRolesList() {
        return Arrays.asList(clientRoles.split(","));
    }

    // Standard getters and setters...
}