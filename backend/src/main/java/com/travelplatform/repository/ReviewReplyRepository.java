package com.travelplatform.repository;

import com.travelplatform.entity.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {
    List<ReviewReply> findByReviewIdAndStatusOrderByCreatedAtAsc(Long reviewId, String status);
}
