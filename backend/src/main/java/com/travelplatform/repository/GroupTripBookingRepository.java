package com.travelplatform.repository;

import com.travelplatform.entity.GroupTripBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GroupTripBookingRepository extends JpaRepository<GroupTripBooking, Long> {
    List<GroupTripBooking> findByGroupTripId(Long groupTripId);
    Optional<GroupTripBooking> findByGroupTripIdAndBookingId(Long groupTripId, Long bookingId);
}
