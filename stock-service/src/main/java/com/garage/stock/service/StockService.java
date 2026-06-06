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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final StockKafkaProducer kafkaProducer;

    @Transactional
    public StockDto initializeStock(Long productId, Integer initialQuantity, Integer minThreshold, Integer maxThreshold) {
        log.info("Initializing stock for product: {}, quantity: {}", productId, initialQuantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        Stock stock = Stock.builder()
                .product(product)
                .quantity(initialQuantity)
                .minThreshold(minThreshold)
                .maxThreshold(maxThreshold)
                .reserved(0)
                .build();
        
        Stock saved = stockRepository.save(stock);
        
        // Record the initial stock entry
        recordStockMovement(
            saved,
            StockHistory.StockMovementType.IN,
            initialQuantity,
            "Product ID: " + productId,
            "Initial stock insertion"
        );
        
        return mapToDto(saved);
    }

    @Transactional
    public StockDto addStock(Long productId, Integer quantity, String reference) {
        log.info("Adding stock for product: {}, quantity: {}", productId, quantity);
        
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Stock record not found for product: " + productId));
        
        stock.setQuantity(stock.getQuantity() + quantity);
        stock.setLastRestockDate(LocalDateTime.now());
        Stock updated = stockRepository.save(stock);
        
        // Record the stock movement
        recordStockMovement(updated, StockHistory.StockMovementType.IN, quantity, reference, "Stock added");
        
        // Publish event
        kafkaProducer.publishProductAdded(KafkaEvents.ProductAddedEvent.builder()
                .productId(productId)
                .productCode(stock.getProduct().getCode())
                .productName(stock.getProduct().getName())
                .category(stock.getProduct().getCategory())
                .addedAt(LocalDateTime.now())
                .build());
        
        return mapToDto(updated);
    }

    @Transactional
    public StockDto removeStock(Long productId, Integer quantity, String reference, Long mechanicId) {
        log.info("Removing stock for product: {}, quantity: {}", productId, quantity);
        
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Stock record not found for product: " + productId));
        
        if (stock.getAvailableQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for product: " + productId + 
                    ". Required: " + quantity + ", Available: " + stock.getAvailableQuantity());
        }
        
        stock.setQuantity(stock.getQuantity() - quantity);
        Stock updated = stockRepository.save(stock);
        
        // Record the stock movement
        StockHistory history = recordStockMovement(updated, StockHistory.StockMovementType.OUT, quantity, reference, "Stock removed");
        if (mechanicId != null) {
            history.setMechanicId(mechanicId);
            stockHistoryRepository.save(history);
        }
        
        return mapToDto(updated);
    }

    @Transactional
    public StockDto reserveStock(Long productId, Integer quantity, Long serviceRecordId, Long vehicleId) {
        log.info("Reserving stock for product: {}, quantity: {}, service: {}", productId, quantity, serviceRecordId);
        
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Stock record not found for product: " + productId));
        
        if (stock.getAvailableQuantity() < quantity) {
            throw new RuntimeException("Cannot reserve stock. Required: " + quantity + 
                    ", Available: " + stock.getAvailableQuantity());
        }
        
        stock.setReserved(stock.getReserved() + quantity);
        Stock updated = stockRepository.save(stock);
        
        // Record the stock movement
        recordStockMovement(updated, StockHistory.StockMovementType.RESERVED, quantity, 
                "Service: " + serviceRecordId, "Stock reserved for service");
        
        // Publish event
        kafkaProducer.publishStockReserved(KafkaEvents.StockReservedEvent.builder()
                .stockId(updated.getId())
                .productId(productId)
                .productCode(stock.getProduct().getCode())
                .reservedQuantity(quantity)
                .serviceRecordId(serviceRecordId)
                .vehicleId(vehicleId)
                .reservedAt(LocalDateTime.now())
                .build());
        
        return mapToDto(updated);
    }

    @Transactional
    public void checkAndAlertLowStock(String category) {
        log.debug("Checking low stock for category: {}", category);
        
        List<Stock> lowStockItems = stockRepository.findLowStockItems().stream()
                .filter(s -> s.getProduct().getCategory().equals(category))
                .collect(Collectors.toList());
        
        for (Stock stock : lowStockItems) {
            log.warn("Low stock alert for product: {} (Category: {}), Available: {}",
                    stock.getProduct().getCode(), category, stock.getAvailableQuantity());
            
            // Publish low stock event
            kafkaProducer.publishProductLowStock(KafkaEvents.ProductLowStockEvent.builder()
                    .productId(stock.getProduct().getId())
                    .productCode(stock.getProduct().getCode())
                    .productName(stock.getProduct().getName())
                    .currentQuantity(stock.getQuantity())
                    .minThreshold(stock.getMinThreshold())
                    .availableQuantity(stock.getAvailableQuantity())
                    .alertAt(LocalDateTime.now())
                    .build());
        }
    }

    public List<StockDto> getLowStockItems() {
        return stockRepository.findLowStockItems().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public StockDto getStockByProductId(Long productId) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Stock record not found for product: " + productId));
        return mapToDto(stock);
    }

    public List<StockDto> getStockByCategory(String category) {
        return stockRepository.findByProductCategoryAndQuantityGreaterThanZero(category).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<StockDto> getAllStock() {
        return stockRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private StockHistory recordStockMovement(Stock stock, StockHistory.StockMovementType type, 
                                           Integer quantity, String reference, String notes) {
        StockHistory history = StockHistory.builder()
                .product(stock.getProduct())
                .stock(stock)
                .movementType(type)
                .quantity(quantity)
                .reference(reference)
                .notes(notes)
                .build();
        
        return stockHistoryRepository.save(history);
    }

    private StockDto mapToDto(Stock stock) {
        return StockDto.builder()
                .id(stock.getId())
                .productId(stock.getProduct().getId())
                .productName(stock.getProduct().getName())
                .productCode(stock.getProduct().getCode())
                .quantity(stock.getQuantity())
                .minThreshold(stock.getMinThreshold())
                .maxThreshold(stock.getMaxThreshold())
                .reserved(stock.getReserved())
                .availableQuantity(stock.getAvailableQuantity())
                .lowStock(stock.isLowStock())
                .lastRestockDate(stock.getLastRestockDate())
                .updatedAt(stock.getUpdatedAt())
                .build();
    }
}

