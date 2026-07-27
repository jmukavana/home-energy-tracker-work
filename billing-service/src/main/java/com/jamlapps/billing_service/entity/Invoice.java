package com.jamlapps.billing_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoice")
public class Invoice {

    public enum Status { DRAFT, FINALIZED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private double totalKwh;
    private long amountCents;
    private String currency;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String stripeInvoiceId;
    private Instant createdAt;
}
