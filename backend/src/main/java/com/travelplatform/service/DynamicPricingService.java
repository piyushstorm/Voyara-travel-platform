package com.travelplatform.service;

import com.travelplatform.dto.pricing.PricingBreakdownResponse;
import com.travelplatform.entity.Flight;
import com.travelplatform.entity.PriceHistory;
import com.travelplatform.entity.Room;
import com.travelplatform.repository.FlightRepository;
import com.travelplatform.repository.PriceHistoryRepository;
import com.travelplatform.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Dynamic pricing engine that recalculates prices based on:
 * 1. Peak-period markup (+20% during holidays/peak dates)
 * 2. Demand multiplier (booked/total capacity ratio)
 * 3. Time-decay (prices increase as departure/check-in approaches)
 *
 * Runs on a schedule, logs every change to price_history table,
 * and pushes updates via WebSocket.
 */
@Service
public class DynamicPricingService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicPricingService.class);

    private final FlightRepository flightRepo;
    private final RoomRepository roomRepo;
    private final PriceHistoryRepository priceHistoryRepo;
    private final WebSocketNotificationService notificationService;

    // Peak/holiday date ranges (month-day format)
    private static final List<int[]> PEAK_PERIODS = List.of(
        new int[]{12, 20, 1, 5},   // Christmas/New Year
        new int[]{3, 15, 3, 31},   // Holi/spring break
        new int[]{5, 1, 5, 15},    // Summer vacation start
        new int[]{10, 15, 11, 15}, // Diwali/festive season
        new int[]{8, 1, 8, 15}     // Independence Day week
    );

    public DynamicPricingService(FlightRepository flightRepo,
                                  RoomRepository roomRepo,
                                  PriceHistoryRepository priceHistoryRepo,
                                  WebSocketNotificationService notificationService) {
        this.flightRepo = flightRepo;
        this.roomRepo = roomRepo;
        this.priceHistoryRepo = priceHistoryRepo;
        this.notificationService = notificationService;
    }

    /** Recalculate prices for all flights every 60 seconds */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateFlightPrices() {
        List<Flight> flights = flightRepo.findAll();
        int updated = 0;

        for (Flight flight : flights) {
            if (flight.getDepartureTime() == null) continue;

            updated += recalculateFlightPrice(flight, "ECONOMY", flight.getEconomyPrice());
            updated += recalculateFlightPrice(flight, "PREMIUM_ECONOMY", flight.getPremiumEconomyPrice());
            updated += recalculateFlightPrice(flight, "BUSINESS", flight.getBusinessPrice());
            updated += recalculateFlightPrice(flight, "FIRST", flight.getFirstClassPrice());
        }

        if (updated > 0) {
            logger.info("Dynamic pricing updated {} price points", updated);
        }
    }

    /** Recalculate hotel room prices every 60 seconds */
    @Scheduled(fixedRate = 65000) // Offset from flight pricing
    @Transactional
    public void updateHotelPrices() {
        List<Room> rooms = roomRepo.findAll();
        int updated = 0;

        for (Room room : rooms) {
            BigDecimal originalPrice = room.getPricePerNight();
            BigDecimal newPrice = calculateHotelPrice(room);

            if (newPrice.compareTo(originalPrice) != 0) {
                PriceHistory history = new PriceHistory(
                    "HOTEL", room.getHotel().getId(), room.getRoomType(),
                    originalPrice, newPrice, determineReason(originalPrice, newPrice),
                    "Demand: " + getDemandDescription(room)
                );
                priceHistoryRepo.save(history);
                if (room.getBasePrice() == null) {
                    room.setBasePrice(originalPrice); // Store original only once
                }
                room.setPricePerNight(newPrice);
                roomRepo.save(room);

                notificationService.sendPriceUpdate(
                    "HOTEL", room.getHotel().getId(), room.getRoomType(),
                    originalPrice, newPrice, determineReason(originalPrice, newPrice)
                );
                updated++;
            }
        }

        if (updated > 0) {
            logger.info("Dynamic pricing updated {} hotel room prices", updated);
        }
    }

    private int recalculateFlightPrice(Flight flight, String cabinClass, BigDecimal currentPrice) {
        BigDecimal basePrice = getBasePrice(flight, cabinClass);
        BigDecimal newPrice = calculateFlightPrice(flight, cabinClass, basePrice);

        if (newPrice.compareTo(currentPrice) != 0) {
            double demandRatio = getDemandRatio(flight, cabinClass);
            double demandMultiplier = 0.85 + (demandRatio * 0.55);
            double peakMultiplier = isPeakPeriod(flight.getDepartureDate()) ? 1.20 : 1.0;
            int total = getTotalSeats(flight, cabinClass);
            int booked = getBookedSeats(flight, cabinClass);
            double availableRatio = total > 0 ? (double)(total - booked) / total : 1.0;
            double inventoryMultiplier = getInventoryMultiplier(availableRatio);

            PriceHistory history = new PriceHistory(
                "FLIGHT", flight.getId(), cabinClass,
                currentPrice, newPrice, determineReason(currentPrice, newPrice),
                "Demand: " + getDemandDescription(flight, cabinClass) +
                ", Time: " + getTimeDecayDescription(flight) +
                ", Inventory: " + getInventoryDescription(availableRatio, total - booked, "seats"),
                basePrice, 
                BigDecimal.valueOf(demandMultiplier).setScale(2, RoundingMode.HALF_UP), 
                BigDecimal.valueOf(peakMultiplier).setScale(2, RoundingMode.HALF_UP), 
                BigDecimal.valueOf(inventoryMultiplier).setScale(2, RoundingMode.HALF_UP)
            );
            priceHistoryRepo.save(history);

            setPrice(flight, cabinClass, newPrice);
            flightRepo.save(flight);

            notificationService.sendPriceUpdate(
                "FLIGHT", flight.getId(), cabinClass,
                currentPrice, newPrice, determineReason(currentPrice, newPrice)
            );
            return 1;
        }
        return 0;
    }

    /** Calculate flight price using base price for class */
    public BigDecimal calculateFlightPrice(Flight flight, String cabinClass) {
        BigDecimal basePrice = getBasePrice(flight, cabinClass);
        return calculateFlightPrice(flight, cabinClass, basePrice);
    }

    /** Calculate flight price: base × peak × demand × inventory × time-decay (with safety clamp) */
    public BigDecimal calculateFlightPrice(Flight flight, String cabinClass, BigDecimal basePrice) {
        // 1. Peak period markup
        double peakMultiplier = isPeakPeriod(flight.getDepartureDate()) ? 1.20 : 1.0;

        // 2. Demand multiplier based on booking ratio
        double demandRatio = getDemandRatio(flight, cabinClass);
        double demandMultiplier = 0.85 + (demandRatio * 0.55);

        // 3. Inventory scarcity factor
        int total = getTotalSeats(flight, cabinClass);
        int booked = getBookedSeats(flight, cabinClass);
        double availableRatio = total > 0 ? (double)(total - booked) / total : 1.0;
        double inventoryMultiplier = getInventoryMultiplier(availableRatio);

        // 4. Time-decay: prices increase as departure approaches
        long hoursUntilDeparture = ChronoUnit.HOURS.between(
            LocalDateTime.now(), flight.getDepartureTime());
        double timeMultiplier = getTimeMultiplier(hoursUntilDeparture);

        // Clamped safety multiplier [0.80, 2.00]
        double rawMultiplier = peakMultiplier * demandMultiplier * inventoryMultiplier * timeMultiplier;
        double clampedMultiplier = Math.max(0.80, Math.min(2.00, rawMultiplier));

        return basePrice.multiply(BigDecimal.valueOf(clampedMultiplier)).setScale(0, RoundingMode.HALF_UP);
    }

    /** Calculate hotel price with demand, weekend, and inventory factors */
    private BigDecimal calculateHotelPrice(Room room, BigDecimal basePrice) {
        // Demand multiplier
        double demandRatio = room.getTotalRooms() > 0
            ? (double)(room.getTotalRooms() - room.getAvailableRooms()) / room.getTotalRooms()
            : 0;
        double demandMultiplier = 0.85 + (demandRatio * 0.55);

        // Weekend surge (Fri-Sat)
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
        double weekendMultiplier = dayOfWeek >= 5 ? 1.10 : 1.0;

        // Inventory factor
        double availableRatio = room.getTotalRooms() > 0 ? (double) room.getAvailableRooms() / room.getTotalRooms() : 1.0;
        double inventoryMultiplier = getInventoryMultiplier(availableRatio);

        double rawMultiplier = demandMultiplier * weekendMultiplier * inventoryMultiplier;
        double clampedMultiplier = Math.max(0.80, Math.min(2.00, rawMultiplier));

        return basePrice.multiply(BigDecimal.valueOf(clampedMultiplier)).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateHotelPrice(Room room) {
        BigDecimal basePrice = room.getBasePrice() != null ? room.getBasePrice() : room.getPricePerNight();
        return calculateHotelPrice(room, basePrice);
    }

    private BigDecimal getBasePrice(Flight flight, String cabinClass) {
        return switch (cabinClass.toUpperCase()) {
            case "ECONOMY" -> flight.getEconomyBasePrice() != null ? flight.getEconomyBasePrice() : flight.getEconomyPrice();
            case "PREMIUM_ECONOMY" -> flight.getPremiumEconomyBasePrice() != null ? flight.getPremiumEconomyBasePrice() : flight.getPremiumEconomyPrice();
            case "BUSINESS" -> flight.getBusinessBasePrice() != null ? flight.getBusinessBasePrice() : flight.getBusinessPrice();
            case "FIRST" -> flight.getFirstClassBasePrice() != null ? flight.getFirstClassBasePrice() : flight.getFirstClassPrice();
            default -> flight.getEconomyPrice();
        };
    }

    private void setPrice(Flight flight, String cabinClass, BigDecimal price) {
        switch (cabinClass.toUpperCase()) {
            case "ECONOMY" -> flight.setEconomyPrice(price);
            case "PREMIUM_ECONOMY" -> flight.setPremiumEconomyPrice(price);
            case "BUSINESS" -> flight.setBusinessPrice(price);
            case "FIRST" -> flight.setFirstClassPrice(price);
        }
    }

    private double getDemandRatio(Flight flight, String cabinClass) {
        int total = getTotalSeats(flight, cabinClass);
        int booked = getBookedSeats(flight, cabinClass);
        return total > 0 ? (double) booked / total : 0;
    }

    private int getTotalSeats(Flight flight, String cabinClass) {
        return switch (cabinClass.toUpperCase()) {
            case "ECONOMY" -> flight.getTotalSeatsEconomy();
            case "PREMIUM_ECONOMY" -> flight.getTotalSeatsPremiumEconomy();
            case "BUSINESS" -> flight.getTotalSeatsBusiness();
            case "FIRST" -> flight.getTotalSeatsFirst();
            default -> 150;
        };
    }

    private int getBookedSeats(Flight flight, String cabinClass) {
        return switch (cabinClass.toUpperCase()) {
            case "ECONOMY" -> flight.getBookedSeatsEconomy();
            case "PREMIUM_ECONOMY" -> flight.getBookedSeatsPremiumEconomy();
            case "BUSINESS" -> flight.getBookedSeatsBusiness();
            case "FIRST" -> flight.getBookedSeatsFirst();
            default -> 0;
        };
    }

    private double getInventoryMultiplier(double availableRatio) {
        if (availableRatio < 0.10) return 1.15;
        if (availableRatio < 0.20) return 1.10;
        if (availableRatio < 0.50) return 1.05;
        return 1.00;
    }

    private String getInventoryDescription(double availableRatio, int remaining, String unit) {
        if (availableRatio < 0.10) return "Very low inventory: only " + remaining + " " + unit + " left! (+15%)";
        if (availableRatio < 0.20) return "Low inventory: " + remaining + " " + unit + " left (+10%)";
        if (availableRatio < 0.50) return "Moderate availability: " + remaining + " " + unit + " left (+5%)";
        return "Ample availability (>50% " + unit + " available)";
    }

    /**
     * Time-decay multiplier:
     * >7 days: 0.90 (early bird discount)
     * 3-7 days: 0.95
     * 1-3 days: 1.00
     * <24 hours: 1.15 (last-minute surge)
     */
    private double getTimeMultiplier(long hoursUntilDeparture) {
        if (hoursUntilDeparture > 168) return 0.90;
        if (hoursUntilDeparture > 72) return 0.95;
        if (hoursUntilDeparture > 24) return 1.00;
        if (hoursUntilDeparture > 0) return 1.15;
        return 1.00; // Past departure, no more changes
    }

    private boolean isPeakPeriod(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        for (int[] period : PEAK_PERIODS) {
            int startMonth = period[0], startDay = period[1];
            int endMonth = period[2], endDay = period[3];
            if (startMonth > endMonth || (startMonth == endMonth && startDay > endDay)) {
                // Wraps year boundary (e.g., Dec 20 - Jan 5)
                if (month > startMonth || (month == startMonth && day >= startDay)) return true;
                if (month < endMonth || (month == endMonth && day <= endDay)) return true;
            } else {
                if ((month > startMonth || (month == startMonth && day >= startDay)) &&
                    (month < endMonth || (month == endMonth && day <= endDay))) return true;
            }
        }
        return false;
    }

    private String determineReason(BigDecimal oldPrice, BigDecimal newPrice) {
        return newPrice.compareTo(oldPrice) > 0 ? "PRICE_INCREASE" : "PRICE_DECREASE";
    }

    private String getDemandDescription(Flight flight, String cabinClass) {
        double ratio = getDemandRatio(flight, cabinClass);
        return String.format("%.0f%% booked", ratio * 100);
    }

    private String getDemandDescription(Room room) {
        double ratio = room.getTotalRooms() > 0
            ? (double)(room.getTotalRooms() - room.getAvailableRooms()) / room.getTotalRooms() : 0;
        return String.format("%.0f%% occupied", ratio * 100);
    }

    private String getTimeDecayDescription(Flight flight) {
        long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), flight.getDepartureTime());
        if (hours > 168) return "Early bird";
        if (hours > 72) return "Advance booking";
        if (hours > 24) return "Normal";
        if (hours > 0) return "Last-minute surge";
        return "Departed";
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPricingFactors(Long flightId, String cabinClass) {
        Flight flight = flightRepo.findById(flightId).orElse(null);
        if (flight == null || flight.getDepartureTime() == null) {
            return Map.of("error", "Flight not found or invalid");
        }

        BigDecimal basePrice = getBasePrice(flight, cabinClass);
        BigDecimal currentPrice = flight.getPriceForClass(cabinClass);

        boolean peakPeriod = isPeakPeriod(flight.getDepartureDate());
        double peakMultiplier = peakPeriod ? 1.20 : 1.0;

        double demandRatio = getDemandRatio(flight, cabinClass);
        double demandMultiplier = 0.85 + (demandRatio * 0.55);

        int total = getTotalSeats(flight, cabinClass);
        int booked = getBookedSeats(flight, cabinClass);
        double availableRatio = total > 0 ? (double)(total - booked) / total : 1.0;
        double inventoryMultiplier = getInventoryMultiplier(availableRatio);

        long hoursUntilDeparture = ChronoUnit.HOURS.between(LocalDateTime.now(), flight.getDepartureTime());
        double timeMultiplier = getTimeMultiplier(hoursUntilDeparture);

        double rawMultiplier = peakMultiplier * demandMultiplier * inventoryMultiplier * timeMultiplier;
        double clampedMultiplier = Math.max(0.80, Math.min(2.00, rawMultiplier));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("basePrice", basePrice);
        map.put("currentPrice", currentPrice);
        map.put("isPeakPeriod", peakPeriod);
        map.put("peakMultiplier", peakMultiplier);
        map.put("demandRatio", demandRatio);
        map.put("demandMultiplier", demandMultiplier);
        map.put("demandDescription", getDemandDescription(flight, cabinClass));
        map.put("inventoryMultiplier", inventoryMultiplier);
        map.put("inventoryDescription", getInventoryDescription(availableRatio, total - booked, "seats"));
        map.put("hoursUntilDeparture", hoursUntilDeparture);
        map.put("timeMultiplier", timeMultiplier);
        map.put("timeDescription", getTimeDecayDescription(flight));
        map.put("totalMultiplier", clampedMultiplier);
        map.put("safetyCapApplied", rawMultiplier != clampedMultiplier);
        return map;
    }

    @Transactional(readOnly = true)
    public PricingBreakdownResponse getDetailedPricingBreakdown(String entityType, Long entityId, String cabinClass) {
        if ("HOTEL".equalsIgnoreCase(entityType)) {
            Room room = roomRepo.findById(entityId).orElse(null);
            if (room == null) return null;
            BigDecimal base = room.getBasePrice() != null ? room.getBasePrice() : room.getPricePerNight();
            BigDecimal current = room.getPricePerNight();

            double demandRatio = room.getTotalRooms() > 0
                ? (double)(room.getTotalRooms() - room.getAvailableRooms()) / room.getTotalRooms() : 0;
            double demandMultiplier = 0.85 + (demandRatio * 0.55);

            int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
            boolean isWeekend = dayOfWeek >= 5;
            double seasonalMultiplier = isWeekend ? 1.10 : 1.0;

            double availableRatio = room.getTotalRooms() > 0 ? (double) room.getAvailableRooms() / room.getTotalRooms() : 1.0;
            double inventoryMultiplier = getInventoryMultiplier(availableRatio);

            double rawMultiplier = demandMultiplier * seasonalMultiplier * inventoryMultiplier;
            double clampedMultiplier = Math.max(0.80, Math.min(2.00, rawMultiplier));

            PricingBreakdownResponse res = new PricingBreakdownResponse();
            res.setBasePrice(base);
            res.setCurrentPrice(current);
            res.setDemandMultiplier(demandMultiplier);
            res.setDemandAdjustment(base.multiply(BigDecimal.valueOf(demandMultiplier - 1.0)).setScale(0, RoundingMode.HALF_UP));
            res.setDemandDescription(getDemandDescription(room));
            res.setSeasonalMultiplier(seasonalMultiplier);
            res.setSeasonalAdjustment(base.multiply(BigDecimal.valueOf(seasonalMultiplier - 1.0)).setScale(0, RoundingMode.HALF_UP));
            res.setSeasonalDescription(isWeekend ? "Weekend surge (+10%)" : "Standard weekday rate");
            res.setInventoryMultiplier(inventoryMultiplier);
            res.setInventoryAdjustment(base.multiply(BigDecimal.valueOf(inventoryMultiplier - 1.0)).setScale(0, RoundingMode.HALF_UP));
            res.setInventoryDescription(getInventoryDescription(availableRatio, room.getAvailableRooms(), "rooms"));
            res.setTotalMultiplier(clampedMultiplier);
            res.setSafetyCapApplied(rawMultiplier != clampedMultiplier);
            res.setPeakPeriod(isWeekend);
            return res;
        }

        // Default FLIGHT
        Flight flight = flightRepo.findById(entityId).orElse(null);
        if (flight == null || flight.getDepartureTime() == null) {
            return null;
        }
        String cls = (cabinClass == null || cabinClass.isBlank()) ? "ECONOMY" : cabinClass.toUpperCase();
        BigDecimal base = getBasePrice(flight, cls);
        BigDecimal current = flight.getPriceForClass(cls);

        boolean peakPeriod = isPeakPeriod(flight.getDepartureDate());
        double peakMultiplier = peakPeriod ? 1.20 : 1.0;

        double demandRatio = getDemandRatio(flight, cls);
        double demandMultiplier = 0.85 + (demandRatio * 0.55);

        int total = getTotalSeats(flight, cls);
        int booked = getBookedSeats(flight, cls);
        double availableRatio = total > 0 ? (double)(total - booked) / total : 1.0;
        double inventoryMultiplier = getInventoryMultiplier(availableRatio);

        long hoursUntilDeparture = ChronoUnit.HOURS.between(LocalDateTime.now(), flight.getDepartureTime());
        double timeMultiplier = getTimeMultiplier(hoursUntilDeparture);

        double rawMultiplier = peakMultiplier * demandMultiplier * inventoryMultiplier * timeMultiplier;
        double clampedMultiplier = Math.max(0.80, Math.min(2.00, rawMultiplier));

        PricingBreakdownResponse res = new PricingBreakdownResponse();
        res.setBasePrice(base);
        res.setCurrentPrice(current);
        res.setDemandMultiplier(demandMultiplier);
        res.setDemandAdjustment(base.multiply(BigDecimal.valueOf(demandMultiplier - 1.0)).setScale(0, RoundingMode.HALF_UP));
        res.setDemandDescription(getDemandDescription(flight, cls));
        res.setSeasonalMultiplier(peakMultiplier);
        res.setSeasonalAdjustment(base.multiply(BigDecimal.valueOf(peakMultiplier - 1.0)).setScale(0, RoundingMode.HALF_UP));
        res.setSeasonalDescription(peakPeriod ? "Peak holiday season (+20%)" : "Regular season pricing");
        res.setInventoryMultiplier(inventoryMultiplier);
        res.setInventoryAdjustment(base.multiply(BigDecimal.valueOf(inventoryMultiplier - 1.0)).setScale(0, RoundingMode.HALF_UP));
        res.setInventoryDescription(getInventoryDescription(availableRatio, total - booked, "seats"));
        res.setTimeMultiplier(timeMultiplier);
        res.setTimeAdjustment(base.multiply(BigDecimal.valueOf(timeMultiplier - 1.0)).setScale(0, RoundingMode.HALF_UP));
        res.setTimeDescription(getTimeDecayDescription(flight));
        res.setTotalMultiplier(clampedMultiplier);
        res.setSafetyCapApplied(rawMultiplier != clampedMultiplier);
        res.setPeakPeriod(peakPeriod);
        res.setHoursUntilDeparture(hoursUntilDeparture);
        return res;
    }
}
