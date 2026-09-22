package com.travelplatform.service;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Travel Timeline — generates a unified chronological trip timeline.
 * All events come from actual booking data. Do NOT hardcode events.
 */
@Service
public class SmartTimelineService {

    private final TripTimelineEventRepository timelineRepo;
    private final BookingRepository bookingRepo;
    private final FlightStatusRepository flightStatusRepo;

    public SmartTimelineService(TripTimelineEventRepository timelineRepo,
                                 BookingRepository bookingRepo,
                                 FlightStatusRepository flightStatusRepo) {
        this.timelineRepo = timelineRepo;
        this.bookingRepo = bookingRepo;
        this.flightStatusRepo = flightStatusRepo;
    }

    /**
     * Generate timeline events for a booking. Regenerates if flight status changed.
     */
    @Transactional
    public List<TripTimelineEvent> generateTimeline(Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Clear existing events for this booking
        timelineRepo.deleteByBookingId(bookingId);

        List<TripTimelineEvent> events = new ArrayList<>();
        User user = booking.getUser();
        int order = 0;

        if (booking.getFlight() != null) {
            Flight flight = booking.getFlight();
            Optional<FlightStatus> statusOpt = flightStatusRepo.findByFlightId(flight.getId());

            // Flight departure
            LocalDateTime depTime = statusOpt.map(FlightStatus::getEstimatedDeparture)
                .orElse(flight.getDepartureTime());
            boolean depAffected = statusOpt.map(s -> "DELAYED".equals(s.getStatus())).orElse(false);

            events.add(createEvent(booking, user, "FLIGHT_DEPARTURE",
                "✈ Departure from " + flight.getOriginCode(),
                depTime, flight.getOriginCode() + " Airport",
                String.format("Flight %s departs from %s", flight.getFlightNumber(), flight.getOriginCode()),
                "✈", depAffected, depAffected ? "Flight is delayed" : null, order++));

            // Flight arrival
            LocalDateTime arrTime = statusOpt.map(FlightStatus::getEstimatedArrival)
                .orElse(flight.getArrivalTime());
            boolean arrAffected = statusOpt.map(s ->
                ChronoUnit.MINUTES.between(s.getScheduledArrival(), s.getEstimatedArrival()) > 30
            ).orElse(false);

            events.add(createEvent(booking, user, "FLIGHT_ARRIVAL",
                "🛬 Arrival at " + flight.getDestinationCode(),
                arrTime, flight.getDestinationCode() + " Airport",
                String.format("Flight %s arrives at %s", flight.getFlightNumber(), flight.getDestinationCode()),
                "🛬", arrAffected,
                arrAffected ? "Delayed arrival may affect subsequent plans" : null, order++));

            // Gate/terminal info
            if (statusOpt.isPresent() && statusOpt.get().getGate() != null) {
                FlightStatus s = statusOpt.get();
                events.add(createEvent(booking, user, "AIRPORT",
                    "Terminal " + s.getTerminal() + " · Gate " + s.getGate(),
                    depTime.minusMinutes(60), s.getTerminal(),
                    "Proceed to Gate " + s.getGate() + ", Terminal " + s.getTerminal(),
                    "🏢", false, null, order++));
            }
        }

        if (booking.getHotel() != null) {
            // Hotel check-in
            if (booking.getCheckInDate() != null) {
                events.add(createEvent(booking, user, "HOTEL_CHECKIN",
                    "🏨 Check-in at " + booking.getHotel().getName(),
                    booking.getCheckInDate(), booking.getHotel().getCity(),
                    String.format("Check-in at %s, %s", booking.getHotel().getName(), booking.getHotel().getCity()),
                    "🏨", false, null, order++));
            }

            // Hotel check-out
            if (booking.getCheckOutDate() != null) {
                events.add(createEvent(booking, user, "HOTEL_CHECKOUT",
                    "🏨 Check-out from " + booking.getHotel().getName(),
                    booking.getCheckOutDate(), booking.getHotel().getCity(),
                    String.format("Check-out from %s", booking.getHotel().getName()),
                    "🏨", false, null, order++));
            }
        }

        // Sort by time and assign final order
        events.sort(Comparator.comparing(TripTimelineEvent::getEventTime));
        for (int i = 0; i < events.size(); i++) {
            events.get(i).setDisplayOrder(i);
        }

        return timelineRepo.saveAll(events);
    }

    /**
     * Get timeline for a booking.
     */
    @Transactional(readOnly = true)
    public List<TripTimelineEvent> getTimeline(Long bookingId) {
        List<TripTimelineEvent> existing = timelineRepo.findByBookingIdOrderByEventTimeAsc(bookingId);
        if (existing.isEmpty()) {
            return generateTimeline(bookingId);
        }
        return existing;
    }

    /**
     * Get all upcoming timeline events for a user across all bookings.
     */
    @Transactional(readOnly = true)
    public List<TripTimelineEvent> getUserUpcomingTimeline(Long userId) {
        return timelineRepo.findByUserIdOrderByEventTimeDesc(userId).stream()
            .filter(e -> e.getEventTime().isAfter(LocalDateTime.now().minusHours(24)))
            .limit(20)
            .collect(Collectors.toList());
    }

    private TripTimelineEvent createEvent(Booking booking, User user, String type,
                                           String title, LocalDateTime time, String location,
                                           String description, String icon,
                                           boolean affected, String affectedReason, int order) {
        TripTimelineEvent event = new TripTimelineEvent();
        event.setBooking(booking);
        event.setUser(user);
        event.setEventType(type);
        event.setEventTitle(title);
        event.setEventTime(time);
        event.setEventLocation(location);
        event.setEventDescription(description);
        event.setIcon(icon);
        event.setAffected(affected);
        event.setAffectedReason(affectedReason);
        event.setDisplayOrder(order);
        return event;
    }
}
