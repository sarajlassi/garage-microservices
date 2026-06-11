package com.garage.invoice.service;

import com.garage.invoice.dto.InvoiceDto;
import com.garage.invoice.dto.QuoteDto;

import java.util.List;

public interface IQuoteService {

    List<QuoteDto.QuoteResponse> getAllQuotes();

    QuoteDto.QuoteResponse getById(Long id);

    QuoteDto.QuoteResponse create(QuoteDto.CreateRequest request);

    QuoteDto.QuoteResponse update(Long id, QuoteDto.UpdateRequest request);

    InvoiceDto.InvoiceResponse convertToInvoice(Long quoteId);

    void delete(Long id);
}
