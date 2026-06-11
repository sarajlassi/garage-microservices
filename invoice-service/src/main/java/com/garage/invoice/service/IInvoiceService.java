package com.garage.invoice.service;

import com.garage.invoice.dto.InvoiceDto;

import java.util.List;

public interface IInvoiceService {

    List<InvoiceDto.InvoiceResponse> getAllInvoices();

    List<InvoiceDto.InvoiceResponse> getByClientId(Long clientId);

    InvoiceDto.InvoiceResponse getById(Long id);

    InvoiceDto.InvoiceResponse create(InvoiceDto.CreateRequest request);

    InvoiceDto.InvoiceResponse update(Long id, InvoiceDto.UpdateRequest request);

    InvoiceDto.InvoiceResponse pay(Long id, InvoiceDto.PayRequest request);

    void delete(Long id);
}
