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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive security and authorization tests.
 * Tests user isolation, admin access control, and sensitive data protection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private User userA, userB, admin;
    private String tokenA, tokenB, adminToken;

    @BeforeEach
    void setUp() {
        userA = new User("User A", "usera-security@test.com", passwordEncoder.encode("pass123"));
        userA.setRole(Role.USER);
        userA = userRepository.save(userA);

        userB = new User("User B", "userb-security@test.com", passwordEncoder.encode("pass123"));
        userB.setRole(Role.USER);
        userB = userRepository.save(userB);

        admin = new User("Admin", "admin-security@test.com", passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        admin = userRepository.save(admin);

        tokenA = tokenProvider.generateAccessToken(userA.getEmail());
        tokenB = tokenProvider.generateAccessToken(userB.getEmail());
        adminToken = tokenProvider.generateAccessToken(admin.getEmail());
    }

    // ─── Unauthenticated Access ───────────────────────────

    @Test
    void unauthenticatedUserCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessBookings() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessProfile() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessRewards() throws Exception {
        mockMvc.perform(get("/api/rewards"))
                .andExpect(status().isForbidden());
    }

    // ─── USER → Admin APIs = 403 ─────────────────────────

    @Test
    void userCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAdminBookings() throws Exception {
        mockMvc.perform(get("/api/admin/bookings")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAdminFlights() throws Exception {
        mockMvc.perform(get("/api/admin/flights")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAdminHotels() throws Exception {
        mockMvc.perform(get("/api/admin/hotels")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAdminPayments() throws Exception {
        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAdminRefunds() throws Exception {
        mockMvc.perform(get("/api/admin/refunds")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAdminAuditLogs() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAdminCoupons() throws Exception {
        mockMvc.perform(get("/api/admin/coupons")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAdminRewardsConfig() throws Exception {
        mockMvc.perform(put("/api/rewards/admin/config")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ─── ADMIN → Admin APIs = Allowed ─────────────────────

    @Test
    void adminCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanAccessUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanAccessBookings() throws Exception {
        mockMvc.perform(get("/api/admin/bookings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ─── User Data Isolation ──────────────────────────────

    @Test
    void userCanAccessOwnBookings() throws Exception {
        // User A creates a booking
        Booking booking = new Booking();
        booking.setBookingReference("TP-SEC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        booking.setUser(userA);
        booking.setBookingType("FLIGHT");
        booking.setStatus("CONFIRMED");
        booking.setTotalAmount(new BigDecimal("5000.00"));
        booking.setPassengerCount(1);
        bookingRepository.save(booking);

        // User A can see their booking
        mockMvc.perform(get("/api/bookings")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    void userCanAccessOwnProfile() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("usera-security@test.com"));
    }

    @Test
    void userCanAccessOwnNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    void userCanAccessOwnRewards() throws Exception {
        mockMvc.perform(get("/api/rewards")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    void userCanAccessOwnNotificationPreferences() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    // ─── Sensitive Field Protection ───────────────────────

    @Test
    void profileDoesNotExposePassword() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordResetToken").doesNotExist());
    }

    @Test
    void userRegistrationDoesNotReturnPassword() throws Exception {
        String email = "newuser-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"email\":\"" + email + "\",\"password\":\"securePass123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void loginDoesNotReturnPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"usera-security@test.com\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    // ─── Admin Role Management Security ───────────────────

    @Test
    void userCannotPromoteThemselves() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/role", userA.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanPromoteUser() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/role", userA.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void adminCannotChangeOwnRole() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/role", admin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest());
    }

    // ─── Payment Security ─────────────────────────────────

    @Test
    void createOrderRequiresPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/payments/create-order")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -500}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrderRequiresZeroOrPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/payments/create-order")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyRequiresBothOrderIdAndPaymentId() throws Exception {
        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"order123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedCannotCreatePayment() throws Exception {
        mockMvc.perform(post("/api/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotVerifyPayment() throws Exception {
        mockMvc.perform(post("/api/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"x\",\"paymentId\":\"y\",\"signature\":\"z\"}"))
                .andExpect(status().isForbidden());
    }

    // ─── Registration Security ────────────────────────────

    @Test
    void duplicateEmailRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"email\":\"usera-security@test.com\",\"password\":\"securePass123!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void weakPasswordRejected() throws Exception {
        String email = "weak-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"email\":\"" + email + "\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingFieldsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\"}"))
                .andExpect(status().isBadRequest());
    }

    // ─── Webhook Security ─────────────────────────────────

    @Test
    void webhookEndpointIsPublic() throws Exception {
        // Webhook should be accessible without auth (for Razorpay to call)
        String payload = "{\"event\":\"payment.captured\",\"id\":\"evt_test_" + System.currentTimeMillis() + "\"}";
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(payload))
                .andExpect(status().isOk());
    }
}
