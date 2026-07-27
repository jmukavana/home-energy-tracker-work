package com.jamlapps.billing_service.repository;

import com.jamlapps.billing_service.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUserIdOrderByCreatedAtDesc(Long userId);
}
