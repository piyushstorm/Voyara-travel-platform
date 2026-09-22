package com.travelplatform.repository;

import com.travelplatform.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
    List<PriceHistory> findTop50ByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
}
