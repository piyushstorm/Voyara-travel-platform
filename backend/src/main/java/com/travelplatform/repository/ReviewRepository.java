package com.travelplatform.repository;

import com.travelplatform.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Page<Review> findByFlightIdAndStatusOrderByCreatedAtDesc(Long flightId, String status, Pageable pageable);
    Page<Review> findByHotelIdAndStatusOrderByCreatedAtDesc(Long hotelId, String status, Pageable pageable);

    Page<Review> findByFlightIdAndStatusOrderByRatingDesc(Long flightId, String status, Pageable pageable);
    Page<Review> findByHotelIdAndStatusOrderByRatingDesc(Long hotelId, String status, Pageable pageable);

    Page<Review> findByFlightIdAndStatusOrderByHelpfulCountDesc(Long flightId, String status, Pageable pageable);
    Page<Review> findByHotelIdAndStatusOrderByHelpfulCountDesc(Long hotelId, String status, Pageable pageable);

    List<Review> findByStatus(String status);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.flight.id = :flightId AND r.status IN ('PUBLISHED', 'APPROVED', 'FLAGGED')")
    Double getAverageRatingByFlightId(@Param("flightId") Long flightId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.hotel.id = :hotelId AND r.status IN ('PUBLISHED', 'APPROVED', 'FLAGGED')")
    Double getAverageRatingByHotelId(@Param("hotelId") Long hotelId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.flight.id = :flightId AND r.status IN ('PUBLISHED', 'APPROVED', 'FLAGGED')")
    long getCountByFlightId(@Param("flightId") Long flightId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.hotel.id = :hotelId AND r.status IN ('PUBLISHED', 'APPROVED', 'FLAGGED')")
    long getCountByHotelId(@Param("hotelId") Long hotelId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.flight.id = :flightId AND r.status IN ('PUBLISHED', 'APPROVED', 'FLAGGED') GROUP BY r.rating")
    List<Object[]> getRatingDistributionByFlightId(@Param("flightId") Long flightId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.hotel.id = :hotelId AND r.status IN ('PUBLISHED', 'APPROVED', 'FLAGGED') GROUP BY r.rating")
    List<Object[]> getRatingDistributionByHotelId(@Param("hotelId") Long hotelId);

    @Query("SELECT r FROM Review r WHERE r.status IN ('FLAGGED', 'PENDING_MODERATION', 'UNDER_REVIEW') OR r.reportCount > 0 ORDER BY r.reportCount DESC, r.createdAt DESC")
    List<Review> findModerationQueue();

    @Query("SELECT r FROM Review r WHERE r.status = 'PENDING_MODERATION' ORDER BY r.createdAt DESC")
    List<Review> findPendingModeration();

    boolean existsByUserIdAndFlightIdAndStatus(Long userId, Long flightId, String status);
    boolean existsByUserIdAndHotelIdAndStatus(Long userId, Long hotelId, String status);
    boolean existsByUserIdAndFlightIdAndStatusNot(Long userId, Long flightId, String status);
    boolean existsByUserIdAndHotelIdAndStatusNot(Long userId, Long hotelId, String status);
    boolean existsByUserIdAndBookingIdAndStatusNot(Long userId, Long bookingId, String status);
}
