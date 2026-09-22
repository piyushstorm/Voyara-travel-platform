package com.travelplatform.repository;

import com.travelplatform.entity.RoomHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomHoldRepository extends JpaRepository<RoomHold, Long> {

    Optional<RoomHold> findByRoomIdAndUserIdAndStatus(Long roomId, Long userId, String status);

    List<RoomHold> findByUserIdAndStatus(Long userId, String status);

    @Query("SELECT rh FROM RoomHold rh WHERE rh.status = 'ACTIVE' AND rh.expiresAt < :now")
    List<RoomHold> findExpiredHolds(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(rh) FROM RoomHold rh WHERE rh.room.id = :roomId AND rh.status = 'ACTIVE' AND rh.expiresAt > :now")
    long countActiveHoldsForRoom(@Param("roomId") Long roomId, @Param("now") LocalDateTime now);
}
