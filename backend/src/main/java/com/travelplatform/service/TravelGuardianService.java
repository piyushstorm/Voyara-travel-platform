package com.travelplatform.service;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Voyara Travel Guardian — proactive disruption management.
 *
 * Analyzes flight status, hotel check-in times, connections, and itinerary timing
 * to detect how disruptions affect the traveler's complete trip.
 *
 * Severity levels: INFO, LOW, MEDIUM, HIGH, CRITICAL
 */
@Service
public class TravelGuardianService {

    private static final Logger log = LoggerFactory.getLogger(TravelGuardianService.class);

    private final GuardianAlertRepository alertRepo;
    private final FlightStatusRepository flightStatusRepo;
    private final FlightRepository flightRepo;
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final WebSocketNotificationService wsNotificationService;

    /** Minimum comfortable connection time in minutes */
    private static final int MIN_CONNECTION_MINUTES = 90;

    public TravelGuardianService(GuardianAlertRepository alertRepo,
                                  FlightStatusRepository flightStatusRepo,
                                  FlightRepository flightRepo,
                                  BookingRepository bookingRepo,
                                  UserRepository userRepo,
                                  NotificationService notificationService,
                                  WebSocketNotificationService wsNotificationService) {
        this.alertRepo = alertRepo;
        this.flightStatusRepo = flightStatusRepo;
        this.flightRepo = flightRepo;
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
        this.wsNotificationService = wsNotificationService;
    }

    /**
     * Analyze a user's upcoming bookings and generate meaningful guardian alerts.
     * Called after flight status changes or on-demand.
     */
    @Transactional
    public List<GuardianAlert> analyzeAndGenerateAlerts(Long userId) {
        User user = userRepo.findById(userId).orElseThrow();
        List<Booking> upcomingBookings = bookingRepo.findByUserId(userId).stream()
            .filter(b -> "CONFIRMED".equals(b.getStatus()) || "PENDING".equals(b.getStatus()))
            .collect(java.util.stream.Collectors.toList());

        List<GuardianAlert> newAlerts = new ArrayList<>();

        for (Booking booking : upcomingBookings) {
            if (booking.getFlight() != null) {
                newAlerts.addAll(analyzeFlightBooking(user, booking));
            }
        }

        // Analyze connections between sequential flight bookings
        newAlerts.addAll(analyzeConnections(user, upcomingBookings));

        return newAlerts;
    }

