package com.travelplatform.repository;

import com.travelplatform.entity.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {
    Optional<SeatHold> findBySeatIdAndUserIdAndStatus(Long seatId, Long userId, String status);
    List<SeatHold> findByUserIdAndStatus(Long userId, String status);
    List<SeatHold> findBySeatIdAndStatus(Long seatId, String status);

    @Query("SELECT sh FROM SeatHold sh WHERE sh.status = 'ACTIVE' AND sh.expiresAt <= CURRENT_TIMESTAMP")
    List<SeatHold> findExpiredHolds();
}
