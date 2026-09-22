package com.travelplatform.service;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Connection Risk Score — calculates risk for connecting flights.
 *
 * Risk factors:
 * - Connection duration
 * - Terminal change required
 * - Current delay status
 * - Time of day
 *
 * Levels: LOW, MEDIUM, HIGH, CRITICAL
 * Labelled as estimated calculations, not airline-official MCTs.
 */
@Service
public class ConnectionRiskService {

    private final ConnectionRiskRepository riskRepo;
    private final FlightStatusRepository flightStatusRepo;
    private final FlightRepository flightRepo;
    private final UserRepository userRepo;

    /** Estimated minimum connection time in minutes (standard domestic) */
    private static final int DEFAULT_MCT = 90;

    public ConnectionRiskService(ConnectionRiskRepository riskRepo,
                                  FlightStatusRepository flightStatusRepo,
                                  FlightRepository flightRepo,
                                  UserRepository userRepo) {
        this.riskRepo = riskRepo;
        this.flightStatusRepo = flightStatusRepo;
        this.flightRepo = flightRepo;
        this.userRepo = userRepo;
    }

    /**
     * Calculate connection risk between two flights.
     */
    @Transactional
    public ConnectionRisk calculateRisk(Long userId, Long firstFlightId, Long secondFlightId) {
        Flight firstFlight = flightRepo.findById(firstFlightId).orElseThrow();
        Flight secondFlight = flightRepo.findById(secondFlightId).orElseThrow();
        User user = userRepo.findById(userId).orElseThrow();

        Optional<FlightStatus> firstStatusOpt = flightStatusRepo.findByFlightId(firstFlightId);
        Optional<FlightStatus> secondStatusOpt = flightStatusRepo.findByFlightId(secondFlightId);

        // Determine actual arrival of first flight
        LocalDateTime firstArrival = firstStatusOpt
            .filter(s -> s.getEstimatedArrival() != null)
            .map(FlightStatus::getEstimatedArrival)
            .orElse(firstFlight.getArrivalTime());

        // Determine actual departure of second flight
        LocalDateTime secondDeparture = secondStatusOpt
            .filter(s -> s.getEstimatedDeparture() != null)
            .map(FlightStatus::getEstimatedDeparture)
            .orElse(secondFlight.getDepartureTime());

        long connectionMinutes = ChronoUnit.MINUTES.between(firstArrival, secondDeparture);
        if (connectionMinutes < 0) connectionMinutes = 0;

        // Calculate risk level
        String riskLevel;
        List<String> factors = new ArrayList<>();

        boolean firstDelayed = firstStatusOpt.map(s -> "DELAYED".equals(s.getStatus())).orElse(false);
        boolean sameAirport = firstFlight.getDestinationCode().equals(secondFlight.getOriginCode());
        boolean sameTerminal = firstFlight.getDestination() != null && secondFlight.getOrigin() != null
            && firstFlight.getDestination().getCity().equals(secondFlight.getOrigin().getCity());

        if (firstDelayed) {
            long delayMins = firstStatusOpt
                .map(s -> ChronoUnit.MINUTES.between(s.getScheduledArrival(), s.getEstimatedArrival()))
                .orElse(0L);
            factors.add("First flight delayed by " + delayMins + " minutes");
        }

        if (!sameTerminal) {
            factors.add("Terminal change may be required");
        }

        if (!sameAirport) {
            factors.add("Different airports — ground transfer needed");
            // For different airports, add extra minimum connection time
            long effectiveConnection = connectionMinutes;
            if (effectiveConnection < 180) {
                factors.add("Insufficient time for airport transfer");
            }
        }

        // Determine risk based on connection time vs MCT
        if (connectionMinutes < 30) {
            riskLevel = "CRITICAL";
            factors.add("Connection window critically short: " + connectionMinutes + " minutes");
        } else if (connectionMinutes < 60 || (firstDelayed && connectionMinutes < 90)) {
            riskLevel = "HIGH";
            factors.add("Connection window tight: " + connectionMinutes + " minutes (recommended: " + DEFAULT_MCT + ")");
        } else if (connectionMinutes < DEFAULT_MCT) {
            riskLevel = "MEDIUM";
            factors.add("Below recommended minimum: " + connectionMinutes + " minutes");
        } else {
            riskLevel = "LOW";
            factors.add("Adequate connection time: " + connectionMinutes + " minutes");
        }

        // Persist
        ConnectionRisk risk = new ConnectionRisk();
        risk.setUser(user);
        risk.setFirstFlight(firstFlight);
        risk.setSecondFlight(secondFlight);
        risk.setRiskLevel(riskLevel);
        risk.setConnectionMinutes((int) connectionMinutes);
        risk.setRequiredMinutes(DEFAULT_MCT);
        risk.setRiskFactors(String.join(" | ", factors));
        risk.setLastCalculatedAt(LocalDateTime.now());

        return riskRepo.save(risk);
    }

    /**
     * Get risk for a specific booking.
     */
    @Transactional(readOnly = true)
    public Optional<ConnectionRisk> getRiskForBooking(Long bookingId) {
        return riskRepo.findByBookingId(bookingId);
    }

    /**
     * Get all risks for a user.
     */
    @Transactional(readOnly = true)
    public List<ConnectionRisk> getUserRisks(Long userId) {
        return riskRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
