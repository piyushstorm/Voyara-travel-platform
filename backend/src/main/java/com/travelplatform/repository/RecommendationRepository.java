package com.travelplatform.repository;

import com.travelplatform.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserIdAndEntityTypeOrderByScoreDesc(Long userId, String entityType);

    @Query("SELECT r FROM Recommendation r WHERE r.user.id = :userId ORDER BY r.score DESC")
    List<Recommendation> findTopRecommendations(@Param("userId") Long userId);

    List<Recommendation> findByUserIdAndFeedbackIsNullOrderByScoreDesc(Long userId);

    void deleteByUserIdAndEntityType(Long userId, String entityType);

    void deleteByUserId(Long userId);
}
