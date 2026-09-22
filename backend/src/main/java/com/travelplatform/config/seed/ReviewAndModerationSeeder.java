package com.travelplatform.config.seed;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class ReviewAndModerationSeeder {

    private static final Logger log = LoggerFactory.getLogger(ReviewAndModerationSeeder.class);

    private final ReviewRepository reviewRepo;
    private final ReviewPhotoRepository photoRepo;
    private final ReviewReplyRepository replyRepo;
    private final ReviewVoteRepository voteRepo;
    private final ReviewReportRepository reportRepo;
    private final ReviewModerationActionRepository actionRepo;

    public ReviewAndModerationSeeder(ReviewRepository reviewRepo,
                                     ReviewPhotoRepository photoRepo,
                                     ReviewReplyRepository replyRepo,
                                     ReviewVoteRepository voteRepo,
                                     ReviewReportRepository reportRepo,
                                     ReviewModerationActionRepository actionRepo) {
        this.reviewRepo = reviewRepo;
        this.photoRepo = photoRepo;
        this.replyRepo = replyRepo;
        this.voteRepo = voteRepo;
        this.reportRepo = reportRepo;
        this.actionRepo = actionRepo;
    }

    @Transactional
    public void seedReviewsAndSocialUgc(List<Hotel> hotels, List<Flight> flights,
                                       Map<String, User> userMap, List<Booking> bookings) {
        long currentReviewCount = reviewRepo.count();
        if (currentReviewCount >= 1000) {
            log.info("Reviews already seeded ({}), skipping bulk review generation.", currentReviewCount);
            return;
        }

        log.info("Generating 1,000+ realistic reviews across hotels and flights with ratings, replies, and moderation...");

        List<User> userList = new ArrayList<>(userMap.values());
        if (userList.isEmpty() || hotels.isEmpty() || flights.isEmpty()) return;

        User reviewerUser = userMap.getOrDefault("reviewer@travel.com", userList.get(0));
        User moderatorUser = userMap.getOrDefault("moderator@travel.com", userList.get(0));
        User regularUser = userMap.getOrDefault("user@travel.com", userList.get(0));
        User travelerUser = userMap.getOrDefault("traveler@travel.com", userList.get(0));

        List<Review> reviewBatch = new ArrayList<>(1200);
        Random rng = new Random(42);

        // 1. Seed Hotel Reviews (approx 650 reviews across hotels)
        int hotelCountToReview = Math.min(hotels.size(), 180);
        for (int hIdx = 0; hIdx < hotelCountToReview; hIdx++) {
            Hotel hotel = hotels.get(hIdx);
            int reviewsForHotel = 3 + rng.nextInt(4);

            for (int r = 0; r < reviewsForHotel; r++) {
                int starRating = pickDeterministicRating(rng);
                String title = pickTitle(starRating, rng);
                String text = pickText(starRating, rng);

                Review rev = new Review();
                rev.setUser(userList.get((hIdx + r) % userList.size()));
                rev.setHotel(hotel);
                rev.setRating(starRating);
                rev.setTitle(title);
                rev.setText(text);
                rev.setHelpfulCount(rng.nextInt(25));
                rev.setNotHelpfulCount(rng.nextInt(3));
                rev.setVerifiedBooking(rng.nextBoolean());
                rev.setStatus(Review.STATUS_PUBLISHED);
                reviewBatch.add(rev);
            }
        }

        // 2. Seed Flight Reviews (approx 450 reviews across flights)
        int flightCountToReview = Math.min(flights.size(), 150);
        for (int fIdx = 0; fIdx < flightCountToReview; fIdx++) {
            Flight flight = flights.get(fIdx);
            int reviewsForFlight = 2 + rng.nextInt(3);

            for (int r = 0; r < reviewsForFlight; r++) {
                int starRating = pickDeterministicRating(rng);
                String title = pickTitle(starRating, rng);
                String text = pickText(starRating, rng);

                Review rev = new Review();
                rev.setUser(userList.get((fIdx + r + 3) % userList.size()));
                rev.setFlight(flight);
                rev.setRating(starRating);
                rev.setTitle(title);
                rev.setText(text);
                rev.setHelpfulCount(rng.nextInt(18));
                rev.setNotHelpfulCount(rng.nextInt(2));
                rev.setVerifiedBooking(rng.nextBoolean());
                rev.setStatus(Review.STATUS_PUBLISHED);
                reviewBatch.add(rev);
            }
        }

        // Attach completed bookings to reviewerUser reviews where available
        for (Booking b : bookings) {
            if ("COMPLETED".equals(b.getStatus())) {
                Review bookingRev = new Review();
                bookingRev.setUser(reviewerUser);
                bookingRev.setBooking(b);
                if (b.getFlight() != null) bookingRev.setFlight(b.getFlight());
                if (b.getHotel() != null) bookingRev.setHotel(b.getHotel());
                bookingRev.setRating(5);
                bookingRev.setTitle("Verified travel booking: flawless journey!");
                bookingRev.setText("Confirmed reservation completed seamlessly. Everything as promised on Voyara.");
                bookingRev.setVerifiedBooking(true);
                bookingRev.setHelpfulCount(12);
                bookingRev.setStatus(Review.STATUS_PUBLISHED);
                reviewBatch.add(bookingRev);
            }
        }

        // Add controlled moderation scenario reviews
        Review flaggedRev = new Review();
        flaggedRev.setUser(userList.get(0));
        flaggedRev.setHotel(hotels.get(0));
        flaggedRev.setRating(1);
        flaggedRev.setTitle("Suspicious review flagged for promotional content");
        flaggedRev.setText("Contains external promotional links and contact numbers www.example.com call 999999.");
        flaggedRev.setStatus(Review.STATUS_FLAGGED);
        flaggedRev.setReportCount(2);
        flaggedRev.setFlagReason("SPAM");
        reviewBatch.add(flaggedRev);

        Review underReviewRev = new Review();
        underReviewRev.setUser(userList.get(1));
        underReviewRev.setHotel(hotels.get(1));
        underReviewRev.setRating(2);
        underReviewRev.setTitle("Reported for offensive language");
        underReviewRev.setText("Review currently withheld under community moderation guidelines.");
        underReviewRev.setStatus(Review.STATUS_UNDER_REVIEW);
        underReviewRev.setReportCount(1);
        underReviewRev.setFlagReason("OFFENSIVE");
        reviewBatch.add(underReviewRev);

        List<Review> savedReviews = reviewRepo.saveAll(reviewBatch);
        log.info("Persisted {} reviews. Now seeding review photos, replies, votes, and moderation reports...", savedReviews.size());

        // 3. Review Photos (150+ photos)
        List<ReviewPhoto> photoBatch = new ArrayList<>();
        int photoLimit = Math.min(savedReviews.size(), 160);
        for (int i = 0; i < photoLimit; i++) {
            Review rev = savedReviews.get(i);
            String photoUrl = SeedConstants.REVIEW_PHOTOS[i % SeedConstants.REVIEW_PHOTOS.length];
            ReviewPhoto photo = new ReviewPhoto(rev, photoUrl, "Guest photo taken during stay/journey", 245000L + (i * 1200L), "image/jpeg");
            photoBatch.add(photo);
        }
        photoRepo.saveAll(photoBatch);
        log.info("Persisted {} review photos.", photoRepo.count());

        // 4. Review Replies (200+ user & management replies)
        List<ReviewReply> replyBatch = new ArrayList<>();
        int replyLimit = Math.min(savedReviews.size(), 220);
        for (int i = 0; i < replyLimit; i++) {
            Review rev = savedReviews.get(i);
            boolean isMgmt = (i % 2 == 0);
            String replyText = isMgmt ?
                    SeedConstants.REVIEW_REPLIES_MGMT[(i / 2) % SeedConstants.REVIEW_REPLIES_MGMT.length] :
                    SeedConstants.REVIEW_REPLIES_USER[(i / 2) % SeedConstants.REVIEW_REPLIES_USER.length];
            User replier = isMgmt ? moderatorUser : userList.get((i + 2) % userList.size());
            replyBatch.add(new ReviewReply(rev, replier, replyText));
        }
        replyRepo.saveAll(replyBatch);
        log.info("Persisted {} review replies.", replyRepo.count());

        // 5. Review Helpful Votes (deterministic, unique(review_id, user_id))
        List<ReviewVote> voteBatch = new ArrayList<>();
        int voteReviewLimit = Math.min(savedReviews.size(), 100);
        for (int i = 0; i < voteReviewLimit; i++) {
            Review rev = savedReviews.get(i);
            // User 1 votes helpful
            voteBatch.add(new ReviewVote(rev, regularUser, "HELPFUL"));
            // User 2 votes helpful
            if (!regularUser.getId().equals(travelerUser.getId())) {
                voteBatch.add(new ReviewVote(rev, travelerUser, (i % 4 == 0) ? "NOT_HELPFUL" : "HELPFUL"));
            }
        }
        voteRepo.saveAll(voteBatch);
        log.info("Persisted {} review votes.", voteRepo.count());

        // 6. Review Reports & Moderation Actions
        if (reportRepo.count() == 0) {
            List<ReviewReport> reportBatch = new ArrayList<>();
            reportBatch.add(new ReviewReport(flaggedRev, travelerUser, "SPAM", "Contains commercial promotional link."));
            reportBatch.add(new ReviewReport(underReviewRev, reviewerUser, "OFFENSIVE", "Inappropriate language."));
            reportRepo.saveAll(reportBatch);

            List<ReviewModerationAction> actionBatch = new ArrayList<>();
            actionBatch.add(new ReviewModerationAction(moderatorUser, flaggedRev, "FLAG", "Automated keyword filter flagged for review"));
            actionBatch.add(new ReviewModerationAction(moderatorUser, underReviewRev, "UNDER_REVIEW", "Moderator placed review under manual review queue"));
            actionRepo.saveAll(actionBatch);
            log.info("Persisted moderation reports and audit log actions.");
        }
    }

    private int pickDeterministicRating(Random rng) {
        // Realistic distribution: 35% 5-star, 35% 4-star, 18% 3-star, 8% 2-star, 4% 1-star
        int val = rng.nextInt(100);
        if (val < 35) return 5;
        if (val < 70) return 4;
        if (val < 88) return 3;
        if (val < 96) return 2;
        return 1;
    }

    private String pickTitle(int star, Random rng) {
        return switch (star) {
            case 5 -> SeedConstants.REVIEW_TITLES_5_STAR[rng.nextInt(SeedConstants.REVIEW_TITLES_5_STAR.length)];
            case 4 -> SeedConstants.REVIEW_TITLES_4_STAR[rng.nextInt(SeedConstants.REVIEW_TITLES_4_STAR.length)];
            case 3 -> SeedConstants.REVIEW_TITLES_3_STAR[rng.nextInt(SeedConstants.REVIEW_TITLES_3_STAR.length)];
            case 2 -> SeedConstants.REVIEW_TITLES_2_STAR[rng.nextInt(SeedConstants.REVIEW_TITLES_2_STAR.length)];
            default -> SeedConstants.REVIEW_TITLES_1_STAR[rng.nextInt(SeedConstants.REVIEW_TITLES_1_STAR.length)];
        };
    }

    private String pickText(int star, Random rng) {
        return switch (star) {
            case 5 -> SeedConstants.REVIEW_TEXTS_5_STAR[rng.nextInt(SeedConstants.REVIEW_TEXTS_5_STAR.length)];
            case 4 -> SeedConstants.REVIEW_TEXTS_4_STAR[rng.nextInt(SeedConstants.REVIEW_TEXTS_4_STAR.length)];
            case 3 -> SeedConstants.REVIEW_TEXTS_3_STAR[rng.nextInt(SeedConstants.REVIEW_TEXTS_3_STAR.length)];
            case 2 -> SeedConstants.REVIEW_TEXTS_2_STAR[rng.nextInt(SeedConstants.REVIEW_TEXTS_2_STAR.length)];
            default -> SeedConstants.REVIEW_TEXTS_1_STAR[rng.nextInt(SeedConstants.REVIEW_TEXTS_1_STAR.length)];
        };
    }
}
