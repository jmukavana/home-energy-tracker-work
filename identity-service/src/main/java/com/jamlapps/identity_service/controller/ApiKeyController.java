package com.jamlapps.identity_service.controller;

import com.jamlapps.identity_service.dto.ApiKeyCreatedResponse;
import com.jamlapps.identity_service.dto.ApiKeyDto;
import com.jamlapps.identity_service.dto.CreateApiKeyRequest;
import com.jamlapps.identity_service.entity.ApiKey;
import com.jamlapps.identity_service.service.ApiKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/identity/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyCreatedResponse create(@RequestBody CreateApiKeyRequest request) {
        String rawKey = apiKeyService.generate(request.userId(), request.label());
        return new ApiKeyCreatedResponse(rawKey);
    }

    @GetMapping("/user/{userId}")
    public List<ApiKeyDto> listForUser(@PathVariable Long userId) {
        return apiKeyService.listForUser(userId).stream()
                .map(ApiKeyController::toDto)
                .toList();
    }

    @DeleteMapping("/{id}")
    public void revoke(@PathVariable Long id) {
        apiKeyService.revoke(id);
    }

    private static ApiKeyDto toDto(ApiKey key) {
        return ApiKeyDto.builder()
                .id(key.getId())
                .userId(key.getUserId())
                .label(key.getLabel())
                .keyPrefix(key.getKeyPrefix())
                .createdAt(key.getCreatedAt())
                .revokedAt(key.getRevokedAt())
                .lastUsedAt(key.getLastUsedAt())
                .build();
    }
}
