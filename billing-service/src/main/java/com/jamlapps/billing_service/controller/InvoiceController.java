package com.jamlapps.billing_service.controller;

import com.jamlapps.billing_service.dto.InvoiceDto;
import com.jamlapps.billing_service.entity.Invoice;
import com.jamlapps.billing_service.service.InvoiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/generate/{userId}")
    public InvoiceDto generate(@PathVariable Long userId, @RequestParam(defaultValue = "30") int days) {
        return toDto(invoiceService.generateInvoice(userId, days));
    }

    @GetMapping("/user/{userId}")
    public List<InvoiceDto> listForUser(@PathVariable Long userId) {
        return invoiceService.listForUser(userId).stream().map(InvoiceController::toDto).toList();
    }

    private static InvoiceDto toDto(Invoice invoice) {
        return InvoiceDto.builder()
                .id(invoice.getId())
                .userId(invoice.getUserId())
                .periodStart(invoice.getPeriodStart())
                .periodEnd(invoice.getPeriodEnd())
                .totalKwh(invoice.getTotalKwh())
                .amountCents(invoice.getAmountCents())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus().name())
                .stripeInvoiceId(invoice.getStripeInvoiceId())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
