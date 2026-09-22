package com.travelplatform.controller;

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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecommendationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepo;
    @Autowired private HotelRepository hotelRepo;
    @Autowired private RecommendationRepository recRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private User userA, userB;
    private String tokenA, tokenB;

    @BeforeEach
    void setUp() {
        userA = new User("User A", "reca-" + UUID.randomUUID() + "@test.com", passwordEncoder.encode("pass123"));
        userA.setRole(Role.USER);
        userA = userRepo.save(userA);

        userB = new User("User B", "recb-" + UUID.randomUUID() + "@test.com", passwordEncoder.encode("pass123"));
        userB.setRole(Role.USER);
        userB = userRepo.save(userB);

        tokenA = tokenProvider.generateAccessToken(userA.getEmail());
        tokenB = tokenProvider.generateAccessToken(userB.getEmail());
    }

    // ─── GET Recommendations ────────────────────────────────

    @Test
    @DisplayName("GET /api/recommendations — authenticated user receives recommendations")
    void authenticatedUserGetsRecommendations() throws Exception {
        // Seed a recommendation
        Hotel hotel = new Hotel();
        hotel.setName("Recommendation Test Hotel");
        hotel.setCity("Goa");
        hotel.setStarRating(4);
        hotel = hotelRepo.save(hotel);

        Recommendation rec = new Recommendation();
        rec.setUser(userA);
        rec.setEntityType("HOTEL");
        rec.setEntityId(hotel.getId());
        rec.setScore(BigDecimal.valueOf(75));
        rec.setAlgorithm("CONTENT_BASED");
        rec.setReason("Matches your beach preferences");
        recRepo.save(rec);

        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].entityType").value("HOTEL"))
                .andExpect(jsonPath("$[0].reason").value("Matches your beach preferences"));
    }

    @Test
    @DisplayName("GET /api/recommendations — cold-start user returns empty (no personalization)")
    void coldStartUserReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/recommendations — filters by entityType")
    void filterByEntityType() throws Exception {
        Hotel hotel = new Hotel(); hotel.setName("H1"); hotel.setCity("Goa"); hotel.setStarRating(4);
        hotel = hotelRepo.save(hotel);

        Recommendation rec = new Recommendation();
        rec.setUser(userA); rec.setEntityType("HOTEL"); rec.setEntityId(hotel.getId());
        rec.setScore(BigDecimal.valueOf(80)); rec.setAlgorithm("CONTENT_BASED");
        rec.setReason("Beach hotel"); recRepo.save(rec);

        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("entityType", "HOTEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entityType").value("HOTEL"));
    }

    @Test
    @DisplayName("GET /api/recommendations — unauthenticated access returns empty list")
    void unauthenticatedAccessReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ─── CROSS-USER SECURITY ────────────────────────────────

    @Test
    @DisplayName("GET /api/recommendations — User B cannot see User A's recommendations via userId param")
    void crossUserAccessDenied() throws Exception {
        Hotel hotel = new Hotel(); hotel.setName("Private Hotel"); hotel.setCity("Goa"); hotel.setStarRating(4);
        hotel = hotelRepo.save(hotel);

        Recommendation rec = new Recommendation();
        rec.setUser(userA); rec.setEntityType("HOTEL"); rec.setEntityId(hotel.getId());
        rec.setScore(BigDecimal.valueOf(90)); rec.setAlgorithm("CONTENT_BASED");
        rec.setReason("Personal recommendation for A"); recRepo.save(rec);

        // User B tries to access User A's recommendations — should get only their own and not User A's private rec
        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.reason == 'Personal recommendation for A')]").doesNotExist());
    }

    // ─── FEEDBACK ───────────────────────────────────────────

    @Test
    @DisplayName("POST /api/recommendations/{id}/feedback — submit helpful feedback")
    void submitHelpfulFeedback() throws Exception {
        Hotel hotel = new Hotel(); hotel.setName("Feedback Hotel"); hotel.setCity("Goa"); hotel.setStarRating(3);
        hotel = hotelRepo.save(hotel);

        Recommendation rec = new Recommendation();
        rec.setUser(userA); rec.setEntityType("HOTEL"); rec.setEntityId(hotel.getId());
        rec.setScore(BigDecimal.valueOf(70)); rec.setAlgorithm("CONTENT_BASED");
        rec.setReason("Budget match"); recRepo.save(rec);

        mockMvc.perform(post("/api/recommendations/" + rec.getId() + "/feedback")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"HELPFUL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Feedback recorded")));

        Recommendation updated = recRepo.findById(rec.getId()).orElseThrow();
        assert "HELPFUL".equals(updated.getFeedback());
    }

    @Test
    @DisplayName("POST /api/recommendations/{id}/feedback — submit NOT_HELPFUL feedback")
    void submitNotHelpfulFeedback() throws Exception {
        Hotel hotel = new Hotel(); hotel.setName("Feedback2 Hotel"); hotel.setCity("Jaipur"); hotel.setStarRating(4);
        hotel = hotelRepo.save(hotel);

        Recommendation rec = new Recommendation();
        rec.setUser(userA); rec.setEntityType("HOTEL"); rec.setEntityId(hotel.getId());
        rec.setScore(BigDecimal.valueOf(65)); rec.setAlgorithm("HYBRID");
        rec.setReason("Similar users liked it"); recRepo.save(rec);

        mockMvc.perform(post("/api/recommendations/" + rec.getId() + "/feedback")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"NOT_HELPFUL\"}"))
                .andExpect(status().isOk());

        Recommendation updated = recRepo.findById(rec.getId()).orElseThrow();
        assert "IRRELEVANT".equals(updated.getFeedback());
    }

    @Test
    @DisplayName("POST /api/recommendations/{id}/feedback — invalid feedback value rejected")
    void invalidFeedbackRejected() throws Exception {
        Hotel hotel = new Hotel(); hotel.setName("Inv Hotel"); hotel.setCity("Delhi"); hotel.setStarRating(4);
        hotel = hotelRepo.save(hotel);

        Recommendation rec = new Recommendation();
        rec.setUser(userA); rec.setEntityType("HOTEL"); rec.setEntityId(hotel.getId());
        rec.setScore(BigDecimal.valueOf(60)); rec.setAlgorithm("CONTENT_BASED");
        rec.setReason("Some reason"); recRepo.save(rec);

        mockMvc.perform(post("/api/recommendations/" + rec.getId() + "/feedback")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/recommendations/{id}/feedback — non-owner cannot give feedback")
    void nonOwnerCannotGiveFeedback() throws Exception {
        Hotel hotel = new Hotel(); hotel.setName("Owner Hotel"); hotel.setCity("Mumbai"); hotel.setStarRating(4);
        hotel = hotelRepo.save(hotel);

        Recommendation rec = new Recommendation();
        rec.setUser(userA); rec.setEntityType("HOTEL"); rec.setEntityId(hotel.getId());
        rec.setScore(BigDecimal.valueOf(70)); rec.setAlgorithm("CONTENT_BASED");
        rec.setReason("Personal for A"); recRepo.save(rec);

        mockMvc.perform(post("/api/recommendations/" + rec.getId() + "/feedback")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"HELPFUL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/recommendations/{id}/feedback — nonexistent recommendation returns 404")
    void nonexistentRecommendationReturns404() throws Exception {
        mockMvc.perform(post("/api/recommendations/99999/feedback")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"HELPFUL\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/recommendations/{id}/feedback — unauthenticated rejected")
    void unauthenticatedFeedbackRejected() throws Exception {
        mockMvc.perform(post("/api/recommendations/1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"HELPFUL\"}"))
                .andExpect(status().isForbidden());
    }

    // ─── RESPONSE STRUCTURE ─────────────────────────────────

    @Test
    @DisplayName("Recommendation response includes algorithm, score, and reason")
    void responseHasRequiredFields() throws Exception {
        Hotel hotel = new Hotel(); hotel.setName("Structure Hotel"); hotel.setCity("Bangalore"); hotel.setStarRating(3);
        hotel = hotelRepo.save(hotel);

        Recommendation rec = new Recommendation();
        rec.setUser(userA); rec.setEntityType("HOTEL"); rec.setEntityId(hotel.getId());
        rec.setScore(BigDecimal.valueOf(82.5)); rec.setAlgorithm("HYBRID");
        rec.setReason("Combines your preferences with similar users");
        recRepo.save(rec);

        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].algorithm").value("HYBRID"))
                .andExpect(jsonPath("$[0].score").value(82.5))
                .andExpect(jsonPath("$[0].reason").value("Combines your preferences with similar users"));
    }

    @Test
    @DisplayName("Recommendation response does not expose batchCycle or internal fields")
    void responseNoInternalFields() throws Exception {
        Hotel hotel = new Hotel(); hotel.setName("Internal Hotel"); hotel.setCity("Chennai"); hotel.setStarRating(4);
        hotel = hotelRepo.save(hotel);

        Recommendation rec = new Recommendation();
        rec.setUser(userA); rec.setEntityType("HOTEL"); rec.setEntityId(hotel.getId());
        rec.setScore(BigDecimal.valueOf(70)); rec.setAlgorithm("CONTENT_BASED");
        rec.setReason("Beach"); recRepo.save(rec);

        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchCycle").doesNotExist());
    }
}
