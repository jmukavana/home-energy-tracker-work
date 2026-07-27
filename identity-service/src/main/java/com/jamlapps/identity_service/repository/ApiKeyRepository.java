package com.jamlapps.identity_service.repository;

import com.jamlapps.identity_service.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    List<ApiKey> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ApiKey> findByHashedKey(String hashedKey);
}
