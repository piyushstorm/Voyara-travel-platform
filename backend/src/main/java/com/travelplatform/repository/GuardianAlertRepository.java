package com.travelplatform.repository;

import com.travelplatform.entity.GuardianAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GuardianAlertRepository extends JpaRepository<GuardianAlert, Long> {
    List<GuardianAlert> findByUserIdAndIsDismissedOrderByCreatedAtDesc(Long userId, boolean dismissed);
    long countByUserIdAndIsReadFalseAndIsDismissedFalse(Long userId);
    Optional<GuardianAlert> findByIdempotencyKey(String idempotencyKey);
    List<GuardianAlert> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
