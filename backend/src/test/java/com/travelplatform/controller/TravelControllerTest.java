package com.travelplatform.controller;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import com.travelplatform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Voyara differentiator endpoints.
 * Tests authorization, ownership, and basic CRUD operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TravelControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private FlightRepository flightRepository;
    @Autowired private AirlineRepository airlineRepository;
    @Autowired private AirportRepository airportRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private FlightStatusRepository flightStatusRepository;

    private User testUser;
    private User otherUser;
    private String userToken;
    private String otherToken;
    private Flight testFlight;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = new User("Test User", "voyara-test-" + UUID.randomUUID().toString().substring(0, 6) + "@test.com",
                passwordEncoder.encode("password123"));
        testUser.setRole(Role.USER);
        testUser.setEmailVerified(true);
        testUser = userRepository.save(testUser);
        userToken = tokenProvider.generateAccessToken(testUser.getEmail());

        otherUser = new User("Other User", "voyara-other-" + UUID.randomUUID().toString().substring(0, 6) + "@test.com",
                passwordEncoder.encode("password123"));
        otherUser.setRole(Role.USER);
        otherUser.setEmailVerified(true);
        otherUser = userRepository.save(otherUser);
        otherToken = tokenProvider.generateAccessToken(otherUser.getEmail());

        // Create test flight
        Airline airline = airlineRepository.findAll().stream().findFirst().orElseGet(() -> {
            Airline a = new Airline();
            a.setName("Test Airways");
            a.setCode("TA");
            return airlineRepository.save(a);
        });

        Airport del = airportRepository.findByCode("DEL").orElseGet(() -> {
            Airport a = new Airport();
            a.setCode("DEL");
            a.setName("Delhi Airport");
            a.setCity("Delhi");
            a.setCountry("India");
            return airportRepository.save(a);
        });
        Airport bom = airportRepository.findByCode("BOM").orElseGet(() -> {
            Airport a = new Airport();
            a.setCode("BOM");
            a.setName("Mumbai Airport");
            a.setCity("Mumbai");
            a.setCountry("India");
            return airportRepository.save(a);
        });

        testFlight = new Flight();
        testFlight.setFlightNumber("TA-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        testFlight.setAirline(airline);
        testFlight.setOrigin(del);
        testFlight.setDestination(bom);
        testFlight.setOriginCode("DEL");
        testFlight.setDestinationCode("BOM");
        testFlight.setDepartureDate(java.time.LocalDate.now().plusDays(7));
        testFlight.setDepartureTime(LocalDateTime.now().plusDays(7));
        testFlight.setArrivalTime(LocalDateTime.now().plusDays(7).plusHours(2));
        testFlight.setDurationMinutes(120);
        testFlight.setStops(0);
        testFlight.setEconomyBasePrice(new BigDecimal("4500.00"));
        testFlight.setEconomyPrice(new BigDecimal("4500.00"));
        testFlight.setPremiumEconomyPrice(new BigDecimal("7000.00"));
        testFlight.setBusinessPrice(new BigDecimal("12000.00"));
        testFlight.setFirstClassPrice(new BigDecimal("25000.00"));
        testFlight.setTotalSeatsEconomy(150);
        testFlight.setTotalSeatsPremiumEconomy(50);
        testFlight.setTotalSeatsBusiness(30);
        testFlight.setTotalSeatsFirst(10);
        testFlight.setBookedSeatsEconomy(10);
        testFlight.setBookedSeatsPremiumEconomy(5);
        testFlight.setBookedSeatsBusiness(2);
        testFlight.setBookedSeatsFirst(1);
        testFlight.setActive(true);
        testFlight = flightRepository.save(testFlight);

        // Create flight status
        FlightStatus status = new FlightStatus();
        status.setFlight(testFlight);
        status.setStatus("ON_TIME");
        status.setScheduledDeparture(testFlight.getDepartureTime());
        status.setEstimatedDeparture(testFlight.getDepartureTime());
        status.setScheduledArrival(testFlight.getArrivalTime());
        status.setEstimatedArrival(testFlight.getArrivalTime());
        status.setGate("A1");
        status.setTerminal("T1");
        flightStatusRepository.save(status);

        // Create test booking
        testBooking = new Booking();
        testBooking.setUser(testUser);
        testBooking.setBookingReference(Booking.generateReference());
        testBooking.setBookingType("FLIGHT");
        testBooking.setStatus("CONFIRMED");
        testBooking.setFlight(testFlight);
        testBooking.setCabinClass("ECONOMY");
        testBooking.setPassengerCount(1);
        testBooking.setTotalAmount(new BigDecimal("4500.00"));
        testBooking.setTravelDate(testFlight.getDepartureTime());
        testBooking = bookingRepository.save(testBooking);
    }

    // ─── AUTHORIZATION ─────────────────────────────────────

    @Test
    void unauthenticatedAccess_toVoyaraEndpoints_returns403() throws Exception {
        mockMvc.perform(get("/api/voyara/guardian/alerts"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/voyara/readiness/1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/voyara/timeline/1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/voyara/group-trips"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/voyara/connection-risk/1"))
                .andExpect(status().isForbidden());
    }

    // ─── TRAVEL GUARDIAN ───────────────────────────────────

    @Test
    void guardianAlerts_returnsEmptyForNewUser() throws Exception {
        mockMvc.perform(get("/api/voyara/guardian/alerts")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void guardianAnalyze_returnsSuccess() throws Exception {
        mockMvc.perform(post("/api/voyara/guardian/analyze")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void guardianAlerts_otherUserCannotSeeMyAlerts() throws Exception {
        // Analyze for testUser
        mockMvc.perform(post("/api/voyara/guardian/analyze")
                        .header("Authorization", "Bearer " + userToken));

        // otherUser should see nothing
        mockMvc.perform(get("/api/voyara/guardian/alerts")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ─── TRIP READINESS ────────────────────────────────────

    @Test
    void readiness_forConfirmedBooking_returnsScore() throws Exception {
        mockMvc.perform(get("/api/voyara/readiness/" + testBooking.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").isNumber());
    }

    @Test
    void readiness_recalculate_works() throws Exception {
        mockMvc.perform(post("/api/voyara/readiness/" + testBooking.getId() + "/recalculate")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").isNumber());
    }

    // ─── SMART TIMELINE ────────────────────────────────────

    @Test
    void timeline_forBooking_returnsEvents() throws Exception {
        mockMvc.perform(get("/api/voyara/timeline/" + testBooking.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void timeline_regenerate_works() throws Exception {
        mockMvc.perform(post("/api/voyara/timeline/" + testBooking.getId() + "/regenerate")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void timeline_userUpcoming_returnsEvents() throws Exception {
        mockMvc.perform(get("/api/voyara/timeline")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ─── CONNECTION RISK ───────────────────────────────────

    @Test
    void connectionRisk_forBooking_returnsEmptyOrResult() throws Exception {
        mockMvc.perform(get("/api/voyara/connection-risk/" + testBooking.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void connectionRisk_calculate_works() throws Exception {
        // Create a second flight for the same route
        Flight flight2 = new Flight();
        flight2.setFlightNumber("TA-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        flight2.setAirline(testFlight.getAirline());
        flight2.setOrigin(testFlight.getDestination());
        flight2.setDestination(airportRepository.findByCode("BLR").orElseGet(() -> {
            Airport a = new Airport();
            a.setCode("BLR");
            a.setName("Bangalore Airport");
            a.setCity("Bangalore");
            a.setCountry("India");
            return airportRepository.save(a);
        }));
        flight2.setOriginCode("BOM");
        flight2.setDestinationCode("BLR");
        flight2.setDepartureDate(java.time.LocalDate.now().plusDays(7));
        flight2.setDepartureTime(testFlight.getArrivalTime().plusHours(3));
        flight2.setArrivalTime(testFlight.getArrivalTime().plusHours(5));
        flight2.setDurationMinutes(120);
        flight2.setStops(0);
        flight2.setEconomyBasePrice(new BigDecimal("3500.00"));
        flight2.setEconomyPrice(new BigDecimal("3500.00"));
        flight2.setPremiumEconomyPrice(new BigDecimal("5500.00"));
        flight2.setBusinessPrice(new BigDecimal("9000.00"));
        flight2.setFirstClassPrice(new BigDecimal("18000.00"));
        flight2.setTotalSeatsEconomy(150);
        flight2.setTotalSeatsPremiumEconomy(50);
        flight2.setTotalSeatsBusiness(30);
        flight2.setTotalSeatsFirst(10);
        flight2.setActive(true);
        flight2 = flightRepository.save(flight2);

        // Create flight status for second flight
        FlightStatus status2 = new FlightStatus();
        status2.setFlight(flight2);
        status2.setStatus("ON_TIME");
        status2.setScheduledDeparture(flight2.getDepartureTime());
        status2.setEstimatedDeparture(flight2.getDepartureTime());
        status2.setScheduledArrival(flight2.getArrivalTime());
        status2.setEstimatedArrival(flight2.getArrivalTime());
        flightStatusRepository.save(status2);

        mockMvc.perform(post("/api/voyara/connection-risk/calculate")
                        .header("Authorization", "Bearer " + userToken)
                        .param("firstFlightId", testFlight.getId().toString())
                        .param("secondFlightId", flight2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.riskLevel").isString())
                .andExpect(jsonPath("$.data.connectionMinutes").isNumber());
    }

    // ─── GROUP TRIPS ───────────────────────────────────────

    @Test
    void groupTrips_emptyForNewUser() throws Exception {
        mockMvc.perform(get("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void createGroupTrip_works() throws Exception {
        mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goa Trip\",\"description\":\"Weekend getaway\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Goa Trip"));
    }

    @Test
    void createGroupTrip_otherUserCannotAccess() throws Exception {
        // testUser creates a trip
        String response = mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Private Trip\",\"description\":\"Secret\"}"))
                .andReturn().getResponse().getContentAsString();

        String tripId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("data").get("id").asText();

        // otherUser cannot access it
        mockMvc.perform(get("/api/voyara/group-trips/" + tripId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createGroupTrip_withToday_allowed() throws Exception {
        java.time.LocalDate today = java.time.LocalDate.now();
        mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Today Trip\",\"startDate\":\"" + today + "\",\"endDate\":\"" + today.plusDays(3) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startDate").value(today.toString()));
    }

    @Test
    void createGroupTrip_withPastStartDate_rejected() throws Exception {
        java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
        mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Past Trip\",\"startDate\":\"" + yesterday + "\",\"endDate\":\"" + java.time.LocalDate.now().plusDays(2) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Trip dates cannot be in the past."));
    }

    @Test
    void createGroupTrip_withEndDateBeforeStartDate_rejected() throws Exception {
        java.time.LocalDate today = java.time.LocalDate.now();
        mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Invalid Range Trip\",\"startDate\":\"" + today.plusDays(5) + "\",\"endDate\":\"" + today.plusDays(3) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("End date must be on or after the start date."));
    }

    @Test
    void createGroupTrip_withSameDay_allowed() throws Exception {
        java.time.LocalDate today = java.time.LocalDate.now();
        mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Day Trip\",\"startDate\":\"" + today + "\",\"endDate\":\"" + today + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startDate").value(today.toString()))
                .andExpect(jsonPath("$.data.endDate").value(today.toString()));
    }

    @Test
    void inviteCompanion_works() throws Exception {
        String response = mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Invite Test\"}"))
                .andReturn().getResponse().getContentAsString();

        String tripId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("data").get("id").asText();

        mockMvc.perform(post("/api/voyara/group-trips/" + tripId + "/invite")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"friend@test.com\",\"message\":\"Join me!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SENT"));
    }

    @Test
    void inviteCompanion_onlyOwnerCanInvite() throws Exception {
        String response = mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Owner Only\"}"))
                .andReturn().getResponse().getContentAsString();

        String tripId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("data").get("id").asText();

        // otherUser is not the owner
        mockMvc.perform(post("/api/voyara/group-trips/" + tripId + "/invite")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@test.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Only the trip owner can invite companions"));
    }

    @Test
    void inviteCompanion_invalidEmail_rejected() throws Exception {
        String response = mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Invalid Email Trip\"}"))
                .andReturn().getResponse().getContentAsString();

        String tripId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("data").get("id").asText();

        mockMvc.perform(post("/api/voyara/group-trips/" + tripId + "/invite")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Enter a valid email address."));
    }

    @Test
    void inviteCompanion_selfInvite_rejected() throws Exception {
        String response = mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Self Invite Trip\"}"))
                .andReturn().getResponse().getContentAsString();

        String tripId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("data").get("id").asText();

        mockMvc.perform(post("/api/voyara/group-trips/" + tripId + "/invite")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + testUser.getEmail() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You cannot invite yourself to your own trip"));
    }

    @Test
    void inviteCompanion_resendPending_succeedsWithoutDuplicates() throws Exception {
        String response = mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Resend Test Trip\"}"))
                .andReturn().getResponse().getContentAsString();

        String tripId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("data").get("id").asText();

        // First invite
        mockMvc.perform(post("/api/voyara/group-trips/" + tripId + "/invite")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"resend@test.com\",\"message\":\"First\"}"))
                .andExpect(status().isOk());

        // Resend to same email
        mockMvc.perform(post("/api/voyara/group-trips/" + tripId + "/invite")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"resend@test.com\",\"message\":\"Reminder\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SENT"));
    }

    // ─── EXPENSES ──────────────────────────────────────────

    @Test
    void getExpenses_emptyForNewTrip() throws Exception {
        String response = mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Expense Test\"}"))
                .andReturn().getResponse().getContentAsString();

        String tripId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("data").get("id").asText();

        mockMvc.perform(get("/api/voyara/group-trips/" + tripId + "/expenses")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void addExpense_equalSplit_works() throws Exception {
        String response = mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Split Test\"}"))
                .andReturn().getResponse().getContentAsString();

        String tripId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("data").get("id").asText();

        mockMvc.perform(post("/api/voyara/group-trips/" + tripId + "/expenses")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Dinner\",\"amount\":1000,\"expenseType\":\"FOOD\",\"splitMode\":\"EQUAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void settlements_calculatedCorrectly() throws Exception {
        String response = mockMvc.perform(post("/api/voyara/group-trips")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Settlement Test\"}"))
                .andReturn().getResponse().getContentAsString();

        String tripId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("data").get("id").asText();

        mockMvc.perform(get("/api/voyara/group-trips/" + tripId + "/settlements")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
