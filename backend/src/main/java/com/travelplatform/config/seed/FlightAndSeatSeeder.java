package com.travelplatform.config.seed;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
public class FlightAndSeatSeeder {

    private static final Logger log = LoggerFactory.getLogger(FlightAndSeatSeeder.class);

    private final FlightRepository flightRepo;
    private final SeatRepository seatRepo;
    private final FareOptionRepository fareOptionRepo;

    public FlightAndSeatSeeder(FlightRepository flightRepo, SeatRepository seatRepo, FareOptionRepository fareOptionRepo) {
        this.flightRepo = flightRepo;
        this.seatRepo = seatRepo;
        this.fareOptionRepo = fareOptionRepo;
    }

    @Transactional
    public List<Flight> seedFlights(Map<String, Airport> airportMap, List<Airline> airlines) {
        long currentFlightCount = flightRepo.count();
        if (currentFlightCount >= 500) {
            log.info("Sufficient flights already seeded ({}), skipping bulk flight generation.", currentFlightCount);
            return flightRepo.findAll();
        }

        log.info("Seeding realistic flight network across corridors...");
        List<Flight> flightsToSave = new ArrayList<>();
        LocalDate today = LocalDate.now();
        Random rng = new Random(42);

        Map<String, Airline> airlineByCode = new HashMap<>();
        for (Airline a : airlines) {
            airlineByCode.put(a.getCode(), a);
        }

        // Standard domestic airlines
        List<Airline> domesticAirlines = airlines.stream()
                .filter(a -> List.of("6E", "AI", "QP", "SG", "UK", "IX").contains(a.getCode()))
                .toList();
        if (domesticAirlines.isEmpty()) domesticAirlines = airlines;

        // International airlines
        List<Airline> internationalAirlines = airlines.stream()
                .filter(a -> List.of("EK", "QR", "SQ", "BA", "LH", "EY", "AI", "6E", "UL", "TG", "MH", "AF").contains(a.getCode()))
                .toList();

        // 5 standard departure slots throughout the day
        int[][] departureSlots = {
            {6, 15},   // Early morning
            {9, 45},   // Mid morning
            {13, 30},  // Afternoon
            {17, 50},  // Evening peak
            {21, 40}   // Night / Red-eye
        };

        int globalFlightSeq = 1000;

        // 16 days coverage: yesterday (-1), today (0), next 14 days (+1 to +14)
        for (int dayOffset = -1; dayOffset <= 14; dayOffset++) {
            LocalDate flightDate = today.plusDays(dayOffset);

            for (int corridorIdx = 0; corridorIdx < SeedConstants.FLIGHT_CORRIDORS.length; corridorIdx++) {
                String corridor = SeedConstants.FLIGHT_CORRIDORS[corridorIdx];
                String[] codes = corridor.split("-");
                String originCode = codes[0];
                String destCode = codes[1];

                Airport origin = airportMap.get(originCode);
                Airport dest = airportMap.get(destCode);
                if (origin == null || dest == null) continue;

                boolean isInternational = !"India".equalsIgnoreCase(origin.getCountry()) || !"India".equalsIgnoreCase(dest.getCountry());
                List<Airline> availableCarriers = isInternational ? internationalAirlines : domesticAirlines;

                // High-density routes (e.g. BOM-DEL, DEL-BLR, BOM-GOI, DEL-DXB) get 3-5 daily flights; others get 1-2
                boolean isTrunkRoute = corridor.equals("BOM-DEL") || corridor.equals("DEL-BOM") ||
                                      corridor.equals("BOM-BLR") || corridor.equals("BLR-BOM") ||
                                      corridor.equals("DEL-BLR") || corridor.equals("BLR-DEL") ||
                                      corridor.equals("BOM-GOI") || corridor.equals("GOI-BOM") ||
                                      corridor.equals("DEL-DXB") || corridor.equals("DXB-DEL") ||
                                      corridor.equals("BOM-DXB") || corridor.equals("DXB-BOM") ||
                                      corridor.equals("BOM-SIN") || corridor.equals("SIN-BOM");

                int flightsOnRoute = isTrunkRoute ? (dayOffset <= 7 ? 4 : 2) : (dayOffset <= 3 ? 2 : 1);

                for (int slotIdx = 0; slotIdx < flightsOnRoute; slotIdx++) {
                    int[] slot = departureSlots[(corridorIdx + slotIdx) % departureSlots.length];
                    Airline airline = availableCarriers.get((corridorIdx + slotIdx + Math.abs(dayOffset)) % availableCarriers.size());

                    int baseDurationMins = estimateDurationMinutes(origin, dest);
                    int durationVariance = rng.nextInt(25) - 10;
                    int duration = Math.max(45, baseDurationMins + durationVariance);

                    int stops = (isInternational && duration > 360 && rng.nextInt(3) == 0) ? 1 : 0;
                    String stopoverCity = stops == 1 ? (isInternational ? "Doha" : "Mumbai") : null;

                    int depHour = (slot[0] + rng.nextInt(2)) % 24;
                    int depMin = (slot[1] + (rng.nextInt(4) * 10)) % 60;
                    LocalDateTime depTime = LocalDateTime.of(flightDate, LocalTime.of(depHour, depMin));
                    LocalDateTime arrTime = depTime.plusMinutes(duration + (stops * 90));

                    Flight flight = new Flight();
                    String flightNum = airline.getCode() + "-" + (globalFlightSeq++);
                    flight.setFlightNumber(flightNum);
                    flight.setAirline(airline);
                    flight.setOrigin(origin);
                    flight.setDestination(dest);
                    flight.setOriginCode(originCode);
                    flight.setDestinationCode(destCode);
                    flight.setDepartureDate(flightDate);
                    flight.setDepartureTime(depTime);
                    flight.setArrivalTime(arrTime);
                    flight.setDurationMinutes(duration);
                    flight.setStops(stops);
                    flight.setStopoverCity(stopoverCity);

                    // Realistic Pricing
                    BigDecimal econPrice;
                    if (isInternational) {
                        int baseIntl = 15000 + (duration * 25) + rng.nextInt(5000);
                        econPrice = BigDecimal.valueOf(baseIntl);
                    } else {
                        int baseDom = 2800 + (duration * 8) + rng.nextInt(1500);
                        econPrice = BigDecimal.valueOf(baseDom);
                    }

                    flight.setEconomyPrice(econPrice);
                    flight.setEconomyBasePrice(econPrice);

                    BigDecimal premEcon = econPrice.multiply(BigDecimal.valueOf(1.45)).setScale(2, RoundingMode.HALF_UP);
                    flight.setPremiumEconomyPrice(premEcon);
                    flight.setPremiumEconomyBasePrice(premEcon);

                    BigDecimal biz = econPrice.multiply(BigDecimal.valueOf(2.8)).setScale(2, RoundingMode.HALF_UP);
                    flight.setBusinessPrice(biz);
                    flight.setBusinessBasePrice(biz);

                    BigDecimal first = econPrice.multiply(BigDecimal.valueOf(4.5)).setScale(2, RoundingMode.HALF_UP);
                    flight.setFirstClassPrice(first);
                    flight.setFirstClassBasePrice(first);

                    // Capacities and realistic deterministic occupancy
                    int totalEcon = 150 + (rng.nextInt(4) * 10);
                    int bookedEcon = (int) (totalEcon * (0.30 + (rng.nextDouble() * 0.50)));
                    flight.setTotalSeatsEconomy(totalEcon);
                    flight.setBookedSeatsEconomy(bookedEcon);

                    int totalPrem = 24;
                    int bookedPrem = rng.nextInt(16);
                    flight.setTotalSeatsPremiumEconomy(totalPrem);
                    flight.setBookedSeatsPremiumEconomy(bookedPrem);

                    int totalBiz = 18;
                    int bookedBiz = rng.nextInt(12);
                    flight.setTotalSeatsBusiness(totalBiz);
                    flight.setBookedSeatsBusiness(bookedBiz);

                    int totalFirst = 6;
                    int bookedFirst = rng.nextInt(4);
                    flight.setTotalSeatsFirst(totalFirst);
                    flight.setBookedSeatsFirst(bookedFirst);

                    flight.setActive(true);
                    flightsToSave.add(flight);

                    // Batch save every 250 flights for memory and speed
                    if (flightsToSave.size() >= 250) {
                        flightRepo.saveAll(flightsToSave);
                        flightsToSave.clear();
                    }
                }
            }
        }

        if (!flightsToSave.isEmpty()) {
            flightRepo.saveAll(flightsToSave);
            flightsToSave.clear();
        }

        long totalSaved = flightRepo.count();
        log.info("Total flights persisted in database: {}", totalSaved);
        return flightRepo.findAll();
    }

