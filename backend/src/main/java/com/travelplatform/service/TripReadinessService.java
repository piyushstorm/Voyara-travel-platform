package com.travelplatform.service;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Trip Readiness — calculates a real readiness score from booking data.
 * Do NOT hardcode scores — always compute from actual state.
 */
@Service
public class TripReadinessService {

    private final TripReadinessRepository readinessRepo;
    private final BookingRepository bookingRepo;
    private final FlightStatusRepository flightStatusRepo;
    private final SeatRepository seatRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;

    public TripReadinessService(TripReadinessRepository readinessRepo,
                                 BookingRepository bookingRepo,
                                 FlightStatusRepository flightStatusRepo,
                                 SeatRepository seatRepo,
                                 RoomRepository roomRepo,
                                 UserRepository userRepo,
                                 ObjectMapper objectMapper) {
        this.readinessRepo = readinessRepo;
        this.bookingRepo = bookingRepo;
        this.flightStatusRepo = flightStatusRepo;
        this.seatRepo = seatRepo;
        this.roomRepo = roomRepo;
        this.userRepo = userRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * Calculate and persist the trip readiness score for a booking.
     * Returns the readiness object with score and detailed checks.
     */
    @Transactional
    public TripReadiness calculateReadiness(Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        User user = booking.getUser();

        ObjectNode checks = objectMapper.createObjectNode();
        ArrayNode checksArray = objectMapper.createArrayNode();

        int totalWeight = 0;
        int achievedWeight = 0;

        // 1. Payment complete (weight: 20)
        totalWeight += 20;
        boolean paymentComplete = "CONFIRMED".equals(booking.getStatus()) || "COMPLETED".equals(booking.getStatus());
        achievedWeight += paymentComplete ? 20 : 0;
        checksArray.add(createCheck("payment_complete", paymentComplete,
            "Payment complete", "Your booking is confirmed and paid.",
            paymentComplete ? null : "Complete payment to confirm your booking"));

        // 2. Booking confirmed (weight: 15)
        totalWeight += 15;
        boolean bookingConfirmed = "CONFIRMED".equals(booking.getStatus());
        achievedWeight += bookingConfirmed ? 15 : 0;
        checksArray.add(createCheck("booking_confirmed", bookingConfirmed,
            "Booking confirmed", "Your booking reference is ready.",
            bookingConfirmed ? null : "Booking is pending confirmation"));

        // 3. Passenger details complete (weight: 10)
        totalWeight += 10;
        boolean passengersComplete = booking.getPassengerCount() > 0;
        achievedWeight += passengersComplete ? 10 : 0;
        checksArray.add(createCheck("passengers_complete", passengersComplete,
            "Passenger details complete", "All traveller information is provided.",
            passengersComplete ? null : "Add passenger details before travel"));

        // Flight-specific checks
        if (booking.getFlight() != null) {
            // 4. Seat selected (weight: 10)
            totalWeight += 10;
            boolean seatSelected = booking.getSelectedSeatNumbers() != null
                && !booking.getSelectedSeatNumbers().isEmpty();
            achievedWeight += seatSelected ? 10 : 0;
            checksArray.add(createCheck("seat_selected", seatSelected,
                "Seat selected", "Your seat is confirmed.",
                seatSelected ? null : "Choose your seat before check-in",
                seatSelected ? null : "/seat-selection/" + booking.getBookingReference()));

            // 5. Flight on time (weight: 10)
            totalWeight += 10;
            boolean flightOnTime = true;
            String flightNote = null;
            Optional<FlightStatus> statusOpt = flightStatusRepo.findByFlightId(booking.getFlight().getId());
            if (statusOpt.isPresent()) {
                FlightStatus status = statusOpt.get();
                if ("DELAYED".equals(status.getStatus())) {
                    flightOnTime = false;
                    long delayMins = ChronoUnit.MINUTES.between(
                        status.getScheduledArrival(), status.getEstimatedArrival());
                    flightNote = String.format("Flight delayed by %d minutes", delayMins);
                } else if ("CANCELLED".equals(status.getStatus())) {
                    flightOnTime = false;
                    flightNote = "Flight has been cancelled";
                }
            }
            achievedWeight += flightOnTime ? 10 : 0;
            checksArray.add(createCheck("flight_on_time", flightOnTime,
                "Flight status", flightOnTime ? "Your flight is on schedule" : "Flight status needs attention",
                flightNote));

            // 6. Departure time approaching (info only)
            long hoursUntilDep = ChronoUnit.HOURS.between(LocalDateTime.now(), booking.getFlight().getDepartureTime());
            if (hoursUntilDep >= 0 && hoursUntilDep < 24) {
                checksArray.add(createCheck("departure_approaching", true,
                    "Departure approaching",
                    String.format("Your flight departs in %d hours", hoursUntilDep),
                    null));
            }
        }

        // Hotel-specific checks
        if (booking.getHotel() != null) {
            // 4. Room selected (weight: 10)
            totalWeight += 10;
            boolean roomSelected = booking.getRoom() != null;
            achievedWeight += roomSelected ? 10 : 0;
            checksArray.add(createCheck("room_selected", roomSelected,
                "Room selected", "Your room type is confirmed.",
                roomSelected ? null : "Select a room to complete your booking"));

            // 5. Check-in date info
            if (booking.getCheckInDate() != null) {
                long daysUntilCheckin = ChronoUnit.DAYS.between(LocalDateTime.now(), booking.getCheckInDate());
                if (daysUntilCheckin >= 0 && daysUntilCheckin < 3) {
                    checksArray.add(createCheck("checkin_approaching", true,
                        "Check-in approaching",
                        String.format("Check-in is in %d days", daysUntilCheckin),
                        null));
                }
            }
        }

        // Calculate score
        int score = totalWeight > 0 ? (int) Math.round((double) achievedWeight / totalWeight * 100) : 0;
        score = Math.min(100, Math.max(0, score));

        // Save or update
        Optional<TripReadiness> existingOpt = readinessRepo.findByBookingId(bookingId);
        TripReadiness readiness;
        if (existingOpt.isPresent()) {
            readiness = existingOpt.get();
            readiness.setScore(score);
            readiness.setChecksJson(checksArray.toString());
            readiness.setLastCalculatedAt(LocalDateTime.now());
        } else {
            readiness = new TripReadiness();
            readiness.setBooking(booking);
            readiness.setUser(user);
            readiness.setScore(score);
            readiness.setChecksJson(checksArray.toString());
            readiness.setLastCalculatedAt(LocalDateTime.now());
        }

        return readinessRepo.save(readiness);
    }

    /**
     * Get readiness for a booking (calculate if stale or missing).
     */
    @Transactional(readOnly = true)
    public TripReadiness getReadiness(Long bookingId) {
        Optional<TripReadiness> existing = readinessRepo.findByBookingId(bookingId);
        if (existing.isPresent()) {
            TripReadiness r = existing.get();
            // Recalculate if older than 5 minutes
            if (r.getLastCalculatedAt() != null &&
                ChronoUnit.MINUTES.between(r.getLastCalculatedAt(), LocalDateTime.now()) < 5) {
                return r;
            }
        }
        // Stale or missing — recalculate
        return calculateReadiness(bookingId);
    }

    private ObjectNode createCheck(String id, boolean passed, String title, String description, String actionNeeded) {
        return createCheck(id, passed, title, description, actionNeeded, null);
    }

    private ObjectNode createCheck(String id, boolean passed, String title, String description,
                                    String actionNeeded, String actionRoute) {
        ObjectNode check = objectMapper.createObjectNode();
        check.put("id", id);
        check.put("passed", passed);
        check.put("title", title);
        check.put("description", description);
        if (actionNeeded != null) check.put("actionNeeded", actionNeeded);
        if (actionRoute != null) check.put("actionRoute", actionRoute);
        return check;
    }
}
