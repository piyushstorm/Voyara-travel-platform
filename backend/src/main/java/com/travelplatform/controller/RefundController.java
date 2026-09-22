package com.travelplatform.controller;

import com.travelplatform.dto.refund.RefundPreviewResponse;
import com.travelplatform.entity.Booking;
import com.travelplatform.entity.CancellationReason;
import com.travelplatform.entity.Refund;
import com.travelplatform.entity.User;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.repository.BookingRepository;
import com.travelplatform.repository.RefundRepository;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.service.CancellationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final CancellationService cancellationService;
    private final BookingRepository bookingRepo;
    private final RefundRepository refundRepo;
    private final UserRepository userRepository;

    public RefundController(CancellationService cancellationService,
                            BookingRepository bookingRepo,
                            RefundRepository refundRepo,
                            UserRepository userRepository) {
        this.cancellationService = cancellationService;
        this.bookingRepo = bookingRepo;
        this.refundRepo = refundRepo;
        this.userRepository = userRepository;
    }

    private User getUserOrThrow(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Get predefined cancellation reasons for the UI dropdown
     */
    @GetMapping("/reasons")
    public ResponseEntity<List<Map<String, String>>> getCancellationReasons() {
        List<Map<String, String>> reasons = Arrays.stream(CancellationReason.values())
                .map(r -> Map.of(
                    "code", r.getCode(),
                    "label", r.getLabel(),
                    "description", r.getDescription()
                ))
                .toList();
        return ResponseEntity.ok(reasons);
    }

    /** Preview authoritative refund amount before cancelling */
    @GetMapping("/preview/{bookingId}")
    public ResponseEntity<Map<String, Object>> previewRefund(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Booking not found");
        }

        RefundPreviewResponse preview = cancellationService.previewRefund(booking);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookingId", bookingId);
        response.put("bookingReference", booking.getBookingReference());
        response.put("bookingType", preview.getBookingType());
        response.put("totalAmount", preview.getOriginalAmount());
        response.put("originalAmount", preview.getOriginalAmount());
        response.put("refundAmount", preview.getRefundAmount());
        response.put("cancellationFee", preview.getCancellationFee());
        response.put("nonRefundableAmount", preview.getNonRefundableAmount());
        response.put("refundPercentage", preview.getEligibleRefundPercentage());
        response.put("eligibleRefundPercentage", preview.getEligibleRefundPercentage());
        response.put("policyName", preview.getPolicyApplied());
        response.put("policyApplied", preview.getPolicyApplied());
        response.put("policyExplanation", preview.getPolicyExplanation());
        response.put("expectedTimeline", preview.getExpectedTimeline());
        response.put("calculatedAt", preview.getCalculatedAt());

        return ResponseEntity.ok(response);
    }

    /** Get all refunds for current user */
    @GetMapping
    public ResponseEntity<List<Refund>> getMyRefunds(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        return ResponseEntity.ok(cancellationService.getUserRefunds(userId));
    }

    /** Get refund status by refund ID */
    @GetMapping("/{refundId}")
    public ResponseEntity<?> getRefundStatus(
            @PathVariable String refundId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        return refundRepo.findByRefundId(refundId)
                .filter(r -> r.getBooking().getUser().getId().equals(userId))
                .map(r -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("refundId", r.getRefundId());
                    data.put("status", r.getStatus());
                    data.put("refundAmount", r.getRefundAmount());
                    data.put("originalAmount", r.getOriginalAmount());
                    data.put("refundPercentage", r.getRefundPercentage());
                    data.put("currency", r.getCurrency() != null ? r.getCurrency() : "INR");
                    data.put("cancellationReason", r.getCancellationReason() != null ? r.getCancellationReason() : "");
                    data.put("cancellationComment", r.getCancellationComment());
                    data.put("cancellationPolicy", r.getCancellationPolicy());
                    data.put("failureReason", r.getFailureReason());
                    data.put("expectedCompletionAt", r.getExpectedCompletionAt() != null ? r.getExpectedCompletionAt().toString() : null);
                    data.put("expectedTimeline", "Expected within 5–7 business days");
                    data.put("processedAt", r.getProcessedAt() != null ? r.getProcessedAt().toString() : null);
                    data.put("completedAt", r.getCompletedAt() != null ? r.getCompletedAt().toString() : null);
                    data.put("externalRefundId", r.getExternalRefundId() != null ? r.getExternalRefundId() : null);
                    data.put("razorpayRefundId", r.getRazorpayRefundId() != null ? r.getRazorpayRefundId() : null);
                    return ResponseEntity.ok(data);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Refund", "id", refundId));
    }
}
