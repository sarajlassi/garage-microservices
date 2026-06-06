package com.garage.stock.repository;

import com.garage.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductId(Long productId);
    
    @Query("SELECT s FROM Stock s WHERE s.quantity - s.reserved <= s.minThreshold")
    List<Stock> findLowStockItems();
    
    @Query("SELECT s FROM Stock s WHERE s.product.category = :category AND s.quantity > 0")
    List<Stock> findByProductCategoryAndQuantityGreaterThanZero(@Param("category") String category);
}

