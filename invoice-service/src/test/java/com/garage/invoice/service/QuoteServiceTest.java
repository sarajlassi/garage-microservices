package com.garage.invoice.service;

import com.garage.invoice.dto.InvoiceDto;
import com.garage.invoice.dto.QuoteDto;
import com.garage.invoice.entity.*;
import com.garage.invoice.repository.InvoiceRepository;
import com.garage.invoice.repository.QuoteRepository;
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
class QuoteServiceTest {

    @Mock private QuoteRepository quoteRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private IInvoiceService invoiceService;

    @InjectMocks
    private QuoteService quoteService;

    private Quote sampleQuote;

    @BeforeEach
    void setUp() {
        sampleQuote = Quote.builder()
                .id(1L)
                .quoteNumber("QUOT-2026-00001")
                .clientId(10L)
                .clientName("Alice")
                .date(LocalDate.now())
                .description("Brake service estimate")
                .total(BigDecimal.valueOf(150))
                .status(QuoteStatus.DRAFT)
                .lines(new ArrayList<>())
                .build();
    }

    // ── getAllQuotes ───────────────────────────────────────────────────────────

    @Test
    void getAllQuotes_returnsMappedList() {
        when(quoteRepository.findAll()).thenReturn(List.of(sampleQuote));

        List<QuoteDto.QuoteResponse> result = quoteService.getAllQuotes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuoteNumber()).isEqualTo("QUOT-2026-00001");
    }

    @Test
    void getById_found_returnsResponse() {
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(sampleQuote));

        QuoteDto.QuoteResponse resp = quoteService.getById(1L);

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getClientName()).isEqualTo("Alice");
    }

    @Test
    void getById_notFound_throws() {
        when(quoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Quote not found");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_noLines_success() {
        QuoteDto.CreateRequest req = QuoteDto.CreateRequest.builder()
                .clientId(10L).clientName("Alice").description("Service estimate").build();

        when(quoteRepository.count()).thenReturn(0L);
        when(quoteRepository.save(any(Quote.class))).thenReturn(sampleQuote);

        QuoteDto.QuoteResponse resp = quoteService.create(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getQuoteNumber()).isEqualTo("QUOT-2026-00001");
        verify(quoteRepository).save(any());
    }

    @Test
    void create_withLines_addsAndComputesTotal() {
        QuoteDto.LineRequest line = QuoteDto.LineRequest.builder()
                .description("Brake pads").quantity(2).unitPrice(BigDecimal.valueOf(40)).build();

        QuoteDto.CreateRequest req = QuoteDto.CreateRequest.builder()
                .clientId(10L).clientName("Alice").lines(List.of(line))
                .date(LocalDate.now()).build();

        QuoteLine ql = QuoteLine.builder().id(1L).description("Brake pads")
                .quantity(2).unitPrice(BigDecimal.valueOf(40)).lineTotal(BigDecimal.valueOf(80)).build();

        Quote quoteWithLines = Quote.builder()
                .id(2L).quoteNumber("QUOT-2026-00002").clientId(10L).clientName("Alice")
                .total(BigDecimal.valueOf(80)).status(QuoteStatus.DRAFT)
                .lines(new ArrayList<>(List.of(ql))).build();

        when(quoteRepository.count()).thenReturn(1L);
        when(quoteRepository.save(any())).thenReturn(quoteWithLines);

        QuoteDto.QuoteResponse resp = quoteService.create(req);

        assertThat(resp.getLines()).hasSize(1);
    }

    @Test
    void create_nullDate_defaultsToToday() {
        QuoteDto.CreateRequest req = QuoteDto.CreateRequest.builder()
                .clientId(10L).clientName("Bob").date(null).build();

        when(quoteRepository.count()).thenReturn(0L);
        when(quoteRepository.save(any())).thenReturn(sampleQuote);

        quoteService.create(req);

        verify(quoteRepository).save(argThat(q -> q.getDate() != null));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_allFields_updatesAndSaves() {
        QuoteDto.UpdateRequest req = QuoteDto.UpdateRequest.builder()
                .clientName("Bob").description("Updated estimate")
                .date(LocalDate.now().plusDays(1)).status("ACCEPTED").build();

        when(quoteRepository.findById(1L)).thenReturn(Optional.of(sampleQuote));
        when(quoteRepository.save(any())).thenReturn(sampleQuote);

        QuoteDto.QuoteResponse resp = quoteService.update(1L, req);

        assertThat(sampleQuote.getClientName()).isEqualTo("Bob");
        assertThat(sampleQuote.getStatus()).isEqualTo(QuoteStatus.ACCEPTED);
        verify(quoteRepository).save(sampleQuote);
    }

    @Test
    void update_invalidStatus_ignoresStatusChange() {
        QuoteDto.UpdateRequest req = QuoteDto.UpdateRequest.builder()
                .status("NOT_A_VALID_STATUS").build();

        when(quoteRepository.findById(1L)).thenReturn(Optional.of(sampleQuote));
        when(quoteRepository.save(any())).thenReturn(sampleQuote);

        quoteService.update(1L, req);

        assertThat(sampleQuote.getStatus()).isEqualTo(QuoteStatus.DRAFT);
    }

    @Test
    void update_withNewLines_replacesLines() {
        QuoteDto.LineRequest line = QuoteDto.LineRequest.builder()
                .description("Oil filter").quantity(1).unitPrice(BigDecimal.valueOf(15)).build();

        QuoteDto.UpdateRequest req = QuoteDto.UpdateRequest.builder()
                .lines(List.of(line)).build();

        when(quoteRepository.findById(1L)).thenReturn(Optional.of(sampleQuote));
        when(quoteRepository.save(any())).thenReturn(sampleQuote);

        quoteService.update(1L, req);

        assertThat(sampleQuote.getLines()).hasSize(1);
        assertThat(sampleQuote.getLines().get(0).getDescription()).isEqualTo("Oil filter");
    }

    @Test
    void update_notFound_throws() {
        when(quoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.update(99L, QuoteDto.UpdateRequest.builder().build()))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── convertToInvoice ──────────────────────────────────────────────────────

    @Test
    void convertToInvoice_success_marksConverted() {
        QuoteLine ql = QuoteLine.builder().description("Part A")
                .quantity(1).unitPrice(BigDecimal.valueOf(100)).lineTotal(BigDecimal.valueOf(100)).build();
        sampleQuote.getLines().add(ql);

        InvoiceDto.InvoiceResponse invoiceResp = InvoiceDto.InvoiceResponse.builder()
                .id(50L).invoiceNumber("INV-2026-00050").clientId(10L).build();

        when(quoteRepository.findById(1L)).thenReturn(Optional.of(sampleQuote));
        when(invoiceService.create(any())).thenReturn(invoiceResp);
        when(quoteRepository.save(any())).thenReturn(sampleQuote);

        InvoiceDto.InvoiceResponse result = quoteService.convertToInvoice(1L);

        assertThat(result.getInvoiceNumber()).isEqualTo("INV-2026-00050");
        assertThat(sampleQuote.getStatus()).isEqualTo(QuoteStatus.CONVERTED);
        assertThat(sampleQuote.getConvertedInvoiceId()).isEqualTo(50L);
    }

    @Test
    void convertToInvoice_alreadyConverted_throws() {
        sampleQuote.setStatus(QuoteStatus.CONVERTED);
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(sampleQuote));

        assertThatThrownBy(() -> quoteService.convertToInvoice(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already converted");
    }

    @Test
    void convertToInvoice_notFound_throws() {
        when(quoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.convertToInvoice(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_callsRepository() {
        doNothing().when(quoteRepository).deleteById(1L);

        quoteService.delete(1L);

        verify(quoteRepository).deleteById(1L);
    }

    // ── entity behaviour ──────────────────────────────────────────────────────

    @Test
    void quoteLine_computeTotal_setsLineTotal() {
        QuoteLine line = QuoteLine.builder()
                .quantity(3).unitPrice(BigDecimal.valueOf(20)).build();
        line.computeTotal();
        assertThat(line.getLineTotal()).isEqualByComparingTo("60");
    }

    @Test
    void quote_recomputeTotal_sumsLines() {
        QuoteLine l1 = QuoteLine.builder().quantity(2).unitPrice(BigDecimal.valueOf(30)).build();
        l1.computeTotal();
        QuoteLine l2 = QuoteLine.builder().quantity(1).unitPrice(BigDecimal.valueOf(50)).build();
        l2.computeTotal();

        Quote q = Quote.builder().lines(new ArrayList<>(List.of(l1, l2))).build();
        q.recomputeTotal();

        assertThat(q.getTotal()).isEqualByComparingTo("110");
    }
}
