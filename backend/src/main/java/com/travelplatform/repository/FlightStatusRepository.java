package com.travelplatform.repository;

import com.travelplatform.entity.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlightStatusRepository extends JpaRepository<FlightStatus, Long> {

    @Query("SELECT fs FROM FlightStatus fs " +
           "LEFT JOIN FETCH fs.flight f " +
           "LEFT JOIN FETCH f.airline " +
           "LEFT JOIN FETCH f.origin " +
           "LEFT JOIN FETCH f.destination " +
           "WHERE f.id = :flightId")
    Optional<FlightStatus> findByFlightId(@Param("flightId") Long flightId);

    @Query("SELECT fs FROM FlightStatus fs " +
           "LEFT JOIN FETCH fs.flight f " +
           "LEFT JOIN FETCH f.airline " +
           "LEFT JOIN FETCH f.origin " +
           "LEFT JOIN FETCH f.destination " +
           "WHERE f.id IN :flightIds")
    List<FlightStatus> findByFlightIdIn(@Param("flightIds") List<Long> flightIds);

    @Query(value = "SELECT fs FROM FlightStatus fs " +
                   "LEFT JOIN FETCH fs.flight f " +
                   "LEFT JOIN FETCH f.airline " +
                   "LEFT JOIN FETCH f.origin " +
                   "LEFT JOIN FETCH f.destination",
           countQuery = "SELECT COUNT(fs) FROM FlightStatus fs")
    org.springframework.data.domain.Page<FlightStatus> findAllWithDetails(org.springframework.data.domain.Pageable pageable);
}
