package com.travelplatform.service.provider;

import com.travelplatform.entity.Flight;
import com.travelplatform.entity.FlightStatus;
import com.travelplatform.entity.FlightStatusHistory;
import com.travelplatform.entity.TrackedFlight;
import com.travelplatform.repository.FlightRepository;
import com.travelplatform.repository.FlightStatusHistoryRepository;
import com.travelplatform.repository.FlightStatusRepository;
import com.travelplatform.repository.TrackedFlightRepository;
import com.travelplatform.entity.Booking;
import com.travelplatform.repository.BookingRepository;
import com.travelplatform.service.NotificationService;
import com.travelplatform.service.TravelGuardianService;
import com.travelplatform.service.WebSocketNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@ConditionalOnProperty(name = "flight.status.provider", havingValue = "mock", matchIfMissing = true)
public class MockFlightStatusProvider implements FlightStatusProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockFlightStatusProvider.class);

    private final FlightStatusRepository statusRepo;
    private final FlightStatusHistoryRepository historyRepo;
    private final FlightRepository flightRepo;
    private final TrackedFlightRepository trackedFlightRepo;
    private final NotificationService notificationService;
    private final WebSocketNotificationService wsNotificationService;
    private final TravelGuardianService guardianService;
    private final BookingRepository bookingRepo;

    private static final String[] WEATHER_REASONS = {
        "Adverse weather conditions at destination",
        "Heavy fog at origin airport",
        "Severe thunderstorm activity along route",
        "Volcanic ash advisory"
    };

    private static final String[] OPERATIONAL_REASONS = {
        "Technical maintenance required",
        "Air traffic control restrictions",
        "Late incoming aircraft",
        "Crew scheduling adjustment"
    };

    public MockFlightStatusProvider(FlightStatusRepository statusRepo,
                                  FlightStatusHistoryRepository historyRepo,
                                  FlightRepository flightRepo,
                                  TrackedFlightRepository trackedFlightRepo,
                                  NotificationService notificationService,
                                  WebSocketNotificationService wsNotificationService,
                                  TravelGuardianService guardianService,
                                  BookingRepository bookingRepo) {
        this.statusRepo = statusRepo;
        this.historyRepo = historyRepo;
        this.flightRepo = flightRepo;
        this.trackedFlightRepo = trackedFlightRepo;
        this.notificationService = notificationService;
        this.wsNotificationService = wsNotificationService;
        this.guardianService = guardianService;
        this.bookingRepo = bookingRepo;
    }

    @Override
    public Optional<FlightStatus> getLiveStatus(Long flightId) {
        return statusRepo.findByFlightId(flightId);
    }

    @Override
    public List<FlightStatus> getBatchLiveStatus(List<Long> flightIds) {
        return statusRepo.findByFlightIdIn(flightIds);
    }

    /**
     * Manually transition or simulate a flight into a specific scenario.
     * Supports: ON_TIME, DELAYED_WEATHER, DELAYED_OPERATIONAL, DELAYED, BOARDING, DEPARTED, IN_FLIGHT, APPROACHING, LANDED, CANCELLED.
     */
    @Override
    @Transactional
    public FlightStatus simulateFlightTransition(Long flightId, String targetScenario) {
        FlightStatus status = statusRepo.findByFlightId(flightId)
                .orElseThrow(() -> new NoSuchElementException("FlightStatus not found for flightId: " + flightId));
        Flight flight = status.getFlight();
        if (flight == null) return status;

        String previousStatus = status.getStatus();
        LocalDateTime previousEstDeparture = status.getEstimatedDeparture();
        LocalDateTime previousEstArrival = status.getEstimatedArrival();
        int previousDelay = status.getDelayMinutes();

        LocalDateTime scheduledDep = status.getScheduledDeparture() != null ? status.getScheduledDeparture() : flight.getDepartureTime();
        LocalDateTime scheduledArr = status.getScheduledArrival() != null ? status.getScheduledArrival() : flight.getArrivalTime();
        if (scheduledDep == null) scheduledDep = LocalDateTime.now().plusHours(2);
        if (scheduledArr == null) scheduledArr = scheduledDep.plusMinutes(flight.getDurationMinutes() > 0 ? flight.getDurationMinutes() : 120);

        status.setScheduledDeparture(scheduledDep);
        status.setScheduledArrival(scheduledArr);

        String normalizedScenario = targetScenario != null ? targetScenario.toUpperCase().trim() : "ON_TIME";
        status.setScenario(normalizedScenario);

        switch (normalizedScenario) {
            case "DELAYED_WEATHER" -> {
                status.setStatus("DELAYED");
                int delay = previousDelay > 0 ? previousDelay : 60;
                status.setDelayMinutes(delay);
                status.setDelayReason(WEATHER_REASONS[(int) (flightId % WEATHER_REASONS.length)]);
                status.setEstimatedDeparture(scheduledDep.plusMinutes(delay));
                status.setEstimatedArrival(scheduledArr.plusMinutes(delay));
            }
            case "DELAYED_OPERATIONAL" -> {
                status.setStatus("DELAYED");
                int delay = previousDelay > 0 ? previousDelay : 45;
                status.setDelayMinutes(delay);
                status.setDelayReason(OPERATIONAL_REASONS[(int) (flightId % OPERATIONAL_REASONS.length)]);
                status.setEstimatedDeparture(scheduledDep.plusMinutes(delay));
                status.setEstimatedArrival(scheduledArr.plusMinutes(delay));
            }
            case "DELAYED" -> {
                status.setStatus("DELAYED");
                int delay = status.getDelayMinutes() > 0 ? status.getDelayMinutes() : 60;
                status.setDelayMinutes(delay);
                if (status.getDelayReason() == null || status.getDelayReason().isBlank()) {
                    status.setDelayReason(WEATHER_REASONS[(int) (flightId % WEATHER_REASONS.length)]);
                }
                status.setEstimatedDeparture(scheduledDep.plusMinutes(delay));
                status.setEstimatedArrival(scheduledArr.plusMinutes(delay));
            }
            case "BOARDING" -> {
                status.setStatus("BOARDING");
                if (status.getBoardingTime() == null) {
                    status.setBoardingTime(LocalDateTime.now());
                }
                if (status.getGate() == null || status.getGate().isBlank()) {
                    status.setGate("G" + ((flightId % 20) + 1));
                }
                if (status.getTerminal() == null || status.getTerminal().isBlank()) {
                    status.setTerminal("T" + ((flightId % 3) + 1));
                }
            }
            case "DEPARTED" -> {
                status.setStatus("DEPARTED");
                if (status.getActualDeparture() == null) {
                    status.setActualDeparture(status.getEstimatedDeparture() != null ? status.getEstimatedDeparture() : scheduledDep);
                }
            }
            case "IN_FLIGHT" -> {
                status.setStatus("IN_FLIGHT");
                if (status.getActualDeparture() == null) {
                    status.setActualDeparture(status.getEstimatedDeparture() != null ? status.getEstimatedDeparture() : scheduledDep);
                }
                // Dynamic ETA: calculated from actual departure + simulated remaining duration
                long flightDuration = ChronoUnit.MINUTES.between(scheduledDep, scheduledArr);
                if (flightDuration <= 0) flightDuration = 120;
                status.setEstimatedArrival(status.getActualDeparture().plusMinutes(flightDuration));
            }
            case "APPROACHING" -> {
                status.setStatus("APPROACHING");
                if (status.getActualDeparture() == null) {
                    status.setActualDeparture(status.getEstimatedDeparture() != null ? status.getEstimatedDeparture() : scheduledDep);
                }
            }
            case "LANDED", "ARRIVED" -> {
                status.setStatus("LANDED");
                if (status.getActualArrival() == null) {
                    status.setActualArrival(status.getEstimatedArrival() != null ? status.getEstimatedArrival() : scheduledArr);
                }
                status.setEstimatedArrival(status.getActualArrival());
            }
            case "CANCELLED" -> {
                status.setStatus("CANCELLED");
                status.setDelayReason("Flight cancelled due to severe operational disruption");
            }
            case "CHECK_IN_OPEN" -> {
                status.setStatus("CHECK_IN_OPEN");
                status.setDelayMinutes(0);
                status.setDelayReason(null);
                status.setEstimatedDeparture(scheduledDep);
                status.setEstimatedArrival(scheduledArr);
            }
            case "SCHEDULED", "ON_TIME" -> {
                status.setStatus("ON_TIME");
                status.setDelayMinutes(0);
                status.setDelayReason(null);
                status.setEstimatedDeparture(scheduledDep);
                status.setEstimatedArrival(scheduledArr);
                status.setActualDeparture(null);
                status.setActualArrival(null);
            }
            default -> {
                status.setStatus(normalizedScenario);
            }
        }

        status.setLastSimulatedAt(LocalDateTime.now());
        FlightStatus saved = statusRepo.save(status);

        // Process and dispatch change detection events
        processChangesAndNotify(saved, previousStatus, previousEstDeparture, previousEstArrival, previousDelay);
        return saved;
    }

    /**
     * Scheduled synchronization running every 45 seconds.
     * Evaluates active flight statuses and progresses them deterministically.
     */
    @Override
    @Scheduled(fixedRate = 45000)
    @Transactional
    public void synchronizeStatuses() {
        List<FlightStatus> allStatuses = statusRepo.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (FlightStatus status : allStatuses) {
            Flight flight = status.getFlight();
            if (flight == null || flight.getDepartureTime() == null) continue;

            String previousStatus = status.getStatus();
            LocalDateTime previousEstDeparture = status.getEstimatedDeparture();
            LocalDateTime previousEstArrival = status.getEstimatedArrival();
            int previousDelay = status.getDelayMinutes();

            // Calculate minutes relative to scheduled/estimated departure
            LocalDateTime effectiveDeparture = status.getEstimatedDeparture() != null ? status.getEstimatedDeparture() : flight.getDepartureTime();
            LocalDateTime effectiveArrival = status.getEstimatedArrival() != null ? status.getEstimatedArrival() : flight.getArrivalTime();
            long minutesUntilDeparture = ChronoUnit.MINUTES.between(now, effectiveDeparture);
            long minutesUntilArrival = ChronoUnit.MINUTES.between(now, effectiveArrival);

            // Time-based deterministic progression
            if ("CANCELLED".equals(status.getStatus()) || "LANDED".equals(status.getStatus())) {
                continue; // Terminal states
            }

            String currentScenario = status.getScenario() != null ? status.getScenario() : "ON_TIME";
            boolean isDelayScenario = currentScenario.startsWith("DELAYED");

            // Evaluate state progression
            if (isDelayScenario && "ON_TIME".equals(previousStatus)) {
                // Transition into DELAYED
                status.setStatus("DELAYED");
                int delay = status.getDelayMinutes() > 0 ? status.getDelayMinutes() : 60;
                status.setDelayMinutes(delay);
                if (status.getDelayReason() == null) {
                    status.setDelayReason(WEATHER_REASONS[(int) (flight.getId() % WEATHER_REASONS.length)]);
                }
                status.setEstimatedDeparture(flight.getDepartureTime().plusMinutes(delay));
                status.setEstimatedArrival(flight.getArrivalTime().plusMinutes(delay));
            } else if (minutesUntilDeparture <= 0 && minutesUntilArrival > 20) {
                if (!"DEPARTED".equals(previousStatus) && !"IN_FLIGHT".equals(previousStatus)) {
                    status.setStatus("DEPARTED");
                    if (status.getActualDeparture() == null) {
                        status.setActualDeparture(now);
                    }
                } else if (minutesUntilDeparture < -15 && !"IN_FLIGHT".equals(previousStatus)) {
                    status.setStatus("IN_FLIGHT");
                }
            } else if (minutesUntilDeparture <= 45 && minutesUntilDeparture > 0) {
                if (!"BOARDING".equals(previousStatus) && !"DELAYED".equals(previousStatus)) {
                    status.setStatus("BOARDING");
                    if (status.getBoardingTime() == null) {
                        status.setBoardingTime(now);
                    }
                    if (status.getGate() == null) {
                        status.setGate("G" + ((flight.getId() % 20) + 1));
                    }
                }
            } else if (minutesUntilArrival <= 20 && minutesUntilArrival > 0) {
                if ("IN_FLIGHT".equals(previousStatus) || "DEPARTED".equals(previousStatus)) {
                    status.setStatus("APPROACHING");
                }
            } else if (minutesUntilArrival <= 0) {
                if (!"LANDED".equals(previousStatus)) {
                    status.setStatus("LANDED");
                    if (status.getActualArrival() == null) {
                        status.setActualArrival(now);
                    }
                    status.setEstimatedArrival(status.getActualArrival());
                }
            }

            status.setLastSimulatedAt(now);
            statusRepo.save(status);

            // Detect and dispatch changes
            processChangesAndNotify(status, previousStatus, previousEstDeparture, previousEstArrival, previousDelay);
        }
    }

    /**
     * Change Detection & Event Dispatcher.
     * Evaluates state diffs, records audit history, pushes STOMP updates,
     * and generates user-scoped, deduplicated notifications.
     */
    private void processChangesAndNotify(FlightStatus status, String previousStatus,
                                         LocalDateTime previousEstDep, LocalDateTime previousEstArr,
                                         int previousDelay) {
        Flight flight = status.getFlight();
        if (flight == null) return;

        boolean statusChanged = !status.getStatus().equals(previousStatus);
        boolean depChanged = previousEstDep != null && status.getEstimatedDeparture() != null
                && Math.abs(ChronoUnit.MINUTES.between(previousEstDep, status.getEstimatedDeparture())) >= 5;
        boolean etaChanged = previousEstArr != null && status.getEstimatedArrival() != null
                && Math.abs(ChronoUnit.MINUTES.between(previousEstArr, status.getEstimatedArrival())) >= 15;
        boolean delayChanged = status.getDelayMinutes() > 0 && status.getDelayMinutes() != previousDelay;

        if (!statusChanged && !depChanged && !etaChanged && !delayChanged) {
            // No significant change; suppress duplicate events
            return;
        }

        String eventMessage = buildStatusMessage(status, previousStatus);

        // 1. Audit trail in FlightStatusHistory
        FlightStatusHistory history = new FlightStatusHistory();
        history.setFlight(flight);
        history.setPreviousStatus(previousStatus != null ? previousStatus : "UNKNOWN");
        history.setNewStatus(status.getStatus());
        history.setDelayReason(status.getDelayReason());
        history.setPreviousEstimatedDeparture(previousEstDep);
        history.setNewEstimatedDeparture(status.getEstimatedDeparture());
        history.setMessage(eventMessage);
        historyRepo.save(history);

        // 2. Real-time WebSocket push
        wsNotificationService.sendFlightStatusUpdate(status, eventMessage);

        // 3. User Notifications (Dispatched individually per subscriber with user-scoped idempotency keys)
        List<TrackedFlight> subscribers = trackedFlightRepo.findByFlightId(flight.getId());
        for (TrackedFlight tf : subscribers) {
            if (!tf.isNotificationsEnabled()) continue;

            if (statusChanged) {
                switch (status.getStatus()) {
                    case "DELAYED" -> notificationService.notifyFlightDelayed(
                            tf.getUser(), flight.getFlightNumber(), flight.getOriginCode(),
                            flight.getDestinationCode(), eventMessage, flight.getId(),
                            status.getDelayMinutes(), status.getEstimatedDeparture()
                    );
                    case "BOARDING" -> notificationService.notifyBoardingStarted(
                            tf.getUser(), flight.getFlightNumber(), flight.getOriginCode(),
                            flight.getDestinationCode(), status.getGate(), status.getTerminal(), flight.getId()
                    );
                    case "DEPARTED" -> notificationService.notifyFlightStatusChanged(
                            tf.getUser(), flight.getFlightNumber(), flight.getOriginCode(),
                            flight.getDestinationCode(), "DEPARTED", flight.getId()
                    );
                    case "LANDED" -> notificationService.notifyFlightStatusChanged(
                            tf.getUser(), flight.getFlightNumber(), flight.getOriginCode(),
                            flight.getDestinationCode(), "LANDED", flight.getId()
                    );
                    case "CANCELLED" -> notificationService.notifyFlightStatusChanged(
                            tf.getUser(), flight.getFlightNumber(), flight.getOriginCode(),
                            flight.getDestinationCode(), "CANCELLED", flight.getId()
                    );
                    default -> notificationService.notifyFlightStatusChanged(
                            tf.getUser(), flight.getFlightNumber(), flight.getOriginCode(),
                            flight.getDestinationCode(), status.getStatus(), flight.getId()
                    );
                }
            } else if (depChanged) {
                notificationService.notifyDepartureTimeChanged(
                        tf.getUser(), flight.getFlightNumber(), flight.getOriginCode(),
                        flight.getDestinationCode(), previousEstDep, status.getEstimatedDeparture(), flight.getId()
                );
            } else if (etaChanged) {
                notificationService.notifyFlightEtaChanged(
                        tf.getUser(), flight.getFlightNumber(), flight.getOriginCode(),
                        flight.getDestinationCode(), previousEstArr, status.getEstimatedArrival(), flight.getId()
                );
            }
        }

        logger.info("Flight {} transition: {} → {} (Delay: {}m, Dep: {}, Arr: {})",
                flight.getFlightNumber(), previousStatus, status.getStatus(),
                status.getDelayMinutes(), status.getEstimatedDeparture(), status.getEstimatedArrival());

        // 4. Trigger Travel Guardian alerts for confirmed/pending bookings
        if ("DELAYED".equals(status.getStatus()) || "CANCELLED".equals(status.getStatus()) || "DEPARTED".equals(status.getStatus())) {
            try {
                bookingRepo.findByFlightId(flight.getId()).stream()
                        .filter(b -> "CONFIRMED".equals(b.getStatus()) || "PENDING".equals(b.getStatus()))
                        .map(b -> b.getUser().getId())
                        .distinct()
                        .forEach(userId -> {
                            try {
                                guardianService.analyzeAndGenerateAlerts(userId);
                            } catch (Exception e) {
                                logger.warn("Guardian analysis failed for user {}: {}", userId, e.getMessage());
                            }
                        });
            } catch (Exception e) {
                logger.warn("Could not trigger guardian analysis: {}", e.getMessage());
            }
        }
    }

    private String buildStatusMessage(FlightStatus status, String previousStatus) {
        Flight flight = status.getFlight();
        String flightNum = flight != null ? flight.getFlightNumber() : "Flight";
        return switch (status.getStatus()) {
            case "DELAYED" -> flightNum + " is delayed by " + status.getDelayMinutes() + " minutes due to " +
                    (status.getDelayReason() != null ? status.getDelayReason() : "operational requirements") + ".";
            case "BOARDING" -> "Boarding has commenced for " + flightNum +
                    (status.getGate() != null ? " at Gate " + status.getGate() : "") + ".";
            case "DEPARTED" -> flightNum + " has departed.";
            case "IN_FLIGHT" -> flightNum + " is now in flight.";
            case "APPROACHING" -> flightNum + " is approaching its destination.";
            case "LANDED" -> flightNum + " has landed safely.";
            case "CANCELLED" -> flightNum + " has been cancelled.";
            default -> flightNum + " status updated to " + status.getStatus() + ".";
        };
    }
}