    /**
     * Analyze a single flight booking for disruption impacts.
     */
    private List<GuardianAlert> analyzeFlightBooking(User user, Booking booking) {
        List<GuardianAlert> alerts = new ArrayList<>();
        Flight flight = booking.getFlight();
        if (flight == null) return alerts;

        Optional<FlightStatus> statusOpt = flightStatusRepo.findByFlightId(flight.getId());
        if (statusOpt.isEmpty()) return alerts;

        FlightStatus status = statusOpt.get();

        // Check: Flight delay affects hotel check-in
        if (booking.getCheckInDate() != null && "DELAYED".equals(status.getStatus())) {
            long delayMinutes = ChronoUnit.MINUTES.between(
                status.getScheduledArrival(), status.getEstimatedArrival());

            if (delayMinutes > 30) {
                LocalDateTime originalArrival = status.getScheduledArrival();
                LocalDateTime newArrival = status.getEstimatedArrival();
                LocalDateTime checkIn = booking.getCheckInDate();

                if (newArrival.isAfter(checkIn)) {
                    String idempotencyKey = "DELAY_HOTEL_" + booking.getId() + "_" + delayMinutes;
                    if (!alertRepo.findByIdempotencyKey(idempotencyKey).isPresent()) {
                        GuardianAlert alert = createAlert(user, booking, flight,
                            "FLIGHT_DELAY_AFFECTS_HOTEL", "HIGH",
                            "Flight delay may affect your hotel check-in",
                            String.format("Your %s flight is delayed by %d minutes (new arrival: %s). " +
                                "Your hotel check-in is at %s. You may arrive late.",
                                flight.getFlightNumber(), delayMinutes,
                                newArrival.toLocalTime(),
                                checkIn.toLocalTime()),
                            "View Hotel", "/my-trips/" + booking.getBookingReference(),
                            idempotencyKey);
                        alerts.add(alert);
                    }
                }
            }
        }

        // Check: Flight cancellation affects trip
        if ("CANCELLED".equals(status.getStatus())) {
            String idempotencyKey = "CANCEL_TRIP_" + booking.getId();
            if (!alertRepo.findByIdempotencyKey(idempotencyKey).isPresent()) {
                GuardianAlert alert = createAlert(user, booking, flight,
                    "FLIGHT_CANCELLATION_AFFECTS_TRIP", "CRITICAL",
                    "Your flight has been cancelled",
                    String.format("Your %s flight from %s to %s has been cancelled. " +
                        "Your trip itinerary is affected. Please review cancellation and refund options.",
                        flight.getFlightNumber(),
                        flight.getOriginCode(),
                        flight.getDestinationCode()),
                    "View Options", "/my-trips/" + booking.getBookingReference(),
                    idempotencyKey);
                alerts.add(alert);
            }
        }

        // Check: Gate change
        if (status.getGate() != null && status.getUpdatedAt() != null) {
            long minutesSinceUpdate = ChronoUnit.MINUTES.between(status.getUpdatedAt(), LocalDateTime.now());
            if (minutesSinceUpdate < 15) {
                String idempotencyKey = "GATE_CHANGE_" + flight.getId() + "_" + status.getGate();
                if (!alertRepo.findByIdempotencyKey(idempotencyKey).isPresent()) {
                    GuardianAlert alert = createAlert(user, booking, flight,
                        "GATE_CHANGED", "INFO",
                        "Gate changed to " + status.getGate(),
                        String.format("Your %s flight gate has changed to %s (Terminal %s).",
                            flight.getFlightNumber(), status.getGate(), status.getTerminal()),
                        "Track Flight", "/live-tracker",
                        idempotencyKey);
                    alerts.add(alert);
                }
            }
        }

        // Check: Early departure warning (less than 2 hours until departure, still at ON_TIME)
        long minutesUntilDep = ChronoUnit.MINUTES.between(LocalDateTime.now(), flight.getDepartureTime());
        if (minutesUntilDep > 0 && minutesUntilDep < 120 && "ON_TIME".equals(status.getStatus())) {
            String idempotencyKey = "EARLY_DEP_" + booking.getId();
            if (!alertRepo.findByIdempotencyKey(idempotencyKey).isPresent()) {
                GuardianAlert alert = createAlert(user, booking, flight,
                    "DEPARTURE_UPCOMING", "LOW",
                    String.format("Your %s flight departs in %d minutes", flight.getFlightNumber(), minutesUntilDep),
                    String.format("Flight %s to %s departs at %s from Gate %s, Terminal %s.",
                        flight.getFlightNumber(),
                        flight.getDestinationCode(),
                        flight.getDepartureTime().toLocalTime(),
                        status.getGate() != null ? status.getGate() : "TBD",
                        status.getTerminal() != null ? status.getTerminal() : "TBD"),
                    "Track Flight", "/live-tracker",
                    idempotencyKey);
                alerts.add(alert);
            }
        }

        return alerts;
    }

    /**
     * Analyze connections between sequential flight bookings.
     */
    private List<GuardianAlert> analyzeConnections(User user, List<Booking> bookings) {
        List<GuardianAlert> alerts = new ArrayList<>();

        // Group flight bookings by date
        List<Booking> flightBookings = bookings.stream()
            .filter(b -> b.getFlight() != null && b.getTravelDate() != null)
            .sorted(Comparator.comparing(b -> b.getFlight().getDepartureTime()))
            .collect(Collectors.toList());

        for (int i = 0; i < flightBookings.size() - 1; i++) {
            Booking first = flightBookings.get(i);
            Booking second = flightBookings.get(i + 1);

            Flight firstFlight = first.getFlight();
            Flight secondFlight = second.getFlight();

            if (firstFlight == null || secondFlight == null) continue;

            // Check if first flight's destination connects to second flight's origin
            if (!firstFlight.getDestinationCode().equals(secondFlight.getOriginCode())) continue;

            Optional<FlightStatus> firstStatusOpt = flightStatusRepo.findByFlightId(firstFlight.getId());
            Optional<FlightStatus> secondStatusOpt = flightStatusRepo.findByFlightId(secondFlight.getId());

            if (firstStatusOpt.isEmpty() || secondStatusOpt.isEmpty()) continue;

            FlightStatus firstStatus = firstStatusOpt.get();
            FlightStatus secondStatus = secondStatusOpt.get();

            LocalDateTime firstArrival = firstStatus.getEstimatedArrival() != null
                ? firstStatus.getEstimatedArrival() : firstFlight.getArrivalTime();
            LocalDateTime secondDeparture = secondStatus.getEstimatedDeparture() != null
                ? secondStatus.getEstimatedDeparture() : secondFlight.getDepartureTime();

            long connectionMinutes = ChronoUnit.MINUTES.between(firstArrival, secondDeparture);

            if (connectionMinutes < MIN_CONNECTION_MINUTES) {
                String severity = connectionMinutes < 30 ? "CRITICAL" :
                    connectionMinutes < 60 ? "HIGH" : "MEDIUM";

                String idempotencyKey = "CONN_RISK_" + first.getId() + "_" + second.getId() + "_" + connectionMinutes;
                if (!alertRepo.findByIdempotencyKey(idempotencyKey).isPresent()) {
                    String delayInfo = "";
                    if ("DELAYED".equals(firstStatus.getStatus())) {
                        long delayMins = ChronoUnit.MINUTES.between(
                            firstStatus.getScheduledArrival(), firstStatus.getEstimatedArrival());
                        delayInfo = String.format(" (delayed by %d min)", delayMins);
                    }

                    GuardianAlert alert = createAlert(user, first, firstFlight,
                        "CONNECTION_AT_RISK", severity,
                        String.format("Connection window: %d minutes%s", connectionMinutes, delayInfo),
                        String.format("Your connection at %s from %s to %s has only %d minutes. " +
                            "Recommended minimum is %d minutes.%s",
                            firstFlight.getDestinationCode(),
                            firstFlight.getFlightNumber(),
                            secondFlight.getFlightNumber(),
                            connectionMinutes,
                            MIN_CONNECTION_MINUTES,
                            delayInfo.isEmpty() ? "" : " The first flight is currently delayed."),
                        "View Details", "/my-trips/" + first.getBookingReference(),
                        idempotencyKey);
                    alerts.add(alert);
                }
            }
        }

        return alerts;
    }

