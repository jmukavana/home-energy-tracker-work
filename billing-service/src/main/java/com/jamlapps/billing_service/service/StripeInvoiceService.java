package com.jamlapps.billing_service.service;

import com.jamlapps.billing_service.entity.Invoice;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.InvoiceItem;
import com.stripe.param.InvoiceItemCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Real Stripe SDK usage, gated behind {@code billing.stripe.enabled} (default
 * false): this environment has no live Stripe test-mode secret key (that
 * requires the caller's own Stripe account, exactly like Keycloak realm setup
 * or insight-service's Ollama dependency already require user-supplied
 * external setup elsewhere in this repo) — documented plainly rather than
 * faked. With Stripe disabled, invoice generation/record-keeping works
 * entirely standalone against this service's own database.
 */
@Slf4j
@Service
public class StripeInvoiceService {

    @Value("${billing.stripe.enabled:false}")
    private boolean enabled;

    @Value("${billing.stripe.api-key:}")
    private String apiKey;

    @PostConstruct
    void init() {
        if (enabled) {
            if (apiKey.isBlank()) {
                log.warn("billing.stripe.enabled=true but billing.stripe.api-key is not set; Stripe calls will fail");
            }
            Stripe.apiKey = apiKey;
        }
    }

    /** Returns the Stripe invoice item id, or {@code null} if Stripe integration is disabled. */
    public String recordUsageCharge(Invoice invoice, String stripeCustomerId) {
        if (!enabled) {
            log.info("Stripe integration disabled (billing.stripe.enabled=false); skipping remote invoice for userId {}", invoice.getUserId());
            return null;
        }

        try {
            InvoiceItemCreateParams params = InvoiceItemCreateParams.builder()
                    .setCustomer(stripeCustomerId)
                    .setAmount(invoice.getAmountCents())
                    .setCurrency(invoice.getCurrency().toLowerCase())
                    .setDescription("Energy usage %.2f kWh, %s to %s".formatted(
                            invoice.getTotalKwh(), invoice.getPeriodStart(), invoice.getPeriodEnd()))
                    .build();
            InvoiceItem item = InvoiceItem.create(params);
            return item.getId();
        } catch (StripeException e) {
            log.error("Stripe invoice item creation failed for userId {}: {}", invoice.getUserId(), e.getMessage());
            throw new IllegalStateException("Stripe invoice creation failed", e);
        }
    }
}
