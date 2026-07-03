package com.garage.stock.repository;

import com.garage.stock.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT p FROM Product p WHERE p.supplierId = :supplierId AND p.supplierCatalog = true AND p.mechanicId IS NULL")
    List<Product> findSupplierCatalogBySupplierId(@Param("supplierId") Long supplierId);
    List<Product> findBySupplierCatalogFalseOrSupplierCatalogIsNull();
    List<Product> findByMechanicId(Long mechanicId);
    Optional<Product> findByCodeAndMechanicId(String code, Long mechanicId);

    @Query("SELECT p FROM Product p WHERE (p.supplierCatalog = false OR p.supplierCatalog IS NULL) AND p.mechanicId IS NULL")
    List<Product> findGarageStock();
}

