package com.travelplatform.config;

import com.travelplatform.config.seed.*;
import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.*;

/**
 * Main seed orchestrator for Voyara / TravelPlatform.
 * Coordinates modular, deterministic, relational data seeding across all travel verticals.
 * Enforces strict production safety and idempotent execution.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${seed.data.enabled:false}")
    private boolean seedDataEnabled;

    @Bean
    CommandLineRunner seedData(Environment env,
                               AddOnRepository addOnRepo,
                               CancellationPolicyRepository cancellationPolicyRepo,
                               AirportAndAirlineSeeder airportAirlineSeeder,
                               FlightAndSeatSeeder flightSeatSeeder,
                               FlightStatusAndTrackingSeeder flightStatusTrackingSeeder,
                               HotelAndRoomSeeder hotelRoomSeeder,
                               GroundTransportSeeder groundTransportSeeder,
                               HolidayPackageSeeder holidayPackageSeeder,
                               UserAndBookingSeeder userBookingSeeder,
                               PricingAndFreezeSeeder pricingFreezeSeeder,
                               ReviewAndModerationSeeder reviewModerationSeeder,
                               SocialAndNotificationSeeder socialNotificationSeeder,
                               DestinationAndRecommendationSeeder destRecSeeder,
                               DestinationRepository destinationRepo,
                               RecommendationRepository recRepo,
                               AirportRepository airportRepo,
                               AirlineRepository airlineRepo,
                               FlightRepository flightRepo,
                               SeatRepository seatRepo,
                               HotelRepository hotelRepo,
                               RoomRepository roomRepo,
                               TrainRepository trainRepo,
                               BusRepository busRepo,
                               CabRepository cabRepo,
                               HolidayPackageRepository holidayRepo,
                               ReviewRepository reviewRepo,
                               BookingRepository bookingRepo,
                               PriceHistoryRepository priceHistoryRepo,
                               PriceFreezeRepository priceFreezeRepo,
                               NotificationRepository notificationRepo,
                               TrackedFlightRepository trackedFlightRepo) {
        return args -> {
            // =========================================================================
            // 1. SEED DATA ENABLEMENT & PRODUCTION SAFETY GUARD
            // =========================================================================
            if (!seedDataEnabled) {
                log.info("SEED_DATA_ENABLED=false (seed.data.enabled=false) — skipping mock/catalog data seeding.");
                return;
            }

            boolean isProdProfile = Arrays.stream(env.getActiveProfiles())
                    .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));

            if (isProdProfile) {
                log.warn("SEED_DATA_ENABLED=true in PRODUCTION environment! Running idempotent mock/catalog data seeder pipeline on production database.");
            } else {
                log.info("SEED_DATA_ENABLED=true — Starting mock/catalog data seeder pipeline.");
            }

            long startTime = System.currentTimeMillis();
            log.info("================================================================================");
            log.info("STARTING DETERMINISTIC MOCK DATA SEED PIPELINE FOR VOYARA (SEED_DATA_ENABLED=true)");
            log.info("================================================================================");

            // 1. Static Catalog: Add-Ons & Policies (Always idempotent)
            seedAddOns(addOnRepo);
            seedCancellationPolicies(cancellationPolicyRepo);

            // 2. Users & Roles (ADMIN, USER, frequent traveler, reviewer, moderator, trip organizers)
            Map<String, User> userMap = userBookingSeeder.seedTestUsers();

            // 3. Geographic & Aviation Backbone: Airports & Airlines
            Map<String, Airport> airportMap = airportAirlineSeeder.seedAirports();
            List<Airline> airlines = airportAirlineSeeder.seedAirlines();

            // 4. Flight Inventory & Dynamic Schedules
            List<Flight> flights = flightSeatSeeder.seedFlights(airportMap, airlines);

            // 5. Flight Seats & Fare Options
            flightSeatSeeder.seedSeatsAndFareOptions(flights);

            // 6. Flight Live Statuses (All simulator scenarios) & Flight Tracking
            flightStatusTrackingSeeder.seedFlightStatuses(flights);
            flightStatusTrackingSeeder.seedTrackedFlights(flights, userMap.get("user@travel.com"), userMap.get("traveler@travel.com"));

            // 7. Hotel Inventory & Room Types across 35+ destinations
            List<Hotel> hotels = hotelRoomSeeder.seedHotelsAndRooms();

            // 8. Ground Transport: Trains, Buses, Cabs
            groundTransportSeeder.seedTrains();
            groundTransportSeeder.seedBuses();
            groundTransportSeeder.seedCabs();

            // 9. Holiday Packages with Day-by-Day Itineraries
            holidayPackageSeeder.seedHolidayPackages();

            // 10. Bookings, Payments, Cancellations & Refunds
            List<Booking> bookings = userBookingSeeder.seedControlledBookings(userMap, flights, hotels);

            // 11. Dynamic Pricing History & Price Freeze Scenarios
            pricingFreezeSeeder.seedPriceHistoryAndFreezes(flights, hotels, userMap, bookings);

            // 12. Reviews, Ratings, Photos, Replies, Helpful Votes & Moderation
            reviewModerationSeeder.seedReviewsAndSocialUgc(hotels, flights, userMap, bookings);

            // 13. Notifications, Travel Preferences, Group Trips & Shared Expense Splits
            socialNotificationSeeder.seedSocialAndNotifications(userMap, bookings);

            // 14. Destination Taxonomy & Personalized Recommendations Engine
            destRecSeeder.seedDestinations();
            destRecSeeder.seedUserInteractionsAndInitialRecs(userMap);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("================================================================================");
            log.info("VOYARA SEED PIPELINE COMPLETED IN {} ms (SEED_DATA_ENABLED=true)", elapsed);
            log.info("--------------------------------------------------------------------------------");
            log.info("Major Entity Record Counts in Database (Existing & Seeded):");
            log.info("  Airports:         {}", airportRepo.count());
            log.info("  Airlines:         {}", airlineRepo.count());
            log.info("  Flights:          {}", flightRepo.count());
            log.info("  Flight Seats:     {}", seatRepo.count());
            log.info("  Hotels:           {}", hotelRepo.count());
            log.info("  Rooms:            {}", roomRepo.count());
            log.info("  Destinations:     {}", destinationRepo.count());
            log.info("  Recommendations:  {}", recRepo.count());
            log.info("  Trains:           {}", trainRepo.count());
            log.info("  Buses:            {}", busRepo.count());
            log.info("  Cabs:             {}", cabRepo.count());
            log.info("  Holiday Packages: {}", holidayRepo.count());
            log.info("  Reviews:          {}", reviewRepo.count());
            log.info("  Bookings:         {}", bookingRepo.count());
            log.info("  Price History:    {}", priceHistoryRepo.count());
            log.info("  Price Freezes:    {}", priceFreezeRepo.count());
            log.info("  Notifications:    {}", notificationRepo.count());
            log.info("  Tracked Flights:  {}", trackedFlightRepo.count());
            log.info("================================================================================");
        };
    }

    private void seedAddOns(AddOnRepository addOnRepo) {
        if (addOnRepo.count() > 0) return;
        addOnRepo.save(new AddOn("Extra Baggage - 5kg", "Additional 5kg checked baggage allowance", "BAGGAGE", BigDecimal.valueOf(750), null, "ALL"));
        addOnRepo.save(new AddOn("Extra Baggage - 10kg", "Additional 10kg checked baggage allowance", "BAGGAGE", BigDecimal.valueOf(1400), null, "ALL"));
        addOnRepo.save(new AddOn("Extra Baggage - 15kg", "Additional 15kg checked baggage allowance", "BAGGAGE", BigDecimal.valueOf(2000), null, "ALL"));
        addOnRepo.save(new AddOn("Pre-book Meal - Veg", "Indian vegetarian thali with dessert", "MEAL", BigDecimal.valueOf(350), null, "ALL"));
        addOnRepo.save(new AddOn("Pre-book Meal - Non-Veg", "Chicken tikka with rice and naan", "MEAL", BigDecimal.valueOf(450), null, "ALL"));
        addOnRepo.save(new AddOn("Pre-book Meal - Vegan", "Plant-based healthy meal option", "MEAL", BigDecimal.valueOf(400), null, "ALL"));
        addOnRepo.save(new AddOn("Preferred Seat Selection", "Choose your preferred seat", "SEAT", BigDecimal.valueOf(250), null, "ALL"));
        addOnRepo.save(new AddOn("Priority Boarding", "Board the aircraft before general boarding", "SEAT", BigDecimal.valueOf(300), null, "ALL"));
        addOnRepo.save(new AddOn("Travel Protection - Basic", "Trip protection up to Rs.25,000", "PROTECTION", BigDecimal.valueOf(199), null, "ALL"));
        addOnRepo.save(new AddOn("Travel Protection - Premium", "Comprehensive protection up to Rs.1,00,000", "PROTECTION", BigDecimal.valueOf(599), null, "ALL"));
        addOnRepo.save(new AddOn("Lounge Access - Domestic", "Access to domestic lounge", "SEAT", BigDecimal.valueOf(800), null, "ALL"));
        log.info("Seeded 11 add-ons");
    }

    private void seedCancellationPolicies(CancellationPolicyRepository cancellationPolicyRepo) {
        if (cancellationPolicyRepo.count() > 0) return;
        // Flight policies
        cancellationPolicyRepo.save(new CancellationPolicy("FLIGHT", "More than 7 days", 168, 99999, BigDecimal.valueOf(100), BigDecimal.ZERO));
        cancellationPolicyRepo.save(new CancellationPolicy("FLIGHT", "3-7 days", 72, 168, BigDecimal.valueOf(70), BigDecimal.valueOf(200)));
        cancellationPolicyRepo.save(new CancellationPolicy("FLIGHT", "24-72 hours", 24, 72, BigDecimal.valueOf(50), BigDecimal.valueOf(500)));
        cancellationPolicyRepo.save(new CancellationPolicy("FLIGHT", "12-24 hours", 12, 24, BigDecimal.valueOf(25), BigDecimal.valueOf(1000)));
        cancellationPolicyRepo.save(new CancellationPolicy("FLIGHT", "Less than 12 hours", 0, 12, BigDecimal.ZERO, BigDecimal.ZERO));

        // Hotel policies
        cancellationPolicyRepo.save(new CancellationPolicy("HOTEL", "More than 7 days", 168, 99999, BigDecimal.valueOf(100), BigDecimal.ZERO));
        cancellationPolicyRepo.save(new CancellationPolicy("HOTEL", "3-7 days", 72, 168, BigDecimal.valueOf(80), BigDecimal.valueOf(500)));
        cancellationPolicyRepo.save(new CancellationPolicy("HOTEL", "24-72 hours", 24, 72, BigDecimal.valueOf(50), BigDecimal.valueOf(1000)));
        cancellationPolicyRepo.save(new CancellationPolicy("HOTEL", "Less than 24 hours", 0, 24, BigDecimal.ZERO, BigDecimal.ZERO));

        // Holiday policies
        cancellationPolicyRepo.save(new CancellationPolicy("HOLIDAY", "More than 7 days", 168, 99999, BigDecimal.valueOf(100), BigDecimal.ZERO));
        cancellationPolicyRepo.save(new CancellationPolicy("HOLIDAY", "48-168 hours", 48, 168, BigDecimal.valueOf(80), BigDecimal.valueOf(1000)));
        cancellationPolicyRepo.save(new CancellationPolicy("HOLIDAY", "24-48 hours", 24, 48, BigDecimal.valueOf(50), BigDecimal.valueOf(2000)));
        cancellationPolicyRepo.save(new CancellationPolicy("HOLIDAY", "Less than 24 hours", 0, 24, BigDecimal.ZERO, BigDecimal.ZERO));

        // Train policies
        cancellationPolicyRepo.save(new CancellationPolicy("TRAIN", "More than 48 hours", 48, 99999, BigDecimal.valueOf(100), BigDecimal.valueOf(100)));
        cancellationPolicyRepo.save(new CancellationPolicy("TRAIN", "24-48 hours", 24, 48, BigDecimal.valueOf(75), BigDecimal.valueOf(200)));
        cancellationPolicyRepo.save(new CancellationPolicy("TRAIN", "4-24 hours", 4, 24, BigDecimal.valueOf(50), BigDecimal.valueOf(300)));
        cancellationPolicyRepo.save(new CancellationPolicy("TRAIN", "Less than 4 hours", 0, 4, BigDecimal.ZERO, BigDecimal.ZERO));

        log.info("Seeded cancellation policies for Flight, Hotel, Holiday, and Train.");
    }

    /**
     * Always runs: ensures admin and user passwords are valid even when DB already exists.
     * Fixes cases where passwords were encoded with a different encoder version.
     */
    @Bean
    CommandLineRunner ensureAdminPassword(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            userRepo.findByEmail("admin@travel.com").ifPresent(admin -> {
                if (!passwordEncoder.matches("admin123", admin.getPassword())) {
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setRole(Role.ADMIN);
                    userRepo.save(admin);
                    log.info("Reset admin password for admin@travel.com");
                }
            });
            userRepo.findByEmail("user@travel.com").ifPresent(user -> {
                if (!passwordEncoder.matches("user123", user.getPassword())) {
                    user.setPassword(passwordEncoder.encode("user123"));
                    userRepo.save(user);
                    log.info("Reset user password for user@travel.com");
                }
            });
        };
    }
}
