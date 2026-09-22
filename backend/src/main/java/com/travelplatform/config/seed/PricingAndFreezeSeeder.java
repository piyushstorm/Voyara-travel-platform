package com.travelplatform.config.seed;

import com.travelplatform.entity.*;
import com.travelplatform.repository.PriceFreezeRepository;
import com.travelplatform.repository.PriceHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class PricingAndFreezeSeeder {

    private static final Logger log = LoggerFactory.getLogger(PricingAndFreezeSeeder.class);

    private final PriceHistoryRepository priceHistoryRepo;
    private final PriceFreezeRepository priceFreezeRepo;

    public PricingAndFreezeSeeder(PriceHistoryRepository priceHistoryRepo, PriceFreezeRepository priceFreezeRepo) {
        this.priceHistoryRepo = priceHistoryRepo;
        this.priceFreezeRepo = priceFreezeRepo;
    }

    @Transactional
    public void seedPriceHistoryAndFreezes(List<Flight> flights, List<Hotel> hotels,
                                          Map<String, User> users, List<Booking> bookings) {
        long currentHistoryCount = priceHistoryRepo.count();
        if (currentHistoryCount < 200 && !flights.isEmpty() && !hotels.isEmpty()) {
            log.info("Generating realistic historical price trajectories for inventory items...");
            List<PriceHistory> historyBatch = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            // Historical price trajectories for top 50 flights
            int flightLimit = Math.min(flights.size(), 60);
            for (int i = 0; i < flightLimit; i++) {
                Flight flight = flights.get(i);
                BigDecimal base = flight.getEconomyBasePrice() != null ? flight.getEconomyBasePrice() : flight.getEconomyPrice();

                // 7 days ago
                BigDecimal p7 = base.multiply(BigDecimal.valueOf(0.92)).setScale(2, java.math.RoundingMode.HALF_UP);
                historyBatch.add(new PriceHistory("FLIGHT", flight.getId(), "ECONOMY", base, p7, "EARLY_BIRD_DISCOUNT",
                        "Early bird booking window open", base, BigDecimal.valueOf(0.90), BigDecimal.valueOf(1.0), BigDecimal.valueOf(1.0)));

                // 5 days ago
                BigDecimal p5 = base.multiply(BigDecimal.valueOf(0.98)).setScale(2, java.math.RoundingMode.HALF_UP);
                historyBatch.add(new PriceHistory("FLIGHT", flight.getId(), "ECONOMY", p7, p5, "DEMAND_MULTIPLIER",
                        "Booking velocity increased +15%", base, BigDecimal.valueOf(1.05), BigDecimal.valueOf(1.0), BigDecimal.valueOf(1.0)));

                // 3 days ago
                BigDecimal p3 = base.multiply(BigDecimal.valueOf(1.08)).setScale(2, java.math.RoundingMode.HALF_UP);
                historyBatch.add(new PriceHistory("FLIGHT", flight.getId(), "ECONOMY", p5, p3, "PEAK_MARKUP",
                        "Weekend corridor demand surge", base, BigDecimal.valueOf(1.10), BigDecimal.valueOf(1.15), BigDecimal.valueOf(1.0)));

                // 1 day ago
                BigDecimal p1 = base.multiply(BigDecimal.valueOf(1.15)).setScale(2, java.math.RoundingMode.HALF_UP);
                historyBatch.add(new PriceHistory("FLIGHT", flight.getId(), "ECONOMY", p3, p1, "INVENTORY_SURGE",
                        "Remaining capacity dropped below 20%", base, BigDecimal.valueOf(1.15), BigDecimal.valueOf(1.15), BigDecimal.valueOf(1.10)));
            }

            // Historical price trajectories for top 40 hotels
            int hotelLimit = Math.min(hotels.size(), 40);
            for (int i = 0; i < hotelLimit; i++) {
                Hotel hotel = hotels.get(i);
                BigDecimal base = hotel.getStartingPrice();

                BigDecimal p5 = base.multiply(BigDecimal.valueOf(0.95)).setScale(2, java.math.RoundingMode.HALF_UP);
                historyBatch.add(new PriceHistory("HOTEL", hotel.getId(), "STANDARD", base, p5, "OFF_PEAK_ADJUSTMENT",
                        "Midweek occupancy promotion", base, BigDecimal.valueOf(0.95), BigDecimal.valueOf(1.0), BigDecimal.valueOf(1.0)));

                BigDecimal p3 = base.multiply(BigDecimal.valueOf(1.05)).setScale(2, java.math.RoundingMode.HALF_UP);
                historyBatch.add(new PriceHistory("HOTEL", hotel.getId(), "STANDARD", p5, p3, "DEMAND_MULTIPLIER",
                        "Weekend leisure demand spike", base, BigDecimal.valueOf(1.10), BigDecimal.valueOf(1.05), BigDecimal.valueOf(1.0)));

                BigDecimal p1 = base.multiply(BigDecimal.valueOf(1.18)).setScale(2, java.math.RoundingMode.HALF_UP);
                historyBatch.add(new PriceHistory("HOTEL", hotel.getId(), "STANDARD", p3, p1, "PEAK_MARKUP",
                        "High tourist season booking influx", base, BigDecimal.valueOf(1.15), BigDecimal.valueOf(1.20), BigDecimal.valueOf(1.05)));
            }

            priceHistoryRepo.saveAll(historyBatch);
            log.info("Total price history records persisted: {}", priceHistoryRepo.count());
        }

        // Price Freezes
        if (priceFreezeRepo.count() < 4 && !flights.isEmpty()) {
            log.info("Seeding price freeze test scenarios (ACTIVE, EXPIRED, USED, CANCELLED)...");
            User user = users.get("user@travel.com");
            User traveler = users.get("traveler@travel.com");
            LocalDateTime now = LocalDateTime.now();

            if (user != null && traveler != null) {
                List<PriceFreeze> freezeList = new ArrayList<>();

                // 1. ACTIVE Freeze for user@travel.com (valid for next 2 hours)
                PriceFreeze fActive = new PriceFreeze();
                fActive.setUser(user);
                fActive.setEntityType("FLIGHT");
                fActive.setEntityId(flights.get(0).getId());
                fActive.setCabinClass("ECONOMY");
                fActive.setFrozenPrice(flights.get(0).getEconomyPrice());
                fActive.setFreezeFee(BigDecimal.valueOf(199.00));
                fActive.setExpiresAt(now.plusHours(2));
                fActive.setStatus("ACTIVE");
                freezeList.add(fActive);

                // 2. EXPIRED Freeze for user@travel.com (expired yesterday)
                PriceFreeze fExpired = new PriceFreeze();
                fExpired.setUser(user);
                fExpired.setEntityType("FLIGHT");
                fExpired.setEntityId(flights.get(1).getId());
                fExpired.setCabinClass("ECONOMY");
                fExpired.setFrozenPrice(flights.get(1).getEconomyPrice().subtract(BigDecimal.valueOf(400)));
                fExpired.setFreezeFee(BigDecimal.valueOf(199.00));
                fExpired.setExpiresAt(now.minusDays(1));
                fExpired.setStatus("EXPIRED");
                freezeList.add(fExpired);

                // 3. USED Freeze for traveler@travel.com (applied to booking)
                Booking usedBooking = !bookings.isEmpty() ? bookings.get(0) : null;
                PriceFreeze fUsed = new PriceFreeze();
                fUsed.setUser(traveler);
                fUsed.setEntityType("FLIGHT");
                fUsed.setEntityId(flights.get(0).getId());
                fUsed.setCabinClass("ECONOMY");
                fUsed.setFrozenPrice(flights.get(0).getEconomyPrice());
                fUsed.setFreezeFee(BigDecimal.valueOf(199.00));
                fUsed.setExpiresAt(now.minusHours(3));
                fUsed.setStatus("USED");
                fUsed.setBooking(usedBooking);
                freezeList.add(fUsed);

                // 4. CANCELLED Freeze for traveler@travel.com
                PriceFreeze fCancelled = new PriceFreeze();
                fCancelled.setUser(traveler);
                fCancelled.setEntityType("HOTEL");
                fCancelled.setEntityId(hotels.get(0).getId());
                fCancelled.setFrozenPrice(hotels.get(0).getStartingPrice());
                fCancelled.setFreezeFee(BigDecimal.valueOf(299.00));
                fCancelled.setExpiresAt(now.minusDays(2));
                fCancelled.setStatus("CANCELLED");
                freezeList.add(fCancelled);

                priceFreezeRepo.saveAll(freezeList);
                log.info("Seeded {} price freeze test scenarios.", freezeList.size());
            }
        }
    }
}
