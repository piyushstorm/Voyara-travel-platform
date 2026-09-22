package com.travelplatform.repository;

import com.travelplatform.entity.ConnectionRisk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConnectionRiskRepository extends JpaRepository<ConnectionRisk, Long> {
    List<ConnectionRisk> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ConnectionRisk> findByBookingId(Long bookingId);
    Optional<ConnectionRisk> findByUserIdAndFirstFlightIdAndSecondFlightId(Long userId, Long firstFlightId, Long secondFlightId);
    Optional<ConnectionRisk> findTopByFirstFlightIdAndSecondFlightIdOrderByCreatedAtDesc(Long firstFlightId, Long secondFlightId);
}
