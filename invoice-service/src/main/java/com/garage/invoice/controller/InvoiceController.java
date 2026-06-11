package com.garage.invoice.controller;

import com.garage.invoice.dto.InvoiceDto;
import com.garage.invoice.service.IInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final IInvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<List<InvoiceDto.InvoiceResponse>> getAll(
            @RequestParam(required = false) Long clientId) {
        if (clientId != null) return ResponseEntity.ok(invoiceService.getByClientId(clientId));
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDto.InvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    @PostMapping
    public ResponseEntity<InvoiceDto.InvoiceResponse> create(
            @Valid @RequestBody InvoiceDto.CreateRequest request) {
        log.info("Creating invoice for client {}", request.getClientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceDto.InvoiceResponse> update(
            @PathVariable Long id,
            @RequestBody InvoiceDto.UpdateRequest request) {
        return ResponseEntity.ok(invoiceService.update(id, request));
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<InvoiceDto.InvoiceResponse> pay(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceDto.PayRequest request) {
        log.info("Paying invoice {} via {}", id, request.getPaymentMethod());
        return ResponseEntity.ok(invoiceService.pay(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
