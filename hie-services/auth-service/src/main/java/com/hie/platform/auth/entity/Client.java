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

    public void setId(UUID id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientRoles() {
        return clientRoles;
    }

    public void setClientRoles(String clientRoles) {
        this.clientRoles = clientRoles;
    }

    public String getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(String grantTypes) {
        this.grantTypes = grantTypes;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public Integer getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public void setAccessTokenValidity(Integer accessTokenValidity) {
        this.accessTokenValidity = accessTokenValidity;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public UUID getId() {
        return id;
    }
    // Standard getters and setters...
}