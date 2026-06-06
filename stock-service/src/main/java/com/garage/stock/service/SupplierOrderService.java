package com.garage.stock.service;

import com.garage.stock.dto.SupplierOrderDto;
import com.garage.stock.entity.Product;
import com.garage.stock.entity.SupplierOrder;
import com.garage.stock.kafka.KafkaEvents;
import com.garage.stock.kafka.StockKafkaProducer;
import com.garage.stock.repository.ProductRepository;
import com.garage.stock.repository.SupplierOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierOrderService {

    private final SupplierOrderRepository supplierOrderRepository;
    private final ProductRepository productRepository;
    private final StockKafkaProducer kafkaProducer;

    @Transactional
    public SupplierOrderDto placeOrder(Long productId, Integer quantity, BigDecimal unitPrice, 
                                       String supplier, LocalDate expectedDeliveryDate) {
        log.info("Placing supplier order - Product: {}, Quantity: {}, Supplier: {}", 
                productId, quantity, supplier);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        String referenceNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        SupplierOrder order = SupplierOrder.builder()
                .product(product)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                .supplier(supplier)
                .status(SupplierOrder.OrderStatus.PENDING)
                .orderDate(LocalDate.now())
                .expectedDeliveryDate(expectedDeliveryDate)
                .referenceNumber(referenceNumber)
                .build();
        
        SupplierOrder saved = supplierOrderRepository.save(order);
        
        // Publish event
        kafkaProducer.publishSupplierOrderPlaced(KafkaEvents.SupplierOrderPlacedEvent.builder()
                .orderId(saved.getId())
                .productId(productId)
                .productCode(product.getCode())
                .quantity(quantity)
                .totalPrice(saved.getTotalPrice())
                .supplier(supplier)
                .expectedDeliveryDate(expectedDeliveryDate)
                .referenceNumber(referenceNumber)
                .placedAt(LocalDateTime.now())
                .build());
        
        return mapToDto(saved);
    }

    @Transactional
    public SupplierOrderDto updateOrderStatus(Long orderId, SupplierOrder.OrderStatus newStatus) {
        log.info("Updating order status - Order: {}, New Status: {}", orderId, newStatus);
        
        SupplierOrder order = supplierOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        order.setStatus(newStatus);
        
        if (newStatus == SupplierOrder.OrderStatus.RECEIVED) {
            order.setActualDeliveryDate(LocalDate.now());
        }
        
        SupplierOrder updated = supplierOrderRepository.save(order);
        return mapToDto(updated);
    }

    @Transactional
    public SupplierOrderDto receiveOrder(Long orderId) {
        log.info("Receiving order: {}", orderId);
        return updateOrderStatus(orderId, SupplierOrder.OrderStatus.RECEIVED);
    }

    public SupplierOrderDto getOrder(Long orderId) {
        SupplierOrder order = supplierOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return mapToDto(order);
    }

    public List<SupplierOrderDto> getOrdersByProduct(Long productId) {
        return supplierOrderRepository.findByProductId(productId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<SupplierOrderDto> getOrdersByStatus(SupplierOrder.OrderStatus status) {
        return supplierOrderRepository.findByStatus(status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<SupplierOrderDto> getPendingOrders() {
        return getOrdersByStatus(SupplierOrder.OrderStatus.PENDING);
    }

    public List<SupplierOrderDto> getOrdersBySupplier(String supplier) {
        return supplierOrderRepository.findBySupplier(supplier).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<SupplierOrderDto> getOrdersByExpectedDeliveryDate(LocalDate startDate, LocalDate endDate) {
        return supplierOrderRepository.findByExpectedDeliveryDateBetween(startDate, endDate).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<SupplierOrderDto> getAllOrders() {
        return supplierOrderRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private SupplierOrderDto mapToDto(SupplierOrder order) {
        return SupplierOrderDto.builder()
                .id(order.getId())
                .productId(order.getProduct().getId())
                .productName(order.getProduct().getName())
                .productCode(order.getProduct().getCode())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalPrice(order.getTotalPrice())
                .supplier(order.getSupplier())
                .status(order.getStatus().toString())
                .orderDate(order.getOrderDate())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .actualDeliveryDate(order.getActualDeliveryDate())
                .referenceNumber(order.getReferenceNumber())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

