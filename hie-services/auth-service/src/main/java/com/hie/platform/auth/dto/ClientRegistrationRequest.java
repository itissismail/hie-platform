package com.hie.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for client registration
 */
public class ClientRegistrationRequest {

    @NotBlank(message = "Client name is required")
    @Size(min = 3, max = 100, message = "Client name must be between 3 and 100 characters")
    private String clientName;

    @NotNull(message = "Client roles are required")
    @Size(min = 1, message = "At least one role must be specified")
    private List<String> clientRoles;

    private List<String> scopes;
    private Integer accessTokenValidity;

    // Constructors
    public ClientRegistrationRequest() {}

    public ClientRegistrationRequest(String clientName, List<String> clientRoles) {
        this.clientName = clientName;
        this.clientRoles = clientRoles;
    }

    // Getters and Setters
    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public List<String> getClientRoles() {
        return clientRoles;
    }

    public void setClientRoles(List<String> clientRoles) {
        this.clientRoles = clientRoles;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public Integer getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public void setAccessTokenValidity(Integer accessTokenValidity) {
        this.accessTokenValidity = accessTokenValidity;
    }
}