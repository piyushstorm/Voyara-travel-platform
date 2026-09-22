package com.travelplatform.repository;

import com.travelplatform.entity.Seat;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByFlightIdAndCabinClassAndAvailableTrue(Long flightId, String cabinClass);

    @Query("SELECT s FROM Seat s WHERE s.flight.id = :flightId AND s.cabinClass = :cabinClass AND (s.available = true OR (s.heldByUserId = :userId AND s.heldUntil > CURRENT_TIMESTAMP))")
    List<Seat> findAvailableOrHeldSeats(@Param("flightId") Long flightId, @Param("cabinClass") String cabinClass, @Param("userId") String userId);

    List<Seat> findByFlightIdAndCabinClass(Long flightId, String cabinClass);

    List<Seat> findByFlightIdAndCabinClassOrderByRowNumberAscColumnLetterAsc(Long flightId, String cabinClass);

    /** Find seats held by a specific user that haven't expired yet */
    List<Seat> findByHeldByUserIdAndHeldUntilAfter(String userId, java.time.LocalDateTime now);

    /**
     * PESSIMISTIC_WRITE lock — the calling transaction acquires an exclusive
     * database row lock on this seat. Any concurrent transaction trying to
     * lock the same row will BLOCK until this transaction commits or rolls back.
     * This is the primary defense against the seat double-hold race condition.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    @QueryHints({ @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000") })
    Optional<Seat> findByIdWithPessimisticLock(@Param("id") Long id);
}
