package com.travelplatform.repository;

import com.travelplatform.entity.FlightStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightStatusHistoryRepository extends JpaRepository<FlightStatusHistory, Long> {
    List<FlightStatusHistory> findByFlightIdOrderByCreatedAtDesc(Long flightId);
}
