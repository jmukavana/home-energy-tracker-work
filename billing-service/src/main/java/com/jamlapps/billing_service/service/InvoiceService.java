package com.jamlapps.billing_service.service;

import com.jamlapps.billing_service.client.UsageClient;
import com.jamlapps.billing_service.dto.UsageDto;
import com.jamlapps.billing_service.entity.Invoice;
import com.jamlapps.billing_service.repository.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class InvoiceService {

    private final UsageClient usageClient;
    private final InvoiceRepository invoiceRepository;
    private final StripeInvoiceService stripeInvoiceService;

    @Value("${billing.rate-per-kwh-cents:15}")
    private long ratePerKwhCents;

    public InvoiceService(UsageClient usageClient, InvoiceRepository invoiceRepository, StripeInvoiceService stripeInvoiceService) {
        this.usageClient = usageClient;
        this.invoiceRepository = invoiceRepository;
        this.stripeInvoiceService = stripeInvoiceService;
    }

    public Invoice generateInvoice(Long userId, int days) {
        UsageDto usage = usageClient.getXDaysUsageForUser(userId, days);

        double totalKwh = usage == null || usage.devices() == null
                ? 0.0
                : usage.devices().stream()
                        .mapToDouble(d -> d.energyConsumed() == null ? 0.0 : d.energyConsumed())
                        .sum();

        long amountCents = Math.round(totalKwh * ratePerKwhCents);

        Invoice invoice = Invoice.builder()
                .userId(userId)
                .periodStart(LocalDate.now().minusDays(days))
                .periodEnd(LocalDate.now())
                .totalKwh(totalKwh)
                .amountCents(amountCents)
                .currency("USD")
                .status(Invoice.Status.DRAFT)
                .createdAt(Instant.now())
                .build();

        invoice = invoiceRepository.save(invoice);

        // Stripe requires a customer id we don't have a source for yet (Phase 3
        // doesn't add Stripe customer provisioning) — recordUsageCharge no-ops
        // whenever Stripe integration is disabled, which is the shipped default.
        String stripeItemId = stripeInvoiceService.recordUsageCharge(invoice, "cus_" + userId);
        if (stripeItemId != null) {
            invoice.setStripeInvoiceId(stripeItemId);
            invoice.setStatus(Invoice.Status.FINALIZED);
            invoice = invoiceRepository.save(invoice);
        }

        log.info("Generated invoice {} for userId {}: {} kWh, {} cents", invoice.getId(), userId, totalKwh, amountCents);
        return invoice;
    }

    public List<Invoice> listForUser(Long userId) {
        return invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
