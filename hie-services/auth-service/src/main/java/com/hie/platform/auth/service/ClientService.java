package com.hie.platform.auth.service;

import com.hie.platform.auth.dto.ClientRegistrationRequest;
import com.hie.platform.auth.dto.ClientRegistrationResponse;
import com.hie.platform.auth.entity.Client;
import com.hie.platform.auth.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing OAuth2 client operations.
 * Handles client registration, authentication, and management.
 */
@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ClientService(ClientRepository clientRepository, PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new OAuth2 client
     *
     * @param request Client registration request
     * @return ClientRegistrationResponse with client details
     */
    public ClientRegistrationResponse registerClient(ClientRegistrationRequest request) {
        // Create new client entity
        Client client = new Client(
                request.getClientName(),
                String.join(",", request.getClientRoles())
        );

        // Set optional fields
        if (request.getScopes() != null && !request.getScopes().isEmpty()) {
            client.setScopes(String.join(",", request.getScopes()));
        }

        if (request.getAccessTokenValidity() != null) {
            client.setAccessTokenValidity(request.getAccessTokenValidity());
        }

        String rawSecret = client.getClientSecret();
        // Encode the client secret before saving
        client.setClientSecret(passwordEncoder.encode(rawSecret));

        // Save to database
        Client savedClient = clientRepository.save(client);

        // Return response (note: we return the raw secret only during registration)
        ClientRegistrationResponse obj= mapToResponse(savedClient, true);
        obj.setClientSecret(rawSecret);
        return obj;
    }

    /**
     * Get all active clients
     *
     * @return List of active clients
     */
    @Transactional(readOnly = true)
    public List<ClientRegistrationResponse> getAllActiveClients() {
        return clientRepository.findByIsActiveTrue()
                .stream()
                .map(client -> mapToResponse(client, false))
                .collect(Collectors.toList());
    }

    /**
     * Get client by client ID
     *
     * @param clientId The client ID
     * @return Client information
     * @throws RuntimeException if client not found
     */
    @Transactional(readOnly = true)
    public ClientRegistrationResponse getClientById(String clientId) {
        Optional<Client> clientOpt = clientRepository.findByClientIdAndIsActiveTrue(clientId);
        if (clientOpt.isEmpty()) {
            throw new RuntimeException("Client not found: " + clientId);
        }
        return mapToResponse(clientOpt.get(), false);
    }

    /**
     * Authenticate client for OAuth2 flow
     *
     * @param clientId Client ID
     * @param clientSecret Raw client secret
     * @return Client if authentication successful
     */
    @Transactional(readOnly = true)
    public Optional<Client> authenticateClient(String clientId, String clientSecret) {
        Optional<Client> clientOpt = clientRepository.findByClientIdAndIsActiveTrue(clientId);

        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            if (passwordEncoder.matches(clientSecret, client.getClientSecret())) {
                return Optional.of(client);
            }
        }
        return Optional.empty();
    }

    /**
     * Deactivate a client
     *
     * @param clientId Client ID to deactivate
     */
    public void deactivateClient(String clientId) {
        Optional<Client> clientOpt = clientRepository.findByClientId(clientId);
        if (clientOpt.isEmpty()) {
            throw new RuntimeException("Client not found: " + clientId);
        }

        Client client = clientOpt.get();
        client.setActive(false);
        clientRepository.save(client);
    }

    /**
     * Find client by client ID (for internal use)
     *
     * @param clientId Client ID
     * @return Client entity
     */
    @Transactional(readOnly = true)
    public Optional<Client> findByClientId(String clientId) {
        return clientRepository.findByClientIdAndIsActiveTrue(clientId);
    }

    /**
     * Map Client entity to ClientRegistrationResponse
     *
     * @param client Client entity
     * @param includeSecret Whether to include client secret in response
     * @return ClientRegistrationResponse
     */
    private ClientRegistrationResponse mapToResponse(Client client, boolean includeSecret) {
        List<String> roles = Arrays.asList(client.getClientRoles().split(","));
        List<String> scopes = Arrays.asList(client.getScopes().split(","));

        ClientRegistrationResponse response = new ClientRegistrationResponse(
                client.getClientId(),
                includeSecret ? client.getClientSecret() : null,
                client.getClientName(),
                roles,
                scopes,
                client.getGrantTypes(),
                client.getAccessTokenValidity(),
                client.getActive(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );

        return response;
    }
}