package com.travelplatform.repository;

import com.travelplatform.entity.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    @Query("""
        SELECT h FROM Hotel h
        WHERE h.active = true
        AND LOWER(h.city) = LOWER(:city)
        AND (:minPrice IS NULL OR h.startingPrice >= :minPrice)
        AND (:maxPrice IS NULL OR h.startingPrice <= :maxPrice)
        AND (:minStar IS NULL OR h.starRating >= :minStar)
        AND (:maxStar IS NULL OR h.starRating <= :maxStar)
        AND (:amenity IS NULL OR :amenity = '' OR :amenity IN (SELECT a FROM h.amenities a))
        """)
    Page<Hotel> searchHotels(
        @Param("city") String city,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("minStar") Integer minStar,
        @Param("maxStar") Integer maxStar,
        @Param("amenity") String amenity,
        Pageable pageable
    );

    @Query("SELECT h FROM Hotel h WHERE h.active = true AND LOWER(h.city) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Hotel> searchByQuery(@Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT h.city FROM Hotel h WHERE h.active = true ORDER BY h.city")
    List<String> findPopularCities();
}
