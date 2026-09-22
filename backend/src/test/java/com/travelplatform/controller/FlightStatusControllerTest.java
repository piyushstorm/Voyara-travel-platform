package com.travelplatform.controller;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import com.travelplatform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlightStatusControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepo;
    @Autowired private FlightRepository flightRepo;
    @Autowired private FlightStatusRepository flightStatusRepo;
    @Autowired private FlightStatusHistoryRepository historyRepo;
    @Autowired private AirlineRepository airlineRepo;
    @Autowired private AirportRepository airportRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private TrackedFlightRepository trackedFlightRepo;

    private User user;
    private String userToken;
    private Flight testFlight;
    private FlightStatus testStatus;

    @BeforeEach
    void setUp() {
        user = new User("Flight Test User", "flight-status-test-" + UUID.randomUUID() + "@test.com", passwordEncoder.encode("pass123"));
        user.setRole(Role.USER);
        user = userRepo.save(user);
        userToken = tokenProvider.generateAccessToken(user.getEmail());

        Airline airline = airlineRepo.findAll().stream().findFirst().orElseGet(() -> {
            Airline a = new Airline(); a.setName("Status Test Air"); a.setCode("ST"); return airlineRepo.save(a);
        });

        Airport origin = airportRepo.findByCode("DEL").orElseGet(() -> {
            Airport ap = new Airport(); ap.setCode("DEL"); ap.setName("Delhi Airport"); ap.setCity("Delhi"); ap.setCountry("India"); return airportRepo.save(ap);
        });
        Airport dest = airportRepo.findByCode("BOM").orElseGet(() -> {
            Airport ap = new Airport(); ap.setCode("BOM"); ap.setName("Mumbai Airport"); ap.setCity("Mumbai"); ap.setCountry("India"); return airportRepo.save(ap);
        });

        testFlight = new Flight();
        testFlight.setFlightNumber("FS-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        testFlight.setAirline(airline);
        testFlight.setOrigin(origin);
        testFlight.setDestination(dest);
        testFlight.setOriginCode("DEL");
        testFlight.setDestinationCode("BOM");
        testFlight.setDepartureDate(java.time.LocalDate.now().plusDays(1));
        testFlight.setDepartureTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        testFlight.setArrivalTime(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0));
        testFlight.setDurationMinutes(120);
        testFlight.setStops(0);
        testFlight.setEconomyPrice(java.math.BigDecimal.valueOf(5000));
        testFlight.setPremiumEconomyPrice(java.math.BigDecimal.valueOf(7500));
        testFlight.setBusinessPrice(java.math.BigDecimal.valueOf(12500));
        testFlight.setFirstClassPrice(java.math.BigDecimal.valueOf(20000));
        testFlight.setTotalSeatsEconomy(150);
        testFlight.setTotalSeatsPremiumEconomy(50);
        testFlight.setTotalSeatsBusiness(30);
        testFlight.setTotalSeatsFirst(10);
        testFlight.setActive(true);
        testFlight = flightRepo.save(testFlight);

        testStatus = new FlightStatus();
        testStatus.setFlight(testFlight);
        testStatus.setStatus("ON_TIME");
        testStatus.setScheduledDeparture(testFlight.getDepartureTime());
        testStatus.setEstimatedDeparture(testFlight.getDepartureTime());
        testStatus.setScheduledArrival(testFlight.getArrivalTime());
        testStatus.setEstimatedArrival(testFlight.getArrivalTime());
        testStatus.setGate("A12");
        testStatus.setTerminal("T1");
        testStatus = flightStatusRepo.save(testStatus);
    }

    // ─── GET Single Status ──────────────────────────────────

    @Test
    @DisplayName("GET /api/flight-status/{id} — returns flight status for valid flight")
    void getFlightStatusValid() throws Exception {
        mockMvc.perform(get("/api/flight-status/" + testFlight.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_TIME"))
                .andExpect(jsonPath("$.gate").value("A12"))
                .andExpect(jsonPath("$.terminal").value("T1"));
    }

    @Test
    @DisplayName("GET /api/flight-status/{id} — unknown flight returns 404")
    void getFlightStatusUnknown() throws Exception {
        mockMvc.perform(get("/api/flight-status/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/flight-status/{id} — unauthenticated access allowed (public endpoint)")
    void getFlightStatusUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/flight-status/" + testFlight.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_TIME"));
    }

    @Test
    @DisplayName("GET /api/flight-status/{id} — status includes delay info")
    void flightStatusIncludesDelayInfo() throws Exception {
        testStatus.setStatus("DELAYED");
        testStatus.setDelayReason("Weather conditions");
        testStatus.setEstimatedDeparture(testFlight.getDepartureTime().plusMinutes(45));
        flightStatusRepo.save(testStatus);

        mockMvc.perform(get("/api/flight-status/" + testFlight.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELAYED"))
                .andExpect(jsonPath("$.delayReason").value("Weather conditions"));
    }

    @Test
    @DisplayName("GET /api/flight-status/{id} — status includes gate and terminal")
    void flightStatusIncludesGateTerminal() throws Exception {
        testStatus.setStatus("BOARDING");
        testStatus.setGate("B7");
        testStatus.setTerminal("T2");
        flightStatusRepo.save(testStatus);

        mockMvc.perform(get("/api/flight-status/" + testFlight.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BOARDING"))
                .andExpect(jsonPath("$.gate").value("B7"))
                .andExpect(jsonPath("$.terminal").value("T2"));
    }

    @Test
    @DisplayName("GET /api/flight-status/{id} — includes updatedAt timestamp")
    void flightStatusIncludesTimestamp() throws Exception {
        mockMvc.perform(get("/api/flight-status/" + testFlight.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    // ─── TRACKING FLIGHTS ───────────────────────────────────

    @Test
    @DisplayName("GET /api/flight-status/tracked — returns empty list initially")
    void getTrackedFlightsEmpty() throws Exception {
        mockMvc.perform(get("/api/flight-status/tracked")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("POST /api/flight-status/{id}/track — tracks a flight")
    void trackFlight() throws Exception {
        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/track")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Flight tracked successfully"));

        // Verify it shows up in tracked list
        mockMvc.perform(get("/api/flight-status/tracked")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].flightId").value(testFlight.getId()));
    }

    @Test
    @DisplayName("DELETE /api/flight-status/{id}/untrack — untracks a flight")
    void untrackFlight() throws Exception {
        // Track first
        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/track")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Untrack
        mockMvc.perform(delete("/api/flight-status/" + testFlight.getId() + "/untrack")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Flight untracked successfully"));

        // Verify it's removed from tracked list
        mockMvc.perform(get("/api/flight-status/tracked")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ─── GET Status History ─────────────────────────────────

    @Test
    @DisplayName("GET /api/flight-status/{id}/history — returns status change history")
    void getFlightStatusHistory() throws Exception {
        FlightStatusHistory history = new FlightStatusHistory();
        history.setFlight(testFlight);
        history.setPreviousStatus("ON_TIME");
        history.setNewStatus("DELAYED");
        history.setDelayReason("Air traffic control");
        history.setMessage("Flight delayed by 30 minutes");
        historyRepo.save(history);

        mockMvc.perform(get("/api/flight-status/" + testFlight.getId() + "/history")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].previousStatus").value("ON_TIME"))
                .andExpect(jsonPath("$[0].newStatus").value("DELAYED"))
                .andExpect(jsonPath("$[0].delayReason").value("Air traffic control"));
    }

    @Test
    @DisplayName("GET /api/flight-status/{id}/history — unknown flight returns empty list")
    void getHistoryUnknownFlight() throws Exception {
        // History endpoint returns empty list for unknown flights
        mockMvc.perform(get("/api/flight-status/99999/history")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ─── STATUS TRANSITIONS ─────────────────────────────────

    @Test
    @DisplayName("GET /api/flight-status/{id} — shows DEPARTED status")
    void showsDepartedStatus() throws Exception {
        testStatus.setStatus("DEPARTED");
        flightStatusRepo.save(testStatus);

        mockMvc.perform(get("/api/flight-status/" + testFlight.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEPARTED"));
    }

    @Test
    @DisplayName("GET /api/flight-status/{id} — shows CANCELLED status")
    void showsCancelledStatus() throws Exception {
        testStatus.setStatus("CANCELLED");
        testStatus.setDelayReason("Aircraft mechanical issue");
        flightStatusRepo.save(testStatus);

        mockMvc.perform(get("/api/flight-status/" + testFlight.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.delayReason").value("Aircraft mechanical issue"));
    }

    @Test
    @DisplayName("GET /api/flight-status/{id} — shows ARRIVED status")
    void showsArrivedStatus() throws Exception {
        testStatus.setStatus("ARRIVED");
        flightStatusRepo.save(testStatus);

        mockMvc.perform(get("/api/flight-status/" + testFlight.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARRIVED"));
    }

    // ─── DATA INTEGRITY ─────────────────────────────────────

    @Test
    @DisplayName("Flight status response contains estimated departure when delayed")
    void delayedFlightShowsEstimatedTimes() throws Exception {
        testStatus.setStatus("DELAYED");
        testStatus.setEstimatedDeparture(testFlight.getDepartureTime().plusMinutes(60));
        testStatus.setEstimatedArrival(testFlight.getArrivalTime().plusMinutes(60));
        flightStatusRepo.save(testStatus);

        mockMvc.perform(get("/api/flight-status/" + testFlight.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedDeparture").exists())
                .andExpect(jsonPath("$.estimatedArrival").exists())
                .andExpect(jsonPath("$.scheduledDeparture").exists())
                .andExpect(jsonPath("$.scheduledArrival").exists());
    }

    @Test
    @DisplayName("Flight status response does not expose user or internal data")
    void flightStatusNoInternalData() throws Exception {
        mockMvc.perform(get("/api/flight-status/" + testFlight.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    // ─── SECURITY & AUTHORIZATION ────────────────────────────

    @Test
    @DisplayName("GET /api/flight-status/tracked — unauthenticated returns 403")
    void getTrackedUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/flight-status/tracked"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/flight-status/{id}/track — unauthenticated returns 403")
    void trackFlightUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/track"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/flight-status/{id}/track — duplicate tracking is idempotent")
    void duplicateTrackFlightIsIdempotent() throws Exception {
        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/track")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Track again
        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/track")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Flight tracked successfully"));

        // Verify only 1 tracking entry exists for this user
        mockMvc.perform(get("/api/flight-status/tracked")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/flight-status/tracked — tracks and lists multiple flights simultaneously")
    void multiFlightTracking() throws Exception {
        Flight f2 = createSecondFlight();

        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/track")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/flight-status/" + f2.getId() + "/track")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/flight-status/tracked")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/flight-status/tracked — user isolation: other users cannot see tracked flights")
    void userIsolationInTracking() throws Exception {
        User otherUser = new User("Other User", "other-" + UUID.randomUUID() + "@test.com", passwordEncoder.encode("pass123"));
        otherUser.setRole(Role.USER);
        otherUser = userRepo.save(otherUser);
        String otherToken = tokenProvider.generateAccessToken(otherUser.getEmail());

        // User 1 tracks testFlight
        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/track")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Other user's tracked list should be 0
        mockMvc.perform(get("/api/flight-status/tracked")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ─── SEARCH & SIMULATION ────────────────────────────────

    @Test
    @DisplayName("GET /api/flight-status/search — finds flights matching query")
    void searchFlights() throws Exception {
        mockMvc.perform(get("/api/flight-status/search")
                        .param("query", testFlight.getFlightNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].flightNumber").value(testFlight.getFlightNumber()))
                .andExpect(jsonPath("$[0].departureAirportCode").value("DEL"))
                .andExpect(jsonPath("$[0].arrivalAirportCode").value("BOM"));
    }

    @Test
    @DisplayName("POST /api/flight-status/{id}/simulate — simulates DELAYED_WEATHER scenario")
    void simulateDelayedWeatherScenario() throws Exception {
        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"DELAYED_WEATHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELAYED"))
                .andExpect(jsonPath("$.delayMinutes").value(60))
                .andExpect(jsonPath("$.delayReason").isNotEmpty())
                .andExpect(jsonPath("$.scenario").value("DELAYED_WEATHER"));
    }

    @Test
    @DisplayName("Notification deduplication — repeated transitions do not generate duplicate notifications")
    void notificationDeduplication() throws Exception {
        // Track the flight
        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/track")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        long initialNotifications = notificationRepo.countUnreadByUserId(user.getId());

        // Simulate DELAYED_WEATHER
        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"DELAYED_WEATHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELAYED"));

        long afterFirstSim = notificationRepo.countUnreadByUserId(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(initialNotifications + 1, afterFirstSim);

        // Simulate DELAYED_WEATHER again (same state)
        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"DELAYED_WEATHER\"}"))
                .andExpect(status().isOk());

        long afterSecondSim = notificationRepo.countUnreadByUserId(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(afterFirstSim, afterSecondSim, "Repeated simulation of same state must not create duplicate notifications");
    }

    @Test
    @DisplayName("Multi-user notification — multiple users tracking same flight both receive notifications without collision")
    void multiUserNotificationDelivery() throws Exception {
        User user2 = new User("User Two", "user2-" + UUID.randomUUID() + "@test.com", passwordEncoder.encode("pass123"));
        user2.setRole(Role.USER);
        user2 = userRepo.save(user2);
        String user2Token = tokenProvider.generateAccessToken(user2.getEmail());

        // Both users track testFlight
        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/track")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/track")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk());

        // Simulate BOARDING
        mockMvc.perform(post("/api/flight-status/" + testFlight.getId() + "/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"BOARDING\"}"))
                .andExpect(status().isOk());

        // Both users should have received at least 1 notification
        org.junit.jupiter.api.Assertions.assertTrue(notificationRepo.countUnreadByUserId(user.getId()) >= 1);
        org.junit.jupiter.api.Assertions.assertTrue(notificationRepo.countUnreadByUserId(user2.getId()) >= 1);
    }

    private Flight createSecondFlight() {
        Flight f2 = new Flight();
        f2.setFlightNumber("FS-ALT" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        f2.setAirline(testFlight.getAirline());
        f2.setOrigin(testFlight.getOrigin());
        f2.setDestination(testFlight.getDestination());
        f2.setOriginCode("DEL");
        f2.setDestinationCode("BOM");
        f2.setDepartureDate(java.time.LocalDate.now().plusDays(2));
        f2.setDepartureTime(LocalDateTime.now().plusDays(2).withHour(14).withMinute(0));
        f2.setArrivalTime(LocalDateTime.now().plusDays(2).withHour(16).withMinute(0));
        f2.setDurationMinutes(120);
        f2.setStops(0);
        f2.setEconomyPrice(java.math.BigDecimal.valueOf(6000));
        f2.setPremiumEconomyPrice(java.math.BigDecimal.valueOf(8500));
        f2.setBusinessPrice(java.math.BigDecimal.valueOf(14000));
        f2.setFirstClassPrice(java.math.BigDecimal.valueOf(22000));
        f2.setTotalSeatsEconomy(150);
        f2.setTotalSeatsPremiumEconomy(50);
        f2.setTotalSeatsBusiness(30);
        f2.setTotalSeatsFirst(10);
        f2.setActive(true);
        return flightRepo.save(f2);
    }
}
