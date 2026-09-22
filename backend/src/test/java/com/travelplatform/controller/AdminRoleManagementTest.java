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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminRoleManagementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String adminToken;
    private String userToken;
    private User testAdmin;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test admin
        testAdmin = new User("Admin Test", "admintest-role@test.com", passwordEncoder.encode("admin123"));
        testAdmin.setRole(Role.ADMIN);
        testAdmin = userRepository.save(testAdmin);

        // Create test user
        testUser = new User("User Test", "usertest-role@test.com", passwordEncoder.encode("user123"));
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);

        // Create tokens
        adminToken = tokenProvider.generateAccessToken(testAdmin.getEmail());
        userToken = tokenProvider.generateAccessToken(testUser.getEmail());
    }

    @Test
    void adminCanPromoteUserToAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/role", testUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void adminCanDemoteAdminToUser() throws Exception {
        // Create a second admin to demote
        User secondAdmin = new User("Second Admin", "secondadmin@test.com", passwordEncoder.encode("admin123"));
        secondAdmin.setRole(Role.ADMIN);
        secondAdmin = userRepository.save(secondAdmin);

        mockMvc.perform(put("/api/admin/users/{id}/role", secondAdmin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void unauthorizedUserCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/role", testUser.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void noTokenCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/role", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotChangeOwnRole() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/role", testAdmin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("cannot change your own role")));
    }

    @Test
    void lastAdminProtectionWorksWithSelfRoleCheck() throws Exception {
        // TestAdmin is the only admin (created in setUp)
        // Trying to demote self is blocked by self-role check first
        mockMvc.perform(put("/api/admin/users/{id}/role", testAdmin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // With 2 admins, one CAN demote the other (last-admin protection only triggers at count <= 1)
        User secondAdmin = new User("Second Admin 2", "secondadmin2@test.com", passwordEncoder.encode("admin123"));
        secondAdmin.setRole(Role.ADMIN);
        secondAdmin = userRepository.save(secondAdmin);

        String secondAdminToken = tokenProvider.generateAccessToken(secondAdmin.getEmail());

        // Second admin demotes testAdmin — should succeed since there are 2 admins
        mockMvc.perform(put("/api/admin/users/{id}/role", testAdmin.getId())
                        .header("Authorization", "Bearer " + secondAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void invalidRoleRejected() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/role", testUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPERADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void invalidUserIdReturns404() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/role", 99999L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isNotFound());
    }
}
