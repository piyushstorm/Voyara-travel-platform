package com.travelplatform.repository;

import com.travelplatform.entity.FareOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FareOptionRepository extends JpaRepository<FareOption, Long> {

    List<FareOption> findByFlightIdOrderByPriceMultiplierAsc(Long flightId);

    List<FareOption> findByFlightIdAndFareType(Long flightId, String fareType);
}
