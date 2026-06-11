package com.garage.invoice.service;

import com.garage.invoice.dto.InvoiceDto;
import com.garage.invoice.entity.Invoice;
import com.garage.invoice.entity.InvoiceLine;
import com.garage.invoice.entity.InvoiceStatus;
import com.garage.invoice.kafka.InvoiceKafkaProducer;
import com.garage.invoice.kafka.KafkaEvents;
import com.garage.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService implements IInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceKafkaProducer kafkaProducer;

    public List<InvoiceDto.InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public List<InvoiceDto.InvoiceResponse> getByClientId(Long clientId) {
        return invoiceRepository.findByClientId(clientId).stream().map(this::mapToResponse).toList();
    }

    public InvoiceDto.InvoiceResponse getById(Long id) {
        return mapToResponse(findById(id));
    }

    @Transactional
    public InvoiceDto.InvoiceResponse create(InvoiceDto.CreateRequest req) {
        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .clientId(req.getClientId())
                .clientName(req.getClientName())
                .clientPhone(req.getClientPhone())
                .vehicleId(req.getVehicleId())
                .vehicleName(req.getVehicleName())
                .licensePlate(req.getLicensePlate())
                .serviceRecordId(req.getServiceRecordId())
                .mechanicName(req.getMechanicName())
                .description(req.getDescription())
                .entryDate(req.getEntryDate() != null ? req.getEntryDate() : LocalDate.now())
                .invoiceDate(req.getInvoiceDate() != null ? req.getInvoiceDate() : LocalDate.now())
                .laborDescription(req.getLaborDescription())
                .laborCost(req.getLaborCost() != null ? req.getLaborCost() : BigDecimal.ZERO)
                .build();

        if (req.getLines() != null) {
            List<InvoiceLine> lines = req.getLines().stream().map(l -> {
                InvoiceLine line = InvoiceLine.builder()
                        .invoice(invoice)
                        .stockItemId(l.getStockItemId())
                        .name(l.getName())
                        .ref(l.getRef())
                        .quantity(l.getQuantity())
                        .unitPrice(l.getUnitPrice())
                        .build();
                line.computeTotal();
                return line;
            }).collect(Collectors.toList());
            invoice.getLines().addAll(lines);
        }

        invoice.recomputeTotal();
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Created invoice {}", saved.getInvoiceNumber());
        return mapToResponse(saved);
    }

    @Transactional
    public InvoiceDto.InvoiceResponse update(Long id, InvoiceDto.UpdateRequest req) {
        Invoice invoice = findById(id);
        if (req.getClientName() != null) invoice.setClientName(req.getClientName());
        if (req.getClientPhone() != null) invoice.setClientPhone(req.getClientPhone());
        if (req.getMechanicName() != null) invoice.setMechanicName(req.getMechanicName());
        if (req.getDescription() != null) invoice.setDescription(req.getDescription());
        if (req.getInvoiceDate() != null) invoice.setInvoiceDate(req.getInvoiceDate());
        if (req.getLaborDescription() != null) invoice.setLaborDescription(req.getLaborDescription());
        if (req.getLaborCost() != null) invoice.setLaborCost(req.getLaborCost());

        if (req.getLines() != null) {
            invoice.getLines().clear();
            req.getLines().forEach(l -> {
                InvoiceLine line = InvoiceLine.builder()
                        .invoice(invoice)
                        .stockItemId(l.getStockItemId())
                        .name(l.getName())
                        .ref(l.getRef())
                        .quantity(l.getQuantity())
                        .unitPrice(l.getUnitPrice())
                        .build();
                line.computeTotal();
                invoice.getLines().add(line);
            });
        }

        invoice.recomputeTotal();
        return mapToResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceDto.InvoiceResponse pay(Long id, InvoiceDto.PayRequest req) {
        Invoice invoice = findById(id);
        BigDecimal amount = req.getAmount() != null ? req.getAmount() : invoice.getTotal();
        invoice.setPaidAmount(amount);
        invoice.setPaymentMethod(req.getPaymentMethod());
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setStatus(amount.compareTo(invoice.getTotal()) >= 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIAL);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} paid via {}", saved.getInvoiceNumber(), req.getPaymentMethod());

        kafkaProducer.publishInvoicePaid(KafkaEvents.InvoicePaidEvent.builder()
                .invoiceId(saved.getId())
                .invoiceNumber(saved.getInvoiceNumber())
                .clientId(saved.getClientId())
                .amount(amount)
                .paidAt(saved.getPaidAt())
                .build());

        return mapToResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        invoiceRepository.deleteById(id);
    }

    private Invoice findById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + id));
    }

    private String generateInvoiceNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        long count = invoiceRepository.count() + 1;
        return String.format("INV-%s-%05d", year, count);
    }

    private InvoiceDto.InvoiceResponse mapToResponse(Invoice inv) {
        List<InvoiceDto.LineResponse> lines = inv.getLines() == null ? new ArrayList<>() :
            inv.getLines().stream().map(l -> InvoiceDto.LineResponse.builder()
                    .id(l.getId())
                    .stockItemId(l.getStockItemId())
                    .name(l.getName())
                    .ref(l.getRef())
                    .quantity(l.getQuantity())
                    .unitPrice(l.getUnitPrice())
                    .lineTotal(l.getLineTotal())
                    .build()).toList();

        BigDecimal remaining = (inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO)
                .subtract(inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO);

        return InvoiceDto.InvoiceResponse.builder()
                .id(inv.getId())
                .invoiceNumber(inv.getInvoiceNumber())
                .clientId(inv.getClientId())
                .clientName(inv.getClientName())
                .clientPhone(inv.getClientPhone())
                .vehicleId(inv.getVehicleId())
                .vehicleName(inv.getVehicleName())
                .licensePlate(inv.getLicensePlate())
                .serviceRecordId(inv.getServiceRecordId())
                .mechanicName(inv.getMechanicName())
                .description(inv.getDescription())
                .entryDate(inv.getEntryDate() != null ? inv.getEntryDate().toString() : null)
                .invoiceDate(inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : null)
                .laborDescription(inv.getLaborDescription())
                .laborCost(inv.getLaborCost())
                .totalParts(inv.getTotalParts())
                .total(inv.getTotal())
                .paidAmount(inv.getPaidAmount())
                .remaining(remaining.max(BigDecimal.ZERO))
                .status(inv.getStatus() != null ? inv.getStatus().name() : null)
                .paymentMethod(inv.getPaymentMethod())
                .paidAt(inv.getPaidAt() != null ? inv.getPaidAt().toString() : null)
                .createdAt(inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : null)
                .lines(lines)
                .build();
    }
}
