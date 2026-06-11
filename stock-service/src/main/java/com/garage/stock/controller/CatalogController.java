package com.garage.stock.controller;

import com.garage.stock.dto.ProductDto;
import com.garage.stock.entity.Product;
import com.garage.stock.entity.Stock;
import com.garage.stock.repository.ProductRepository;
import com.garage.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Catalog endpoint for supplier view.
 * Returns products enriched with available stock quantity.
 */
@Slf4j
@RestController
@RequestMapping("/api/stock/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    @GetMapping
    public ResponseEntity<List<CatalogItem>> getCatalog() {
        return ResponseEntity.ok(buildCatalog(productRepository.findAll()));
    }

    @GetMapping("/{supplierId}")
    public ResponseEntity<List<CatalogItem>> getCatalogBySupplier(@PathVariable Long supplierId) {
        log.info("Fetching catalog for supplierId {}", supplierId);
        List<Product> products = productRepository.findBySupplierId(supplierId);
        return ResponseEntity.ok(buildCatalog(products));
    }

    @PutMapping("/piece/{id}")
    public ResponseEntity<ProductDto> updatePiece(@PathVariable Long id, @RequestBody ProductDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Product not found: " + id));
        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getUnitPrice() != null) product.setUnitPrice(dto.getUnitPrice());
        if (dto.getCategory() != null) product.setCategory(dto.getCategory());
        if (dto.getSupplier() != null) product.setSupplier(dto.getSupplier());
        if (dto.getSupplierId() != null) product.setSupplierId(dto.getSupplierId());
        Product saved = productRepository.save(product);
        return ResponseEntity.ok(toDto(saved));
    }

    private List<CatalogItem> buildCatalog(List<Product> products) {
        return products.stream().map(p -> {
            Optional<Stock> stock = stockRepository.findByProductId(p.getId());
            int availableQty = stock.map(s -> s.getQuantity() - (s.getReserved() != null ? s.getReserved() : 0))
                    .orElse(0);
            return CatalogItem.builder()
                    .id(p.getId())
                    .ref(p.getCode())
                    .name(p.getName())
                    .category(p.getCategory())
                    .unitPrice(p.getUnitPrice())
                    .availableQty(availableQty)
                    .supplierId(p.getSupplierId())
                    .supplierName(p.getSupplier())
                    .description(p.getDescription())
                    .build();
        }).toList();
    }

    private ProductDto toDto(Product p) {
        return ProductDto.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .unitPrice(p.getUnitPrice())
                .category(p.getCategory())
                .supplier(p.getSupplier())
                .supplierId(p.getSupplierId())
                .sku(p.getSku())
                .active(p.getActive())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CatalogItem {
        private Long id;
        private String ref;
        private String name;
        private String category;
        private java.math.BigDecimal unitPrice;
        private Integer availableQty;
        private Long supplierId;
        private String supplierName;
        private String description;
    }
}
