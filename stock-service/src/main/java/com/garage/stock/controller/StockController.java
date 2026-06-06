package com.garage.stock.controller;

import com.garage.stock.dto.StockDto;
import com.garage.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "stock-service"));
    }

    @PostMapping("/initialize")
    public ResponseEntity<StockDto> initializeStock(
            @RequestParam Long productId,
            @RequestParam Integer initialQuantity,
            @RequestParam(defaultValue = "5") Integer minThreshold,
            @RequestParam(defaultValue = "100") Integer maxThreshold) {
        
        log.info("Initializing stock for product: {}", productId);
        StockDto stock = stockService.initializeStock(productId, initialQuantity, minThreshold, maxThreshold);
        return ResponseEntity.status(HttpStatus.CREATED).body(stock);
    }

    @PostMapping("/{productId}/add")
    public ResponseEntity<StockDto> addStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false, defaultValue = "Manual entry") String reference) {
        
        log.info("Adding stock for product: {}, quantity: {}", productId, quantity);
        StockDto stock = stockService.addStock(productId, quantity, reference);
        return ResponseEntity.ok(stock);
    }

    @PostMapping("/{productId}/remove")
    public ResponseEntity<StockDto> removeStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) Long mechanicId) {
        
        log.info("Removing stock for product: {}, quantity: {}", productId, quantity);
        StockDto stock = stockService.removeStock(productId, quantity, reference, mechanicId);
        return ResponseEntity.ok(stock);
    }

    @PostMapping("/{productId}/reserve")
    public ResponseEntity<StockDto> reserveStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity,
            @RequestParam Long serviceRecordId,
            @RequestParam Long vehicleId) {
        
        log.info("Reserving stock for product: {}, quantity: {}, service: {}", 
                productId, quantity, serviceRecordId);
        StockDto stock = stockService.reserveStock(productId, quantity, serviceRecordId, vehicleId);
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<StockDto>> getLowStockItems() {
        log.info("Fetching low stock items");
        List<StockDto> lowStockItems = stockService.getLowStockItems();
        return ResponseEntity.ok(lowStockItems);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<StockDto> getStock(@PathVariable Long productId) {
        log.info("Fetching stock for product: {}", productId);
        StockDto stock = stockService.getStockByProductId(productId);
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<StockDto>> getStockByCategory(@PathVariable String category) {
        log.info("Fetching stock by category: {}", category);
        List<StockDto> stocks = stockService.getStockByCategory(category);
        return ResponseEntity.ok(stocks);
    }

    @GetMapping
    public ResponseEntity<List<StockDto>> getAllStock() {
        log.info("Fetching all stock");
        List<StockDto> stocks = stockService.getAllStock();
        return ResponseEntity.ok(stocks);
    }
}

