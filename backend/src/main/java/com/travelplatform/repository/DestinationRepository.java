package com.travelplatform.repository;

import com.travelplatform.entity.Destination;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Long> {

    Optional<Destination> findByNameIgnoreCase(String name);

    Optional<Destination> findByCityIgnoreCase(String city);

    List<Destination> findByCategoryIgnoreCase(String category);

    @Query("SELECT d FROM Destination d ORDER BY d.popularityScore DESC")
    List<Destination> findTopPopular(Pageable pageable);

    @Query("SELECT d FROM Destination d WHERE LOWER(d.category) = LOWER(:category) OR LOWER(d.tags) LIKE LOWER(CONCAT('%', :tag, '%')) ORDER BY d.popularityScore DESC")
    List<Destination> findMatchingDestinations(@Param("category") String category, @Param("tag") String tag, Pageable pageable);
}
