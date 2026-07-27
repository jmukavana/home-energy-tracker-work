package com.jamlapps.identity_service.service;

import com.jamlapps.identity_service.entity.ApiKey;
import com.jamlapps.identity_service.repository.ApiKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ApiKeyService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final AuditPublisher auditPublisher;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, AuditPublisher auditPublisher) {
        this.apiKeyRepository = apiKeyRepository;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Returns the raw key exactly once — it is never stored or retrievable again,
     * matching every real API-key system's UX (GitHub, Stripe, AWS, etc.).
     */
    public String generate(Long userId, String label) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawKey = "het_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        ApiKey apiKey = ApiKey.builder()
                .userId(userId)
                .label(label)
                .hashedKey(hash(rawKey))
                .keyPrefix(rawKey.substring(0, 12))
                .createdAt(LocalDateTime.now())
                .build();
        apiKey = apiKeyRepository.save(apiKey);

        auditPublisher.publish(userId, "API_KEY_CREATED", "ApiKey", String.valueOf(apiKey.getId()), "label=" + label);
        return rawKey;
    }

    public void revoke(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));
        apiKey.setRevokedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        auditPublisher.publish(apiKey.getUserId(), "API_KEY_REVOKED", "ApiKey", String.valueOf(id), "label=" + apiKey.getLabel());
    }

    public List<ApiKey> listForUser(Long userId) {
        return apiKeyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Returns the owning userId if the raw key is valid and not revoked, updating
     * {@code lastUsedAt} as a side effect. Empty otherwise.
     */
    public Optional<Long> validate(String rawKey) {
        return apiKeyRepository.findByHashedKey(hash(rawKey))
                .filter(k -> k.getRevokedAt() == null)
                .map(k -> {
                    k.setLastUsedAt(LocalDateTime.now());
                    apiKeyRepository.save(k);
                    return k.getUserId();
                });
    }

    private static String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
