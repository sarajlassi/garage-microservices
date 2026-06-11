package com.garage.stock.controller;

import com.garage.stock.dto.SupplierOrderDto;
import com.garage.stock.entity.SupplierOrder;
import com.garage.stock.service.SupplierOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/stock/orders")
@RequiredArgsConstructor
public class SupplierOrderController {

    private final SupplierOrderService supplierOrderService;

    @PostMapping
    public ResponseEntity<SupplierOrderDto> placeOrder(
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam BigDecimal unitPrice,
            @RequestParam String supplier,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedDeliveryDate) {
        
        log.info("Placing order - Product: {}, Quantity: {}, Supplier: {}", productId, quantity, supplier);
        
        if (expectedDeliveryDate == null) {
            expectedDeliveryDate = LocalDate.now().plusDays(7);
        }
        
        SupplierOrderDto order = supplierOrderService.placeOrder(productId, quantity, unitPrice, supplier, expectedDeliveryDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<SupplierOrderDto> getOrder(@PathVariable Long orderId) {
        log.info("Fetching order: {}", orderId);
        SupplierOrderDto order = supplierOrderService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<SupplierOrderDto>> getOrdersByProduct(@PathVariable Long productId) {
        log.info("Fetching orders for product: {}", productId);
        List<SupplierOrderDto> orders = supplierOrderService.getOrdersByProduct(productId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<SupplierOrderDto>> getOrdersByStatus(@PathVariable String status) {
        log.info("Fetching orders with status: {}", status);
        SupplierOrder.OrderStatus orderStatus = SupplierOrder.OrderStatus.valueOf(status.toUpperCase());
        List<SupplierOrderDto> orders = supplierOrderService.getOrdersByStatus(orderStatus);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<SupplierOrderDto>> getPendingOrders() {
        log.info("Fetching pending orders");
        List<SupplierOrderDto> orders = supplierOrderService.getPendingOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/supplier/{supplier}")
    public ResponseEntity<List<SupplierOrderDto>> getOrdersBySupplier(@PathVariable String supplier) {
        log.info("Fetching orders from supplier: {}", supplier);
        List<SupplierOrderDto> orders = supplierOrderService.getOrdersBySupplier(supplier);
        return ResponseEntity.ok(orders);
    }

    @GetMapping
    public ResponseEntity<List<SupplierOrderDto>> getAllOrders() {
        log.info("Fetching all orders");
        List<SupplierOrderDto> orders = supplierOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<SupplierOrderDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        log.info("Updating order status - Order: {}, Status: {}", orderId, status);
        SupplierOrder.OrderStatus orderStatus = SupplierOrder.OrderStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(supplierOrderService.updateOrderStatus(orderId, orderStatus));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<SupplierOrderDto> patchOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        log.info("Patching order status - Order: {}, Status: {}", orderId, status);
        SupplierOrder.OrderStatus orderStatus = SupplierOrder.OrderStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(supplierOrderService.updateOrderStatus(orderId, orderStatus));
    }

    @PutMapping("/{orderId}/receive")
    public ResponseEntity<SupplierOrderDto> receiveOrder(@PathVariable Long orderId) {
        log.info("Receiving order: {}", orderId);
        SupplierOrderDto order = supplierOrderService.receiveOrder(orderId);
        return ResponseEntity.ok(order);
    }
}

