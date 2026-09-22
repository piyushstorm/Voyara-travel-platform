package com.travelplatform.repository;

import com.travelplatform.entity.HolidayPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface HolidayPackageRepository extends JpaRepository<HolidayPackage, Long> {

    List<HolidayPackage> findByActiveTrue();

    @Query("SELECT h FROM HolidayPackage h WHERE h.active = true " +
           "AND (:destination IS NULL OR :destination = '' OR h.destination LIKE %:destination%) " +
           "AND (:tripType IS NULL OR :tripType = '' OR h.tripType = :tripType) " +
           "AND (:minPrice IS NULL OR h.pricePerPerson >= :minPrice) " +
           "AND (:maxPrice IS NULL OR h.pricePerPerson <= :maxPrice) " +
           "AND (:hotelCategory IS NULL OR :hotelCategory = '' OR h.hotelCategory = :hotelCategory) " +
           "AND (:minRating IS NULL OR h.rating >= :minRating) " +
           "AND (:mealPlan IS NULL OR :mealPlan = '' OR h.mealPlan = :mealPlan) " +
           "AND (:transportType IS NULL OR :transportType = '' OR h.transportType = :transportType)")
    List<HolidayPackage> search(
        @Param("destination") String destination,
        @Param("tripType") String tripType,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("hotelCategory") String hotelCategory,
        @Param("minRating") Double minRating,
        @Param("mealPlan") String mealPlan,
        @Param("transportType") String transportType
    );

    List<HolidayPackage> findByFeaturedTrueAndActiveTrue();
}
