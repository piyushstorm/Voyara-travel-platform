package com.travelplatform.repository;

import com.travelplatform.entity.TripTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TripTimelineEventRepository extends JpaRepository<TripTimelineEvent, Long> {
    List<TripTimelineEvent> findByBookingIdOrderByEventTimeAsc(Long bookingId);
    List<TripTimelineEvent> findByUserIdOrderByEventTimeDesc(Long userId);
    void deleteByBookingId(Long bookingId);
}