    private int estimateDurationMinutes(Airport origin, Airport dest) {
        // Approximate great-circle flight duration using coordinates
        double lat1 = Math.toRadians(origin.getLatitude());
        double lon1 = Math.toRadians(origin.getLongitude());
        double lat2 = Math.toRadians(dest.getLatitude());
        double lon2 = Math.toRadians(dest.getLongitude());

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = 6371 * c;

        // Approx 750 km/h average commercial flight speed + 30 mins taxi/climb/descent
        int flightMins = (int) ((distanceKm / 750.0) * 60) + 30;
        return Math.max(50, flightMins);
    }

    @Transactional
    public void seedSeatsAndFareOptions(List<Flight> allFlights) {
        long currentSeatCount = seatRepo.count();
        if (currentSeatCount >= 5000) {
            log.info("Seats already seeded ({}), skipping bulk seat generation.", currentSeatCount);
        } else {
            log.info("Generating realistic seat maps for scheduled flights...");
            List<Seat> seatBatch = new ArrayList<>(1000);
            Random seatRng = new Random(42);
            String[] columns = {"A", "B", "C", "D", "E", "F"};

            // Generate full seat map for representative flights (up to 120 key flights across corridors)
            int flightsWithFullSeats = Math.min(allFlights.size(), 120);
            for (int i = 0; i < flightsWithFullSeats; i++) {
                Flight flight = allFlights.get(i);

                // Rows 1-4: Business / First (wider layout)
                for (int row = 1; row <= 4; row++) {
                    for (String col : new String[]{"A", "C", "D", "F"}) {
                        Seat s = buildSeat(flight, "BUSINESS", row, col, seatRng);
                        seatBatch.add(s);
                    }
                }

                // Rows 5-7: Premium Economy
                for (int row = 5; row <= 7; row++) {
                    for (String col : columns) {
                        Seat s = buildSeat(flight, "PREMIUM_ECONOMY", row, col, seatRng);
                        seatBatch.add(s);
                    }
                }

                // Rows 8-28: Economy (including extra legroom row 8, exit row 12 & 13)
                for (int row = 8; row <= 28; row++) {
                    for (String col : columns) {
                        Seat s = buildSeat(flight, "ECONOMY", row, col, seatRng);
                        seatBatch.add(s);
                    }
                }

                if (seatBatch.size() >= 1000) {
                    seatRepo.saveAll(seatBatch);
                    seatBatch.clear();
                }
            }

            if (!seatBatch.isEmpty()) {
                seatRepo.saveAll(seatBatch);
                seatBatch.clear();
            }

            log.info("Total seats persisted: {}", seatRepo.count());
        }

        // Fare Options
        if (fareOptionRepo.count() == 0) {
            log.info("Seeding fare options (SAVER, STANDARD, FLEX)...");
            List<FareOption> fareBatch = new ArrayList<>(300);
            int fareTarget = Math.min(allFlights.size(), 150);

            for (int i = 0; i < fareTarget; i++) {
                Flight flight = allFlights.get(i);

                FareOption saver = new FareOption("SAVER", "Saver", "Basic fare with 7kg cabin baggage and minimal inclusions",
                        BigDecimal.valueOf(1.0), 15, 7, false, false, false, "Non-changeable", false, "Non-refundable");
                saver.setFlight(flight);
                saver.setCabinBaggageDimensions("55x35x25 cm");
                fareBatch.add(saver);

                FareOption standard = new FareOption("STANDARD", "Standard", "Includes complimentary meal, 15kg checked bag, and standard seat selection",
                        BigDecimal.valueOf(1.25), 20, 7, true, true, true, "₹500 change fee", true, "70% refund up to 48h before departure");
                standard.setFlight(flight);
                standard.setCabinBaggageDimensions("55x35x25 cm");
                fareBatch.add(standard);

                FareOption flex = new FareOption("FLEX", "Flex", "Premium fare with full flexibility, free date change, priority boarding & lounge access",
                        BigDecimal.valueOf(1.65), 30, 10, true, true, true, "Free unlimited changes", true, "Full refund up to 24h before departure");
                flex.setFlight(flight);
                flex.setPriorityBoarding(true);
                flex.setCabinBaggageDimensions("55x35x25 cm");
                fareBatch.add(flex);
            }

            fareOptionRepo.saveAll(fareBatch);
            log.info("Seeded fare options for {} flights ({} records)", fareTarget, fareBatch.size());
        }
    }

