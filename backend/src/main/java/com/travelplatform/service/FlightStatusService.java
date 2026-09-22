package com.travelplatform.service;

import com.travelplatform.entity.Flight;
import com.travelplatform.entity.FlightStatus;
import com.travelplatform.entity.FlightStatusHistory;
import com.travelplatform.entity.TrackedFlight;
import com.travelplatform.entity.User;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.repository.FlightRepository;
import com.travelplatform.repository.FlightStatusHistoryRepository;
import com.travelplatform.repository.FlightStatusRepository;
import com.travelplatform.repository.TrackedFlightRepository;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.service.provider.FlightStatusProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlightStatusService {

    private final FlightStatusRepository statusRepo;
    private final FlightStatusHistoryRepository historyRepo;
    private final FlightRepository flightRepo;
    private final TrackedFlightRepository trackedFlightRepo;
    private final UserRepository userRepo;
    private final FlightStatusProvider provider;

    public FlightStatusService(FlightStatusRepository statusRepo,
                                FlightStatusHistoryRepository historyRepo,
                                FlightRepository flightRepo,
                                TrackedFlightRepository trackedFlightRepo,
                                UserRepository userRepo,
                                FlightStatusProvider provider) {
        this.statusRepo = statusRepo;
        this.historyRepo = historyRepo;
        this.flightRepo = flightRepo;
        this.trackedFlightRepo = trackedFlightRepo;
        this.userRepo = userRepo;
        this.provider = provider;
    }

    /** Get current live status for a flight. Creates initial ON_TIME record if flight exists but status record is missing. */
    @Transactional
    public FlightStatus getFlightStatus(Long flightId) {
        Optional<FlightStatus> statusOpt = provider.getLiveStatus(flightId);
        if (statusOpt.isPresent()) {
            return statusOpt.get();
        }

        // Check if flight actually exists
        Flight flight = flightRepo.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", flightId));

        // Create default ON_TIME status
        FlightStatus status = new FlightStatus();
        status.setFlight(flight);
        status.setStatus("ON_TIME");
        status.setScheduledDeparture(flight.getDepartureTime());
        status.setEstimatedDeparture(flight.getDepartureTime());
        status.setScheduledArrival(flight.getArrivalTime());
        status.setEstimatedArrival(flight.getArrivalTime());
        status.setGate("G" + ((flight.getId() % 20) + 1));
        status.setTerminal("T" + ((flight.getId() % 3) + 1));
        status.setScenario("ON_TIME");
        return statusRepo.save(status);
    }

    /** Get live status for multiple tracked flights */
    @Transactional(readOnly = true)
    public List<FlightStatus> getTrackedFlightStatuses(List<Long> flightIds) {
        if (flightIds == null || flightIds.isEmpty()) return List.of();
        return provider.getBatchLiveStatus(flightIds);
    }

    /** Search flights and their statuses */
    @Transactional
    public List<FlightStatus> searchFlightStatuses(String query) {
        if (query == null || query.trim().isEmpty()) {
            return statusRepo.findAllWithDetails(PageRequest.of(0, 20)).getContent();
        }
        List<Flight> matchingFlights = flightRepo.searchByQuery(query.trim(), PageRequest.of(0, 20));
        List<FlightStatus> results = new ArrayList<>();
        for (Flight f : matchingFlights) {
            results.add(getFlightStatus(f.getId()));
        }
        return results;
    }

    /** Manually trigger simulation transition */
    @Transactional
    public FlightStatus simulateTransition(Long flightId, String targetScenario) {
        return provider.simulateFlightTransition(flightId, targetScenario);
    }

    /** Get status change history for a flight */
    @Transactional(readOnly = true)
    public List<FlightStatusHistory> getFlightStatusHistory(Long flightId) {
        return historyRepo.findByFlightIdOrderByCreatedAtDesc(flightId);
    }

    /** Track a flight for a user */
    @Transactional
    public void trackFlight(Long userId, Long flightId) {
        if (trackedFlightRepo.existsByUserIdAndFlightId(userId, flightId)) {
            return; // Already tracked (idempotent)
        }
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Flight flight = flightRepo.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", flightId));

        // Ensure status record exists
        getFlightStatus(flightId);

        TrackedFlight trackedFlight = new TrackedFlight(user, flight);
        trackedFlightRepo.save(trackedFlight);
    }

    /** Untrack a flight for a user */
    @Transactional
    public void untrackFlight(Long userId, Long flightId) {
        trackedFlightRepo.findByUserIdAndFlightId(userId, flightId)
                .ifPresent(trackedFlightRepo::delete);
    }

    /** Get all tracked flights for a user */
    @Transactional
    public List<FlightStatus> getUserTrackedFlights(Long userId) {
        List<Long> flightIds = trackedFlightRepo.findByUserIdOrderByTrackedAtDesc(userId)
                .stream().map(tf -> tf.getFlight().getId()).collect(Collectors.toList());
        if (flightIds.isEmpty()) return List.of();

        List<FlightStatus> statuses = new ArrayList<>();
        for (Long fid : flightIds) {
            try {
                statuses.add(getFlightStatus(fid));
            } catch (Exception ignored) {}
        }
        return statuses;
    }
}
