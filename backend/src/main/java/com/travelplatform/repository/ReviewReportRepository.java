package com.travelplatform.repository;

import com.travelplatform.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    boolean existsByReporterUserIdAndReviewId(Long reporterUserId, Long reviewId);

    Optional<ReviewReport> findByReporterUserIdAndReviewId(Long reporterUserId, Long reviewId);

    List<ReviewReport> findByReviewIdOrderByCreatedAtDesc(Long reviewId);

    List<ReviewReport> findByStatusOrderByCreatedAtDesc(String status);
}
