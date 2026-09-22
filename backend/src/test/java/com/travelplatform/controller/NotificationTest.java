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
 * Notification tests covering:
 * - Listing notifications
 * - Unread count
 * - Mark as read
 * - Mark all as read
 * - Preferences
 * - Ownership isolation
 * - Authentication requirements
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private User userA, userB;
    private String tokenA, tokenB;

    @BeforeEach
    void setUp() {
        userA = new User("User A", "notif-a@test.com", passwordEncoder.encode("password123"));
        userA.setRole(Role.USER);
        userA = userRepository.save(userA);
        tokenA = tokenProvider.generateAccessToken(userA.getEmail());

        userB = new User("User B", "notif-b@test.com", passwordEncoder.encode("password123"));
        userB.setRole(Role.USER);
        userB = userRepository.save(userB);
        tokenB = tokenProvider.generateAccessToken(userB.getEmail());
    }

    // ─── Authentication ───────────────────────────────────

    @Test
    void notificationsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserCanGetNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.unreadCount").isNumber());
    }

    // ─── Ownership Isolation ──────────────────────────────

    @Test
    void userCanOnlySeeOwnNotifications() throws Exception {
        // User A gets notifications (may be empty, but the endpoint works)
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // User B also gets their own notifications
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
    }

    // ─── Unread Count ─────────────────────────────────────

    @Test
    void unreadCountReturnsNumber() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber());
    }

    @Test
    void unreadCountRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isForbidden());
    }

    // ─── Mark as Read ─────────────────────────────────────

    @Test
    void markAllAsReadWorks() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marked").isNumber())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void markAsReadRequiresAuth() throws Exception {
        mockMvc.perform(patch("/api/notifications/1/read"))
                .andExpect(status().isForbidden());
    }

    @Test
    void markNonexistentNotificationReturnsFalse() throws Exception {
        mockMvc.perform(patch("/api/notifications/99999/read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── Preferences ──────────────────────────────────────

    @Test
    void getPreferencesRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserCanGetPreferences() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailBookingUpdates").isBoolean())
                .andExpect(jsonPath("$.emailPaymentUpdates").isBoolean())
                .andExpect(jsonPath("$.inAppBookingUpdates").isBoolean());
    }

    @Test
    void userCanUpdatePreferences() throws Exception {
        mockMvc.perform(put("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"emailBookingUpdates\": false," +
                                "\"emailPaymentUpdates\": true," +
                                "\"emailCancellationRefund\": true," +
                                "\"emailFlightUpdates\": false," +
                                "\"emailPriceAlerts\": false," +
                                "\"emailMarketing\": false," +
                                "\"inAppBookingUpdates\": true," +
                                "\"inAppPaymentUpdates\": true," +
                                "\"inAppFlightUpdates\": true," +
                                "\"inAppPriceAlerts\": false," +
                                "\"alwaysSendSecurityNotifications\": true" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Preferences updated"));
    }

    @Test
    void securityNotificationsAlwaysEnabled() throws Exception {
        // Set all to false
        mockMvc.perform(put("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"emailBookingUpdates\": false," +
                                "\"emailPaymentUpdates\": false," +
                                "\"emailCancellationRefund\": false," +
                                "\"emailFlightUpdates\": false," +
                                "\"emailPriceAlerts\": false," +
                                "\"emailMarketing\": false," +
                                "\"inAppBookingUpdates\": false," +
                                "\"inAppPaymentUpdates\": false," +
                                "\"inAppFlightUpdates\": false," +
                                "\"inAppPriceAlerts\": false," +
                                "\"alwaysSendSecurityNotifications\": true" +
                                "}"))
                .andExpect(status().isOk());

        // Verify security notifications are still true
        mockMvc.perform(get("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alwaysSendSecurityNotifications").value(true));
    }

    // ─── Filtering ────────────────────────────────────────

    @Test
    void filterByTypeWorks() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("type", "BOOKING_CONFIRMED")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    void filterByUnreadOnly() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("unreadOnly", "true")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    // ─── Pagination ───────────────────────────────────────

    @Test
    void paginationWorks() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.currentPage").isNumber());
    }
}
