package com.garage.invoice.service;

import com.garage.invoice.dto.InvoiceDto;
import com.garage.invoice.entity.Invoice;
import com.garage.invoice.entity.InvoiceLine;
import com.garage.invoice.entity.InvoiceStatus;
import com.garage.invoice.kafka.InvoiceKafkaProducer;
import com.garage.invoice.kafka.KafkaEvents;
import com.garage.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceKafkaProducer kafkaProducer;

    @InjectMocks
    private InvoiceService invoiceService;

    private Invoice sampleInvoice;

    @BeforeEach
    void setUp() {
        sampleInvoice = Invoice.builder()
                .id(1L)
                .invoiceNumber("INV-2026-00001")
                .clientId(10L)
                .clientName("Alice Dupont")
                .clientPhone("0600000001")
                .vehicleId(5L)
                .vehicleName("Renault Clio")
                .licensePlate("AB-123-CD")
                .mechanicName("Bob")
                .description("Oil change")
                .entryDate(LocalDate.now())
                .invoiceDate(LocalDate.now())
                .laborCost(BigDecimal.valueOf(80))
                .totalParts(BigDecimal.ZERO)
                .total(BigDecimal.valueOf(80))
                .paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.DRAFT)
                .lines(new ArrayList<>())
                .build();
    }

    // ── getAllInvoices ─────────────────────────────────────────────────────────

    @Test
    void getAllInvoices_returnsMappedList() {
        when(invoiceRepository.findAll()).thenReturn(List.of(sampleInvoice));

        List<InvoiceDto.InvoiceResponse> result = invoiceService.getAllInvoices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvoiceNumber()).isEqualTo("INV-2026-00001");
    }

    @Test
    void getByClientId_returnsList() {
        when(invoiceRepository.findByClientId(10L)).thenReturn(List.of(sampleInvoice));

        List<InvoiceDto.InvoiceResponse> result = invoiceService.getByClientId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClientId()).isEqualTo(10L);
    }

    @Test
    void getById_found_returnsResponse() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(sampleInvoice));

        InvoiceDto.InvoiceResponse resp = invoiceService.getById(1L);

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getClientName()).isEqualTo("Alice Dupont");
    }

    @Test
    void getById_notFound_throws() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Invoice not found");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_withNoLines_success() {
        InvoiceDto.CreateRequest req = InvoiceDto.CreateRequest.builder()
                .clientId(10L).clientName("Alice").laborCost(BigDecimal.valueOf(80))
                .invoiceDate(LocalDate.now()).build();

        when(invoiceRepository.findMaxSequenceForYear(anyString())).thenReturn(null);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(sampleInvoice);

        InvoiceDto.InvoiceResponse resp = invoiceService.create(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getInvoiceNumber()).isEqualTo("INV-2026-00001");
        verify(invoiceRepository).save(any());
    }

    @Test
    void create_withLines_addsLinesAndComputesTotal() {
        InvoiceDto.LineRequest line = InvoiceDto.LineRequest.builder()
                .name("Brake Pad").quantity(2).unitPrice(BigDecimal.valueOf(30)).build();

        InvoiceDto.CreateRequest req = InvoiceDto.CreateRequest.builder()
                .clientId(10L).clientName("Alice").laborCost(BigDecimal.valueOf(50))
                .lines(List.of(line)).build();

        // Build a proper invoice with lines for the saved result
        InvoiceLine invoiceLine = InvoiceLine.builder()
                .id(1L).name("Brake Pad").quantity(2).unitPrice(BigDecimal.valueOf(30))
                .lineTotal(BigDecimal.valueOf(60)).build();

        Invoice invoiceWithLines = Invoice.builder()
                .id(2L).invoiceNumber("INV-2026-00002")
                .clientId(10L).clientName("Alice")
                .laborCost(BigDecimal.valueOf(50)).totalParts(BigDecimal.valueOf(60))
                .total(BigDecimal.valueOf(110)).paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.DRAFT).lines(List.of(invoiceLine)).build();

        when(invoiceRepository.findMaxSequenceForYear(anyString())).thenReturn(1L);
        when(invoiceRepository.save(any())).thenReturn(invoiceWithLines);

        InvoiceDto.InvoiceResponse resp = invoiceService.create(req);

        assertThat(resp.getLines()).hasSize(1);
        assertThat(resp.getTotal()).isEqualByComparingTo("110");
    }

    @Test
    void create_noInvoiceDate_defaultsToToday() {
        InvoiceDto.CreateRequest req = InvoiceDto.CreateRequest.builder()
                .clientId(10L).clientName("Bob").laborCost(null).build();

        when(invoiceRepository.findMaxSequenceForYear(anyString())).thenReturn(0L);
        when(invoiceRepository.save(any())).thenReturn(sampleInvoice);

        invoiceService.create(req);

        verify(invoiceRepository).save(argThat(inv ->
                inv.getInvoiceDate() != null && inv.getEntryDate() != null));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_allFields_updatesAndSaves() {
        InvoiceDto.UpdateRequest req = InvoiceDto.UpdateRequest.builder()
                .clientName("Bob").description("Brake service")
                .laborCost(BigDecimal.valueOf(120)).mechanicName("Charlie").build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(sampleInvoice));
        when(invoiceRepository.save(any())).thenReturn(sampleInvoice);

        InvoiceDto.InvoiceResponse resp = invoiceService.update(1L, req);

        assertThat(sampleInvoice.getClientName()).isEqualTo("Bob");
        assertThat(sampleInvoice.getLaborCost()).isEqualByComparingTo("120");
        verify(invoiceRepository).save(sampleInvoice);
    }

    @Test
    void update_withNewLines_replacesLines() {
        InvoiceDto.LineRequest line = InvoiceDto.LineRequest.builder()
                .name("Filter").quantity(1).unitPrice(BigDecimal.valueOf(20)).build();

        InvoiceDto.UpdateRequest req = InvoiceDto.UpdateRequest.builder()
                .lines(List.of(line)).build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(sampleInvoice));
        when(invoiceRepository.save(any())).thenReturn(sampleInvoice);

        invoiceService.update(1L, req);

        assertThat(sampleInvoice.getLines()).hasSize(1);
        assertThat(sampleInvoice.getLines().get(0).getName()).isEqualTo("Filter");
    }

    @Test
    void update_notFound_throws() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.update(99L, InvoiceDto.UpdateRequest.builder().build()))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── pay ───────────────────────────────────────────────────────────────────

    @Test
    void pay_fullPayment_setsStatusPaid() {
        InvoiceDto.PayRequest req = InvoiceDto.PayRequest.builder()
                .amount(BigDecimal.valueOf(80)).paymentMethod("CARD").build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(sampleInvoice));
        when(invoiceRepository.save(any())).thenReturn(sampleInvoice);
        doNothing().when(kafkaProducer).publishInvoicePaid(any());

        InvoiceDto.InvoiceResponse resp = invoiceService.pay(1L, req);

        assertThat(sampleInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(sampleInvoice.getPaymentMethod()).isEqualTo("CARD");
        verify(kafkaProducer).publishInvoicePaid(any());
    }

    @Test
    void pay_partialPayment_setsStatusPartial() {
        InvoiceDto.PayRequest req = InvoiceDto.PayRequest.builder()
                .amount(BigDecimal.valueOf(40)).paymentMethod("CASH").build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(sampleInvoice));
        when(invoiceRepository.save(any())).thenReturn(sampleInvoice);
        doNothing().when(kafkaProducer).publishInvoicePaid(any());

        invoiceService.pay(1L, req);

        assertThat(sampleInvoice.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
    }

    @Test
    void pay_nullAmount_usesInvoiceTotal() {
        InvoiceDto.PayRequest req = InvoiceDto.PayRequest.builder()
                .amount(null).paymentMethod("CASH").build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(sampleInvoice));
        when(invoiceRepository.save(any())).thenReturn(sampleInvoice);
        doNothing().when(kafkaProducer).publishInvoicePaid(any());

        invoiceService.pay(1L, req);

        assertThat(sampleInvoice.getPaidAmount()).isEqualByComparingTo("80");
        assertThat(sampleInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void pay_notFound_throws() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.pay(99L,
                InvoiceDto.PayRequest.builder().amount(BigDecimal.TEN).paymentMethod("CASH").build()))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_callsRepository() {
        doNothing().when(invoiceRepository).deleteById(1L);

        invoiceService.delete(1L);

        verify(invoiceRepository).deleteById(1L);
    }

    // ── entity behaviour ──────────────────────────────────────────────────────

    @Test
    void invoice_recomputeTotal_laborPlusParts() {
        InvoiceLine line = InvoiceLine.builder()
                .lineTotal(BigDecimal.valueOf(60)).build();

        Invoice inv = Invoice.builder()
                .laborCost(BigDecimal.valueOf(40)).lines(new ArrayList<>(List.of(line))).build();

        inv.recomputeTotal();

        assertThat(inv.getTotal()).isEqualByComparingTo("100");
        assertThat(inv.getTotalParts()).isEqualByComparingTo("60");
    }

    @Test
    void invoice_recomputeTotal_emptyLines_onlyLabor() {
        Invoice inv = Invoice.builder()
                .laborCost(BigDecimal.valueOf(50)).lines(new ArrayList<>()).build();

        inv.recomputeTotal();

        assertThat(inv.getTotal()).isEqualByComparingTo("50");
        assertThat(inv.getTotalParts()).isEqualByComparingTo("0");
    }

    @Test
    void mapToResponse_remaining_isNeverNegative() {
        sampleInvoice.setPaidAmount(BigDecimal.valueOf(100));
        sampleInvoice.setTotal(BigDecimal.valueOf(80));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(sampleInvoice));

        InvoiceDto.InvoiceResponse resp = invoiceService.getById(1L);

        assertThat(resp.getRemaining()).isEqualByComparingTo("0");
    }
}
