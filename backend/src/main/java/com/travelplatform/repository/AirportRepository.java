package com.travelplatform.repository;

import com.travelplatform.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Long> {
    Optional<Airport> findByCode(String code);

    @Query("SELECT a FROM Airport a WHERE LOWER(a.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.code) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Airport> search(@Param("query") String query);
}
