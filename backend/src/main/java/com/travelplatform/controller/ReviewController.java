package com.travelplatform.controller;

import com.travelplatform.dto.review.*;
import com.travelplatform.entity.Review;
import com.travelplatform.entity.ReviewPhoto;
import com.travelplatform.entity.ReviewReply;
import com.travelplatform.entity.ReviewReport;
import com.travelplatform.entity.User;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public ReviewController(ReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BadRequestException("Authentication required");
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }

    private Long getUserIdSafe(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElse(null);
    }

    /** Create a review */
    @PostMapping
    public ResponseEntity<?> createReview(
            @Valid @RequestBody ReviewCreateRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        Review review = reviewService.createReview(userId, request);
        ReviewResponseDto dto = reviewService.mapToResponseDto(review, userId);
        return ResponseEntity.ok(dto);
    }

    /** Get reviews for a flight with sorting & filtering */
    @GetMapping("/flight/{flightId}")
    public ResponseEntity<Page<ReviewResponseDto>> getFlightReviews(
            @PathVariable Long flightId,
            @RequestParam(defaultValue = "NEWEST") String sortBy,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean verifiedOnly,
            @RequestParam(required = false) Boolean hasPhotos,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = getUserIdSafe(userDetails);
        return ResponseEntity.ok(reviewService.getFlightReviews(flightId, sortBy, rating, verifiedOnly, hasPhotos, currentUserId, page, size));
    }

    /** Get reviews for a hotel with sorting & filtering */
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<Page<ReviewResponseDto>> getHotelReviews(
            @PathVariable Long hotelId,
            @RequestParam(defaultValue = "NEWEST") String sortBy,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean verifiedOnly,
            @RequestParam(required = false) Boolean hasPhotos,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = getUserIdSafe(userDetails);
        return ResponseEntity.ok(reviewService.getHotelReviews(hotelId, sortBy, rating, verifiedOnly, hasPhotos, currentUserId, page, size));
    }

    /** Get rating summary for a flight */
    @GetMapping("/flight/{flightId}/summary")
    public ResponseEntity<RatingSummaryDto> getFlightSummary(@PathVariable Long flightId) {
        return ResponseEntity.ok(reviewService.getFlightRatingSummary(flightId));
    }

    /** Get rating summary for a hotel */
    @GetMapping("/hotel/{hotelId}/summary")
    public ResponseEntity<RatingSummaryDto> getHotelSummary(@PathVariable Long hotelId) {
        return ResponseEntity.ok(reviewService.getHotelRatingSummary(hotelId));
    }

    /** Add a reply to a review */
    @PostMapping("/{reviewId}/replies")
    public ResponseEntity<?> addReply(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        ReviewReply reply = reviewService.addReply(reviewId, userId, body.get("text"));
        ReviewReplyDto dto = new ReviewReplyDto(
                reply.getId(),
                reviewId,
                userId,
                reply.getUser().getName(),
                reply.getUser().getRole() != null ? reply.getUser().getRole().name() : "USER",
                reply.getText(),
                reply.getCreatedAt(),
                reply.getUpdatedAt()
        );
        return ResponseEntity.ok(dto);
    }

    /** Update a reply */
    @PutMapping("/replies/{replyId}")
    public ResponseEntity<?> updateReply(
            @PathVariable Long replyId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        ReviewReply reply = reviewService.updateReply(replyId, userId, body.get("text"));
        ReviewReplyDto dto = new ReviewReplyDto(
                reply.getId(),
                reply.getReview() != null ? reply.getReview().getId() : null,
                userId,
                reply.getUser().getName(),
                reply.getUser().getRole() != null ? reply.getUser().getRole().name() : "USER",
                reply.getText(),
                reply.getCreatedAt(),
                reply.getUpdatedAt()
        );
        return ResponseEntity.ok(dto);
    }

    /** Delete a reply */
    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<?> deleteReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        reviewService.deleteReply(replyId, userId, isAdmin);
        return ResponseEntity.ok(Map.of("message", "Reply deleted successfully"));
    }

    /** Vote helpful/not-helpful on a review */
    @PostMapping("/{reviewId}/vote")
    public ResponseEntity<?> voteReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        reviewService.voteReview(reviewId, userId, body.get("voteType"));
        return ResponseEntity.ok(Map.of("message", "Vote recorded"));
    }

    /** Flag a review for moderation */
    @PostMapping("/{reviewId}/flag")
    public ResponseEntity<?> flagReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        reviewService.flagReview(reviewId, userId, body.get("reason"));
        return ResponseEntity.ok(Map.of("message", "Review flagged for moderation"));
    }

    /** Report a review with predefined reason and optional description */
    @PostMapping("/{reviewId}/report")
    public ResponseEntity<?> reportReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewReportRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        ReviewReport report = reviewService.reportReview(reviewId, userId, requestDto);
        return ResponseEntity.ok(Map.of(
                "message", "Review reported successfully",
                "reportId", report.getId(),
                "status", report.getStatus()
        ));
    }

    /** Admin: get moderation queue */
    @GetMapping("/moderation/queue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getModerationQueue(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reviewService.getModerationQueue());
    }

    /** Admin: moderate a review (approve, remove, dismiss, restore) */
    @PostMapping("/{reviewId}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> moderateReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        reviewService.moderateReview(reviewId, body.get("action"), body.get("reason"), userId);
        return ResponseEntity.ok(Map.of("message", "Review moderated"));
    }

    /** Check if user is eligible to review a booking */
    @GetMapping("/eligibility")
    public ResponseEntity<?> checkEligibility(
            @RequestParam Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(reviewService.checkReviewEligibility(userId, bookingId));
    }

    /** Upload photos for a review */
    @PostMapping("/{reviewId}/photos")
    public ResponseEntity<?> uploadPhotos(
            @PathVariable Long reviewId,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        List<ReviewPhoto> saved = reviewService.addPhotos(reviewId, userId, files);
        List<ReviewPhotoDto> dtos = saved.stream()
                .map(p -> new ReviewPhotoDto(p.getId(), p.getPhotoUrl(), p.getCaption(), p.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /** Delete a photo from a review */
    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<?> deletePhoto(
            @PathVariable Long photoId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        reviewService.deletePhoto(photoId, userId, isAdmin);
        return ResponseEntity.ok(Map.of("message", "Photo deleted successfully"));
    }
}
