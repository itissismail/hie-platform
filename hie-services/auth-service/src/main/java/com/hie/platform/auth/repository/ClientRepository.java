package com.hie.platform.auth.repository;

import com.hie.platform.auth.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ClientRepository.java
@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByClientId(String clientId);
    Optional<Client> findByClientIdAndIsActiveTrue(String clientId);
    List<Client> findByIsActiveTrue();
}

