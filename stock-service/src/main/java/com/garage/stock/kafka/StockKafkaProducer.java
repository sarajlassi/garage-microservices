package com.garage.stock.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.product-added}")
    private String productAddedTopic;

    @Value("${kafka.topics.product-updated}")
    private String productUpdatedTopic;

    @Value("${kafka.topics.product-low-stock}")
    private String productLowStockTopic;

    @Value("${kafka.topics.stock-reserved}")
    private String stockReservedTopic;

    @Value("${kafka.topics.supplier-order-placed}")
    private String supplierOrderPlacedTopic;

    public void publishProductAdded(KafkaEvents.ProductAddedEvent event) {
        log.info("Publishing ProductAddedEvent for product: {}", event.getProductCode());
        kafkaTemplate.send(productAddedTopic, event.getProductId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("ProductAddedEvent sent successfully: {}", event.getProductCode());
                    } else {
                        log.error("Failed to send ProductAddedEvent", ex);
                    }
                });
    }

    public void publishProductLowStock(KafkaEvents.ProductLowStockEvent event) {
        log.warn("Publishing ProductLowStockEvent for product: {} (Available: {})",
                event.getProductCode(), event.getAvailableQuantity());
        kafkaTemplate.send(productLowStockTopic, event.getProductId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("ProductLowStockEvent sent: {}", event.getProductCode());
                    } else {
                        log.error("Failed to send ProductLowStockEvent", ex);
                    }
                });
    }

    public void publishStockReserved(KafkaEvents.StockReservedEvent event) {
        log.info("Publishing StockReservedEvent for product: {}, quantity: {}",
                event.getProductCode(), event.getReservedQuantity());
        kafkaTemplate.send(stockReservedTopic, event.getProductId().toString(), event);
    }

    public void publishSupplierOrderPlaced(KafkaEvents.SupplierOrderPlacedEvent event) {
        log.info("Publishing SupplierOrderPlacedEvent for product: {}, quantity: {}, supplier: {}",
                event.getProductCode(), event.getQuantity(), event.getSupplier());
        kafkaTemplate.send(supplierOrderPlacedTopic, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("SupplierOrderPlacedEvent sent: {} (ref: {})",
                                event.getProductCode(), event.getReferenceNumber());
                    } else {
                        log.error("Failed to send SupplierOrderPlacedEvent", ex);
                    }
                });
    }
}

