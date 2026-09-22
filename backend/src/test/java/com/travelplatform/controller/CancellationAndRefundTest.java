package com.travelplatform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplatform.dto.booking.CancelBookingRequest;
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
class CancellationAndRefundTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private AirportRepository airportRepository;
    @Autowired private AirlineRepository airlineRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private CancellationPolicyRepository policyRepository;
    @Autowired private RefundRepository refundRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private ObjectMapper objectMapper;

    private User testUser;
    private String userToken;
    private User adminUser;
    private String adminToken;
    private User otherUser;
    private String otherUserToken;
    private Flight testFlight;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testUser = new User("Cancel User", "cancel@test.com", passwordEncoder.encode("password123"));
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);
        userToken = tokenProvider.generateAccessToken(testUser.getEmail());

        adminUser = new User("Admin User", "admin_cancel@test.com", passwordEncoder.encode("password123"));
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);
        adminToken = tokenProvider.generateAccessToken(adminUser.getEmail());

        otherUser = new User("Other User", "other_cancel@test.com", passwordEncoder.encode("password123"));
        otherUser.setRole(Role.USER);
        otherUser = userRepository.save(otherUser);
        otherUserToken = tokenProvider.generateAccessToken(otherUser.getEmail());

        List<Flight> flights = flightRepository.findAll();
        if (flights.isEmpty()) {
            Airport origin = airportRepository.findByCode("DEL").orElseGet(() ->
                    airportRepository.save(new Airport("DEL", "Indira Gandhi International", "Delhi", "India", 28.5562, 77.1000)));
            Airport dest = airportRepository.findByCode("BOM").orElseGet(() ->
                    airportRepository.save(new Airport("BOM", "Chhatrapati Shivaji International", "Mumbai", "India", 19.0896, 72.8656)));
            Airline airline = airlineRepository.findByCode("AI").orElseGet(() ->
                    airlineRepository.save(new Airline("AI", "Air India", "https://example.com/ai.png", 4.5)));

            Flight f = new Flight();
            f.setFlightNumber("AI-TEST-99");
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
            testFlight = flightRepository.save(f);
        } else {
            testFlight = flights.get(0);
            testFlight.setDepartureTime(LocalDateTime.now().plusDays(5));
            testFlight = flightRepository.save(testFlight);
        }

        testBooking = new Booking();
        testBooking.setBookingReference(Booking.generateReference());
        testBooking.setUser(testUser);
        testBooking.setFlight(testFlight);
        testBooking.setBookingType("FLIGHT");
        testBooking.setCabinClass("ECONOMY");
        testBooking.setPassengerCount(1);
        testBooking.setStatus("CONFIRMED");
        testBooking.setTotalAmount(new BigDecimal("5000.00"));
        testBooking.setTravelDate(testFlight.getDepartureTime());
        testBooking.setPaymentId("pay_test_123");
        testBooking = bookingRepository.save(testBooking);

        // Ensure baseline cancellation policy exists
        policyRepository.deleteAll();
        CancellationPolicy policy = new CancellationPolicy(
                "FLIGHT",
                "Test Full Refund Policy",
                72,
                0,
                new BigDecimal("100.00"),
                BigDecimal.ZERO
        );
        policyRepository.save(policy);
    }

    @Test
    @DisplayName("Preview authoritative refund calculation")
    void testPreviewRefund() throws Exception {
        mockMvc.perform(get("/api/refunds/preview/" + testBooking.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundAmount").value(5000.0))
                .andExpect(jsonPath("$.policyName").exists())
                .andExpect(jsonPath("$.expectedTimeline").value("Expected within 5–7 business days"));
    }

    @Test
    @DisplayName("Get predefined cancellation reasons list")
    void testGetCancellationReasonsEndpoint() throws Exception {
        mockMvc.perform(get("/api/refunds/reasons")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").value("CHANGE_OF_PLANS"))
                .andExpect(jsonPath("$[0].label").value("Change of plans"));
    }

    @Test
    @DisplayName("Cancel booking with query param reason (backward compatibility)")
    void testCancelBookingAndTrackRefund() throws Exception {
        // 1. Cancel the booking
        mockMvc.perform(post("/api/bookings/" + testBooking.getBookingReference() + "/cancel")
                        .header("Authorization", "Bearer " + userToken)
                        .param("reason", "Change of plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // 2. Verify refund is created
        String refundsResponse = mockMvc.perform(get("/api/refunds")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode refundsArray = objectMapper.readTree(refundsResponse);
        assertTrue(refundsArray.size() > 0);

        JsonNode refundNode = refundsArray.get(0);
        assertEquals(5000.0, refundNode.get("refundAmount").asDouble());
        assertEquals("PENDING", refundNode.get("status").asText());
    }

    @Test
    @DisplayName("Cancel booking with structured JSON body and optional comment")
    void testCancelBookingWithJsonBodyAndComment() throws Exception {
        CancelBookingRequest request = new CancelBookingRequest("FOUND_BETTER_PRICE", "Found flight cheaper on competitor");

        mockMvc.perform(post("/api/bookings/" + testBooking.getBookingReference() + "/cancel")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("FOUND_BETTER_PRICE"));

        // Verify refund comment persisted
        Refund refund = refundRepository.findByBookingId(testBooking.getId()).get(0);
        assertEquals("FOUND_BETTER_PRICE", refund.getCancellationReason());
        assertEquals("Found flight cheaper on competitor", refund.getCancellationComment());
    }

    @Test
    @DisplayName("Reject invalid/unrecognized cancellation reason")
    void testRejectInvalidCancellationReason() throws Exception {
        CancelBookingRequest request = new CancelBookingRequest("INVALID_JUNK_REASON_12345", "None");

        mockMvc.perform(post("/api/bookings/" + testBooking.getBookingReference() + "/cancel")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Prevent double cancellation of already cancelled booking")
    void testPreventDoubleCancellation() throws Exception {
        // First cancellation succeeds
        mockMvc.perform(post("/api/bookings/" + testBooking.getBookingReference() + "/cancel")
                        .header("Authorization", "Bearer " + userToken)
                        .param("reason", "CHANGE_OF_PLANS"))
                .andExpect(status().isOk());

        // Second cancellation attempt must fail
        mockMvc.perform(post("/api/bookings/" + testBooking.getBookingReference() + "/cancel")
                        .header("Authorization", "Bearer " + userToken)
                        .param("reason", "CHANGE_OF_PLANS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("IDOR protection: User B cannot cancel User A's booking")
    void testIdorProtectionUnauthorizedUserCannotCancel() throws Exception {
        mockMvc.perform(post("/api/bookings/" + testBooking.getBookingReference() + "/cancel")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .param("reason", "CHANGE_OF_PLANS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("24-Hour Reservation Rule: 50% refund when travel policy gives zero refund")
    void testReservationWithin24HoursGuarantee() throws Exception {
        // Setup flight departing in 5 hours (policy gives 0%)
        Flight lastMinuteFlight = flightRepository.findAll().get(0);
        lastMinuteFlight.setDepartureTime(LocalDateTime.now().plusHours(5));
        flightRepository.save(lastMinuteFlight);

        Booking lastMinuteBooking = new Booking();
        lastMinuteBooking.setBookingReference(Booking.generateReference());
        lastMinuteBooking.setUser(testUser);
        lastMinuteBooking.setFlight(lastMinuteFlight);
        lastMinuteBooking.setBookingType("FLIGHT");
        lastMinuteBooking.setCabinClass("ECONOMY");
        lastMinuteBooking.setPassengerCount(1);
        lastMinuteBooking.setStatus("CONFIRMED");
        lastMinuteBooking.setTotalAmount(new BigDecimal("10000.00"));
        lastMinuteBooking.setTravelDate(lastMinuteFlight.getDepartureTime());
        // Booking created 2 hours ago (within 24 hours of reservation)
        lastMinuteBooking.setCreatedAt(LocalDateTime.now().minusHours(2));
        lastMinuteBooking = bookingRepository.save(lastMinuteBooking);

        // Policy for < 24 hours gives 0% refund
        policyRepository.deleteAll();
        policyRepository.save(new CancellationPolicy("FLIGHT", "Less than 24 hours", 0, 24, BigDecimal.ZERO, BigDecimal.ZERO));

        // Preview should guarantee 50% refund due to 24h reservation rule
        mockMvc.perform(get("/api/refunds/preview/" + lastMinuteBooking.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundAmount").value(5000.0))
                .andExpect(jsonPath("$.refundPercentage").value(50.0))
                .andExpect(jsonPath("$.policyName").value("24-Hour Reservation Guarantee (50% refund)"));
    }

    @Test
    @DisplayName("Admin cancellation analytics endpoint returns reason and refund breakdown")
    void testAdminCancellationAnalytics() throws Exception {
        // Cancel a booking to produce analytics data
        mockMvc.perform(post("/api/bookings/" + testBooking.getBookingReference() + "/cancel")
                        .header("Authorization", "Bearer " + userToken)
                        .param("reason", "MEDICAL_EMERGENCY"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/cancellations/analytics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCancellations").isNumber())
                .andExpect(jsonPath("$.data.reasonDistribution.MEDICAL_EMERGENCY").value(1));
    }
}
