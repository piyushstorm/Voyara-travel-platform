package com.travelplatform.repository;

import com.travelplatform.entity.TripReadiness;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TripReadinessRepository extends JpaRepository<TripReadiness, Long> {
    Optional<TripReadiness> findByBookingId(Long bookingId);
    Optional<TripReadiness> findByUserIdAndBookingId(Long userId, Long bookingId);
}
