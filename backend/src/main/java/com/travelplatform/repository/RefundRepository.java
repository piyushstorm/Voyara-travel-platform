package com.travelplatform.repository;

import com.travelplatform.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByRefundId(String refundId);
    Optional<Refund> findByRazorpayRefundId(String razorpayRefundId);
    List<Refund> findByBookingId(Long bookingId);
    @Query("SELECT r FROM Refund r JOIN r.booking b WHERE b.user.id = :userId")
    List<Refund> findByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM Refund r WHERE r.status IN ('PENDING', 'PROCESSING')")
    List<Refund> findPendingRefunds();

    @Query("SELECT r FROM Refund r WHERE r.status = 'PROCESSING' AND r.expectedCompletionAt <= CURRENT_TIMESTAMP")
    List<Refund> findRefundsReadyForCompletion();
}
