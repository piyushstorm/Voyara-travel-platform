package com.travelplatform.repository;

import com.travelplatform.entity.SavedTraveller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedTravellerRepository extends JpaRepository<SavedTraveller, Long> {
    List<SavedTraveller> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
}
