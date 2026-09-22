package com.travelplatform.service;

import com.travelplatform.dto.review.*;
import com.travelplatform.entity.*;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final String UPLOAD_DIR = "uploads/review-photos/";

    private final ReviewRepository reviewRepo;
    private final ReviewReplyRepository replyRepo;
    private final ReviewVoteRepository voteRepo;
    private final ReviewPhotoRepository photoRepo;
    private final UserRepository userRepo;
    private final FlightRepository flightRepo;
    private final HotelRepository hotelRepo;
    private final BookingRepository bookingRepo;
    private final ReviewReportRepository reportRepo;
    private final ReviewModerationActionRepository actionRepo;
    private final NotificationService notificationService;

    public ReviewService(ReviewRepository reviewRepo, ReviewReplyRepository replyRepo,
                         ReviewVoteRepository voteRepo, ReviewPhotoRepository photoRepo,
                         UserRepository userRepo, FlightRepository flightRepo,
                         HotelRepository hotelRepo, BookingRepository bookingRepo,
                         ReviewReportRepository reportRepo, ReviewModerationActionRepository actionRepo,
                         NotificationService notificationService) {
        this.reviewRepo = reviewRepo;
        this.replyRepo = replyRepo;
        this.voteRepo = voteRepo;
        this.photoRepo = photoRepo;
        this.userRepo = userRepo;
        this.flightRepo = flightRepo;
        this.hotelRepo = hotelRepo;
        this.bookingRepo = bookingRepo;
        this.reportRepo = reportRepo;
        this.actionRepo = actionRepo;
        this.notificationService = notificationService;
    }

    // ─── Input Sanitization ───────────────────────────────────

    private String sanitize(String input) {
        if (input == null) return null;
        String noScript = SCRIPT_PATTERN.matcher(input.trim()).replaceAll("");
        // Strip remaining HTML tags to prevent XSS
        return HTML_TAG_PATTERN.matcher(noScript).replaceAll("").trim();
    }

    // ─── Create Review ────────────────────────────────────────

    @Transactional
    public Review createReview(Long userId, Long flightId, Long hotelId, int rating, String text) {
        ReviewCreateRequestDto dto = new ReviewCreateRequestDto();
        dto.setFlightId(flightId);
        dto.setHotelId(hotelId);
        dto.setRating(rating);
        dto.setReviewText(text);
        return createReviewInternal(userId, dto);
    }

    @Transactional
    public Review createReview(Long userId, ReviewCreateRequestDto dto) {
        return createReviewInternal(userId, dto);
    }

    private Review createReviewInternal(Long userId, ReviewCreateRequestDto dto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        int rating = dto.getRating();
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating must be strictly between 1 and 5");
        }

        String rawText = dto.getReviewText();
        if (rawText == null || rawText.trim().length() < 3) {
            throw new BadRequestException("Review text must be at least 3 characters");
        }
        if (rawText.length() > 2500) {
            throw new BadRequestException("Review text cannot exceed 2500 characters");
        }
        String cleanText = sanitize(rawText);
        if (cleanText.isEmpty()) {
            throw new BadRequestException("Review text contains invalid content");
        }

        String cleanTitle = null;
        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            cleanTitle = sanitize(dto.getTitle());
            if (cleanTitle.length() > 120) {
                cleanTitle = cleanTitle.substring(0, 120);
            }
        }

        Review review = new Review();
        review.setUser(user);
        review.setRating(rating);
        review.setTitle(cleanTitle);
        review.setText(cleanText);
        review.setStatus(Review.STATUS_PUBLISHED);

        Long flightId = dto.getFlightId();
        Long hotelId = dto.getHotelId();
        Long bookingId = dto.getBookingId();

        // Infer target type if targetType string was passed
        if ("FLIGHT".equalsIgnoreCase(dto.getTargetType()) && dto.getTargetId() != null) {
            flightId = dto.getTargetId();
        } else if ("HOTEL".equalsIgnoreCase(dto.getTargetType()) && dto.getTargetId() != null) {
            hotelId = dto.getTargetId();
        }

        if (flightId == null && hotelId == null && bookingId != null) {
            Booking b = bookingRepo.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
            if ("FLIGHT".equalsIgnoreCase(b.getBookingType()) && b.getFlight() != null) {
                flightId = b.getFlight().getId();
            } else if ("HOTEL".equalsIgnoreCase(b.getBookingType()) && b.getHotel() != null) {
                hotelId = b.getHotel().getId();
            }
        }

        if (flightId == null && hotelId == null) {
            throw new BadRequestException("Review must specify a flight or hotel");
        }
        if (flightId != null && hotelId != null) {
            throw new BadRequestException("Review cannot be for both flight and hotel simultaneously");
        }

        final Long targetFlightId = flightId;
        final Long targetHotelId = hotelId;

        // Process Flight Review
        if (targetFlightId != null) {
            Flight flight = flightRepo.findById(targetFlightId)
                    .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", targetFlightId));
            review.setFlight(flight);

            if (bookingId != null) {
                Booking booking = bookingRepo.findById(bookingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
                if (!booking.getUser().getId().equals(userId)) {
                    throw new BadRequestException("Booking does not belong to the authenticated user");
                }
                if (!"COMPLETED".equals(booking.getStatus())) {
                    throw new BadRequestException("You can only review flights after completing your travel");
                }
                if (booking.getFlight() == null || !booking.getFlight().getId().equals(targetFlightId)) {
                    throw new BadRequestException("Booking does not match the specified flight");
                }
                if (reviewRepo.existsByUserIdAndBookingIdAndStatusNot(userId, bookingId, "REMOVED")) {
                    throw new BadRequestException("You have already submitted a review for this booking");
                }
                review.setBooking(booking);
                review.setVerifiedBooking(true);
            } else {
                // Check if user has any COMPLETED booking for this flight
                if (reviewRepo.existsByUserIdAndFlightIdAndStatusNot(userId, targetFlightId, "REMOVED")) {
                    throw new BadRequestException("You have already submitted a review for this flight");
                }
                boolean hasCompletedBooking = bookingRepo.existsByUserIdAndFlightIdAndStatus(userId, targetFlightId, "COMPLETED");
                if (!hasCompletedBooking) {
                    throw new BadRequestException("You can only review flights after completing your travel");
                }
                review.setVerifiedBooking(true);
            }
        }

        // Process Hotel Review
        if (targetHotelId != null) {
            Hotel hotel = hotelRepo.findById(targetHotelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", targetHotelId));
            review.setHotel(hotel);

            if (bookingId != null) {
                Booking booking = bookingRepo.findById(bookingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
                if (!booking.getUser().getId().equals(userId)) {
                    throw new BadRequestException("Booking does not belong to the authenticated user");
                }
                if (!"COMPLETED".equals(booking.getStatus())) {
                    throw new BadRequestException("You can only review hotels after completing your stay");
                }
                if (booking.getHotel() == null || !booking.getHotel().getId().equals(hotelId)) {
                    throw new BadRequestException("Booking does not match the specified hotel");
                }
                if (reviewRepo.existsByUserIdAndBookingIdAndStatusNot(userId, bookingId, "REMOVED")) {
                    throw new BadRequestException("You have already submitted a review for this booking");
                }
                review.setBooking(booking);
                review.setVerifiedBooking(true);
            } else {
                if (reviewRepo.existsByUserIdAndHotelIdAndStatusNot(userId, hotelId, "REMOVED")) {
                    throw new BadRequestException("You have already submitted a review for this hotel");
                }
                boolean hasCompletedBooking = bookingRepo.existsByUserIdAndHotelIdAndStatus(userId, hotelId, "COMPLETED");
                if (!hasCompletedBooking) {
                    throw new BadRequestException("You can only review hotels after completing your stay");
                }
                review.setVerifiedBooking(true);
            }
        }

        review = reviewRepo.save(review);
        logger.info("Review {} created by user {} (rating={}, flight={}, hotel={})",
                review.getId(), userId, rating, flightId, hotelId);
        return review;
    }

    // ─── Listing Reviews ──────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Review> getFlightReviews(Long flightId, String sortBy, int page, int size) {
        return getFlightReviews(flightId, sortBy, null, null, null, null, page, size)
                .map(dto -> reviewRepo.findById(dto.getId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public Page<Review> getHotelReviews(Long hotelId, String sortBy, int page, int size) {
        return getHotelReviews(hotelId, sortBy, null, null, null, null, page, size)
                .map(dto -> reviewRepo.findById(dto.getId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getFlightReviews(Long flightId, String sortBy, Integer ratingFilter,
                                                   Boolean verifiedOnly, Boolean hasPhotos, Long currentUserId,
                                                   int page, int size) {
        return getReviewsInternal(flightId, null, sortBy, ratingFilter, verifiedOnly, hasPhotos, currentUserId, page, size);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getHotelReviews(Long hotelId, String sortBy, Integer ratingFilter,
                                                  Boolean verifiedOnly, Boolean hasPhotos, Long currentUserId,
                                                  int page, int size) {
        return getReviewsInternal(null, hotelId, sortBy, ratingFilter, verifiedOnly, hasPhotos, currentUserId, page, size);
    }

    private Page<ReviewResponseDto> getReviewsInternal(Long flightId, Long hotelId, String sortBy,
                                                       Integer ratingFilter, Boolean verifiedOnly,
                                                       Boolean hasPhotos, Long currentUserId,
                                                       int page, int size) {
        Specification<Review> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (flightId != null) {
                predicates.add(cb.equal(root.get("flight").get("id"), flightId));
            }
            if (hotelId != null) {
                predicates.add(cb.equal(root.get("hotel").get("id"), hotelId));
            }

            // Publicly visible: PUBLISHED or FLAGGED (flagged stays visible until moderator removes it)
            predicates.add(root.get("status").in("PUBLISHED", "FLAGGED", "APPROVED"));

            if (ratingFilter != null && ratingFilter >= 1 && ratingFilter <= 5) {
                predicates.add(cb.equal(root.get("rating"), ratingFilter));
            }

            if (Boolean.TRUE.equals(verifiedOnly)) {
                predicates.add(cb.isTrue(root.get("verifiedBooking")));
            }

            if (Boolean.TRUE.equals(hasPhotos)) {
                predicates.add(cb.isNotEmpty(root.get("photos")));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Sort sort = switch (sortBy != null ? sortBy.toUpperCase() : "NEWEST") {
            case "RATING", "HIGHEST_RATED" -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("helpfulCount"), Sort.Order.desc("createdAt"));
            case "HELPFUL", "MOST_HELPFUL" -> Sort.by(Sort.Order.desc("helpfulCount"), Sort.Order.desc("createdAt"));
            default -> Sort.by(Sort.Order.desc("createdAt"));
        };

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Review> reviewPage = reviewRepo.findAll(spec, pageable);

        List<ReviewResponseDto> dtos = reviewPage.getContent().stream()
                .map(r -> mapToResponseDto(r, currentUserId))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, reviewPage.getTotalElements());
    }

    public ReviewResponseDto mapToResponseDto(Review review, Long currentUserId) {
        if (review == null) return null;
        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setId(review.getId());
        if (review.getUser() != null) {
            dto.setUserId(review.getUser().getId());
            dto.setUserName(review.getUser().getName());
        }
        if (review.getFlight() != null) {
            dto.setTargetType("FLIGHT");
            dto.setTargetId(review.getFlight().getId());
            dto.setTargetName(review.getFlight().getFlightNumber());
        } else if (review.getHotel() != null) {
            dto.setTargetType("HOTEL");
            dto.setTargetId(review.getHotel().getId());
            dto.setTargetName(review.getHotel().getName());
        }
        if (review.getBooking() != null) {
            dto.setBookingId(review.getBooking().getId());
        }
        dto.setRating(review.getRating());
        dto.setTitle(review.getTitle());
        dto.setText(review.getText());
        dto.setHelpfulCount(review.getHelpfulCount());
        dto.setNotHelpfulCount(review.getNotHelpfulCount());
        dto.setStatus(review.getStatus());
        dto.setVerifiedBooking(review.isVerifiedBooking());
        dto.setReportCount(review.getReportCount());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());

        // Photos
        if (review.getPhotos() != null) {
            List<ReviewPhotoDto> photoDtos = review.getPhotos().stream()
                    .map(p -> new ReviewPhotoDto(p.getId(), p.getPhotoUrl(), p.getCaption(), p.getCreatedAt()))
                    .collect(Collectors.toList());
            dto.setPhotos(photoDtos);
        }

        // Replies (active only)
        if (review.getReplies() != null) {
            List<ReviewReplyDto> replyDtos = review.getReplies().stream()
                    .filter(rep -> rep.getStatus() == null || "ACTIVE".equalsIgnoreCase(rep.getStatus()))
                    .map(rep -> new ReviewReplyDto(
                            rep.getId(),
                            review.getId(),
                            rep.getUser() != null ? rep.getUser().getId() : null,
                            rep.getUser() != null ? rep.getUser().getName() : null,
                            rep.getUser() != null && rep.getUser().getRole() != null ? rep.getUser().getRole().name() : "USER",
                            rep.getText(),
                            rep.getCreatedAt(),
                            rep.getUpdatedAt()
                    ))
                    .collect(Collectors.toList());
            dto.setReplies(replyDtos);
        }

        // User vote
        if (currentUserId != null) {
            voteRepo.findByReviewIdAndUserId(review.getId(), currentUserId).ifPresent(v -> {
                dto.setUserVotedHelpful("HELPFUL".equalsIgnoreCase(v.getVoteType()));
            });
        }

        return dto;
    }

    // ─── Rating Summary ───────────────────────────────────────

    @Transactional(readOnly = true)
    public RatingSummaryDto getFlightRatingSummary(Long flightId) {
        Double avg = reviewRepo.getAverageRatingByFlightId(flightId);
        long total = reviewRepo.getCountByFlightId(flightId);
        double roundedAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;

        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0L);

        List<Object[]> rows = reviewRepo.getRatingDistributionByFlightId(flightId);
        if (rows != null) {
            for (Object[] row : rows) {
                if (row.length >= 2 && row[0] instanceof Number && row[1] instanceof Number) {
                    distribution.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
                }
            }
        }

        return new RatingSummaryDto(roundedAvg, total, distribution);
    }

    @Transactional(readOnly = true)
    public RatingSummaryDto getHotelRatingSummary(Long hotelId) {
        Double avg = reviewRepo.getAverageRatingByHotelId(hotelId);
        long total = reviewRepo.getCountByHotelId(hotelId);
        double roundedAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;

        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0L);

        List<Object[]> rows = reviewRepo.getRatingDistributionByHotelId(hotelId);
        if (rows != null) {
            for (Object[] row : rows) {
                if (row.length >= 2 && row[0] instanceof Number && row[1] instanceof Number) {
                    distribution.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
                }
            }
        }

        return new RatingSummaryDto(roundedAvg, total, distribution);
    }

    // ─── Replies ──────────────────────────────────────────────

    @Transactional
    public ReviewReply addReply(Long reviewId, Long userId, String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new BadRequestException("Reply text cannot be empty");
        }
        if (text.length() > 1000) {
            throw new BadRequestException("Reply text cannot exceed 1000 characters");
        }

        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        ReviewReply reply = new ReviewReply();
        reply.setReview(review);
        reply.setUser(user);
        reply.setText(sanitize(text));
        reply.setStatus("ACTIVE");

        ReviewReply saved = replyRepo.save(reply);

        // Notify review author if someone else replied
        if (notificationService != null && review.getUser() != null && !review.getUser().getId().equals(userId)) {
            try {
                notificationService.createNotification(
                        review.getUser(),
                        "REVIEW_REPLY",
                        "New Reply on Your Review",
                        user.getName() + " replied to your review.",
                        "REVIEW",
                        review.getId(),
                        "REV-" + review.getId(),
                        "IN_APP",
                        "reply:" + saved.getId()
                );
            } catch (Exception e) {
                logger.warn("Could not dispatch review reply notification: {}", e.getMessage());
            }
        }

        return saved;
    }

    @Transactional
    public ReviewReply updateReply(Long replyId, Long userId, String text) {
        ReviewReply reply = replyRepo.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewReply", "id", replyId));

        if (!reply.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only edit your own replies");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new BadRequestException("Reply text cannot be empty");
        }
        reply.setText(sanitize(text));
        reply.setUpdatedAt(LocalDateTime.now());
        return replyRepo.save(reply);
    }

    @Transactional
    public void deleteReply(Long replyId, Long userId, boolean isAdmin) {
        ReviewReply reply = replyRepo.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewReply", "id", replyId));

        if (!isAdmin && !reply.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own replies");
        }
        reply.setStatus("DELETED");
        replyRepo.save(reply);
    }

    // ─── Photos ───────────────────────────────────────────────

    @Transactional
    public List<ReviewPhoto> addPhotos(Long reviewId, Long userId, List<MultipartFile> files) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only add photos to your own reviews");
        }

        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        long existingCount = photoRepo.countByReviewId(reviewId);
        if (existingCount + files.size() > 5) {
            throw new BadRequestException("Maximum 5 photos allowed per review");
        }

        List<ReviewPhoto> savedPhotos = new ArrayList<>();
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String contentType = file.getContentType();
                if (contentType == null || !(contentType.equalsIgnoreCase("image/jpeg")
                        || contentType.equalsIgnoreCase("image/png")
                        || contentType.equalsIgnoreCase("image/webp"))) {
                    throw new BadRequestException("Only JPEG, PNG, and WebP images are allowed");
                }

                if (file.getSize() > 5 * 1024 * 1024) {
                    throw new BadRequestException("File size must not exceed 5MB");
                }

                // Magic bytes validation via ImageIO
                try (InputStream is = file.getInputStream()) {
                    BufferedImage image = ImageIO.read(is);
                    if (image == null) {
                        throw new BadRequestException("Uploaded file is not a valid image");
                    }
                }

                // Generate safe random UUID filename (no user path traversal)
                String extension = switch (contentType.toLowerCase()) {
                    case "image/png" -> ".png";
                    case "image/webp" -> ".webp";
                    default -> ".jpg";
                };
                String safeFilename = UUID.randomUUID().toString() + extension;
                Path filePath = Paths.get(UPLOAD_DIR, safeFilename);
                Files.copy(file.getInputStream(), filePath);

                ReviewPhoto photo = new ReviewPhoto();
                photo.setReview(review);
                photo.setPhotoUrl("/uploads/review-photos/" + safeFilename);
                photo.setCaption("");
                photo.setFileSize(file.getSize());
                photo.setContentType(contentType);

                savedPhotos.add(photoRepo.save(photo));
            }
        } catch (IOException e) {
            logger.error("Failed to store review photos", e);
            throw new RuntimeException("Failed to store photos", e);
        }

        return savedPhotos;
    }

    @Transactional
    public void deletePhoto(Long photoId, Long userId, boolean isAdmin) {
        ReviewPhoto photo = photoRepo.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewPhoto", "id", photoId));

        if (!isAdmin && (photo.getReview() == null || !photo.getReview().getUser().getId().equals(userId))) {
            throw new BadRequestException("You can only delete your own review photos");
        }

        // Attempt physical file removal
        if (photo.getPhotoUrl() != null && photo.getPhotoUrl().startsWith("/uploads/review-photos/")) {
            String filename = photo.getPhotoUrl().replace("/uploads/review-photos/", "");
            try {
                Path filePath = Paths.get(UPLOAD_DIR, filename);
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                logger.warn("Could not delete physical photo file: {}", e.getMessage());
            }
        }

        photoRepo.delete(photo);
    }

    // ─── Helpful Voting ───────────────────────────────────────

    @Transactional
    public void voteReview(Long reviewId, Long userId, String voteType) {
        if (!"HELPFUL".equalsIgnoreCase(voteType) && !"NOT_HELPFUL".equalsIgnoreCase(voteType)) {
            throw new BadRequestException("Vote type must be HELPFUL or NOT_HELPFUL");
        }
        String normalizedType = voteType.toUpperCase();

        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        ReviewVote existingVote = voteRepo.findByReviewIdAndUserId(reviewId, userId).orElse(null);

        if (existingVote != null) {
            if (existingVote.getVoteType().equalsIgnoreCase(normalizedType)) {
                // Toggle off — remove the vote
                voteRepo.delete(existingVote);
                if ("HELPFUL".equals(normalizedType)) {
                    review.setHelpfulCount(Math.max(0, review.getHelpfulCount() - 1));
                } else {
                    review.setNotHelpfulCount(Math.max(0, review.getNotHelpfulCount() - 1));
                }
            } else {
                // Switch vote
                if ("HELPFUL".equalsIgnoreCase(existingVote.getVoteType())) {
                    review.setHelpfulCount(Math.max(0, review.getHelpfulCount() - 1));
                    review.setNotHelpfulCount(review.getNotHelpfulCount() + 1);
                } else {
                    review.setNotHelpfulCount(Math.max(0, review.getNotHelpfulCount() - 1));
                    review.setHelpfulCount(review.getHelpfulCount() + 1);
                }
                existingVote.setVoteType(normalizedType);
                voteRepo.save(existingVote);
            }
        } else {
            // New vote
            ReviewVote vote = new ReviewVote(review, user, normalizedType);
            voteRepo.save(vote);
            if ("HELPFUL".equals(normalizedType)) {
                review.setHelpfulCount(review.getHelpfulCount() + 1);
            } else {
                review.setNotHelpfulCount(review.getNotHelpfulCount() + 1);
            }
        }

        reviewRepo.save(review);
    }

    // ─── Reporting / Flagging ─────────────────────────────────

    @Transactional
    public void flagReview(Long reviewId, Long userId, String reason) {
        ReviewReportRequestDto dto = new ReviewReportRequestDto();
        dto.setReason(reason != null ? reason : "OTHER");
        dto.setDescription("Flagged by user");
        reportReview(reviewId, userId, dto);
    }

    @Transactional
    public ReviewReport reportReview(Long reviewId, Long userId, ReviewReportRequestDto requestDto) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        User reporter = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (reportRepo.existsByReporterUserIdAndReviewId(userId, reviewId)) {
            throw new BadRequestException("You have already reported this review");
        }

        String reason = requestDto.getReason() != null ? requestDto.getReason().toUpperCase() : "OTHER";
        // Validate predefined reasons
        Set<String> validReasons = Set.of(
                "SPAM", "OFFENSIVE", "HARASSMENT", "FAKE_REVIEW",
                "IRRELEVANT", "INAPPROPRIATE_CONTENT", "PERSONAL_INFORMATION", "OTHER"
        );
        if (!validReasons.contains(reason)) {
            reason = "OTHER";
        }

        ReviewReport report = new ReviewReport();
        report.setReview(review);
        report.setReporterUser(reporter);
        report.setReason(reason);
        report.setDescription(sanitize(requestDto.getDescription()));
        report.setStatus("PENDING");
        report = reportRepo.save(report);

        // Update review status and counter
        review.setStatus(Review.STATUS_PENDING_MODERATION);
        review.setFlagReason(reason);
        review.setReportCount(review.getReportCount() + 1);
        reviewRepo.save(review);

        logger.info("Review {} reported by user {} for reason: {}", reviewId, userId, reason);
        return report;
    }

    // ─── Moderation ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ModerationReviewDto> getModerationQueue() {
        List<Review> reviews = reviewRepo.findModerationQueue();
        return reviews.stream().map(r -> {
            ModerationReviewDto dto = new ModerationReviewDto();
            dto.setId(r.getId());
            if (r.getUser() != null) {
                dto.setUserId(r.getUser().getId());
                dto.setUserName(r.getUser().getName());
                dto.setUserEmail(r.getUser().getEmail());
            }
            if (r.getFlight() != null) {
                dto.setTargetType("FLIGHT");
                dto.setTargetId(r.getFlight().getId());
                dto.setTargetName(r.getFlight().getFlightNumber());
            } else if (r.getHotel() != null) {
                dto.setTargetType("HOTEL");
                dto.setTargetId(r.getHotel().getId());
                dto.setTargetName(r.getHotel().getName());
            }
            dto.setRating(r.getRating());
            dto.setTitle(r.getTitle());
            dto.setText(r.getText());
            dto.setStatus(r.getStatus());
            dto.setFlagReason(r.getFlagReason());
            dto.setReportCount(r.getReportCount());
            dto.setCreatedAt(r.getCreatedAt());

            // Add report details
            List<ReviewReport> reports = reportRepo.findByReviewIdOrderByCreatedAtDesc(r.getId());
            List<ReviewReportDto> reportDtos = reports.stream()
                    .map(rep -> new ReviewReportDto(
                            rep.getId(),
                            rep.getReporterUser() != null ? rep.getReporterUser().getName() : "Anonymous",
                            rep.getReason(),
                            rep.getDescription(),
                            rep.getStatus(),
                            rep.getCreatedAt()
                    ))
                    .collect(Collectors.toList());
            dto.setReports(reportDtos);

            // Add photo details
            if (r.getPhotos() != null) {
                List<ReviewPhotoDto> photoDtos = r.getPhotos().stream()
                        .map(p -> new ReviewPhotoDto(p.getId(), p.getPhotoUrl(), p.getCaption(), p.getCreatedAt()))
                        .collect(Collectors.toList());
                dto.setPhotos(photoDtos);
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void moderateReview(Long reviewId, String action, Long moderatorId) {
        moderateReview(reviewId, action, null, moderatorId);
    }

    @Transactional
    public void moderateReview(Long reviewId, String action, String reason, Long moderatorId) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        User moderator = userRepo.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", moderatorId));

        if (action == null) {
            throw new BadRequestException("Moderation action is required");
        }

        String normAction = action.toUpperCase();
        switch (normAction) {
            case "APPROVE" -> {
                review.setStatus(Review.STATUS_PUBLISHED);
                resolveReportsForReview(reviewId, "APPROVED");
            }
            case "DISMISS_REPORTS", "DISMISS" -> {
                review.setStatus(Review.STATUS_PUBLISHED);
                resolveReportsForReview(reviewId, "DISMISSED");
            }
            case "HIDE" -> {
                review.setStatus(Review.STATUS_HIDDEN);
                resolveReportsForReview(reviewId, "HIDDEN");
            }
            case "REMOVE" -> {
                review.setStatus(Review.STATUS_REMOVED);
                resolveReportsForReview(reviewId, "ACTION_TAKEN");
            }
            case "RESTORE" -> {
                review.setStatus(Review.STATUS_PUBLISHED);
            }
            default -> throw new BadRequestException("Invalid action: " + action);
        }

        reviewRepo.save(review);

        // Record Audit Action
        ReviewModerationAction audit = new ReviewModerationAction();
        audit.setReview(review);
        audit.setModerator(moderator);
        audit.setAction(normAction);
        audit.setReason(reason != null ? reason : "Moderated by " + moderator.getName());
        actionRepo.save(audit);

        logger.info("Review {} {} by moderator {} ({})", reviewId, normAction, moderatorId, reason);
    }

    private void resolveReportsForReview(Long reviewId, String resolutionStatus) {
        List<ReviewReport> reports = reportRepo.findByReviewIdOrderByCreatedAtDesc(reviewId);
        LocalDateTime now = LocalDateTime.now();
        for (ReviewReport rep : reports) {
            if ("PENDING".equals(rep.getStatus())) {
                rep.setStatus(resolutionStatus);
                rep.setResolvedAt(now);
                reportRepo.save(rep);
            }
        }
    }

    // ─── Eligibility ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> checkReviewEligibility(Long userId, Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            return Map.of("eligible", false, "reason", "NOT_YOUR_BOOKING");
        }

        if (!"COMPLETED".equals(booking.getStatus())) {
            return Map.of("eligible", false, "reason", "BOOKING_NOT_COMPLETED",
                    "status", booking.getStatus());
        }

        boolean hasExistingReview = false;
        Long targetId = null;
        String targetType = null;

        if ("FLIGHT".equalsIgnoreCase(booking.getBookingType()) && booking.getFlight() != null) {
            targetId = booking.getFlight().getId();
            targetType = "FLIGHT";
            hasExistingReview = reviewRepo.existsByUserIdAndFlightIdAndStatus(userId, targetId, "PUBLISHED")
                    || reviewRepo.existsByUserIdAndFlightIdAndStatus(userId, targetId, "PENDING_MODERATION")
                    || reviewRepo.existsByUserIdAndBookingIdAndStatusNot(userId, bookingId, "REMOVED");
        } else if ("HOTEL".equalsIgnoreCase(booking.getBookingType()) && booking.getHotel() != null) {
            targetId = booking.getHotel().getId();
            targetType = "HOTEL";
            hasExistingReview = reviewRepo.existsByUserIdAndHotelIdAndStatus(userId, targetId, "PUBLISHED")
                    || reviewRepo.existsByUserIdAndHotelIdAndStatus(userId, targetId, "PENDING_MODERATION")
                    || reviewRepo.existsByUserIdAndBookingIdAndStatusNot(userId, bookingId, "REMOVED");
        } else {
            return Map.of("eligible", false, "reason", "UNSUPPORTED_BOOKING_TYPE");
        }

        if (hasExistingReview) {
            return Map.of("eligible", false, "reason", "ALREADY_REVIEWED",
                    "targetType", targetType, "targetId", targetId);
        }

        return Map.of("eligible", true, "targetType", targetType, "targetId", targetId,
                "bookingType", booking.getBookingType());
    }
}
