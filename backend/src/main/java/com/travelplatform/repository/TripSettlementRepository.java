package com.travelplatform.repository;

import com.travelplatform.entity.TripSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TripSettlementRepository extends JpaRepository<TripSettlement, Long> {
    List<TripSettlement> findByGroupTripIdAndStatusOrderByCreatedAtDesc(Long groupTripId, String status);
    List<TripSettlement> findByFromUserIdAndStatus(Long userId, String status);
    List<TripSettlement> findByToUserIdAndStatus(Long userId, String status);
    void deleteByGroupTripIdAndStatus(Long groupTripId, String status);
}
