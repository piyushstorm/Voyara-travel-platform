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
 * Comprehensive admin management tests.
 * Covers dashboard, CRUD operations, analytics, and admin security.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminManagementTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private FlightRepository flightRepository;
    @Autowired private AirlineRepository airlineRepository;
    @Autowired private AirportRepository airportRepository;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private User admin, user;
    private String adminToken, userToken;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail("admin-mgmt@test.com").orElseGet(() -> {
            User u = new User("Admin", "admin-mgmt@test.com", passwordEncoder.encode("admin123"));
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        admin = userRepository.save(admin);
        adminToken = tokenProvider.generateAccessToken(admin.getEmail());

        user = userRepository.findByEmail("user-mgmt@test.com").orElseGet(() -> {
            User u = new User("User", "user-mgmt@test.com", passwordEncoder.encode("user123"));
            u.setRole(Role.USER);
            return userRepository.save(u);
        });
        user.setRole(Role.USER);
        user.setEnabled(true);
        user = userRepository.save(user);
        userToken = tokenProvider.generateAccessToken(user.getEmail());
    }

    // ─── Dashboard ────────────────────────────────────────

    @Test
    void adminCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalBookings").isNumber())
                .andExpect(jsonPath("$.data.totalRevenue").exists());
    }

    @Test
    void userCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─── User Management ──────────────────────────────────

    @Test
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void adminCanGetUserDetail() throws Exception {
        mockMvc.perform(get("/api/admin/users/" + user.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("User"));
    }

    @Test
    void adminCanToggleUser() throws Exception {
        User toggleTarget = new User("Toggle Target", "toggle-target-" + System.nanoTime() + "@test.com", passwordEncoder.encode("user123"));
        toggleTarget.setRole(Role.USER);
        toggleTarget.setEnabled(true);
        toggleTarget = userRepository.save(toggleTarget);

        mockMvc.perform(put("/api/admin/users/" + toggleTarget.getId() + "/toggle")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void adminCannotDisableSelf() throws Exception {
        mockMvc.perform(put("/api/admin/users/" + admin.getId() + "/toggle")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    // ─── Role Management ──────────────────────────────────

    @Test
    void adminCanPromoteUser() throws Exception {
        User promoteTarget = new User("Promote Target", "promote-target-" + System.nanoTime() + "@test.com", passwordEncoder.encode("user123"));
        promoteTarget.setRole(Role.USER);
        promoteTarget = userRepository.save(promoteTarget);

        mockMvc.perform(put("/api/admin/users/" + promoteTarget.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void adminCanDemoteAdmin() throws Exception {
        // Create second admin
        User secondAdmin = new User("Second Admin", "secondadmin-" + System.nanoTime() + "@test.com", passwordEncoder.encode("admin123"));
        secondAdmin.setRole(Role.ADMIN);
        secondAdmin = userRepository.save(secondAdmin);

        mockMvc.perform(put("/api/admin/users/" + secondAdmin.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void adminCannotChangeOwnRole() throws Exception {
        mockMvc.perform(put("/api/admin/users/" + admin.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidRoleRejected() throws Exception {
        mockMvc.perform(put("/api/admin/users/" + user.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPERADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    // ─── Flight Management ────────────────────────────────

    @Test
    void adminCanListFlights() throws Exception {
        mockMvc.perform(get("/api/admin/flights")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCreateFlight() throws Exception {
        Airline airline = airlineRepository.findAll().stream().findFirst().orElseGet(() -> {
            Airline a = new Airline();
            a.setName("Test Airlines");
            a.setCode("TA");
            return airlineRepository.save(a);
        });

        Airport origin = airportRepository.findByCode("DEL").orElseGet(() -> {
            Airport ap = new Airport(); ap.setCode("DEL"); ap.setName("Delhi Airport"); ap.setCity("Delhi"); ap.setCountry("India"); return airportRepository.save(ap);
        });

        Airport dest = airportRepository.findByCode("BOM").orElseGet(() -> {
            Airport ap = new Airport(); ap.setCode("BOM"); ap.setName("Mumbai Airport"); ap.setCity("Mumbai"); ap.setCountry("India"); return airportRepository.save(ap);
        });

        mockMvc.perform(post("/api/admin/flights")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"flightNumber\":\"TA-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase() + "\"," +
                                "\"airline\":{\"id\":" + airline.getId() + "}," +
                                "\"origin\":{\"id\":" + origin.getId() + "}," +
                                "\"destination\":{\"id\":" + dest.getId() + "}," +
                                "\"originCode\":\"DEL\",\"destinationCode\":\"BOM\"," +
                                "\"departureTime\":\"" + LocalDateTime.now().plusDays(7) + "\"," +
                                "\"arrivalTime\":\"" + LocalDateTime.now().plusDays(7).plusHours(2) + "\"," +
                                "\"departureDate\":\"" + java.time.LocalDate.now().plusDays(7) + "\"," +
                                "\"durationMinutes\":120,\"stops\":0," +
                                "\"economyPrice\":5000,\"premiumEconomyPrice\":7500," +
                                "\"businessPrice\":12000,\"firstClassPrice\":25000," +
                                "\"totalSeatsEconomy\":100,\"totalSeatsPremiumEconomy\":40," +
                                "\"totalSeatsBusiness\":30,\"totalSeatsFirst\":10," +
                                "\"active\":true" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void userCannotCreateFlight() throws Exception {
        mockMvc.perform(post("/api/admin/flights")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightNumber\":\"TA-X\",\"originCode\":\"DEL\",\"destinationCode\":\"BOM\"}"))
                .andExpect(status().isForbidden());
    }

    // ─── Hotel Management ─────────────────────────────────

    @Test
    void adminCanListHotels() throws Exception {
        mockMvc.perform(get("/api/admin/hotels")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCreateHotel() throws Exception {
        mockMvc.perform(post("/api/admin/hotels")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"name\":\"Admin Hotel\"," +
                                "\"city\":\"Mumbai\"," +
                                "\"description\":\"Test hotel\"," +
                                "\"starRating\":4," +
                                "\"active\":true" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─── Booking Management ───────────────────────────────

    @Test
    void adminCanListBookings() throws Exception {
        mockMvc.perform(get("/api/admin/bookings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanFilterBookingsByStatus() throws Exception {
        mockMvc.perform(get("/api/admin/bookings")
                        .param("status", "CONFIRMED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ─── Analytics ────────────────────────────────────────

    @Test
    void adminCanAccessAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBookings").isNumber())
                .andExpect(jsonPath("$.data.totalRevenue").exists());
    }

    @Test
    void adminCanAccessAnalyticsSummary() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").exists())
                .andExpect(jsonPath("$.data.totalBookings").isNumber());
    }

    // ─── Audit Logs ───────────────────────────────────────

    @Test
    void adminCanAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ─── Global Search ────────────────────────────────────

    @Test
    void adminCanSearchGlobally() throws Exception {
        mockMvc.perform(get("/api/admin/search")
                        .param("q", "test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.users").isArray())
                .andExpect(jsonPath("$.data.bookings").isArray())
                .andExpect(jsonPath("$.data.flights").isArray())
                .andExpect(jsonPath("$.data.hotels").isArray());
    }

    // ─── Payments & Refunds ───────────────────────────────

    @Test
    void adminCanAccessPayments() throws Exception {
        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanAccessRefunds() throws Exception {
        mockMvc.perform(get("/api/admin/refunds")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ─── Coupon Management ────────────────────────────────

    @Test
    void adminCanAccessCoupons() throws Exception {
        mockMvc.perform(get("/api/admin/coupons")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ─── Pagination ───────────────────────────────────────

    @Test
    void paginationWorksForUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
