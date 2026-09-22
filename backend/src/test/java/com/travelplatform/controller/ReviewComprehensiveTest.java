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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewComprehensiveTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepo;
    @Autowired private FlightRepository flightRepo;
    @Autowired private HotelRepository hotelRepo;
    @Autowired private BookingRepository bookingRepo;
    @Autowired private ReviewRepository reviewRepo;
    @Autowired private ReviewReplyRepository replyRepo;
    @Autowired private ReviewVoteRepository voteRepo;
    @Autowired private ReviewPhotoRepository photoRepo;
    @Autowired private ReviewReportRepository reportRepo;
    @Autowired private ReviewModerationActionRepository actionRepo;
    @Autowired private AirlineRepository airlineRepo;
    @Autowired private AirportRepository airportRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private User user1, user2, adminUser;
    private String token1, token2, adminToken;
    private Flight flight;
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        user1 = new User("Alice Reviewer", "alice-" + UUID.randomUUID() + "@test.com", passwordEncoder.encode("pass123"));
        user1.setRole(Role.USER);
        user1 = userRepo.save(user1);

        user2 = new User("Bob Traveler", "bob-" + UUID.randomUUID() + "@test.com", passwordEncoder.encode("pass123"));
        user2.setRole(Role.USER);
        user2 = userRepo.save(user2);

        adminUser = new User("Admin Mod", "admin-" + UUID.randomUUID() + "@test.com", passwordEncoder.encode("admin123"));
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepo.save(adminUser);

        token1 = tokenProvider.generateAccessToken(user1.getEmail());
        token2 = tokenProvider.generateAccessToken(user2.getEmail());
        adminToken = tokenProvider.generateAccessToken(adminUser.getEmail());

        Airline airline = airlineRepo.findAll().stream().findFirst().orElseGet(() -> {
            Airline a = new Airline(); a.setName("Voyara Air"); a.setCode("VA"); return airlineRepo.save(a);
        });

        Airport del = airportRepo.findByCode("DEL").orElseGet(() -> {
            Airport a = new Airport(); a.setCode("DEL"); a.setName("Delhi"); a.setCity("Delhi"); a.setCountry("India"); return airportRepo.save(a);
        });
        Airport bom = airportRepo.findByCode("BOM").orElseGet(() -> {
            Airport a = new Airport(); a.setCode("BOM"); a.setName("Mumbai"); a.setCity("Mumbai"); a.setCountry("India"); return airportRepo.save(a);
        });

        flight = new Flight();
        flight.setFlightNumber("VY-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        flight.setAirline(airline);
        flight.setOrigin(del);
        flight.setDestination(bom);
        flight.setOriginCode("DEL");
        flight.setDestinationCode("BOM");
        flight.setDepartureDate(LocalDate.now().plusDays(5));
        flight.setDepartureTime(LocalDateTime.now().plusDays(5));
        flight.setArrivalTime(LocalDateTime.now().plusDays(5).plusHours(2));
        flight.setDurationMinutes(120);
        flight.setStops(0);
        flight.setEconomyPrice(BigDecimal.valueOf(4500));
        flight.setPremiumEconomyPrice(BigDecimal.valueOf(6500));
        flight.setBusinessPrice(BigDecimal.valueOf(12000));
        flight.setFirstClassPrice(BigDecimal.valueOf(18000));
        flight.setTotalSeatsEconomy(150);
        flight.setTotalSeatsPremiumEconomy(30);
        flight.setTotalSeatsBusiness(20);
        flight.setTotalSeatsFirst(10);
        flight.setActive(true);
        flight = flightRepo.save(flight);

        hotel = new Hotel();
        hotel.setName("Voyara Grand Resort");
        hotel.setCity("Goa");
        hotel.setStarRating(5);
        hotel.setActive(true);
        hotel = hotelRepo.save(hotel);
    }

    private Booking createCompletedBooking(User user, Flight fl, Hotel ht) {
        Booking b = new Booking();
        b.setBookingReference("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        b.setUser(user);
        b.setStatus("COMPLETED");
        b.setTotalAmount(BigDecimal.valueOf(5000));
        b.setPassengerCount(1);
        if (fl != null) {
            b.setBookingType("FLIGHT");
            b.setFlight(fl);
        } else if (ht != null) {
            b.setBookingType("HOTEL");
            b.setHotel(ht);
        }
        return bookingRepo.save(b);
    }

    // ─── 1. Rating Validation & Security ───────────────────────

    @Test
    @DisplayName("Rating must strictly be between 1 and 5 (reject negative, 0, 6, 99)")
    void testRatingBoundaryValidation() throws Exception {
        createCompletedBooking(user1, flight, null);

        int[] invalidRatings = {-1, 0, 6, 99};
        for (int r : invalidRatings) {
            mockMvc.perform(post("/api/reviews")
                            .header("Authorization", "Bearer " + token1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"flightId\":" + flight.getId() + ",\"rating\":" + r + ",\"text\":\"Valid review text here\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── 2. Review Text Validation & XSS Sanitization ──────────

    @Test
    @DisplayName("Review text must meet length constraints and sanitize HTML/script tags")
    void testTextValidationAndXssSanitization() throws Exception {
        createCompletedBooking(user1, flight, null);

        // Empty text rejected
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightId\":" + flight.getId() + ",\"rating\":5,\"text\":\"\"}"))
                .andExpect(status().isBadRequest());

        // Script injection stripped safely
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightId\":" + flight.getId() + ",\"rating\":5,\"title\":\"<script>alert(1)</script>Safe Title\",\"text\":\"<script>badCode()</script>Clean experience!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Safe Title"))
                .andExpect(jsonPath("$.text").value("Clean experience!"))
                .andExpect(jsonPath("$.verifiedBooking").value(true));
    }

    // ─── 3. Verified Traveller & Duplicate Review Prevention ───

    @Test
    @DisplayName("Duplicate review for same completed booking is strictly rejected")
    void testDuplicateReviewPrevention() throws Exception {
        Booking b = createCompletedBooking(user1, null, hotel);

        // First review succeeds
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":" + hotel.getId() + ",\"bookingId\":" + b.getId() + ",\"rating\":5,\"text\":\"Loved this hotel!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifiedBooking").value(true));

        // Duplicate review submission for same booking fails
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":" + hotel.getId() + ",\"bookingId\":" + b.getId() + ",\"rating\":4,\"text\":\"Trying duplicate!\"}"))
                .andExpect(status().isBadRequest());
    }

    // ─── 4. Photo Validation & IDOR Protection ─────────────────

    @Test
    @DisplayName("Photo upload validates real image bytes and prevents IDOR deletion")
    void testPhotoUploadAndIdorProtection() throws Exception {
        createCompletedBooking(user1, null, hotel);

        // Create review first
        Review review = new Review();
        review.setUser(user1);
        review.setHotel(hotel);
        review.setRating(5);
        review.setText("Hotel with photo review");
        review.setStatus("PUBLISHED");
        review = reviewRepo.save(review);

        // 1. Valid BufferedImage upload
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", baos);
        byte[] validImageBytes = baos.toByteArray();

        MockMultipartFile validFile = new MockMultipartFile("files", "room.png", "image/png", validImageBytes);

        mockMvc.perform(multipart("/api/reviews/" + review.getId() + "/photos")
                        .file(validFile)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].photoUrl").value(startsWith("/uploads/review-photos/")));

        ReviewPhoto photo = photoRepo.findByReviewId(review.getId()).get(0);

        // 2. Reject fake image (HTML masquerading as png)
        MockMultipartFile fakeFile = new MockMultipartFile("files", "malicious.png", "image/png", "<html><body>Malicious</body></html>".getBytes());
        mockMvc.perform(multipart("/api/reviews/" + review.getId() + "/photos")
                        .file(fakeFile)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadRequest());

        // 3. IDOR: user2 cannot delete user1's photo
        mockMvc.perform(delete("/api/reviews/photos/" + photo.getId())
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isBadRequest());

        // 4. Author can delete own photo
        mockMvc.perform(delete("/api/reviews/photos/" + photo.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Photo deleted successfully"));
    }

    // ─── 5. Review Replies Lifecycle & Authorization ───────────

    @Test
    @DisplayName("Replies lifecycle: create, update, and author/admin authorization")
    void testRepliesLifecycle() throws Exception {
        Review review = new Review();
        review.setUser(user1);
        review.setHotel(hotel);
        review.setRating(4);
        review.setText("Great pool and spa");
        review.setStatus("PUBLISHED");
        review = reviewRepo.save(review);

        // user2 replies
        String replyResponse = mockMvc.perform(post("/api/reviews/" + review.getId() + "/replies")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Did you try the breakfast?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Did you try the breakfast?"))
                .andExpect(jsonPath("$.userName").value("Bob Traveler"))
                .andReturn().getResponse().getContentAsString();

        Long replyId = ((Number) com.jayway.jsonpath.JsonPath.read(replyResponse, "$.id")).longValue();

        // user1 cannot edit user2's reply
        mockMvc.perform(put("/api/reviews/replies/" + replyId)
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hacked text\"}"))
                .andExpect(status().isBadRequest());

        // user2 can update own reply
        mockMvc.perform(put("/api/reviews/replies/" + replyId)
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Did you try the buffet breakfast?\"}"))
                .andExpect(status().isOk());

        // Admin can delete reply
        mockMvc.perform(delete("/api/reviews/replies/" + replyId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        ReviewReply deletedReply = replyRepo.findById(replyId).orElseThrow();
        assertEquals("DELETED", deletedReply.getStatus());
    }

    // ─── 6. Report / Flagging & Abuse Prevention ───────────────

    @Test
    @DisplayName("Reporting review creates report, deduplicates reports, and triggers moderation")
    void testReportingAndDeduplication() throws Exception {
        Review review = new Review();
        review.setUser(user1);
        review.setFlight(flight);
        review.setRating(1);
        review.setText("Terrible experience");
        review.setStatus("PUBLISHED");
        review = reviewRepo.save(review);

        // user2 reports review
        mockMvc.perform(post("/api/reviews/" + review.getId() + "/report")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"OFFENSIVE\",\"description\":\"Contains inappropriate wording\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Review reported successfully"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Duplicate report from user2 is rejected
        mockMvc.perform(post("/api/reviews/" + review.getId() + "/report")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"SPAM\"}"))
                .andExpect(status().isBadRequest());

        // Review state is updated
        Review reportedReview = reviewRepo.findById(review.getId()).orElseThrow();
        assertEquals("PENDING_MODERATION", reportedReview.getStatus());
        assertEquals(1, reportedReview.getReportCount());
    }

    // ─── 7. Moderation Workflow & Audit Logging ────────────────

    @Test
    @DisplayName("Admin moderation queue, action execution, and audit log generation")
    void testModerationWorkflowAndAuditLog() throws Exception {
        Review review = new Review();
        review.setUser(user1);
        review.setHotel(hotel);
        review.setRating(2);
        review.setText("Problematic review");
        review.setStatus("PENDING_MODERATION");
        review.setReportCount(1);
        review = reviewRepo.save(review);

        // Admin checks moderation queue
        mockMvc.perform(get("/api/reviews/moderation/queue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + review.getId() + ")].status").value("PENDING_MODERATION"));

        // Admin removes review
        mockMvc.perform(post("/api/reviews/" + review.getId() + "/moderate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"REMOVE\",\"reason\":\"Violated terms of service\"}"))
                .andExpect(status().isOk());

        Review moderated = reviewRepo.findById(review.getId()).orElseThrow();
        assertEquals("REMOVED", moderated.getStatus());

        // Check moderation audit log
        var auditLogs = actionRepo.findByReviewIdOrderByCreatedAtDesc(review.getId());
        assertFalse(auditLogs.isEmpty());
        assertEquals("REMOVE", auditLogs.get(0).getAction());
        assertEquals(adminUser.getId(), auditLogs.get(0).getModerator().getId());

        // Verify removed review is excluded from public hotel reviews
        mockMvc.perform(get("/api/reviews/hotel/" + hotel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ─── 8. Helpful Voting & Vote Toggle ───────────────────────

    @Test
    @DisplayName("Helpful vote increments count and toggle removes vote")
    void testHelpfulVotingToggle() throws Exception {
        Review review = new Review();
        review.setUser(user1);
        review.setHotel(hotel);
        review.setRating(5);
        review.setText("Superb stay");
        review.setStatus("PUBLISHED");
        review = reviewRepo.save(review);

        // Vote HELPFUL
        mockMvc.perform(post("/api/reviews/" + review.getId() + "/vote")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\":\"HELPFUL\"}"))
                .andExpect(status().isOk());

        assertEquals(1, reviewRepo.findById(review.getId()).orElseThrow().getHelpfulCount());

        // Same vote toggles it off
        mockMvc.perform(post("/api/reviews/" + review.getId() + "/vote")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\":\"HELPFUL\"}"))
                .andExpect(status().isOk());

        assertEquals(0, reviewRepo.findById(review.getId()).orElseThrow().getHelpfulCount());
    }

    // ─── 9. Rating Distribution & Summary ──────────────────────

    @Test
    @DisplayName("Rating summary returns accurate average and 1-5 star distribution")
    void testRatingSummaryAndDistribution() throws Exception {
        Review r1 = new Review(); r1.setUser(user1); r1.setFlight(flight); r1.setRating(5); r1.setStatus("PUBLISHED"); reviewRepo.save(r1);
        Review r2 = new Review(); r2.setUser(user2); r2.setFlight(flight); r2.setRating(4); r2.setStatus("PUBLISHED"); reviewRepo.save(r2);

        mockMvc.perform(get("/api/reviews/flight/" + flight.getId() + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.totalReviews").value(2))
                .andExpect(jsonPath("$.ratingDistribution.5").value(1))
                .andExpect(jsonPath("$.ratingDistribution.4").value(1))
                .andExpect(jsonPath("$.ratingDistribution.3").value(0))
                .andExpect(jsonPath("$.ratingDistribution.2").value(0))
                .andExpect(jsonPath("$.ratingDistribution.1").value(0));
    }

    // ─── 10. Sorting & Filtering ───────────────────────────────

    @Test
    @DisplayName("Sorting by HIGHEST_RATED and filtering by rating")
    void testSortingAndFiltering() throws Exception {
        Review rLow = new Review(); rLow.setUser(user1); rLow.setHotel(hotel); rLow.setRating(2); rLow.setStatus("PUBLISHED"); reviewRepo.save(rLow);
        Review rHigh = new Review(); rHigh.setUser(user2); rHigh.setHotel(hotel); rHigh.setRating(5); rHigh.setStatus("PUBLISHED"); reviewRepo.save(rHigh);

        // Highest rated sort
        mockMvc.perform(get("/api/reviews/hotel/" + hotel.getId())
                        .param("sortBy", "HIGHEST_RATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.content[1].rating").value(2));

        // Rating filter = 5
        mockMvc.perform(get("/api/reviews/hotel/" + hotel.getId())
                        .param("rating", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }
}
