package com.jamlapps.identity_service.dto;

/** {@code rawKey} is returned exactly once — the caller must store it now. */
public record ApiKeyCreatedResponse(String rawKey) {
}
