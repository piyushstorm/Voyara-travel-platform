package com.travelplatform.repository;

import com.travelplatform.entity.RecommendationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Long> {

    Optional<RecommendationFeedback> findByUserIdAndEntityTypeAndEntityId(Long userId, String entityType, Long entityId);

    List<RecommendationFeedback> findByUserId(Long userId);

    @Query("SELECT rf.entityId FROM RecommendationFeedback rf WHERE rf.user.id = :userId AND rf.entityType = :entityType AND rf.feedbackType = 'IRRELEVANT'")
    List<Long> findIrrelevantEntityIds(@Param("userId") Long userId, @Param("entityType") String entityType);

    @Query("SELECT rf.entityId FROM RecommendationFeedback rf WHERE rf.user.id = :userId AND rf.entityType = :entityType AND rf.feedbackType = 'HELPFUL'")
    List<Long> findHelpfulEntityIds(@Param("userId") Long userId, @Param("entityType") String entityType);
}
