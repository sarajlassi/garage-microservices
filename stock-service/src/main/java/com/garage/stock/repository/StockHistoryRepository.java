package com.garage.stock.repository;

import com.garage.stock.entity.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    List<StockHistory> findByProductId(Long productId);
    List<StockHistory> findByStockId(Long stockId);
    List<StockHistory> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<StockHistory> findByMovementTypeAndCreatedAtAfter(StockHistory.StockMovementType type, LocalDateTime dateTime);
}

