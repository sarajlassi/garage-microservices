package com.garage.stock.service;

import com.garage.stock.dto.ProductDto;
import com.garage.stock.entity.Product;
import com.garage.stock.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductDto createProduct(ProductDto dto) {
        log.info("Creating product: {}", dto.getCode());
        Product product = Product.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .description(dto.getDescription())
                .unitPrice(dto.getUnitPrice())
                .category(dto.getCategory())
                .supplier(dto.getSupplier())
                .sku(dto.getSku())
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();
        
        Product saved = productRepository.save(product);
        return mapToDto(saved);
    }

    public ProductDto getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        return mapToDto(product);
    }

    public ProductDto getProductByCode(String code) {
        Product product = productRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Product not found: " + code));
        return mapToDto(product);
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ProductDto updateProduct(Long id, ProductDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        
        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getUnitPrice() != null) product.setUnitPrice(dto.getUnitPrice());
        if (dto.getCategory() != null) product.setCategory(dto.getCategory());
        if (dto.getSupplier() != null) product.setSupplier(dto.getSupplier());
        if (dto.getActive() != null) product.setActive(dto.getActive());

        product.setUpdatedAt(LocalDateTime.now());
        Product updated = productRepository.save(product);
        return mapToDto(updated);
    }

    public void deleteProduct(Long id) {
        log.info("Deleting product: {}", id);
        productRepository.deleteById(id);
    }

    private ProductDto mapToDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .description(product.getDescription())
                .unitPrice(product.getUnitPrice())
                .category(product.getCategory())
                .supplier(product.getSupplier())
                .sku(product.getSku())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}

