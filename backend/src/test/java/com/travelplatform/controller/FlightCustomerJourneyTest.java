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
 * End-to-end flight customer journey tests.
 * Tests the complete flow: register → login → search → book → trip → cancel
 * Uses mock payment provider (no real Razorpay calls).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlightCustomerJourneyTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private AirlineRepository airlineRepository;
    @Autowired private AirportRepository airportRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private User testUser;
    private String userToken;
    private Flight testFlight;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User("Journey Test", "journey@test.com", passwordEncoder.encode("password123"));
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);
        userToken = tokenProvider.generateAccessToken(testUser.getEmail());

        // Create test airline
        Airline airline = airlineRepository.findAll().stream().findFirst().orElseGet(() -> {
            Airline a = new Airline();
            a.setName("Test Airways");
            a.setCode("TA");
            return airlineRepository.save(a);
        });

        // Create airports
        Airport del = airportRepository.findByCode("DEL").orElseGet(() -> {
            Airport a = new Airport(); a.setCode("DEL"); a.setName("Delhi Airport"); a.setCity("Delhi"); a.setCountry("India"); return airportRepository.save(a);
        });
        Airport bom = airportRepository.findByCode("BOM").orElseGet(() -> {
            Airport a = new Airport(); a.setCode("BOM"); a.setName("Mumbai Airport"); a.setCity("Mumbai"); a.setCountry("India"); return airportRepository.save(a);
        });

        // Create test flight
        testFlight = new Flight();
        testFlight.setFlightNumber("TA-101-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
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
    }

    // ─── Registration ──────────────────────────────────

    @Test
    void registerAndLogin() throws Exception {
        String email = "e2e-reg-" + UUID.randomUUID().toString().substring(0, 6) + "@test.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"E2E User\",\"email\":\"" + email + "\",\"password\":\"securePass123!\"}"))
                .andExpect(status().isCreated());

        // Verify user is unverified, then mark as verified (simulating email verification)
        User registered = userRepository.findByEmail(email).orElseThrow();
        assert !registered.isEmailVerified();
        registered.setEmailVerified(true);
        userRepository.save(registered);

        // Login with the new account
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"securePass123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    // ─── Flight Search ─────────────────────────────────

    @Test
    void searchFlightsByRoute() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "BOM")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void searchFlightsWithoutAuth() throws Exception {
        // Flight search is public - verify it works without auth
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "BOM"))
                .andExpect(status().isOk());
    }

    @Test
    void getFlightDetails() throws Exception {
        mockMvc.perform(get("/api/flights/" + testFlight.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    // ─── Booking ───────────────────────────────────────

    @Test
    void createFlightBooking() throws Exception {
        String bookingRef = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"bookingType\":\"FLIGHT\"," +
                                "\"flightId\":" + testFlight.getId() + "," +
                                "\"cabinClass\":\"ECONOMY\"," +
                                "\"passengerCount\":1," +
                                "\"paymentMethod\":\"RAZORPAY\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingReference").exists())
                .andReturn().getResponse().getContentAsString();

        // Verify booking was created in DB
        assert bookingRepository.count() > 0;
    }

    @Test
    void cannotBookWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"bookingType\":\"FLIGHT\"," +
                                "\"flightId\":" + testFlight.getId() + "," +
                                "\"cabinClass\":\"ECONOMY\"," +
                                "\"passengerCount\":1" +
                                "}"))
                .andExpect(status().isForbidden());
    }

    // ─── My Trips ──────────────────────────────────────

    @Test
    void viewMyTrips() throws Exception {
        mockMvc.perform(get("/api/bookings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    // ─── Cancel Booking ────────────────────────────────

    @Test
    void cancelOwnBooking() throws Exception {
        // Create a booking first
        String createResponse = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"bookingType\":\"FLIGHT\"," +
                                "\"flightId\":" + testFlight.getId() + "," +
                                "\"cabinClass\":\"ECONOMY\"," +
                                "\"passengerCount\":1," +
                                "\"paymentMethod\":\"RAZORPAY\"" +
                                "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String bookingRef = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).get("data").get("bookingReference").asText();

        // Cancel the booking
        mockMvc.perform(post("/api/bookings/" + bookingRef + "/cancel")
                        .header("Authorization", "Bearer " + userToken)
                        .param("reason", "Changed plans"))
                .andExpect(status().isOk());
    }

    @Test
    void cannotCancelOtherUsersBooking() throws Exception {
        // Create booking as testUser
        String createResponse = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"bookingType\":\"FLIGHT\"," +
                                "\"flightId\":" + testFlight.getId() + "," +
                                "\"cabinClass\":\"ECONOMY\"," +
                                "\"passengerCount\":1," +
                                "\"paymentMethod\":\"RAZORPAY\"" +
                                "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String bookingRef = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).get("data").get("bookingReference").asText();

        // Try to cancel as a different user
        User otherUser = new User("Other User", "other-" + UUID.randomUUID().toString().substring(0, 6) + "@test.com",
                passwordEncoder.encode("password123"));
        otherUser.setRole(Role.USER);
        otherUser = userRepository.save(otherUser);
        String otherToken = tokenProvider.generateAccessToken(otherUser.getEmail());

        mockMvc.perform(post("/api/bookings/" + bookingRef + "/cancel")
                        .header("Authorization", "Bearer " + otherToken)
                        .param("reason", "Not my booking"))
                .andExpect(status().isBadRequest());
    }
}
