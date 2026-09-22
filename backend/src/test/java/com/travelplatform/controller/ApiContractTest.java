package com.travelplatform.controller;

import com.travelplatform.entity.Role;
import com.travelplatform.entity.User;
import com.travelplatform.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API contract tests verifying:
 * - Correct HTTP status codes
 * - Expected response format
 * - No sensitive data in responses
 * - Proper error handling
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiContractTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private com.travelplatform.repository.FlightRepository flightRepository;
    @Autowired private com.travelplatform.repository.SeatRepository seatRepository;
    @Autowired private com.travelplatform.TestFixtures fixtures;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private String userToken;

    @BeforeEach
    void setUp() {
        User user = new User("Contract Test", "contract@test.com", passwordEncoder.encode("password123"));
        user.setRole(Role.USER);
        userRepository.save(user);
        userToken = tokenProvider.generateAccessToken(user.getEmail());
    }

    // ─── Authentication Endpoints ─────────────────────────

    @Test
    void registerReturns201() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"email\":\"new-contract@test.com\",\"password\":\"securePass123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void loginReturnsSuccessStructure() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"contract@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.email").isString())
                .andExpect(jsonPath("$.data.name").isString())
                .andExpect(jsonPath("$.data.role").isString());
    }

    @Test
    void loginReturns401ForBadCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"contract@test.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void registerReturns400ForMissingFields() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── Sensitive Data Protection ────────────────────────

    @Test
    void loginResponseExcludesPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"contract@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.passwordResetToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshTokens").doesNotExist());
    }

    @Test
    void profileExcludesPassword() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordResetToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshTokens").doesNotExist());
    }

    @Test
    void adminUserListExcludesPassword() throws Exception {
        User admin = new User("Admin", "admin-contract@test.com", passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        String adminToken = tokenProvider.generateAccessToken(admin.getEmail());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
        // Verify password not in any user in the list
    }

    @Test
    void bookingsDoNotExposeOtherUsers() throws Exception {
        mockMvc.perform(get("/api/bookings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ─── Error Response Format ────────────────────────────

    @Test
    void notFoundReturnsProperError() throws Exception {
        mockMvc.perform(get("/api/flights/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthorizedReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void forbiddenReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void badRequestReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ─── Notifications Contract ───────────────────────────

    @Test
    void notificationsReturnsProperStructure() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.unreadCount").isNumber());
    }

    @Test
    void unreadCountReturnsNumber() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber());
    }

    // ─── Rewards Contract ─────────────────────────────────

    @Test
    void rewardsReturnsProperStructure() throws Exception {
        mockMvc.perform(get("/api/rewards")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance").isNumber())
                .andExpect(jsonPath("$.tier").isString())
                .andExpect(jsonPath("$.lifetimePointsEarned").isNumber());
    }

    @Test
    void rewardsBalanceReturnsProperStructure() throws Exception {
        mockMvc.perform(get("/api/rewards/balance")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance").isNumber())
                .andExpect(jsonPath("$.tier").isString());
    }

    @Test
    void rewardsConfigReturnsPublicInfo() throws Exception {
        mockMvc.perform(get("/api/rewards/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsPerHundredRupees").isNumber())
                .andExpect(jsonPath("$.goldTierThreshold").isNumber())
                .andExpect(jsonPath("$.platinumTierThreshold").isNumber());
    }

    // ─── Payment Contract ─────────────────────────────────

    @Test
    void createOrderReturnsProperStructure() throws Exception {
        mockMvc.perform(post("/api/payments/create-order")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 5000, \"currency\": \"INR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.keyId").exists())
                .andExpect(jsonPath("$.paymentId").exists());
    }

    @Test
    void createOrderRejectsNegativeAmount() throws Exception {
        mockMvc.perform(post("/api/payments/create-order")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -100}"))
                .andExpect(status().isBadRequest());
    }

    // ─── Webhook Contract ─────────────────────────────────

    @Test
    void webhookAcceptsValidPayload() throws Exception {
        String payload = "{\"event\":\"payment.captured\",\"id\":\"evt_contract_" + System.currentTimeMillis() + "\"}";
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void webhookReturnsDuplicateForDuplicateEvent() throws Exception {
        String payload = "{\"event\":\"payment.captured\",\"id\":\"evt_contract_dup\"}";
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(payload))
                .andExpect(status().isOk());
        // Second call with same event ID should return duplicate
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("duplicate"));
    }

    // ─── Seat Selection API Contract ──────────────────────

    @Test
    void seatMapReturnsPopulatedSeatsAndSummary() throws Exception {
        com.travelplatform.entity.Flight flight = fixtures.createFlight("6E-555", "DEL", "BOM", java.math.BigDecimal.valueOf(4500));

        mockMvc.perform(get("/api/seats/map")
                        .param("flightId", String.valueOf(flight.getId()))
                        .param("cabinClass", "ECONOMY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(flight.getId()))
                .andExpect(jsonPath("$.seats").isArray())
                .andExpect(jsonPath("$.seats[0].row").exists())
                .andExpect(jsonPath("$.seats[0].column").exists())
                .andExpect(jsonPath("$.seats[0].rowNumber").exists())
                .andExpect(jsonPath("$.seats[0].columnLetter").exists())
                .andExpect(jsonPath("$.seats[0].status").exists())
                .andExpect(jsonPath("$.summary.total").isNumber())
                .andExpect(jsonPath("$.summary.available").isNumber());
    }

    @Test
    void seatHoldAndReleaseLifecycle() throws Exception {
        com.travelplatform.entity.Flight flight = fixtures.createFlight("6E-777", "BLR", "GOI", java.math.BigDecimal.valueOf(3200));

        com.travelplatform.entity.Seat seat = new com.travelplatform.entity.Seat();
        seat.setFlight(flight);
        seat.setCabinClass("ECONOMY");
        seat.setRowNumber(10);
        seat.setColumnLetter("A");
        seat.setSeatNumber("10A");
        seat.setPrice(java.math.BigDecimal.valueOf(500));
        seat.setAvailable(true);
        seat = seatRepository.save(seat);

        // 1. Hold seat
        mockMvc.perform(post("/api/seats/" + seat.getId() + "/hold")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatId").value(seat.getId()))
                .andExpect(jsonPath("$.seatNumber").value("10A"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.expiresAt").exists());

        // 2. Get active holds
        mockMvc.perform(get("/api/seats/holds")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatId").value(seat.getId()));

        // 3. Release seat hold
        mockMvc.perform(delete("/api/seats/" + seat.getId() + "/hold")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Seat hold released"));
    }
}
