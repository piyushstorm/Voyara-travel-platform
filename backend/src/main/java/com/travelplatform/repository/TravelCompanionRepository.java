package com.travelplatform.repository;

import com.travelplatform.entity.TravelCompanion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TravelCompanionRepository extends JpaRepository<TravelCompanion, Long> {
    List<TravelCompanion> findByGroupTripId(Long groupTripId);
    Optional<TravelCompanion> findByGroupTripIdAndUserId(Long groupTripId, Long userId);
    boolean existsByGroupTripIdAndUserId(Long groupTripId, Long userId);
}
