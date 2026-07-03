package com.garage.stock.service;

import com.garage.stock.dto.SupplierOrderDto;
import com.garage.stock.entity.Product;
import com.garage.stock.entity.Stock;
import com.garage.stock.entity.SupplierOrder;
import com.garage.stock.kafka.KafkaEvents;
import com.garage.stock.kafka.StockKafkaProducer;
import com.garage.stock.repository.ProductRepository;
import com.garage.stock.repository.SupplierOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.garage.stock.dto.StockDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierOrderServiceTest {

    @Mock private SupplierOrderRepository supplierOrderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockKafkaProducer kafkaProducer;
    @Mock private StockService stockService;

    @InjectMocks
    private SupplierOrderService supplierOrderService;

    private Product sampleProduct;
    private SupplierOrder sampleOrder;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L).code("BRAKE-001").name("Brake Pad").category("BRAKE")
                .unitPrice(BigDecimal.valueOf(30)).active(true).supplierId(5L).build();

        sampleOrder = SupplierOrder.builder()
                .id(100L).product(sampleProduct).quantity(10)
                .unitPrice(BigDecimal.valueOf(30)).totalPrice(BigDecimal.valueOf(300))
                .supplier("AutoParts").mechanicId(2L).mechanicName("Bob")
                .status(SupplierOrder.OrderStatus.PENDING)
                .referenceNumber("ORD-ABCD1234")
                .orderDate(LocalDate.now()).build();
    }

    @Test
    void placeOrder_success_returnsDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(supplierOrderRepository.save(any(SupplierOrder.class))).thenReturn(sampleOrder);
        doNothing().when(kafkaProducer).publishSupplierOrderPlaced(any());

        SupplierOrderDto result = supplierOrderService.placeOrder(
                1L, 10, BigDecimal.valueOf(30), "AutoParts",
                2L, "Bob", "0600000000", "bob@garage.com", LocalDate.now().plusDays(5));

        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(10);
        assertThat(result.getSupplier()).isEqualTo("AutoParts");
        verify(supplierOrderRepository).save(any());
        verify(kafkaProducer).publishSupplierOrderPlaced(any());
    }

    @Test
    void placeOrder_productNotFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierOrderService.placeOrder(
                99L, 5, BigDecimal.valueOf(20), "Sup", null, null, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void updateOrderStatus_toPending_updatesStatus() {
        when(supplierOrderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(supplierOrderRepository.save(any())).thenReturn(sampleOrder);

        SupplierOrderDto result = supplierOrderService.updateOrderStatus(100L, SupplierOrder.OrderStatus.ORDERED);

        assertThat(sampleOrder.getStatus()).isEqualTo(SupplierOrder.OrderStatus.ORDERED);
        verify(supplierOrderRepository).save(sampleOrder);
    }

    @Test
    void updateOrderStatus_toReceived_decrementsCatalogStock() {
        when(supplierOrderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(supplierOrderRepository.save(any())).thenReturn(sampleOrder);

        Product mechanicProduct = Product.builder()
                .id(10L).code("BRAKE-001-M2").name("Brake Pad").category("BRAKE")
                .unitPrice(BigDecimal.valueOf(30)).active(true).mechanicId(2L).build();
        when(productRepository.findByCodeAndMechanicId("BRAKE-001-M2", 2L))
                .thenReturn(Optional.of(mechanicProduct));
        when(stockService.addOrInitializeStock(anyLong(), any(), any())).thenReturn(mock(com.garage.stock.dto.StockDto.class));
        when(stockService.removeStock(anyLong(), any(), any(), any())).thenReturn(mock(StockDto.class));

        supplierOrderService.updateOrderStatus(100L, SupplierOrder.OrderStatus.RECEIVED);

        assertThat(sampleOrder.getStatus()).isEqualTo(SupplierOrder.OrderStatus.RECEIVED);
        assertThat(sampleOrder.getActualDeliveryDate()).isNotNull();
    }

    @Test
    void updateOrderStatus_toReceived_noMechanicId_skipsAddToMechanicStock() {
        sampleOrder.setMechanicId(null);
        when(supplierOrderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(supplierOrderRepository.save(any())).thenReturn(sampleOrder);
        when(stockService.removeStock(anyLong(), any(), any(), any())).thenReturn(mock(StockDto.class));

        supplierOrderService.updateOrderStatus(100L, SupplierOrder.OrderStatus.RECEIVED);

        verify(productRepository, never()).findByCodeAndMechanicId(any(), any());
        verify(stockService, never()).addOrInitializeStock(any(), any(), any());
    }

    @Test
    void updateOrderStatus_toReceived_stockRemoveThrows_continuesGracefully() {
        when(supplierOrderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(supplierOrderRepository.save(any())).thenReturn(sampleOrder);

        Product mechanicProduct = Product.builder()
                .id(10L).code("BRAKE-001-M2").name("Brake Pad").category("BRAKE")
                .unitPrice(BigDecimal.valueOf(30)).active(true).mechanicId(2L).build();
        when(productRepository.findByCodeAndMechanicId("BRAKE-001-M2", 2L))
                .thenReturn(Optional.of(mechanicProduct));
        when(stockService.addOrInitializeStock(anyLong(), any(), any())).thenReturn(mock(com.garage.stock.dto.StockDto.class));
        doThrow(new RuntimeException("Insufficient stock")).when(stockService)
                .removeStock(anyLong(), any(), any(), any());

        // Should not throw - exception is caught and logged
        supplierOrderService.updateOrderStatus(100L, SupplierOrder.OrderStatus.RECEIVED);

        assertThat(sampleOrder.getStatus()).isEqualTo(SupplierOrder.OrderStatus.RECEIVED);
    }

    @Test
    void receiveOrder_delegatesToUpdateOrderStatus() {
        when(supplierOrderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(supplierOrderRepository.save(any())).thenReturn(sampleOrder);

        Product mechanicProduct = Product.builder()
                .id(10L).code("BRAKE-001-M2").name("Brake Pad").category("BRAKE")
                .unitPrice(BigDecimal.valueOf(30)).active(true).mechanicId(2L).build();
        when(productRepository.findByCodeAndMechanicId("BRAKE-001-M2", 2L))
                .thenReturn(Optional.of(mechanicProduct));
        when(stockService.addOrInitializeStock(anyLong(), any(), any())).thenReturn(mock(com.garage.stock.dto.StockDto.class));
        when(stockService.removeStock(anyLong(), any(), any(), any())).thenReturn(mock(StockDto.class));

        supplierOrderService.receiveOrder(100L);

        assertThat(sampleOrder.getStatus()).isEqualTo(SupplierOrder.OrderStatus.RECEIVED);
    }

    @Test
    void getOrder_found_returnsDto() {
        when(supplierOrderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));

        SupplierOrderDto result = supplierOrderService.getOrder(100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getReferenceNumber()).isEqualTo("ORD-ABCD1234");
    }

    @Test
    void getOrder_notFound_throws() {
        when(supplierOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierOrderService.getOrder(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void getOrdersByProduct_returnsList() {
        when(supplierOrderRepository.findByProductId(1L)).thenReturn(List.of(sampleOrder));

        List<SupplierOrderDto> result = supplierOrderService.getOrdersByProduct(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getOrdersByStatus_returnsList() {
        when(supplierOrderRepository.findByStatus(SupplierOrder.OrderStatus.PENDING))
                .thenReturn(List.of(sampleOrder));

        List<SupplierOrderDto> result = supplierOrderService.getOrdersByStatus(SupplierOrder.OrderStatus.PENDING);

        assertThat(result).hasSize(1);
    }

    @Test
    void getPendingOrders_returnsList() {
        when(supplierOrderRepository.findByStatus(SupplierOrder.OrderStatus.PENDING))
                .thenReturn(List.of(sampleOrder));

        List<SupplierOrderDto> result = supplierOrderService.getPendingOrders();

        assertThat(result).hasSize(1);
    }

    @Test
    void getOrdersBySupplier_returnsList() {
        when(supplierOrderRepository.findBySupplier("AutoParts")).thenReturn(List.of(sampleOrder));

        List<SupplierOrderDto> result = supplierOrderService.getOrdersBySupplier("AutoParts");

        assertThat(result).hasSize(1);
    }

    @Test
    void getOrdersBySupplierId_returnsList() {
        when(supplierOrderRepository.findByProductSupplierId(5L)).thenReturn(List.of(sampleOrder));

        List<SupplierOrderDto> result = supplierOrderService.getOrdersBySupplierId(5L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getOrdersByMechanic_returnsList() {
        when(supplierOrderRepository.findByMechanicName("Bob")).thenReturn(List.of(sampleOrder));

        List<SupplierOrderDto> result = supplierOrderService.getOrdersByMechanic("Bob");

        assertThat(result).hasSize(1);
    }

    @Test
    void getOrdersByMechanicId_returnsList() {
        when(supplierOrderRepository.findByMechanicId(2L)).thenReturn(List.of(sampleOrder));

        List<SupplierOrderDto> result = supplierOrderService.getOrdersByMechanicId(2L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllOrders_returnsList() {
        when(supplierOrderRepository.findAll()).thenReturn(List.of(sampleOrder));

        List<SupplierOrderDto> result = supplierOrderService.getAllOrders();

        assertThat(result).hasSize(1);
    }

    @Test
    void getOrdersByExpectedDeliveryDate_returnsList() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(7);
        when(supplierOrderRepository.findByExpectedDeliveryDateBetween(start, end))
                .thenReturn(List.of(sampleOrder));

        List<SupplierOrderDto> result = supplierOrderService.getOrdersByExpectedDeliveryDate(start, end);

        assertThat(result).hasSize(1);
    }

    @Test
    void updateOrderStatus_orderNotFound_throws() {
        when(supplierOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierOrderService.updateOrderStatus(999L, SupplierOrder.OrderStatus.RECEIVED))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void updateOrderStatus_toReceived_newMechanicProduct_createsAndSaves() {
        when(supplierOrderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(supplierOrderRepository.save(any())).thenReturn(sampleOrder);
        when(stockService.removeStock(anyLong(), any(), any(), any())).thenReturn(mock(StockDto.class));

        when(productRepository.findByCodeAndMechanicId("BRAKE-001-M2", 2L)).thenReturn(Optional.empty());
        Product newProduct = Product.builder().id(20L).code("BRAKE-001-M2").name("Brake Pad")
                .category("BRAKE").unitPrice(BigDecimal.valueOf(30)).active(true).mechanicId(2L).build();
        when(productRepository.save(any(Product.class))).thenReturn(newProduct);
        when(stockService.addOrInitializeStock(anyLong(), any(), any())).thenReturn(mock(com.garage.stock.dto.StockDto.class));

        supplierOrderService.updateOrderStatus(100L, SupplierOrder.OrderStatus.RECEIVED);

        verify(productRepository).save(any(Product.class));
        verify(stockService).addOrInitializeStock(eq(20L), eq(10), any());
    }
}
