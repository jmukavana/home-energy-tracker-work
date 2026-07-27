package com.jamlapps.identity_service.dto;

public record CreateApiKeyRequest(Long userId, String label) {
}