    /**
     * Get all active (non-dismissed) alerts for a user.
     */
    @Transactional(readOnly = true)
    public List<GuardianAlert> getUserAlerts(Long userId) {
        return alertRepo.findByUserIdAndIsDismissedOrderByCreatedAtDesc(userId, false);
    }

    /**
     * Get unread alert count for a user.
     */
    @Transactional(readOnly = true)
    public long getUnreadAlertCount(Long userId) {
        return alertRepo.countByUserIdAndIsReadFalseAndIsDismissedFalse(userId);
    }

    /**
     * Mark an alert as read.
     */
    @Transactional
    public void markAsRead(Long alertId, Long userId) {
        GuardianAlert alert = alertRepo.findById(alertId).orElseThrow();
        if (!alert.getUser().getId().equals(userId)) throw new RuntimeException("Unauthorized");
        alert.setRead(true);
        alertRepo.save(alert);
    }

    /**
     * Mark all alerts as read for a user.
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<GuardianAlert> alerts = alertRepo.findByUserIdAndIsDismissedOrderByCreatedAtDesc(userId, false);
        alerts.forEach(a -> a.setRead(true));
        alertRepo.saveAll(alerts);
    }

    /**
     * Dismiss an alert.
     */
    @Transactional
    public void dismissAlert(Long alertId, Long userId) {
        GuardianAlert alert = alertRepo.findById(alertId).orElseThrow();
        if (!alert.getUser().getId().equals(userId)) throw new RuntimeException("Unauthorized");
        alert.setDismissed(true);
        alertRepo.save(alert);
    }

    private GuardianAlert createAlert(User user, Booking booking, Flight flight,
                                       String alertType, String severity,
                                       String title, String message,
                                       String actionLabel, String actionRoute,
                                       String idempotencyKey) {
        GuardianAlert alert = new GuardianAlert();
        alert.setUser(user);
        alert.setBooking(booking);
        alert.setRelatedFlight(flight);
        alert.setAlertType(alertType);
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setActionLabel(actionLabel);
        alert.setActionRoute(actionRoute);
        alert.setIdempotencyKey(idempotencyKey);
        alertRepo.save(alert);

        // Also create in-app notification for HIGH/CRITICAL
        if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
            notificationService.createNotification(user, "GUARDIAN_ALERT",
                title, message, "BOOKING", booking.getId(), booking.getBookingReference(), "IN_APP");

            // Push real-time WebSocket alert
            try {
                java.util.Map<String, Object> wsPayload = new java.util.HashMap<>();
                wsPayload.put("id", alert.getId());
                wsPayload.put("alertType", alertType);
                wsPayload.put("severity", severity);
                wsPayload.put("title", title);
                wsPayload.put("message", message);
                wsPayload.put("actionLabel", actionLabel);
                wsPayload.put("actionRoute", actionRoute);
                wsPayload.put("bookingReference", booking.getBookingReference());
                wsPayload.put("createdAt", alert.getCreatedAt());
                wsNotificationService.sendGuardianAlert(user.getId(), wsPayload);
            } catch (Exception e) {
                log.warn("Failed to push guardian WebSocket alert: {}", e.getMessage());
            }
        }

        log.info("Guardian alert created: type={}, severity={}, user={}", alertType, severity, user.getEmail());
        return alert;
    }
}
