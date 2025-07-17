package com.hie.platform.auth.controller;


import com.hie.platform.auth.dto.ClientRegistrationRequest;
import com.hie.platform.auth.dto.ClientRegistrationResponse;
import com.hie.platform.auth.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing OAuth2 client registrations.
 * Provides endpoints for registering new clients and retrieving client information.
 */
@RestController
@RequestMapping("/oauth2")
public class ClientController {

    private final ClientService clientService;

    @Autowired
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Register a new OAuth2 client
     *
     * @param request Client registration details
     * @return ClientRegistrationResponse containing client credentials
     */
    @PostMapping("/register")
    public ResponseEntity<ClientRegistrationResponse> registerClient(
            @Valid @RequestBody ClientRegistrationRequest request) {

        try {
            ClientRegistrationResponse response = clientService.registerClient(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get all active clients (without sensitive information)
     *
     * @return List of active clients
     */
    @GetMapping
    public ResponseEntity<List<ClientRegistrationResponse>> getAllClients() {
        List<ClientRegistrationResponse> clients = clientService.getAllActiveClients();
        return ResponseEntity.ok(clients);
    }

    /**
     * Get client by ID (without sensitive information)
     *
     * @param clientId The client ID
     * @return Client information
     */
    @GetMapping("/{clientId}")
    public ResponseEntity<ClientRegistrationResponse> getClient(@PathVariable String clientId) {
        try {
            ClientRegistrationResponse client = clientService.getClientById(clientId);
            return ResponseEntity.ok(client);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate a client
     *
     * @param clientId The client ID to deactivate
     * @return Success response
     */
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> deactivateClient(@PathVariable String clientId) {
        try {
            clientService.deactivateClient(clientId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}