    private Seat buildSeat(Flight flight, String cabinClass, int row, String col, Random rng) {
        Seat seat = new Seat();
        seat.setFlight(flight);
        seat.setCabinClass(cabinClass);
        seat.setRowNumber(row);
        seat.setColumnLetter(col);
        seat.setSeatNumber(row + col);

        boolean isWin = "A".equals(col) || "F".equals(col);
        boolean isAisle = "C".equals(col) || "D".equals(col);
        boolean isMid = "B".equals(col) || "E".equals(col);
        seat.setWindow(isWin);
        seat.setAisle(isAisle);
        seat.setMiddle(isMid);

        boolean isExit = (row == 12 || row == 13);
        boolean isExtra = (row == 1 || row == 8 || row == 12 || row == 13);
        seat.setEmergencyExit(isExit);
        seat.setExtraLegroom(isExtra);

        int basePrice = switch (cabinClass) {
            case "ECONOMY" -> 350 + rng.nextInt(200);
            case "PREMIUM_ECONOMY" -> 900 + rng.nextInt(400);
            case "BUSINESS" -> 2500 + rng.nextInt(1000);
            case "FIRST" -> 6000 + rng.nextInt(2000);
            default -> 350;
        };
        if (seat.isWindow() || seat.isAisle()) basePrice += 200;
        seat.setPrice(BigDecimal.valueOf(basePrice));

        if (isExtra) {
            seat.setSeatType("EXTRA_LEGROOM");
            seat.setPremiumSurcharge(BigDecimal.valueOf(1200));
        } else if (isExit) {
            seat.setSeatType("EXIT_ROW");
            seat.setPremiumSurcharge(BigDecimal.valueOf(800));
        } else if (row <= 3) {
            seat.setSeatType("PREMIUM");
            seat.setPremiumSurcharge(BigDecimal.valueOf(600));
        } else {
            seat.setSeatType("STANDARD");
            seat.setPremiumSurcharge(BigDecimal.ZERO);
        }

        // Realistic deterministic availability: ~22% occupied
        boolean isOccupied = (rng.nextInt(9) <= 1);
        seat.setAvailable(!isOccupied);

        return seat;
    }
}
