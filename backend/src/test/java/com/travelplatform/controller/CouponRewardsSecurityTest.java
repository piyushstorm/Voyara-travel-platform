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
 * Tests for coupon security, rewards security, and price manipulation prevention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CouponRewardsSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private AirlineRepository airlineRepository;
    @Autowired private AirportRepository airportRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private User testUser, testAdmin;
    private String userToken, adminToken;
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        testUser = new User("Coupon Test", "coupon@test.com", passwordEncoder.encode("password123"));
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);
        userToken = tokenProvider.generateAccessToken(testUser.getEmail());

        testAdmin = new User("Coupon Admin", "coupon-admin@test.com", passwordEncoder.encode("admin123"));
        testAdmin.setRole(Role.ADMIN);
        testAdmin = userRepository.save(testAdmin);
        adminToken = tokenProvider.generateAccessToken(testAdmin.getEmail());

        testCoupon = new Coupon();
        testCoupon.setCode("TEST10");
        testCoupon.setDescription("10% off");
        testCoupon.setDiscountType("PERCENTAGE");
        testCoupon.setDiscountValue(new BigDecimal("10"));
        testCoupon.setMinBookingAmount(new BigDecimal("1000"));
        testCoupon.setApplicableModule("ALL");
        testCoupon.setActive(true);
        testCoupon.setStartDate(LocalDateTime.now().minusDays(1));
        testCoupon.setExpiryDate(LocalDateTime.now().plusDays(30));
        testCoupon.setUsageLimit(100);
        testCoupon.setPerUserLimit(2);
        testCoupon = couponRepository.save(testCoupon);
    }

    // ─── Coupon Validation Security ───────────────────────

    @Test
    void validateCouponRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST10\",\"amount\":5000,\"module\":\"FLIGHT\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validateValidCoupon() throws Exception {
        mockMvc.perform(post("/api/coupons/validate")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST10\",\"amount\":5000,\"module\":\"FLIGHT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.discountAmount").isNumber())
                .andExpect(jsonPath("$.finalAmount").isNumber());
    }

    @Test
    void validateExpiredCoupon() throws Exception {
        Coupon expired = new Coupon();
        expired.setCode("EXPIRED");
        expired.setDiscountType("PERCENTAGE");
        expired.setDiscountValue(new BigDecimal("10"));
        expired.setMinBookingAmount(new BigDecimal("1000"));
        expired.setApplicableModule("ALL");
        expired.setActive(true);
        expired.setStartDate(LocalDateTime.now().minusDays(30));
        expired.setExpiryDate(LocalDateTime.now().minusDays(1)); // Expired
        expired.setUsageLimit(100);
        expired.setPerUserLimit(2);
        couponRepository.save(expired);

        mockMvc.perform(post("/api/coupons/validate")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"EXPIRED\",\"amount\":5000,\"module\":\"FLIGHT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void validateInactiveCoupon() throws Exception {
        Coupon inactive = new Coupon();
        inactive.setCode("INACTIVE");
        inactive.setDiscountType("PERCENTAGE");
        inactive.setDiscountValue(new BigDecimal("10"));
        inactive.setMinBookingAmount(new BigDecimal("1000"));
        inactive.setApplicableModule("ALL");
        inactive.setActive(false);
        inactive.setStartDate(LocalDateTime.now().minusDays(1));
        inactive.setExpiryDate(LocalDateTime.now().plusDays(30));
        inactive.setUsageLimit(100);
        inactive.setPerUserLimit(2);
        couponRepository.save(inactive);

        mockMvc.perform(post("/api/coupons/validate")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"INACTIVE\",\"amount\":5000,\"module\":\"FLIGHT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void validateNonexistentCoupon() throws Exception {
        mockMvc.perform(post("/api/coupons/validate")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DOESNOTEXIST\",\"amount\":5000,\"module\":\"FLIGHT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void validateCouponBelowMinimumAmount() throws Exception {
        mockMvc.perform(post("/api/coupons/validate")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST10\",\"amount\":500,\"module\":\"FLIGHT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // ─── Admin Coupon Management Security ─────────────────

    @Test
    void userCannotAccessAdminCoupons() throws Exception {
        mockMvc.perform(get("/api/admin/coupons")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessCoupons() throws Exception {
        mockMvc.perform(get("/api/admin/coupons")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ─── Rewards Security ─────────────────────────────────

    @Test
    void rewardsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/rewards"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanAccessOwnRewards() throws Exception {
        mockMvc.perform(get("/api/rewards")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance").isNumber())
                .andExpect(jsonPath("$.tier").isString());
    }

    @Test
    void userCannotAccessAdminRewardsConfig() throws Exception {
        mockMvc.perform(put("/api/rewards/admin/config")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointsPerHundredRupees\": 100}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessRewardsConfig() throws Exception {
        mockMvc.perform(put("/api/rewards/admin/config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointsPerHundredRupees\": 1}"))
                .andExpect(status().isOk());
    }

    @Test
    void rewardsBalanceCannotBeManipulatedViaAPI() throws Exception {
        // There is no endpoint to directly set balance
        // Only admin adjustment and booking earning
        mockMvc.perform(put("/api/rewards/balance")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointsBalance\": 999999}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Endpoint should NOT exist - any error status is acceptable
                    assert status >= 400 : "Expected error status but got " + status;
                });
    }

    @Test
    void redemptionRequiresSufficientBalance() throws Exception {
        // User has 0 points, trying to redeem 100
        mockMvc.perform(post("/api/rewards/redeem/validate")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\": 100, \"amount\": 5000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // ─── Price Manipulation Prevention ────────────────────

    @Test
    void bookingCannotManuallySetPrice() throws Exception {
        // Create a flight first
        Airline airline = airlineRepository.findAll().stream().findFirst().orElse(null);
        if (airline == null) {
            airline = new Airline();
            airline.setName("Test Airline");
            airline.setCode("TA");
            airline = airlineRepository.save(airline);
        }

        Airport originAp = airportRepository.findByCode("DEL").orElseGet(() -> { Airport a = new Airport(); a.setCode("DEL"); a.setName("Delhi"); a.setCity("Delhi"); a.setCountry("India"); return airportRepository.save(a); });
        Airport destAp = airportRepository.findByCode("BOM").orElseGet(() -> { Airport a = new Airport(); a.setCode("BOM"); a.setName("Mumbai"); a.setCity("Mumbai"); a.setCountry("India"); return airportRepository.save(a); });

        Flight flight = new Flight();
        flight.setFlightNumber("SEC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        flight.setAirline(airline);
        flight.setOrigin(originAp);
        flight.setDestination(destAp);
        flight.setOriginCode("DEL");
        flight.setDestinationCode("BOM");
        flight.setDepartureDate(java.time.LocalDate.now().plusDays(7));
        flight.setDepartureTime(LocalDateTime.now().plusDays(7));
        flight.setArrivalTime(LocalDateTime.now().plusDays(7).plusHours(2));
        flight.setDurationMinutes(120);
        flight.setStops(0);
        flight.setEconomyBasePrice(new BigDecimal("5000.00"));
        flight.setEconomyPrice(new BigDecimal("5000.00"));
        flight.setPremiumEconomyPrice(new BigDecimal("7500.00"));
        flight.setBusinessPrice(new BigDecimal("12000.00"));
        flight.setFirstClassPrice(new BigDecimal("25000.00"));
        flight.setTotalSeatsEconomy(100);
        flight.setTotalSeatsPremiumEconomy(40);
        flight.setTotalSeatsBusiness(30);
        flight.setTotalSeatsFirst(10);
        flight.setBookedSeatsEconomy(5);
        flight.setActive(true);
        flight = flightRepository.save(flight);

        // The backend should calculate the price from the flight, not accept it from frontend
        // There is no "price" field in BookingRequest - this verifies backend controls pricing
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"bookingType\":\"FLIGHT\"," +
                                "\"flightId\":" + flight.getId() + "," +
                                "\"cabinClass\":\"ECONOMY\"," +
                                "\"passengerCount\":1," +
                                "\"paymentMethod\":\"CARD\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(5000));
    }

    // ─── Coupon Admin CRUD Security ───────────────────────

    @Test
    void userCannotCreateCoupon() throws Exception {
        mockMvc.perform(post("/api/admin/coupons")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NEWCOUPON\",\"discountType\":\"PERCENTAGE\",\"discountValue\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateCoupon() throws Exception {
        mockMvc.perform(post("/api/admin/coupons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ADMIN10\",\"discountType\":\"PERCENTAGE\",\"discountValue\":10," +
                                "\"minimumBookingAmount\":500,\"applicableModule\":\"ALL\"," +
                                "\"usageLimit\":100,\"perUserUsageLimit\":2," +
                                "\"startDate\":\"2024-01-01T00:00:00\",\"expiryDate\":\"2025-12-31T23:59:59\"}"))
                .andExpect(status().isOk());
    }
}
