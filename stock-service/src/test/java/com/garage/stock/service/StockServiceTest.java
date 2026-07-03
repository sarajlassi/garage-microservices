package com.garage.stock.service;

import com.garage.stock.dto.StockDto;
import com.garage.stock.entity.Product;
import com.garage.stock.entity.Stock;
import com.garage.stock.entity.StockHistory;
import com.garage.stock.kafka.KafkaEvents;
import com.garage.stock.kafka.StockKafkaProducer;
import com.garage.stock.repository.ProductRepository;
import com.garage.stock.repository.StockHistoryRepository;
import com.garage.stock.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock private StockRepository stockRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockHistoryRepository stockHistoryRepository;
    @Mock private StockKafkaProducer kafkaProducer;

    @InjectMocks
    private StockService stockService;

    private Product sampleProduct;
    private Stock sampleStock;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L).code("OIL-001").name("Engine Oil").category("OIL")
                .unitPrice(BigDecimal.valueOf(25.99)).active(true).build();

        sampleStock = Stock.builder()
                .id(100L).product(sampleProduct)
                .quantity(50).minThreshold(5).maxThreshold(100).reserved(0).build();
    }

    // ── initializeStock ───────────────────────────────────────────────────────

    @Test
    void initializeStock_success_returnsDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(stockRepository.save(any(Stock.class))).thenReturn(sampleStock);
        when(stockHistoryRepository.save(any())).thenReturn(mock(StockHistory.class));

        StockDto result = stockService.initializeStock(1L, 50, 5, 100);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getQuantity()).isEqualTo(50);
        verify(stockHistoryRepository).save(any());
    }

    @Test
    void initializeStock_productNotFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.initializeStock(99L, 10, 5, 100))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
    }

    // ── addStock ──────────────────────────────────────────────────────────────

    @Test
    void addStock_success_updatesQuantity() {
        when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(sampleStock));
        when(stockRepository.save(any())).thenReturn(sampleStock);
        when(stockHistoryRepository.save(any())).thenReturn(mock(StockHistory.class));
        doNothing().when(kafkaProducer).publishProductAdded(any());

        StockDto result = stockService.addStock(1L, 20, "PO-001");

        assertThat(sampleStock.getQuantity()).isEqualTo(70);
        verify(kafkaProducer).publishProductAdded(any());
    }

    @Test
    void addStock_stockRecordNotFound_throws() {
        when(stockRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.addStock(99L, 10, "PO-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock record not found");
    }

    // ── removeStock ───────────────────────────────────────────────────────────

    @Test
    void removeStock_success_decreasesQuantity() {
        when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(sampleStock));
        when(stockRepository.save(any())).thenReturn(sampleStock);
        when(stockHistoryRepository.save(any())).thenReturn(mock(StockHistory.class));

        StockDto result = stockService.removeStock(1L, 10, "WO-001", null);

        assertThat(sampleStock.getQuantity()).isEqualTo(40);
        verify(stockRepository).save(sampleStock);
    }

    @Test
    void removeStock_withMechanicId_savesHistory() {
        StockHistory history = StockHistory.builder()
                .id(1L).product(sampleProduct).stock(sampleStock)
                .movementType(StockHistory.StockMovementType.OUT).quantity(5).build();

        when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(sampleStock));
        when(stockRepository.save(any())).thenReturn(sampleStock);
        when(stockHistoryRepository.save(any())).thenReturn(history);

        stockService.removeStock(1L, 5, "WO-002", 10L);

        verify(stockHistoryRepository, times(2)).save(any());
    }

    @Test
    void removeStock_insufficientQuantity_throws() {
        when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(sampleStock));

        assertThatThrownBy(() -> stockService.removeStock(1L, 100, "WO-X", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
    }

    // ── reserveStock ──────────────────────────────────────────────────────────

    @Test
    void reserveStock_success_increasesReserved() {
        when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(sampleStock));
        when(stockRepository.save(any())).thenReturn(sampleStock);
        when(stockHistoryRepository.save(any())).thenReturn(mock(StockHistory.class));
        doNothing().when(kafkaProducer).publishStockReserved(any());

        StockDto result = stockService.reserveStock(1L, 10, 5L, 3L);

        assertThat(sampleStock.getReserved()).isEqualTo(10);
        verify(kafkaProducer).publishStockReserved(any());
    }

    @Test
    void reserveStock_insufficient_throws() {
        sampleStock.setReserved(45); // available = 50 - 45 = 5, requesting 10
        when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(sampleStock));

        assertThatThrownBy(() -> stockService.reserveStock(1L, 10, 5L, 3L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot reserve stock");
    }

    // ── checkAndAlertLowStock ─────────────────────────────────────────────────

    @Test
    void checkAndAlertLowStock_publishesForLowItems() {
        Stock lowStock = Stock.builder()
                .id(200L).product(sampleProduct)
                .quantity(3).minThreshold(5).maxThreshold(100).reserved(0).build();

        when(stockRepository.findLowStockItems()).thenReturn(List.of(lowStock));
        doNothing().when(kafkaProducer).publishProductLowStock(any());

        stockService.checkAndAlertLowStock("OIL");

        verify(kafkaProducer).publishProductLowStock(any());
    }

    @Test
    void checkAndAlertLowStock_differentCategory_doesNotPublish() {
        Stock lowStock = Stock.builder()
                .id(200L).product(sampleProduct)
                .quantity(2).minThreshold(5).maxThreshold(100).reserved(0).build();

        when(stockRepository.findLowStockItems()).thenReturn(List.of(lowStock));

        stockService.checkAndAlertLowStock("FILTER");

        verify(kafkaProducer, never()).publishProductLowStock(any());
    }

    // ── addOrInitializeStock ──────────────────────────────────────────────────

    @Test
    void addOrInitializeStock_existingStock_addsQuantity() {
        when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(sampleStock));
        when(stockRepository.save(any())).thenReturn(sampleStock);
        when(stockHistoryRepository.save(any())).thenReturn(mock(StockHistory.class));

        stockService.addOrInitializeStock(1L, 10, "PO-002");

        assertThat(sampleStock.getQuantity()).isEqualTo(60);
    }

    @Test
    void addOrInitializeStock_noExistingStock_initializes() {
        when(stockRepository.findByProductId(2L)).thenReturn(Optional.empty());
        when(productRepository.findById(2L)).thenReturn(Optional.of(sampleProduct));
        Stock newStock = Stock.builder().id(101L).product(sampleProduct)
                .quantity(10).minThreshold(5).maxThreshold(100).reserved(0).build();
        when(stockRepository.save(any())).thenReturn(newStock);
        when(stockHistoryRepository.save(any())).thenReturn(mock(StockHistory.class));

        StockDto result = stockService.addOrInitializeStock(2L, 10, "PO-003");

        assertThat(result).isNotNull();
    }

    // ── queries ───────────────────────────────────────────────────────────────



    @Test
    void getStockByProductId_found_returnsDto() {
        when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(sampleStock));

        StockDto result = stockService.getStockByProductId(1L);

        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getQuantity()).isEqualTo(50);
    }

    @Test
    void getStockByProductId_notFound_throws() {
        when(stockRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.getStockByProductId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock record not found");
    }

    @Test
    void getStockByCategory_returnsList() {
        when(stockRepository.findByProductCategoryAndQuantityGreaterThanZero("OIL"))
                .thenReturn(List.of(sampleStock));

        List<StockDto> result = stockService.getStockByCategory("OIL");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductCode()).isEqualTo("OIL-001");
    }

    @Test
    void getAllStock_returnsList() {
        when(stockRepository.findAll()).thenReturn(List.of(sampleStock));

        List<StockDto> result = stockService.getAllStock();

        assertThat(result).hasSize(1);
    }

    @Test
    void stockEntity_availableQuantity_computedCorrectly() {
        sampleStock.setQuantity(50);
        sampleStock.setReserved(10);
        assertThat(sampleStock.getAvailableQuantity()).isEqualTo(40);
    }

    @Test
    void stockEntity_isLowStock_trueWhenBelowThreshold() {
        sampleStock.setQuantity(4);
        sampleStock.setReserved(0);
        sampleStock.setMinThreshold(5);
        assertThat(sampleStock.isLowStock()).isTrue();
    }

    @Test
    void stockEntity_isLowStock_falseWhenAboveThreshold() {
        sampleStock.setQuantity(20);
        sampleStock.setReserved(0);
        sampleStock.setMinThreshold(5);
        assertThat(sampleStock.isLowStock()).isFalse();
    }
}
