package com.garage.stock.repository;

import com.garage.stock.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByCode(String code);
    List<Product> findByCategory(String category);
    List<Product> findByActive(Boolean active);
    List<Product> findBySupplier(String supplier);
    List<Product> findBySupplierId(Long supplierId);
}

