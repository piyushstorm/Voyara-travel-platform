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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepo;
    @Autowired private FlightRepository flightRepo;
    @Autowired private HotelRepository hotelRepo;
    @Autowired private BookingRepository bookingRepo;
    @Autowired private ReviewRepository reviewRepo;
    @Autowired private ReviewReplyRepository replyRepo;
    @Autowired private ReviewVoteRepository voteRepo;
    @Autowired private AirlineRepository airlineRepo;
    @Autowired private AirportRepository airportRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private User reviewer, otherUser, admin;
    private String reviewerToken, otherToken, adminToken;
    private Flight testFlight;
    private Hotel testHotel;

    @BeforeEach
    void setUp() {
        reviewer = new User("Reviewer", "reviewer-test-" + UUID.randomUUID() + "@test.com", passwordEncoder.encode("pass123"));
        reviewer.setRole(Role.USER);
        reviewer = userRepo.save(reviewer);

        otherUser = new User("Other", "other-reviewer-test-" + UUID.randomUUID() + "@test.com", passwordEncoder.encode("pass123"));
        otherUser.setRole(Role.USER);
        otherUser = userRepo.save(otherUser);

        admin = new User("Admin", "admin-reviewer-test-" + UUID.randomUUID() + "@test.com", passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        admin = userRepo.save(admin);

        reviewerToken = tokenProvider.generateAccessToken(reviewer.getEmail());
        otherToken = tokenProvider.generateAccessToken(otherUser.getEmail());
        adminToken = tokenProvider.generateAccessToken(admin.getEmail());

        // Create airline + airports for flights
        Airline airline = airlineRepo.findAll().stream().findFirst().orElseGet(() -> {
            Airline a = new Airline(); a.setName("Test Air"); a.setCode("TA"); return airlineRepo.save(a);
        });

        Airport origin = airportRepo.findByCode("DEL").orElseGet(() -> {
            Airport ap = new Airport(); ap.setCode("DEL"); ap.setName("Delhi Airport"); ap.setCity("Delhi"); ap.setCountry("India"); return airportRepo.save(ap);
        });
        Airport dest = airportRepo.findByCode("BOM").orElseGet(() -> {
            Airport ap = new Airport(); ap.setCode("BOM"); ap.setName("Mumbai Airport"); ap.setCity("Mumbai"); ap.setCountry("India"); return airportRepo.save(ap);
        });

        testFlight = new Flight();
        testFlight.setFlightNumber("REV-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        testFlight.setAirline(airline);
        testFlight.setOrigin(origin);
        testFlight.setDestination(dest);
        testFlight.setOriginCode("DEL");
        testFlight.setDestinationCode("BOM");
        testFlight.setDepartureDate(java.time.LocalDate.now().plusDays(7));
        testFlight.setDepartureTime(java.time.LocalDateTime.now().plusDays(7));
        testFlight.setArrivalTime(java.time.LocalDateTime.now().plusDays(7).plusHours(2));
        testFlight.setDurationMinutes(120);
        testFlight.setStops(0);
        testFlight.setEconomyPrice(java.math.BigDecimal.valueOf(5000));
        testFlight.setPremiumEconomyPrice(java.math.BigDecimal.valueOf(7500));
        testFlight.setBusinessPrice(java.math.BigDecimal.valueOf(12500));
        testFlight.setFirstClassPrice(java.math.BigDecimal.valueOf(20000));
        testFlight.setTotalSeatsEconomy(150);
        testFlight.setTotalSeatsPremiumEconomy(50);
        testFlight.setTotalSeatsBusiness(30);
        testFlight.setTotalSeatsFirst(10);
        testFlight.setActive(true);
        testFlight = flightRepo.save(testFlight);

        testHotel = new Hotel();
        testHotel.setName("Review Test Hotel");
        testHotel.setCity("Goa");
        testHotel.setStarRating(4);
        testHotel.setActive(true);
        testHotel = hotelRepo.save(testHotel);
    }

    // ─── GET Reviews ────────────────────────────────────────

    @Test
    @DisplayName("GET /api/reviews/flight/{id} — returns reviews for flight")
    void getFlightReviews() throws Exception {
        // Create a review first
        Review review = new Review();
        review.setUser(reviewer);
        review.setFlight(testFlight);
        review.setRating(4);
        review.setText("Great flight experience");
        review.setStatus("PUBLISHED");
        reviewRepo.save(review);

        mockMvc.perform(get("/api/reviews/flight/" + testFlight.getId())
                        .header("Authorization", "Bearer " + reviewerToken)
                        .param("sortBy", "NEWEST")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].rating").value(4))
                .andExpect(jsonPath("$.content[0].text").value("Great flight experience"));
    }

    @Test
    @DisplayName("GET /api/reviews/hotel/{id} — returns reviews for hotel")
    void getHotelReviews() throws Exception {
        Review review = new Review();
        review.setUser(reviewer);
        review.setHotel(testHotel);
        review.setRating(5);
        review.setText("Amazing hotel");
        review.setStatus("PUBLISHED");
        reviewRepo.save(review);

        mockMvc.perform(get("/api/reviews/hotel/" + testHotel.getId())
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }

    @Test
    @DisplayName("GET /api/reviews/flight/{id}/summary — returns rating summary")
    void getFlightReviewSummary() throws Exception {
        Review r1 = new Review(); r1.setUser(reviewer); r1.setFlight(testFlight);
        r1.setRating(4); r1.setStatus("PUBLISHED"); reviewRepo.save(r1);

        mockMvc.perform(get("/api/reviews/flight/" + testFlight.getId() + "/summary")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").isNumber())
                .andExpect(jsonPath("$.reviewCount").isNumber());
    }

    // ─── CREATE Review ──────────────────────────────────────

    @Test
    @DisplayName("POST /api/reviews — authenticated user can create flight review")
    void createFlightReview() throws Exception {
        // Create a confirmed booking for the user
        Booking booking = new Booking();
        booking.setBookingReference("TP-REV-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        booking.setUser(reviewer);
        booking.setFlight(testFlight);
        booking.setBookingType("FLIGHT");
        booking.setStatus("COMPLETED");
        booking.setTotalAmount(java.math.BigDecimal.valueOf(5000));
        booking.setPassengerCount(1);
        bookingRepo.save(booking);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightId\":" + testFlight.getId() + ",\"rating\":4,\"text\":\"Great flight!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.text").value("Great flight!"))
                .andExpect(jsonPath("$.verifiedBooking").value(true));
    }

    @Test
    @DisplayName("POST /api/reviews — hotel review")
    void createHotelReview() throws Exception {
        Booking booking = new Booking();
        booking.setBookingReference("TP-REVH-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        booking.setUser(reviewer);
        booking.setHotel(testHotel);
        booking.setBookingType("HOTEL");
        booking.setStatus("COMPLETED");
        booking.setTotalAmount(java.math.BigDecimal.valueOf(8000));
        booking.setPassengerCount(1);
        bookingRepo.save(booking);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":" + testHotel.getId() + ",\"rating\":5,\"text\":\"Excellent stay!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.verifiedBooking").value(true));
    }

    @Test
    @DisplayName("POST /api/reviews — rating below 1 rejected")
    void rejectRatingBelowRange() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightId\":" + testFlight.getId() + ",\"rating\":0,\"text\":\"Bad\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reviews — rating above 5 rejected")
    void rejectRatingAboveRange() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightId\":" + testFlight.getId() + ",\"rating\":6,\"text\":\"Too good\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reviews — unauthenticated user rejected")
    void unauthenticatedReviewRejected() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightId\":" + testFlight.getId() + ",\"rating\":4,\"text\":\"Good\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/reviews — no completed booking is rejected")
    void reviewWithoutCompletedBookingRejected() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightId\":" + testFlight.getId() + ",\"rating\":3,\"text\":\"Decent\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reviews — CONFIRMED (not COMPLETED) booking is rejected")
    void reviewWithConfirmedBookingRejected() throws Exception {
        Booking booking = new Booking();
        booking.setBookingReference("TP-RCONF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        booking.setUser(otherUser);
        booking.setFlight(testFlight);
        booking.setBookingType("FLIGHT");
        booking.setStatus("CONFIRMED");
        booking.setTotalAmount(java.math.BigDecimal.valueOf(5000));
        booking.setPassengerCount(1);
        bookingRepo.save(booking);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightId\":" + testFlight.getId() + ",\"rating\":3,\"text\":\"Decent\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/reviews/eligibility — checks booking completion")
    void checkReviewEligibility() throws Exception {
        Booking completedBooking = new Booking();
        completedBooking.setBookingReference("TP-ELIG-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        completedBooking.setUser(reviewer);
        completedBooking.setFlight(testFlight);
        completedBooking.setBookingType("FLIGHT");
        completedBooking.setStatus("COMPLETED");
        completedBooking.setTotalAmount(java.math.BigDecimal.valueOf(5000));
        completedBooking.setPassengerCount(1);
        completedBooking = bookingRepo.save(completedBooking);

        mockMvc.perform(get("/api/reviews/eligibility")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .param("bookingId", completedBooking.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));
    }

    // ─── REPLIES ────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/reviews/{id}/replies — add reply to review")
    void addReplyToReview() throws Exception {
        Review review = new Review();
        review.setUser(reviewer); review.setFlight(testFlight);
        review.setRating(4); review.setText("Good"); review.setStatus("PUBLISHED");
        review = reviewRepo.save(review);

        mockMvc.perform(post("/api/reviews/" + review.getId() + "/replies")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"I agree!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("I agree!"));
    }

    @Test
    @DisplayName("POST /api/reviews/{id}/replies — unauthenticated rejected")
    void unauthenticatedReplyRejected() throws Exception {
        Review review = new Review();
        review.setUser(reviewer); review.setFlight(testFlight);
        review.setRating(4); review.setText("Good"); review.setStatus("PUBLISHED");
        review = reviewRepo.save(review);

        mockMvc.perform(post("/api/reviews/" + review.getId() + "/replies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Reply\"}"))
                .andExpect(status().isForbidden());
    }

    // ─── VOTES ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/reviews/{id}/vote — helpful vote")
    void helpfulVote() throws Exception {
        Review review = new Review();
        review.setUser(reviewer); review.setFlight(testFlight);
        review.setRating(4); review.setText("Good"); review.setStatus("PUBLISHED");
        review = reviewRepo.save(review);

        mockMvc.perform(post("/api/reviews/" + review.getId() + "/vote")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\":\"HELPFUL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Vote recorded"));

        Review updated = reviewRepo.findById(review.getId()).orElseThrow();
        assert updated.getHelpfulCount() == 1;
    }

    @Test
    @DisplayName("POST /api/reviews/{id}/vote — invalid vote type rejected")
    void invalidVoteTypeRejected() throws Exception {
        Review review = new Review();
        review.setUser(reviewer); review.setFlight(testFlight);
        review.setRating(4); review.setText("Good"); review.setStatus("PUBLISHED");
        review = reviewRepo.save(review);

        mockMvc.perform(post("/api/reviews/" + review.getId() + "/vote")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest());
    }

    // ─── FLAG ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/reviews/{id}/flag — flag review for moderation")
    void flagReview() throws Exception {
        Review review = new Review();
        review.setUser(reviewer); review.setFlight(testFlight);
        review.setRating(4); review.setText("Good"); review.setStatus("PUBLISHED");
        review = reviewRepo.save(review);

        mockMvc.perform(post("/api/reviews/" + review.getId() + "/flag")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"SPAM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Review flagged for moderation"));

        Review flagged = reviewRepo.findById(review.getId()).orElseThrow();
        assert "PENDING_MODERATION".equals(flagged.getStatus());
    }

    // ─── MODERATION (Admin) ─────────────────────────────────

    @Test
    @DisplayName("GET /api/reviews/moderation/queue — admin sees flagged reviews")
    void adminGetsModerationQueue() throws Exception {
        Review review = new Review();
        review.setUser(reviewer); review.setFlight(testFlight);
        review.setRating(2); review.setText("Terrible"); review.setStatus("PENDING_MODERATION");
        review.setFlagReason("SPAM");
        reviewRepo.save(review);

        mockMvc.perform(get("/api/reviews/moderation/queue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + review.getId() + ")].status").value("PENDING_MODERATION"));
    }

    @Test
    @DisplayName("POST /api/reviews/{id}/moderate — admin approves review")
    void adminModerateApprove() throws Exception {
        Review review = new Review();
        review.setUser(reviewer); review.setFlight(testFlight);
        review.setRating(2); review.setText("Content"); review.setStatus("PENDING_MODERATION");
        review = reviewRepo.save(review);

        mockMvc.perform(post("/api/reviews/" + review.getId() + "/moderate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Review moderated"));

        Review moderated = reviewRepo.findById(review.getId()).orElseThrow();
        assert "PUBLISHED".equals(moderated.getStatus());
    }

    @Test
    @DisplayName("POST /api/reviews/{id}/moderate — admin removes review")
    void adminModerateRemove() throws Exception {
        Review review = new Review();
        review.setUser(reviewer); review.setFlight(testFlight);
        review.setRating(2); review.setText("Content"); review.setStatus("PENDING_MODERATION");
        review = reviewRepo.save(review);

        mockMvc.perform(post("/api/reviews/" + review.getId() + "/moderate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"REMOVE\"}"))
                .andExpect(status().isOk());

        Review removed = reviewRepo.findById(review.getId()).orElseThrow();
        assert "REMOVED".equals(removed.getStatus());
    }

    @Test
    @DisplayName("POST /api/reviews/{id}/moderate — invalid action rejected")
    void adminModerateInvalidAction() throws Exception {
        Review review = new Review();
        review.setUser(reviewer); review.setFlight(testFlight);
        review.setRating(3); review.setText("OK"); review.setStatus("PENDING_MODERATION");
        review = reviewRepo.save(review);

        mockMvc.perform(post("/api/reviews/" + review.getId() + "/moderate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reviews/{id}/moderate — non-admin cannot moderate")
    void nonAdminCannotModerate() throws Exception {
        Review review = new Review();
        review.setUser(reviewer); review.setFlight(testFlight);
        review.setRating(3); review.setText("OK"); review.setStatus("PENDING_MODERATION");
        review = reviewRepo.save(review);

        mockMvc.perform(post("/api/reviews/" + review.getId() + "/moderate")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/reviews/moderation/queue — non-admin cannot access")
    void nonAdminCannotAccessQueue() throws Exception {
        mockMvc.perform(get("/api/reviews/moderation/queue")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isForbidden());
    }

    // ─── SORTING ────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/reviews — supports sort by RATING")
    void sortByRating() throws Exception {
        Review r1 = new Review(); r1.setUser(reviewer); r1.setFlight(testFlight);
        r1.setRating(2); r1.setText("Low"); r1.setStatus("PUBLISHED"); reviewRepo.save(r1);
        Review r2 = new Review(); r2.setUser(otherUser); r2.setFlight(testFlight);
        r2.setRating(5); r2.setText("High"); r2.setStatus("PUBLISHED"); reviewRepo.save(r2);

        mockMvc.perform(get("/api/reviews/flight/" + testFlight.getId())
                        .header("Authorization", "Bearer " + reviewerToken)
                        .param("sortBy", "RATING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }

    // ─── REVIEW RESPONSE STRUCTURE ─────────────────────────

    @Test
    @DisplayName("Review response does not expose internal fields")
    void reviewResponseNoInternalFields() throws Exception {
        Review review = new Review();
        review.setUser(reviewer); review.setFlight(testFlight);
        review.setRating(4); review.setText("Good"); review.setStatus("PUBLISHED");
        review = reviewRepo.save(review);

        mockMvc.perform(get("/api/reviews/flight/" + testFlight.getId())
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].flagReason").doesNotExist());
    }
}
