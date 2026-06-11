package com.garage.invoice.repository;

import com.garage.invoice.entity.Quote;
import com.garage.invoice.entity.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    List<Quote> findByClientId(Long clientId);
    List<Quote> findByStatus(QuoteStatus status);
}
