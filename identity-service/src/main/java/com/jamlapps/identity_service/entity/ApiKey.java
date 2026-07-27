package com.jamlapps.identity_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * The raw key is never stored — only its SHA-256 hash. {@code keyPrefix} (the
 * first 8 characters of the raw key) is kept in plaintext purely so a user can
 * recognize which key is which in a list, matching the standard API-key UX
 * (GitHub/Stripe-style "ghp_1a2b...", never the full secret, after creation).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "api_key")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String label;
    private String hashedKey;
    private String keyPrefix;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;
    private LocalDateTime lastUsedAt;
}
