package com.travelplatform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DynamicPricingAndFreezeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private PriceFreezeRepository priceFreezeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AirportRepository airportRepository;
    @Autowired private AirlineRepository airlineRepository;

    private User testUser;
    private User otherUser;
    private String userToken;
    private String otherToken;
    private Flight testFlight;

    @BeforeEach
    void setUp() {
        testUser = new User("Pricing User", "pricing@test.com", passwordEncoder.encode("password123"));
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);
        userToken = tokenProvider.generateAccessToken(testUser.getEmail());

        otherUser = new User("Other User", "otherprice@test.com", passwordEncoder.encode("password123"));
        otherUser.setRole(Role.USER);
        otherUser = userRepository.save(otherUser);
        otherToken = tokenProvider.generateAccessToken(otherUser.getEmail());

        List<Flight> flights = flightRepository.findAll();
        if (flights.isEmpty()) {
            Airport origin = airportRepository.findByCode("DEL").orElseGet(() ->
                    airportRepository.save(new Airport("DEL", "Indira Gandhi International", "Delhi", "India", 28.5562, 77.1000)));
            Airport dest = airportRepository.findByCode("BOM").orElseGet(() ->
                    airportRepository.save(new Airport("BOM", "Chhatrapati Shivaji International", "Mumbai", "India", 19.0896, 72.8656)));
            Airline airline = airlineRepository.findByCode("AI").orElseGet(() ->
                    airlineRepository.save(new Airline("AI", "Air India", "https://example.com/ai.png", 4.5)));

            Flight f = new Flight();
            f.setFlightNumber("AI-TEST-PRICE");
            f.setAirline(airline);
            f.setOrigin(origin);
            f.setDestination(dest);
            f.setOriginCode("DEL");
            f.setDestinationCode("BOM");
            f.setDepartureTime(LocalDateTime.now().plusDays(5));
            f.setArrivalTime(LocalDateTime.now().plusDays(5).plusHours(2));
            f.setDepartureDate(LocalDateTime.now().plusDays(5).toLocalDate());
            f.setEconomyPrice(new BigDecimal("5000.00"));
            f.setEconomyBasePrice(new BigDecimal("5000.00"));
            f.setPremiumEconomyPrice(new BigDecimal("7500.00"));
            f.setBusinessPrice(new BigDecimal("12000.00"));
            f.setFirstClassPrice(new BigDecimal("20000.00"));
            f.setTotalSeatsEconomy(100);
            f.setDurationMinutes(120);
            f.setActive(true);
            testFlight = flightRepository.save(f);
        } else {
            testFlight = flights.get(0);
        }
    }

    @Test
    void testGetPricingFactors() throws Exception {
        mockMvc.perform(get("/api/flights/" + testFlight.getId() + "/pricing-factors")
                        .param("cabinClass", "ECONOMY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basePrice").exists())
                .andExpect(jsonPath("$.data.demandRatio").exists());
    }

    @Test
    void testCreateAndUsePriceFreeze() throws Exception {
        // 1. Create a freeze
        String freezeResponse = mockMvc.perform(post("/api/prices/freeze")
                        .header("Authorization", "Bearer " + userToken)
                        .param("flightId", testFlight.getId().toString())
                        .param("cabinClass", "ECONOMY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.frozenPrice").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode freezeNode = objectMapper.readTree(freezeResponse);
        Long freezeId = freezeNode.get("data").get("id").asLong();
        assertNotNull(freezeId);

        // 2. Book using the freeze
        String bookingRequest = "{" +
                "\"bookingType\":\"FLIGHT\"," +
                "\"flightId\":" + testFlight.getId() + "," +
                "\"cabinClass\":\"ECONOMY\"," +
                "\"passengerCount\":1," +
                "\"paymentMethod\":\"RAZORPAY\"," +
                "\"freezeId\":" + freezeId +
                "}";

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingReference").exists());

        // 3. Verify freeze is USED
        PriceFreeze usedFreeze = priceFreezeRepository.findById(freezeId).orElseThrow();
        assertEquals("USED", usedFreeze.getStatus());
    }

    @Test
    void testCannotUseOtherUsersPriceFreeze() throws Exception {
        // 1. Create a freeze as testUser
        String freezeResponse = mockMvc.perform(post("/api/prices/freeze")
                        .header("Authorization", "Bearer " + userToken)
                        .param("flightId", testFlight.getId().toString())
                        .param("cabinClass", "ECONOMY"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long freezeId = objectMapper.readTree(freezeResponse).get("data").get("id").asLong();

        // 2. Try to book using that freeze as otherUser
        String bookingRequest = "{" +
                "\"bookingType\":\"FLIGHT\"," +
                "\"flightId\":" + testFlight.getId() + "," +
                "\"cabinClass\":\"ECONOMY\"," +
                "\"passengerCount\":1," +
                "\"paymentMethod\":\"RAZORPAY\"," +
                "\"freezeId\":" + freezeId +
                "}";

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Could not apply price freeze: Price freeze does not belong to this user"));
    }

    @Test
    void testMultipleUsersCanFreezeSameFlight() throws Exception {
        // User A creates freeze
        mockMvc.perform(post("/api/prices/freeze")
                        .header("Authorization", "Bearer " + userToken)
                        .param("flightId", testFlight.getId().toString())
                        .param("cabinClass", "ECONOMY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // User B ALSO independently creates freeze for the SAME flight
        mockMvc.perform(post("/api/prices/freeze")
                        .header("Authorization", "Bearer " + otherToken)
                        .param("flightId", testFlight.getId().toString())
                        .param("cabinClass", "ECONOMY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void testExpiredFreezeCannotBeUsed() throws Exception {
        // 1. Create freeze
        String freezeResponse = mockMvc.perform(post("/api/prices/freeze")
                        .header("Authorization", "Bearer " + userToken)
                        .param("flightId", testFlight.getId().toString())
                        .param("cabinClass", "ECONOMY"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long freezeId = objectMapper.readTree(freezeResponse).get("data").get("id").asLong();

        // 2. Artificially expire the freeze
        PriceFreeze freeze = priceFreezeRepository.findById(freezeId).orElseThrow();
        freeze.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        priceFreezeRepository.save(freeze);

        // 3. Attempt to book
        String bookingRequest = "{" +
                "\"bookingType\":\"FLIGHT\"," +
                "\"flightId\":" + testFlight.getId() + "," +
                "\"cabinClass\":\"ECONOMY\"," +
                "\"passengerCount\":1," +
                "\"paymentMethod\":\"RAZORPAY\"," +
                "\"freezeId\":" + freezeId +
                "}";

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Could not apply price freeze: Price freeze has expired"));
    }

    @Test
    void testCannotReuseAlreadyConsumedFreeze() throws Exception {
        // 1. Create and consume freeze
        String freezeResponse = mockMvc.perform(post("/api/prices/freeze")
                        .header("Authorization", "Bearer " + userToken)
                        .param("flightId", testFlight.getId().toString())
                        .param("cabinClass", "ECONOMY"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long freezeId = objectMapper.readTree(freezeResponse).get("data").get("id").asLong();

        String bookingRequest = "{" +
                "\"bookingType\":\"FLIGHT\"," +
                "\"flightId\":" + testFlight.getId() + "," +
                "\"cabinClass\":\"ECONOMY\"," +
                "\"passengerCount\":1," +
                "\"paymentMethod\":\"RAZORPAY\"," +
                "\"freezeId\":" + freezeId +
                "}";

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest))
                .andExpect(status().isOk());

        // 2. Attempt double-consumption
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Could not apply price freeze: Price freeze is not active (status: USED)"));
    }

    @Test
    void testUnauthenticatedActiveFreezeReturnsFalse() throws Exception {
        mockMvc.perform(get("/api/prices/freeze/active")
                        .param("entityType", "FLIGHT")
                        .param("entityId", testFlight.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void testGetPricingBreakdownEndpoint() throws Exception {
        mockMvc.perform(get("/api/prices/breakdown")
                        .param("entityType", "FLIGHT")
                        .param("entityId", testFlight.getId().toString())
                        .param("cabinClass", "ECONOMY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePrice").exists())
                .andExpect(jsonPath("$.currentPrice").exists())
                .andExpect(jsonPath("$.demandMultiplier").exists())
                .andExpect(jsonPath("$.inventoryMultiplier").exists())
                .andExpect(jsonPath("$.seasonalMultiplier").exists())
                .andExpect(jsonPath("$.totalMultiplier").exists());
    }
}
