package com.garage.invoice.controller;

import com.garage.invoice.dto.InvoiceDto;
import com.garage.invoice.dto.QuoteDto;
import com.garage.invoice.service.IQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final IQuoteService quoteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<List<QuoteDto.QuoteResponse>> getAll() {
        return ResponseEntity.ok(quoteService.getAllQuotes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<QuoteDto.QuoteResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<QuoteDto.QuoteResponse> create(
            @Valid @RequestBody QuoteDto.CreateRequest request) {
        log.info("Creating quote for client {}", request.getClientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<QuoteDto.QuoteResponse> update(
            @PathVariable Long id,
            @RequestBody QuoteDto.UpdateRequest request) {
        return ResponseEntity.ok(quoteService.update(id, request));
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<InvoiceDto.InvoiceResponse> convert(@PathVariable Long id) {
        log.info("Converting quote {} to invoice", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteService.convertToInvoice(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quoteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
