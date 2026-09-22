package com.travelplatform.repository;

import com.travelplatform.entity.TrackedFlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackedFlightRepository extends JpaRepository<TrackedFlight, Long> {
    List<TrackedFlight> findByUserIdOrderByTrackedAtDesc(Long userId);
    List<TrackedFlight> findByFlightId(Long flightId);
    Optional<TrackedFlight> findByUserIdAndFlightId(Long userId, Long flightId);
    boolean existsByUserIdAndFlightId(Long userId, Long flightId);
}
