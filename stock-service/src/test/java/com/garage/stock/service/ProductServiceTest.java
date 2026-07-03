package com.garage.stock.service;

import com.garage.stock.dto.ProductDto;
import com.garage.stock.entity.Product;
import com.garage.stock.repository.ProductRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;
    private ProductDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L).code("BRAKE-001").name("Brake Pad").description("Front brake pad")
                .unitPrice(BigDecimal.valueOf(49.99)).category("BRAKE").supplier("SupplierA")
                .supplierId(10L).sku("SKU-001").active(true).supplierCatalog(false).build();

        sampleDto = ProductDto.builder()
                .code("BRAKE-001").name("Brake Pad").description("Front brake pad")
                .unitPrice(BigDecimal.valueOf(49.99)).category("BRAKE")
                .supplier("SupplierA").supplierId(10L).sku("SKU-001")
                .active(true).supplierCatalog(false).build();
    }

    @Test
    void createProduct_success_returnsDto() {
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductDto result = productService.createProduct(sampleDto);

        assertThat(result.getCode()).isEqualTo("BRAKE-001");
        assertThat(result.getName()).isEqualTo("Brake Pad");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_supplierCatalogNull_defaultsFalse() {
        ProductDto dto = ProductDto.builder()
                .code("X-001").name("X").unitPrice(BigDecimal.ONE)
                .category("OIL").supplierCatalog(null).build();
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            assertThat(p.getSupplierCatalog()).isFalse();
            return sampleProduct;
        });

        productService.createProduct(dto);
    }

    @Test
    void getProduct_found_returnsDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductDto result = productService.getProduct(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCategory()).isEqualTo("BRAKE");
    }

    @Test
    void getProduct_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getProductByCode_found_returnsDto() {
        when(productRepository.findByCode("BRAKE-001")).thenReturn(Optional.of(sampleProduct));

        ProductDto result = productService.getProductByCode("BRAKE-001");

        assertThat(result.getCode()).isEqualTo("BRAKE-001");
    }

    @Test
    void getProductByCode_notFound_throws() {
        when(productRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductByCode("NOPE"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getAllProducts_returnsMappedList() {
        when(productRepository.findGarageStock()).thenReturn(List.of(sampleProduct));

        List<ProductDto> result = productService.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("BRAKE-001");
    }

    @Test
    void getMechanicProducts_returnsFiltered() {
        sampleProduct.setMechanicId(5L);
        when(productRepository.findByMechanicId(5L)).thenReturn(List.of(sampleProduct));

        List<ProductDto> result = productService.getMechanicProducts(5L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getProductsByCategory_returnsFiltered() {
        when(productRepository.findByCategory("BRAKE")).thenReturn(List.of(sampleProduct));

        List<ProductDto> result = productService.getProductsByCategory("BRAKE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("BRAKE");
    }

    @Test
    void updateProduct_allFields_updatesAndSaves() {
        ProductDto update = ProductDto.builder()
                .name("Premium Brake Pad").description("Updated desc")
                .unitPrice(BigDecimal.valueOf(59.99)).category("BRAKE_PREMIUM")
                .supplier("SupplierB").supplierId(20L).active(false).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any())).thenReturn(sampleProduct);

        ProductDto result = productService.updateProduct(1L, update);

        assertThat(sampleProduct.getName()).isEqualTo("Premium Brake Pad");
        assertThat(sampleProduct.getActive()).isFalse();
        assertThat(sampleProduct.getCategory()).isEqualTo("BRAKE_PREMIUM");
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void updateProduct_nullFields_doesNotOverwrite() {
        ProductDto update = ProductDto.builder().build(); // all null

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any())).thenReturn(sampleProduct);

        productService.updateProduct(1L, update);

        assertThat(sampleProduct.getName()).isEqualTo("Brake Pad");
        assertThat(sampleProduct.getCategory()).isEqualTo("BRAKE");
    }

    @Test
    void updateProduct_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, sampleDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void deleteProduct_callsRepository() {
        doNothing().when(productRepository).deleteById(1L);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }
}
