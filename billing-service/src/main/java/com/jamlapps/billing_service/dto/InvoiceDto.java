package com.jamlapps.billing_service.dto;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record InvoiceDto(
        Long id,
        Long userId,
        LocalDate periodStart,
        LocalDate periodEnd,
        double totalKwh,
        long amountCents,
        String currency,
        String status,
        String stripeInvoiceId,
        Instant createdAt
) {
}
