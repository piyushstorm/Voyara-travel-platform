package com.travelplatform.repository;

import com.travelplatform.entity.ReviewModerationAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewModerationActionRepository extends JpaRepository<ReviewModerationAction, Long> {

    List<ReviewModerationAction> findByReviewIdOrderByCreatedAtDesc(Long reviewId);

    List<ReviewModerationAction> findByModeratorIdOrderByCreatedAtDesc(Long moderatorId);
}
