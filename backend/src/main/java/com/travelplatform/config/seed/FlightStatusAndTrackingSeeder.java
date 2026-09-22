package com.travelplatform.config.seed;

import com.travelplatform.entity.Flight;
import com.travelplatform.entity.FlightStatus;
import com.travelplatform.entity.TrackedFlight;
import com.travelplatform.entity.User;
import com.travelplatform.repository.FlightStatusRepository;
import com.travelplatform.repository.TrackedFlightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class FlightStatusAndTrackingSeeder {

    private static final Logger log = LoggerFactory.getLogger(FlightStatusAndTrackingSeeder.class);

    private final FlightStatusRepository flightStatusRepo;
    private final TrackedFlightRepository trackedFlightRepo;

    public FlightStatusAndTrackingSeeder(FlightStatusRepository flightStatusRepo,
                                         TrackedFlightRepository trackedFlightRepo) {
        this.flightStatusRepo = flightStatusRepo;
        this.trackedFlightRepo = trackedFlightRepo;
    }

    @Transactional
    public void seedFlightStatuses(List<Flight> flights) {
        long currentCount = flightStatusRepo.count();
        if (currentCount >= 200) {
            log.info("Flight statuses already seeded ({}), skipping.", currentCount);
            return;
        }

        log.info("Seeding realistic flight statuses exercising all live status scenarios...");
        List<FlightStatus> statusBatch = new ArrayList<>();
        Random rng = new Random(42);

        String[] terminals = {"T1", "T2", "T3"};
        String[] gates = {"A1", "A4", "B2", "B6", "C3", "C8", "D1", "D5", "E2"};

        String[] scenarios = {
            "ON_TIME", "ON_TIME", "ON_TIME", "ON_TIME",
            "BOARDING", "DEPARTED", "IN_FLIGHT", "APPROACHING", "LANDED",
            "DELAYED_WEATHER", "DELAYED_OPERATIONAL", "DELAYED", "CANCELLED"
        };

        String[] weatherReasons = {
            "Heavy fog reducing visibility at destination",
            "Monsoon thunderstorm activity along route",
            "Adverse weather and crosswinds at arrival airport",
            "Air traffic diversion due to storm cell"
        };

        String[] operationalReasons = {
            "Technical maintenance clearance required",
            "Air traffic control congestion at runway",
            "Late arrival of inbound connecting aircraft",
            "Mandatory crew rest and scheduling adjustment"
        };

        Set<Long> existingFlightIds = new HashSet<>();
        for (FlightStatus fs : flightStatusRepo.findAll()) {
            if (fs.getFlight() != null) {
                existingFlightIds.add(fs.getFlight().getId());
            }
        }

        for (Flight flight : flights) {
            if (existingFlightIds.contains(flight.getId())) continue;

            FlightStatus fs = new FlightStatus();
            fs.setFlight(flight);
            fs.setScheduledDeparture(flight.getDepartureTime());
            fs.setScheduledArrival(flight.getArrivalTime());
            fs.setEstimatedDeparture(flight.getDepartureTime());
            fs.setEstimatedArrival(flight.getArrivalTime());
            fs.setTerminal(terminals[rng.nextInt(terminals.length)]);
            fs.setGate(gates[rng.nextInt(gates.length)]);

            String scenario = scenarios[rng.nextInt(scenarios.length)];
            fs.setScenario(scenario);

            switch (scenario) {
                case "BOARDING" -> {
                    fs.setStatus("BOARDING");
                    fs.setBoardingTime(flight.getDepartureTime().minusMinutes(35));
                }
                case "DEPARTED" -> {
                    fs.setStatus("DEPARTED");
                    fs.setActualDeparture(flight.getDepartureTime());
                }
                case "IN_FLIGHT" -> {
                    fs.setStatus("IN_FLIGHT");
                    fs.setActualDeparture(flight.getDepartureTime().minusMinutes(40));
                    fs.setEstimatedArrival(flight.getArrivalTime());
                }
                case "APPROACHING" -> {
                    fs.setStatus("APPROACHING");
                    fs.setActualDeparture(flight.getDepartureTime().minusMinutes(flight.getDurationMinutes() - 20));
                }
                case "LANDED" -> {
                    fs.setStatus("LANDED");
                    fs.setActualDeparture(flight.getDepartureTime());
                    fs.setActualArrival(flight.getArrivalTime());
                }
                case "DELAYED_WEATHER", "DELAYED" -> {
                    fs.setStatus("DELAYED");
                    int delay = 25 + (rng.nextInt(7) * 15);
                    fs.setDelayMinutes(delay);
                    fs.setDelayReason(weatherReasons[rng.nextInt(weatherReasons.length)]);
                    fs.setEstimatedDeparture(flight.getDepartureTime().plusMinutes(delay));
                    fs.setEstimatedArrival(flight.getArrivalTime().plusMinutes(delay));
                }
                case "DELAYED_OPERATIONAL" -> {
                    fs.setStatus("DELAYED");
                    int delay = 30 + (rng.nextInt(6) * 15);
                    fs.setDelayMinutes(delay);
                    fs.setDelayReason(operationalReasons[rng.nextInt(operationalReasons.length)]);
                    fs.setEstimatedDeparture(flight.getDepartureTime().plusMinutes(delay));
                    fs.setEstimatedArrival(flight.getArrivalTime().plusMinutes(delay));
                }
                case "CANCELLED" -> {
                    fs.setStatus("CANCELLED");
                    fs.setDelayReason("Operational schedule consolidation and carrier maintenance");
                }
                default -> {
                    fs.setStatus("ON_TIME");
                    fs.setDelayMinutes(0);
                }
            }

            statusBatch.add(fs);
            if (statusBatch.size() >= 300) {
                flightStatusRepo.saveAll(statusBatch);
                statusBatch.clear();
            }
        }

        if (!statusBatch.isEmpty()) {
            flightStatusRepo.saveAll(statusBatch);
            statusBatch.clear();
        }

        log.info("Total flight statuses persisted: {}", flightStatusRepo.count());
    }

    @Transactional
    public void seedTrackedFlights(List<Flight> flights, User user, User traveler) {
        if (trackedFlightRepo.count() >= 5) {
            log.info("Tracked flights already seeded, skipping.");
            return;
        }

        log.info("Seeding tracked flights for test users...");
        if (flights.size() < 6) return;

        List<TrackedFlight> trackedList = new ArrayList<>();

        if (user != null) {
            if (!trackedFlightRepo.existsByUserIdAndFlightId(user.getId(), flights.get(0).getId())) {
                trackedList.add(new TrackedFlight(user, flights.get(0)));
            }
            if (!trackedFlightRepo.existsByUserIdAndFlightId(user.getId(), flights.get(1).getId())) {
                trackedList.add(new TrackedFlight(user, flights.get(1)));
            }
            if (!trackedFlightRepo.existsByUserIdAndFlightId(user.getId(), flights.get(2).getId())) {
                trackedList.add(new TrackedFlight(user, flights.get(2)));
            }
        }

        if (traveler != null) {
            if (!trackedFlightRepo.existsByUserIdAndFlightId(traveler.getId(), flights.get(3).getId())) {
                trackedList.add(new TrackedFlight(traveler, flights.get(3)));
            }
            if (!trackedFlightRepo.existsByUserIdAndFlightId(traveler.getId(), flights.get(4).getId())) {
                trackedList.add(new TrackedFlight(traveler, flights.get(4)));
            }
            if (!trackedFlightRepo.existsByUserIdAndFlightId(traveler.getId(), flights.get(5).getId())) {
                trackedList.add(new TrackedFlight(traveler, flights.get(5)));
            }
        }

        if (!trackedList.isEmpty()) {
            trackedFlightRepo.saveAll(trackedList);
            log.info("Seeded {} tracked flights for test users.", trackedList.size());
        }
    }
}
