package com.travelplatform.repository;

import com.travelplatform.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByUserId(Long userId);

    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status IN ('CONFIRMED') AND b.travelDate > CURRENT_TIMESTAMP ORDER BY b.travelDate ASC")
    Page<Booking> findUpcomingBookings(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND (b.status = 'COMPLETED' OR b.travelDate <= CURRENT_TIMESTAMP) ORDER BY b.createdAt DESC")
    Page<Booking> findPastBookings(@Param("userId") Long userId, Pageable pageable);

    Page<Booking> findByUserIdAndBookingTypeOrderByCreatedAtDesc(Long userId, String bookingType, Pageable pageable);

    boolean existsByUserIdAndFlightIdAndStatus(Long userId, Long flightId, String status);
    boolean existsByUserIdAndHotelIdAndStatus(Long userId, Long hotelId, String status);
    boolean existsByFlightAndStatus(com.travelplatform.entity.Flight flight, String status);

    Page<Booking> findByStatus(String status, Pageable pageable);

    List<Booking> findByFlightId(Long flightId);
}
