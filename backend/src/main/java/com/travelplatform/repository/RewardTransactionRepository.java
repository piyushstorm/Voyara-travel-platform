package com.travelplatform.repository;

import com.travelplatform.entity.RewardTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {

    Page<RewardTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<RewardTransaction> findByUserIdAndTransactionTypeOrderByCreatedAtDesc(
            Long userId, String transactionType, Pageable pageable);

    Optional<RewardTransaction> findByTransactionTypeAndBookingReference(
            String transactionType, String bookingReference);

    boolean existsByTransactionTypeAndBookingReference(
            String transactionType, String bookingReference);

    Optional<RewardTransaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("SELECT SUM(rt.points) FROM RewardTransaction rt WHERE rt.user.id = :userId AND rt.transactionType = 'BOOKING_EARN'")
    Long sumEarnedPointsByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(rt.points) FROM RewardTransaction rt WHERE rt.user.id = :userId AND rt.points < 0")
    Long sumRedeemedPointsByUserId(@Param("userId") Long userId);

    /** Find points that will expire before the given date, earned after the given date */
    @Query("SELECT rt FROM RewardTransaction rt WHERE rt.user.id = :userId " +
           "AND rt.transactionType = 'BOOKING_EARN' AND rt.createdAt < :expiryCutoff " +
           "AND rt.points > 0 ORDER BY rt.createdAt ASC")
    List<RewardTransaction> findExpiringPoints(@Param("userId") Long userId,
                                                @Param("expiryCutoff") java.time.LocalDateTime expiryCutoff);
}
