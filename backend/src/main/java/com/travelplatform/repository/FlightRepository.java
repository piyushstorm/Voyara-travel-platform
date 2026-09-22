package com.travelplatform.repository;

import com.travelplatform.entity.Flight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
    Optional<Flight> findByFlightNumber(String flightNumber);

    @Query("""
        SELECT f FROM Flight f
        WHERE f.active = true
        AND (UPPER(f.originCode) = UPPER(:origin) OR UPPER(f.origin.city) = UPPER(:origin))
        AND (UPPER(f.destinationCode) = UPPER(:destination) OR UPPER(f.destination.city) = UPPER(:destination))
        AND f.departureDate = :date
        AND (:airlineCode IS NULL OR f.airline.code = :airlineCode)
        AND (:maxStops IS NULL OR f.stops <= :maxStops)
        AND (:minPrice IS NULL OR f.economyPrice >= :minPrice)
        AND (:maxPrice IS NULL OR f.economyPrice <= :maxPrice)
        AND (:cabinClass = 'ECONOMY' OR :cabinClass IS NULL OR f.economyPrice > 0)
        AND (:cabinClass = 'PREMIUM_ECONOMY' OR :cabinClass IS NULL OR f.premiumEconomyPrice > 0)
        AND (:cabinClass = 'BUSINESS' OR :cabinClass IS NULL OR f.businessPrice > 0)
        AND (:cabinClass = 'FIRST' OR :cabinClass IS NULL OR f.firstClassPrice > 0)
        """)
    Page<Flight> searchFlights(
        @Param("origin") String origin,
        @Param("destination") String destination,
        @Param("date") LocalDate date,
        @Param("airlineCode") String airlineCode,
        @Param("maxStops") Integer maxStops,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("cabinClass") String cabinClass,
        Pageable pageable
    );

    @Query("""
        SELECT f FROM Flight f
        WHERE f.active = true
        AND f.originCode = :origin
        AND f.destinationCode = :destination
        AND f.departureDate = :date
        """)
    List<Flight> findDirectFlights(
        @Param("origin") String origin,
        @Param("destination") String destination,
        @Param("date") LocalDate date
    );

    @Query("SELECT DISTINCT f.airline.code FROM Flight f WHERE f.active = true AND f.originCode = :origin AND f.destinationCode = :destination AND f.departureDate = :date")
    List<String> findAirlineCodes(@Param("origin") String origin, @Param("destination") String destination, @Param("date") LocalDate date);

    @Query("""
        SELECT f FROM Flight f
        WHERE f.active = true
        AND (
            LOWER(f.flightNumber) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(f.originCode) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(f.destinationCode) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(f.airline.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(f.airline.code) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        ORDER BY f.departureTime ASC
        """)
    List<Flight> searchByQuery(@Param("query") String query, Pageable pageable);
}
