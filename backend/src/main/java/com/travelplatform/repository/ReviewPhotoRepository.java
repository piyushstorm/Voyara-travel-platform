package com.travelplatform.repository;

import com.travelplatform.entity.ReviewPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewPhotoRepository extends JpaRepository<ReviewPhoto, Long> {
    List<ReviewPhoto> findByReviewId(Long reviewId);
    long countByReviewId(Long reviewId);
}
