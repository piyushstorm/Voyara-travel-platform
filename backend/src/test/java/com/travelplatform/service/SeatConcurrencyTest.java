package com.travelplatform.service;

import com.travelplatform.entity.Flight;
import com.travelplatform.entity.Seat;
import com.travelplatform.entity.SeatHold;
import com.travelplatform.entity.User;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.repository.FlightRepository;
import com.travelplatform.repository.SeatHoldRepository;
import com.travelplatform.repository.SeatRepository;
import com.travelplatform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class SeatConcurrencyTest {

    @Autowired private SeatSelectionService seatSelectionService;
    @Autowired private SeatRepository seatRepo;
    @Autowired private SeatHoldRepository holdRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private FlightRepository flightRepo;
    @Autowired private com.travelplatform.repository.AirportRepository airportRepo;
    @Autowired private com.travelplatform.repository.AirlineRepository airlineRepo;

    private User userA;
    private User userB;
    private List<User> testUsers;
    private Seat singleSeat;

    @BeforeEach
    void setup() {
        holdRepo.deleteAll();

        userA = userRepo.findByEmail("concurrency_a@voyara.com").orElseGet(() -> {
            User u = new User("User A", "concurrency_a@voyara.com", "Password123!");
            return userRepo.save(u);
        });

        userB = userRepo.findByEmail("concurrency_b@voyara.com").orElseGet(() -> {
            User u = new User("User B", "concurrency_b@voyara.com", "Password123!");
            return userRepo.save(u);
        });

        testUsers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final String email = "concurrency_user_" + i + "@voyara.com";
            User u = userRepo.findByEmail(email).orElseGet(() -> {
                User nu = new User("Conc User " + email, email, "Password123!");
                return userRepo.save(nu);
            });
            testUsers.add(u);
        }

        // Find or create a test seat
        List<Seat> seats = seatRepo.findAll();
        if (!seats.isEmpty()) {
            singleSeat = seats.get(0);
        } else {
            Flight flight = flightRepo.findAll().stream().findFirst().orElseGet(() -> {
                com.travelplatform.entity.Airport origin = airportRepo.findByCode("DEL").orElseGet(() ->
                        airportRepo.save(new com.travelplatform.entity.Airport("DEL", "Indira Gandhi International", "Delhi", "India", 28.5562, 77.1000)));
                com.travelplatform.entity.Airport dest = airportRepo.findByCode("BOM").orElseGet(() ->
                        airportRepo.save(new com.travelplatform.entity.Airport("BOM", "Chhatrapati Shivaji International", "Mumbai", "India", 19.0896, 72.8656)));
                com.travelplatform.entity.Airline airline = airlineRepo.findByCode("AI").orElseGet(() ->
                        airlineRepo.save(new com.travelplatform.entity.Airline("AI", "Air India", "https://example.com/ai.png", 4.5)));

                Flight f = new Flight();
                f.setFlightNumber("AI-CONC-99");
                f.setAirline(airline);
                f.setOrigin(origin);
                f.setDestination(dest);
                f.setOriginCode("DEL");
                f.setDestinationCode("BOM");
                f.setDepartureTime(LocalDateTime.now().plusDays(5));
                f.setArrivalTime(LocalDateTime.now().plusDays(5).plusHours(2));
                f.setDepartureDate(LocalDateTime.now().plusDays(5).toLocalDate());
                f.setEconomyPrice(new BigDecimal("5000.00"));
                f.setPremiumEconomyPrice(new BigDecimal("7500.00"));
                f.setBusinessPrice(new BigDecimal("12000.00"));
                f.setFirstClassPrice(new BigDecimal("20000.00"));
                f.setTotalSeatsEconomy(100);
                f.setDurationMinutes(120);
                return flightRepo.save(f);
            });
            singleSeat = new Seat();
            singleSeat.setFlight(flight);
            singleSeat.setSeatNumber("12A");
            singleSeat.setCabinClass("ECONOMY");
            singleSeat.setRowNumber(12);
            singleSeat.setColumnLetter("A");
            singleSeat.setWindow(true);
            singleSeat.setPrice(BigDecimal.valueOf(500));
            singleSeat.setPremiumSurcharge(BigDecimal.valueOf(500));
            singleSeat = seatRepo.save(singleSeat);
        }

        singleSeat.setAvailable(true);
        singleSeat.setHeldByUserId(null);
        singleSeat.setHeldUntil(null);
        seatRepo.save(singleSeat);
    }

    @Test
    @DisplayName("High Concurrency: Exactly 1 out of 10 users obtains seat hold")
    void testConcurrentHoldOnSameSeat() throws Exception {
        int threads = testUsers.size();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        for (User user : testUsers) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    seatSelectionService.holdSeat(singleSeat.getId(), user.getId());
                    successes.incrementAndGet();
                } catch (BadRequestException e) {
                    failures.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire all threads simultaneously
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrency test should finish within 10s");
        assertEquals(1, successes.get(), "Exactly ONE user should successfully acquire the hold");
        assertEquals(threads - 1, failures.get(), "All other users should fail with hold conflict");

        // Verify database state
        Seat freshSeat = seatRepo.findById(singleSeat.getId()).orElseThrow();
        assertTrue(freshSeat.isHeld(), "Seat must be in HELD status");
        assertNotNull(freshSeat.getHeldByUserId(), "Seat must have heldByUserId");
    }

    @Test
    @DisplayName("Idempotent Hold: Re-holding same seat by same user succeeds and refreshes expiry")
    void testIdempotentHoldBySameUser() {
        SeatHold firstHold = seatSelectionService.holdSeat(singleSeat.getId(), userA.getId());
        assertNotNull(firstHold);

        // Same user holds the same seat again
        SeatHold secondHold = seatSelectionService.holdSeat(singleSeat.getId(), userA.getId());
        assertNotNull(secondHold);
        assertEquals(firstHold.getId(), secondHold.getId(), "Should reuse existing active hold record");
    }

    @Test
    @DisplayName("Cannot hold a booked seat")
    void testCannotHoldBookedSeat() {
        Seat fresh = seatRepo.findById(singleSeat.getId()).orElseThrow();
        fresh.setAvailable(false);
        seatRepo.save(fresh);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                seatSelectionService.holdSeat(singleSeat.getId(), userA.getId()));
        assertTrue(ex.getMessage().contains("already booked"));
    }

    @Test
    @DisplayName("Seat hold release allows another user to acquire seat")
    void testHoldReleaseAllowsNextUser() {
        seatSelectionService.holdSeat(singleSeat.getId(), userA.getId());

        // User B cannot hold
        assertThrows(BadRequestException.class, () ->
                seatSelectionService.holdSeat(singleSeat.getId(), userB.getId()));

        // User A releases hold
        seatSelectionService.releaseHold(singleSeat.getId(), userA.getId());

        // User B can now hold
        SeatHold holdB = seatSelectionService.holdSeat(singleSeat.getId(), userB.getId());
        assertNotNull(holdB);
        assertEquals("ACTIVE", holdB.getStatus());
    }
}
