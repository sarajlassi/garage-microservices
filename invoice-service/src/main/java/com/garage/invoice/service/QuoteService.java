package com.garage.invoice.service;

import com.garage.invoice.dto.InvoiceDto;
import com.garage.invoice.dto.QuoteDto;
import com.garage.invoice.entity.*;
import com.garage.invoice.repository.InvoiceRepository;
import com.garage.invoice.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteService implements IQuoteService {

    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final IInvoiceService invoiceService;

    public List<QuoteDto.QuoteResponse> getAllQuotes() {
        return quoteRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public QuoteDto.QuoteResponse getById(Long id) {
        return mapToResponse(findById(id));
    }

    @Transactional
    public QuoteDto.QuoteResponse create(QuoteDto.CreateRequest req) {
        Quote quote = Quote.builder()
                .quoteNumber(generateQuoteNumber())
                .clientId(req.getClientId())
                .clientName(req.getClientName())
                .date(req.getDate() != null ? req.getDate() : LocalDate.now())
                .description(req.getDescription())
                .build();

        if (req.getLines() != null) {
            List<QuoteLine> lines = req.getLines().stream().map(l -> {
                QuoteLine line = QuoteLine.builder()
                        .quote(quote)
                        .description(l.getDescription())
                        .quantity(l.getQuantity())
                        .unitPrice(l.getUnitPrice())
                        .build();
                line.computeTotal();
                return line;
            }).collect(Collectors.toList());
            quote.getLines().addAll(lines);
        }

        quote.recomputeTotal();
        Quote saved = quoteRepository.save(quote);
        log.info("Created quote {}", saved.getQuoteNumber());
        return mapToResponse(saved);
    }

    @Transactional
    public QuoteDto.QuoteResponse update(Long id, QuoteDto.UpdateRequest req) {
        Quote quote = findById(id);
        if (req.getClientName() != null) quote.setClientName(req.getClientName());
        if (req.getDescription() != null) quote.setDescription(req.getDescription());
        if (req.getDate() != null) quote.setDate(req.getDate());
        if (req.getStatus() != null) {
            try {
                quote.setStatus(QuoteStatus.valueOf(req.getStatus()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (req.getLines() != null) {
            quote.getLines().clear();
            req.getLines().forEach(l -> {
                QuoteLine line = QuoteLine.builder()
                        .quote(quote)
                        .description(l.getDescription())
                        .quantity(l.getQuantity())
                        .unitPrice(l.getUnitPrice())
                        .build();
                line.computeTotal();
                quote.getLines().add(line);
            });
        }
        quote.recomputeTotal();
        return mapToResponse(quoteRepository.save(quote));
    }

    @Transactional
    public InvoiceDto.InvoiceResponse convertToInvoice(Long quoteId) {
        Quote quote = findById(quoteId);
        if (quote.getStatus() == QuoteStatus.CONVERTED) {
            throw new IllegalStateException("Quote already converted");
        }

        List<InvoiceDto.LineRequest> lines = quote.getLines().stream()
                .map(l -> InvoiceDto.LineRequest.builder()
                        .name(l.getDescription())
                        .quantity(l.getQuantity())
                        .unitPrice(l.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        InvoiceDto.InvoiceResponse inv = invoiceService.create(InvoiceDto.CreateRequest.builder()
                .clientId(quote.getClientId())
                .clientName(quote.getClientName())
                .description(quote.getDescription())
                .invoiceDate(LocalDate.now())
                .laborCost(BigDecimal.ZERO)
                .lines(lines)
                .build());

        quote.setStatus(QuoteStatus.CONVERTED);
        quote.setConvertedInvoiceId(inv.getId());
        quoteRepository.save(quote);

        log.info("Quote {} converted to invoice {}", quote.getQuoteNumber(), inv.getInvoiceNumber());
        return inv;
    }

    @Transactional
    public void delete(Long id) {
        quoteRepository.deleteById(id);
    }

    private Quote findById(Long id) {
        return quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Quote not found: " + id));
    }

    private String generateQuoteNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        long count = quoteRepository.count() + 1;
        return String.format("QUOT-%s-%05d", year, count);
    }

    private QuoteDto.QuoteResponse mapToResponse(Quote q) {
        List<QuoteDto.LineResponse> lines = q.getLines() == null ? new ArrayList<>() :
            q.getLines().stream().map(l -> QuoteDto.LineResponse.builder()
                    .id(l.getId())
                    .description(l.getDescription())
                    .quantity(l.getQuantity())
                    .unitPrice(l.getUnitPrice())
                    .lineTotal(l.getLineTotal())
                    .build()).toList();

        return QuoteDto.QuoteResponse.builder()
                .id(q.getId())
                .quoteNumber(q.getQuoteNumber())
                .clientId(q.getClientId())
                .clientName(q.getClientName())
                .date(q.getDate() != null ? q.getDate().toString() : null)
                .description(q.getDescription())
                .total(q.getTotal())
                .status(q.getStatus() != null ? q.getStatus().name() : null)
                .convertedInvoiceId(q.getConvertedInvoiceId())
                .createdAt(q.getCreatedAt() != null ? q.getCreatedAt().toString() : null)
                .lines(lines)
                .build();
    }
}
